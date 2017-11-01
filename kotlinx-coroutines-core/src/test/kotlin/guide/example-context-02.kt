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

// This file was automatically generated from coroutines-guide.md by Knit tool. Do not edit.
package guide.context.example02

import kotlinx.coroutines.experimental.*


/**
 * The Unconfined coroutine dispatcher starts coroutine in the caller thread, but only until the first suspension point.
 * After suspension it resumes in the thread that is fully determined by the suspending function that was invoked.
 * 未限定的协程在调用线程里面启动，当协程被挂起并唤醒后，协程所处的线程将取决于唤醒挂起函数的线程。
 * Unconfined dispatcher is appropriate when coroutine does not consume CPU time nor updates any shared data
 * (like UI) that is confined to a specific thread.
 * 未限定的使用场景是：
 * 不消耗CPU，不更新共享数据（例如UI）
 */
fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main(args: Array<String>) = runBlocking<Unit> {
    val jobs = arrayListOf<Job>()
    jobs += launch(Unconfined) {
        // not confined -- will work with main thread
        log("      'Unconfined': I'm working in thread ")//attention:[main ***@coroutine#1 @coroutine#2***]       'Unconfined': I'm working in thread
        delay(500)
        log("      'Unconfined': After delay1 in thread ")
//        delay(500)
//        log("      'Unconfined': After delay2 in thread ")
//        delay(500)
//        log("      'Unconfined': After delay3 in thread ")
    }

    /**
     * This way, a parent context can be inherited. The default context of runBlocking, in particular, is confined to be invoker thread,
     * so inheriting it has the effect of confining execution to this thread with a predictable FIFO scheduling.
     */
    jobs += launch(coroutineContext) {
        // context of the parent, runBlocking coroutine
        log("'coroutineContext': I'm working in thread ")//[main @coroutine#3] 'coroutineContext': I'm working in thread //FIFO
        delay(1000)
        log("'coroutineContext': After delay in thread ")
    }
    jobs.forEach { it.join() }
}
