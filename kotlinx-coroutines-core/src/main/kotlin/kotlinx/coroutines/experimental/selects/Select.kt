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

package kotlinx.coroutines.experimental.selects

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.internal.*
import kotlinx.coroutines.experimental.intrinsics.startCoroutineUndispatched
import kotlinx.coroutines.experimental.sync.Mutex
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

/**
 * Scope for [select] invocation.
 */
public interface SelectBuilder<in R> {
    /**
     * Clause for [Job.join] suspending function that selects the given [block] when the job is complete.
     * This clause never fails, even if the job completes exceptionally.
     */
    public fun Job.onJoin(block: suspend () -> R)

    /**
     * Clause for [Deferred.await] suspending function that selects the given [block] with the deferred value is
     * resolved. The [select] invocation fails if the deferred value completes exceptionally (either fails or
     * it cancelled).
     */
    public fun <T> Deferred<T>.onAwait(block: suspend (T) -> R)

    /**
     * Clause for [SendChannel.send] suspending function that selects the given [block] when the [element] is sent to
     * the channel. The [select] invocation fails with [ClosedSendChannelException] if the channel
     * [isClosedForSend][SendChannel.isClosedForSend] _normally_ or with the original
     * [close][SendChannel.close] cause exception if the channel has _failed_.
     */
    public fun <E> SendChannel<E>.onSend(element: E, block: suspend () -> R)

    /**
     * Clause for [ReceiveChannel.receive] suspending function that selects the given [block] with the element that
     * is received from the channel. The [select] invocation fails with [ClosedReceiveChannelException] if the channel
     * [isClosedForReceive][ReceiveChannel.isClosedForReceive] _normally_ or with the original
     * [close][SendChannel.close] cause exception if the channel has _failed_.
     */
    public fun <E> ReceiveChannel<E>.onReceive(block: suspend (E) -> R)

    /**
     * Clause for [ReceiveChannel.receiveOrNull] suspending function that selects the given [block] with the element that
     * is received from the channel or selects the given [block] with `null` if if the channel
     * [isClosedForReceive][ReceiveChannel.isClosedForReceive] _normally_. The [select] invocation fails with
     * the original [close][SendChannel.close] cause exception if the channel has _failed_.
     */
    public fun <E> ReceiveChannel<E>.onReceiveOrNull(block: suspend (E?) -> R)

    /**
     * Clause for [Mutex.lock] suspending function that selects the given [block] when the mutex is locked.
     *
     * @param owner Optional owner token for debugging. When `owner` is specified (non-null value) and this mutex
     *        is already locked with the same token (same identity), this clause throws [IllegalStateException].
     */
    public fun Mutex.onLock(owner: Any? = null, block: suspend () -> R)

    /**
     * Clause that selects the given [block] after a specified timeout passes.
     *
     * @param time timeout time
     * @param unit timeout unit (milliseconds by default)
     */
    public fun onTimeout(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, block: suspend () -> R)
}

/**
 * Internal representation of select instance. This instance is called _selected_ when
 * the clause to execute is already picked.
 *
 * @suppress **This is unstable API and it is subject to change.**
 */
public interface SelectInstance<in R> {
    /**
     * Returns `true` when this [select] statement had already picked a clause to execute.
     */
    public val isSelected: Boolean

    /**
     * Tries to select this instance.
     */
    public fun trySelect(idempotent: Any?): Boolean

    /**
     * Performs action atomically with [trySelect].
     */
    public fun performAtomicTrySelect(desc: AtomicDesc): Any?

    /**
     * Performs action atomically when [isSelected] is `false`.
     */
    public fun performAtomicIfNotSelected(desc: AtomicDesc): Any?

    /**
     * Returns completion continuation of this select instance.
     * This select instance must be _selected_ first.
     * All resumption through this instance happen _directly_ (as if `mode` is [MODE_DIRECT]).
     */
    public val completion: Continuation<R>

    /**
     * Resumes this instance with [MODE_CANCELLABLE].
     */
    public fun resumeSelectCancellableWithException(exception: Throwable)

