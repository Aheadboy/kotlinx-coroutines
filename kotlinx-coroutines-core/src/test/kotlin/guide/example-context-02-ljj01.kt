package guide

import kotlinx.coroutines.experimental.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
val delayTime = 1000L
fun test1() = launch(newSingleThreadContext("heheda"), CoroutineStart.UNDISPATCHED) {
    log("bf")//由父协程，以及父协程的线程来执行
    delay(delayTime)
    log("af")//由当前协程，以及confine到指定线程执行--newSingleThreadContext("heheda")
}

fun test2() = launch(Unconfined) {
    log("bf")//由父协程，以及父协程的线程来执行
    delay(delayTime)
    log("af")
}

fun test3() = launch(CommonPool, CoroutineStart.UNDISPATCHED) {
    log("bf")//由父协程，以及父协程的线程来执行
    delay(delayTime)
    log("af")
}

fun test4() = launch(CommonPool) {
    log("bf")//由父协程，以及父协程的线程来执行
    delay(delayTime)
    log("af")
}

fun main(args: Array<String>) = runBlocking {
    val n = 2
    val jobs = List<Job>(n) {
        //        test1()
//        test2()
//        test3()
//        test4()
        launch(coroutineContext) {
            log("bf")//由父协程，以及父协程的线程来执行
            delay(delayTime)
            log("af")
        }
    }

    jobs.forEach { it.join() }
    log("runBlocking")
}


/**
 *On the other side, coroutineContext property that is available inside the block of any coroutine via CoroutineScope interface,
 *  is a reference to a context of this particular coroutine. This way, a parent context can be inherited.
 *  The default context of runBlocking, in particular, is confined to be invoker thread,
 * so inheriting it has the effect of confining execution to this thread with a predictable FIFO scheduling.
 *
 * 这种方法，父context可以被继承，
 * runBlocking的默认context，被限定成调用者线程。
 * 所以，继承它会有限定执行在这个线程可预知的FIFO调度的影响
 * */

