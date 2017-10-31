/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.channels

import kotlinx.coroutines.experimental.selects.ALREADY_SELECTED
import kotlinx.coroutines.experimental.selects.SelectInstance
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Broadcast channel with array buffer of a fixed [capacity].
 * Sender suspends only when buffer is full due to one of the receives being slow to consume and
 * receiver suspends only when buffer is empty.
 *
 * Note, that elements that are sent to the broadcast channel while there are no [openSubscription] subscribers are immediately
 * lost.
 *
 * This channel is created by `BroadcastChannel(capacity)` factory function invocation.
 *
 * This implementation uses lock to protect the buffer, which is held only during very short buffer-update operations.
 * The lock at each subscription is also used to manage concurrent attempts to receive from the same subscriber.
 * The lists of suspended senders or receivers are lock-free.
 */
class ArrayBroadcastChannel<E>(
    /**
     * Buffer capacity.
     */
    val capacity: Int
) : AbstractSendChannel<E>(), BroadcastChannel<E> {
    init {
        require(capacity >= 1) { "ArrayBroadcastChannel capacity must be at least 1, but $capacity was specified" }
    }

    private val bufferLock = ReentrantLock()
    private val buffer: Array<Any?> = arrayOfNulls<Any?>(capacity) // guarded by lock

    // head & tail are Long (64 bits) and we assume that they never wrap around
    // head, tail, and size are guarded by bufferLock
    @Volatile
    private var head: Long = 0 // do modulo on use of head
    @Volatile
    private var tail: Long = 0 // do modulo on use of tail
    @Volatile
    private var size: Int = 0

    private val subs = CopyOnWriteArrayList<Subscriber<E>>()

    override val isBufferAlwaysFull: Boolean get() = false
    override val isBufferFull: Boolean get() = size >= capacity

    override fun openSubscription(): SubscriptionReceiveChannel<E> {
        val sub = Subscriber(this, head)
        subs.add(sub)
        // between creating and adding of subscription into the list the buffer head could have been bumped past it,
        // so here we check if it did happen and update the head in subscription in this case
        // we did not leak newly created subscription yet, so its subHead cannot update
        val head = this.head // volatile read after sub was added to subs
        if (head != sub.subHead) {
            // needs update
            sub.subHead = head
            updateHead() // and also must recompute head of the buffer
        }
        return sub
    }

    override fun close(cause: Throwable?): Boolean {
        if (!super.close(cause)) return false
        checkSubOffers()
        return true
    }

    // result is `OFFER_SUCCESS | OFFER_FAILED | Closed`
    override fun offerInternal(element: E): Any {
        bufferLock.withLock {
            // check if closed for send (under lock, so size cannot change)
            closedForSend?.let { return it }
            val size = this.size
            if (size >= capacity) return OFFER_FAILED
            val tail = this.tail
            buffer[(tail % capacity).toInt()] = element
            this.size = size + 1
            this.tail = tail + 1
        }
        // if offered successfully, then check subs outside of lock
        checkSubOffers()
        return OFFER_SUCCESS
    }

    // result is `ALREADY_SELECTED | OFFER_SUCCESS | OFFER_FAILED | Closed`
    override fun offerSelectInternal(element: E, select: SelectInstance<*>): Any {
        bufferLock.withLock {
            // check if closed for send (under lock, so size cannot change)
            closedForSend?.let { return it }
            val size = this.size
            if (size >= capacity) return OFFER_FAILED
            // let's try to select sending this element to buffer
            if (!select.trySelect(null)) { // :todo: move trySelect completion outside of lock
                return ALREADY_SELECTED
            }
            val tail = this.tail
            buffer[(tail % capacity).toInt()] = element
            this.size = size + 1
            this.tail = tail + 1
        }
        // if offered successfully, then check subs outside of lock
        checkSubOffers()
        return OFFER_SUCCESS
    }

    private fun closeSubscriber(sub: Subscriber<E>) {
        subs.remove(sub)
        if (head == sub.subHead)
            updateHead()
    }

    private fun checkSubOffers() {
        var updated = false
        var hasSubs = false
        @Suppress("LoopToCallChain") // must invoke `checkOffer` on every sub
        for (sub in subs) {
            hasSubs = true
            if (sub.checkOffer()) updated = true
        }
        if (updated || !hasSubs)
            updateHead()
    }

    private fun updateHead() {
        // compute minHead w/o lock (it will be eventually consistent)
        val minHead = computeMinHead()
        // update head in a loop
        while (true) {
            var send: Send? = null
            var token: Any? = null
            bufferLock.withLock {
                val tail = this.tail
                var head = this.head
                val targetHead = minHead.coerceAtMost(tail)
                if (targetHead <= head) return // nothing to do -- head was already moved
                var size = this.size
                // clean up removed (on not need if we don't have any subscribers anymore)
                while (head < targetHead) {
                    buffer[(head % capacity).toInt()] = null
                    val wasFull = size >= capacity
                    // update the size before checking queue (no more senders can queue up)
                    this.head = ++head
                    this.size = --size
                    if (wasFull) {
                        while (true) {
                            send = takeFirstSendOrPeekClosed() ?: break // when when no sender
                            if (send is Closed<*>) break // break when closed for send
                            token = send!!.tryResumeSend(idempotent = null)
                            if (token != null) {
                                // put sent element to the buffer
                                buffer[(tail % capacity).toInt()] = (send as Send).pollResult
                                this.size = size + 1
                                this.tail = tail + 1
                                return@withLock // go out of lock to wakeup this sender
                            }
                        }
                    }
                }
                return // done updating here -> return
            }
            // we only get out of the lock normally when there is a sender to resume
            send!!.completeResumeSend(token!!)
            // since we've just sent an element, we might need to resume some receivers
            checkSubOffers()
        }
    }

    private fun computeMinHead(): Long {
        var minHead = Long.MAX_VALUE
        for (sub in subs)
            minHead = minHead.coerceAtMost(sub.subHead) // volatile (atomic) reads of subHead
        return minHead
    }

    @Suppress("UNCHECKED_CAST")
    private fun elementAt(index: Long): E = buffer[(index % capacity).toInt()] as E

    private class Subscriber<E>(
        private val broadcastChannel: ArrayBroadcastChannel<E>,
        @Volatile @JvmField var subHead: Long // guarded by lock
    ) : AbstractChannel<E>(), SubscriptionReceiveChannel<E> {
        private val lock = ReentrantLock()

        override val isBufferAlwaysEmpty: Boolean get() = false
        override val isBufferEmpty: Boolean get() = subHead >= broadcastChannel.tail
        override val isBufferAlwaysFull: Boolean get() = error("Should not be used")
        override val isBufferFull: Boolean get() = error("Should not be used")

        override fun close() {
            if (close(cause = null))
                broadcastChannel.closeSubscriber(this)
        }

        // returns true if subHead was updated and broadcast channel's head must be checked
        // this method is lock-free (it never waits on lock)
        @Suppress("UNCHECKED_CAST")
        fun checkOffer(): Boolean {
            var updated = false
            var closed: Closed<*>? = null
        loop@
            while (needsToCheckOfferWithoutLock()) {
                // just use `tryLock` here and break when some other thread is checking under lock
                // it means that `checkOffer` must be retried after every `unlock`
                if (!lock.tryLock()) break
                val receive: ReceiveOrClosed<E>?
                val token: Any?
                try {
                    val result = peekUnderLock()
                    when {
                        result === POLL_FAILED -> continue@loop // must retest `needsToCheckOfferWithoutLock` outside of the lock
                        result is Closed<*> -> {
                            closed = result
                            break@loop // was closed
                        }
                    }
                    // find a receiver for an element
                    receive = takeFirstReceiveOrPeekClosed() ?: break // break when no one's receiving
                    if (receive is Closed<*>) break // noting more to do if this sub already closed
                    token = receive.tryResumeReceive(result as E, idempotent = null)
                    if (token == null) continue // bail out here to next iteration (see for next receiver)
                    val subHead = this.subHead
                    this.subHead = subHead + 1 // retrieved element for this subscriber
                    updated = true
                } finally {
                    lock.unlock()
                }
                receive!!.completeResumeReceive(token!!)
            }
            // do close outside of lock if needed
            closed?.also { close(cause = it.closeCause) }
            return updated
        }

        // result is `E | POLL_FAILED | Closed`
        override fun pollInternal(): Any? {
            var updated = false
            val result: Any?
            lock.lock()
            try {
                result = peekUnderLock()
                when {
                    result is Closed<*> -> { /* just bail out of lock */ }
                    result === POLL_FAILED -> { /* just bail out of lock */ }
                    else -> {
                        // update subHead after retrieiving element from buffer
                        val subHead = this.subHead
                        this.subHead = subHead + 1
                        updated = true
                    }
                }
            } finally {
                lock.unlock()
            }
            // do close outside of lock
            (result as? Closed<*>)?.also { close(cause = it.closeCause) }
            // there could have been checkOffer attempt while we were holding lock
            // now outside the lock recheck if anything else to offer
            if (checkOffer())
                updated = true
            // and finally update broadcast's channel head if needed
            if (updated)
                broadcastChannel.updateHead()
            return result
        }

        // result is `ALREADY_SELECTED | E | POLL_FAILED | Closed`
        override fun pollSelectInternal(select: SelectInstance<*>): Any? {
            var updated = false
            var result: Any?
            lock.lock()
            try {
                result = peekUnderLock()
                when {
                    result is Closed<*> -> { /* just bail out of lock */ }
                    result === POLL_FAILED -> { /* just bail out of lock */ }
                    else -> {
                        // let's try to select receiving this element from buffer
                        if (!select.trySelect(null)) { // :todo: move trySelect completion outside of lock
                            result = ALREADY_SELECTED
                        } else {
                            // update subHead after retrieiving element from buffer
                            val subHead = this.subHead
                            this.subHead = subHead + 1
                            updated = true
                        }
                    }
                }
            } finally {
                lock.unlock()
            }
            // do close outside of lock
            (result as? Closed<*>)?.also { close(cause = it.closeCause) }
            // there could have been checkOffer attempt while we were holding lock
            // now outside the lock recheck if anything else to offer
            if (checkOffer())
                updated = true
            // and finally update broadcast's channel head if needed
            if (updated)
                broadcastChannel.updateHead()
            return result
        }

        // Must invoke this check this after lock, because offer's invocation of `checkOffer` might have failed
        // to `tryLock` just before the lock was about to unlocked, thus loosing notification to this
        // subscription about an element that was just offered
        private fun needsToCheckOfferWithoutLock(): Boolean {
            if (closedForReceive != null)
                return false // already closed -> nothing to do
            if (isBufferEmpty && broadcastChannel.closedForReceive == null)
                return false // no data for us && broadcast channel was not closed yet -> nothing to do
            return true // check otherwise
        }

        // guarded by lock, returns:
        //      E - the element from the buffer at subHead
        //      Closed<*> when closed;
        //      POLL_FAILED when there seems to be no data, but must retest `needsToCheckOfferWithoutLock` outside of lock
        private fun peekUnderLock(): Any? {
            val subHead = this.subHead // guarded read (can be non-volatile read)
            // note: from the broadcastChannel we must read closed token first, then read its tail
            // because it is Ok if tail moves in between the reads (we make decision based on tail first)
            val closed = broadcastChannel.closedForReceive // unguarded volatile read
            val tail = broadcastChannel.tail // unguarded volatile read
            if (subHead >= tail) {
                // no elements to poll from the queue -- check if closed
                return closed ?: POLL_FAILED // must retest `needsToCheckOfferWithoutLock` outside of the lock
            }
            return broadcastChannel.elementAt(subHead)
        }
    }
}