    public fun disposeOnSelect(handle: DisposableHandle)
}

/**
 * Waits for the result of multiple suspending functions simultaneously, which are specified using _clauses_
 * in the [builder] scope of this select invocation. The caller is suspended until one of the clauses
 * is either _selected_ or _fails_.
 *
 * At most one clause is *atomically* selected and its block is executed. The result of the selected clause
 * becomes the result of the select. If any clause _fails_, then the select invocation produces the
 * corresponding exception. No clause is selected in this case.
 *
 * This select function is _biased_ to the first clause. When multiple clauses can be selected at the same time,
 * the first one of them gets priority. Use [selectUnbiased] for an unbiased (randomized) selection among
 * the clauses.

 * There is no `default` clause for select expression. Instead, each selectable suspending function has the
 * corresponding non-suspending version that can be used with a regular `when` expression to select one
 * of the alternatives or to perform default (`else`) action if none of them can be immediately selected.
 *
 * | **Receiver**     | **Suspending function**                       | **Select clause**                                | **Non-suspending version**
 * | ---------------- | --------------------------------------------- | ------------------------------------------------ | --------------------------
 * | [Job]            | [join][Job.join]                              | [onJoin][SelectBuilder.onJoin]                   | [isCompleted][Job.isCompleted]
 * | [Deferred]       | [await][Deferred.await]                       | [onAwait][SelectBuilder.onAwait]                 | [isCompleted][Job.isCompleted]
 * | [SendChannel]    | [send][SendChannel.send]                      | [onSend][SelectBuilder.onSend]                   | [offer][SendChannel.offer]
 * | [ReceiveChannel] | [receive][ReceiveChannel.receive]             | [onReceive][SelectBuilder.onReceive]             | [poll][ReceiveChannel.poll]
 * | [ReceiveChannel] | [receiveOrNull][ReceiveChannel.receiveOrNull] | [onReceiveOrNull][SelectBuilder.onReceiveOrNull] | [poll][ReceiveChannel.poll]
 * | [Mutex]          | [lock][Mutex.lock]                            | [onLock][SelectBuilder.onLock]                   | [tryLock][Mutex.tryLock]
 * | none             | [delay]                                       | [onTimeout][SelectBuilder.onTimeout]             | none
 *
 * This suspending function is cancellable. If the [Job] of the current coroutine is cancelled or completed while this
 * function is suspended, this function immediately resumes with [CancellationException].
 *
 * Atomicity of cancellation depends on the clause: [onSend][SelectBuilder.onSend], [onReceive][SelectBuilder.onReceive],
 * [onReceiveOrNull][SelectBuilder.onReceiveOrNull], and [onLock][SelectBuilder.onLock] clauses are
 * *atomically cancellable*. When select throws [CancellationException] it means that those clauses had not performed
 * their respective operations.
 * As a side-effect of atomic cancellation, a thread-bound coroutine (to some UI thread, for example) may
 * continue to execute even after it was cancelled from the same thread in the case when this select operation
 * was already resumed on atomically cancellable clause and the continuation was posted for execution to the thread's queue.
 *
 * Note, that this function does not check for cancellation when it is not suspended.
 * Use [yield] or [CoroutineScope.isActive] to periodically check for cancellation in tight loops if needed.
 */
public inline suspend fun <R> select(crossinline builder: SelectBuilder<R>.() -> Unit): R =
    suspendCoroutineOrReturn { cont ->
        val scope = SelectBuilderImpl(cont)
        try {
            builder(scope)
        } catch (e: Throwable) {
            scope.handleBuilderException(e)
        }
        scope.getResult()
    }


internal val ALREADY_SELECTED: Any = Symbol("ALREADY_SELECTED")
private val UNDECIDED: Any = Symbol("UNDECIDED")
private val RESUMED: Any = Symbol("RESUMED")

@PublishedApi
internal class SelectBuilderImpl<in R>(
    private val delegate: Continuation<R>
) : LockFreeLinkedListHead(), SelectBuilder<R>, SelectInstance<R>, Continuation<R> {
    // selection state is "this" (list of nodes) initially and is replaced by idempotent marker (or null) when selected
    @Volatile
    private var _state: Any? = this

    // this is basically our own SafeContinuation
    @Volatile
    private var result: Any? = UNDECIDED

    // cancellability support
    @Volatile
    private var parentHandle: DisposableHandle? = null

    /* Result state machine

        +-----------+   getResult   +---------------------+   resume   +---------+
        | UNDECIDED | ------------> | COROUTINE_SUSPENDED | ---------> | RESUMED |
        +-----------+               +---------------------+            +---------+
              |
              | resume
              V
        +------------+  getResult
        | value/Fail | -----------+
        +------------+            |
              ^                   |
              |                   |
              +-------------------+
     */

    companion object {
        private val STATE: AtomicReferenceFieldUpdater<SelectBuilderImpl<*>, Any?> =
            AtomicReferenceFieldUpdater.newUpdater(SelectBuilderImpl::class.java, Any::class.java, "_state")
        private val RESULT: AtomicReferenceFieldUpdater<SelectBuilderImpl<*>, Any?> =
            AtomicReferenceFieldUpdater.newUpdater(SelectBuilderImpl::class.java, Any::class.java, "result")
    }

    override val context: CoroutineContext get() = delegate.context

    override val completion: Continuation<R> get() = this

    private inline fun doResume(value: () -> Any?, block: () -> Unit) {
        check(isSelected) { "Must be selected first" }
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, value())) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    block()
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    // Resumes in MODE_DIRECT
    override fun resume(value: R) {
        doResume({ value }) {
            delegate.resumeDirect(value)
        }
    }

    // Resumes in MODE_DIRECT
    override fun resumeWithException(exception: Throwable) {
        doResume({ Fail(exception) }) {
            delegate.resumeDirectWithException(exception)
        }
    }

    // Resumes in MODE_CANCELLABLE
    override fun resumeSelectCancellableWithException(exception: Throwable) {
        doResume({ Fail(exception) }) {
            delegate.resumeCancellableWithException(exception)
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        if (!isSelected) initCancellability()
        var result = this.result // atomic read
        if (result === UNDECIDED) {
            if (RESULT.compareAndSet(this, UNDECIDED, COROUTINE_SUSPENDED)) return COROUTINE_SUSPENDED
            result = this.result // reread volatile var
        }
        when {
            result === RESUMED -> throw IllegalStateException("Already resumed")
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }

    private fun initCancellability() {
        val parent = context[Job] ?: return
        val newRegistration = parent.invokeOnCompletion(SelectOnCancellation(parent), onCancelling = true)
        parentHandle = newRegistration
        // now check our state _after_ registering
        if (isSelected) newRegistration.dispose()
    }

    private inner class SelectOnCancellation(job: Job) : JobCancellationNode<Job>(job) {
        override fun invokeOnce(reason: Throwable?) {
            if (trySelect(null))
                resumeSelectCancellableWithException(reason ?: CancellationException("Select was cancelled"))
        }
        override fun toString(): String = "SelectOnCancellation[${this@SelectBuilderImpl}]"
    }

    private val state: Any? get() {
        while (true) { // lock-free helping loop
            val state = _state
            if (state !is OpDescriptor) return state
            state.perform(this)
        }
    }

    @PublishedApi
    internal fun handleBuilderException(e: Throwable) {
        if (trySelect(null))
            resumeWithException(e)
        else
            handleCoroutineException(context, e)
    }

    override val isSelected: Boolean get() = state !== this

    override fun disposeOnSelect(handle: DisposableHandle) {
        val node = DisposeNode(handle)
        while (true) { // lock-free loop on state
            val state = this.state
            if (state === this) {
                if (addLastIf(node, { this.state === this }))
                    return
            } else { // already selected
                handle.dispose()
                return
            }
        }
    }

    private fun doAfterSelect() {
        parentHandle?.dispose()
        forEach<DisposeNode> {
            it.handle.dispose()
        }
    }

    // it is just like start(), but support idempotent start
    override fun trySelect(idempotent: Any?): Boolean {
        check(idempotent !is OpDescriptor) { "cannot use OpDescriptor as idempotent marker"}
        while (true) { // lock-free loop on state
            val state = this.state
            when {
                state === this -> {
                    if (STATE.compareAndSet(this, this, idempotent)) {
                        doAfterSelect()
                        return true
                    }
                }
                // otherwise -- already selected
                idempotent == null -> return false // already selected
                state === idempotent -> return true // was selected with this marker
                else -> return false
            }
        }
    }

    override fun performAtomicTrySelect(desc: AtomicDesc): Any? = AtomicSelectOp(desc, true).perform(null)
    override fun performAtomicIfNotSelected(desc: AtomicDesc): Any? = AtomicSelectOp(desc, false).perform(null)

    private inner class AtomicSelectOp(
        @JvmField val desc: AtomicDesc,
        @JvmField val select: Boolean
    ) : AtomicOp<Any?>() {
        override fun prepare(affected: Any?): Any? {
            // only originator of operation makes preparation move of installing descriptor into this selector's state
            // helpers should never do it, or risk ruining progress when they come late
            if (affected == null) {
                // we are originator (affected reference is not null if helping)
                prepareIfNotSelected()?.let { return it }
            }
            return desc.prepare(this)
        }

        override fun complete(affected: Any?, failure: Any?) {
            completeSelect(failure)
            desc.complete(this, failure)
        }

        fun prepareIfNotSelected(): Any? {
            while (true) { // lock-free loop on state
                val state = _state
                when {
                    state === this@AtomicSelectOp -> return null // already in progress
                    state is OpDescriptor -> state.perform(this@SelectBuilderImpl) // help
                    state === this@SelectBuilderImpl -> {
                        if (STATE.compareAndSet(this@SelectBuilderImpl, this@SelectBuilderImpl, this@AtomicSelectOp))
                            return null // success
                    }
                    else -> return ALREADY_SELECTED
                }
            }
        }

        private fun completeSelect(failure: Any?) {
            val selectSuccess = select && failure == null
            val update = if (selectSuccess) null else this@SelectBuilderImpl
            if (STATE.compareAndSet(this@SelectBuilderImpl, this@AtomicSelectOp, update)) {
                if (selectSuccess)
                    doAfterSelect()
            }
        }
    }

    override fun Job.onJoin(block: suspend () -> R) {
        registerSelectJoin(this@SelectBuilderImpl, block)
    }

    override fun <T> Deferred<T>.onAwait(block: suspend (T) -> R) {
        registerSelectAwait(this@SelectBuilderImpl, block)
    }

    override fun <E> SendChannel<E>.onSend(element: E, block: suspend () -> R) {
        registerSelectSend(this@SelectBuilderImpl, element, block)
    }

    override fun <E> ReceiveChannel<E>.onReceive(block: suspend (E) -> R) {
        registerSelectReceive(this@SelectBuilderImpl, block)
    }

    override fun <E> ReceiveChannel<E>.onReceiveOrNull(block: suspend (E?) -> R) {
        registerSelectReceiveOrNull(this@SelectBuilderImpl, block)
    }

    override fun Mutex.onLock(owner: Any?, block: suspend () -> R) {
        registerSelectLock(this@SelectBuilderImpl, owner, block)
    }

    override fun onTimeout(time: Long, unit: TimeUnit, block: suspend () -> R) {
        require(time >= 0) { "Timeout time $time cannot be negative" }
        if (time == 0L) {
            if (trySelect(null))
                block.startCoroutineUndispatched(completion)
            return
        }
        val action = Runnable {
            // todo: we could have replaced startCoroutine with startCoroutineUndispatched
            // But we need a way to know that Delay.invokeOnTimeout had used the right thread
            if (trySelect(null))
                block.startCoroutineCancellable(completion) // shall be cancellable while waits for dispatch
        }
        disposeOnSelect(context.delay.invokeOnTimeout(time, unit, action))
    }

    private class DisposeNode(
        @JvmField val handle: DisposableHandle
    ) : LockFreeLinkedListNode()

    private class Fail(
        @JvmField val exception: Throwable
    )
}
