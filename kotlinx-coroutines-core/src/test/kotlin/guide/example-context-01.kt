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
package guide.context.example01

import kotlinx.coroutines.experimental.*

/**
 * Coroutine context includes a coroutine dispatcher which determines what thread or threads the corresponding coroutine uses for its execution.
 * 协程的context有个 CoroutineDispatcher协程分发器。
 * 分发器的主要作用是：
 * 决定该协程执行所要用的线程。
 * 包括：一个指定的线程，线程池或未限定（unconfined）
 *
 * Unconfined未限定
 * CommonPool线程池
 * newSingleThreadContext单一线程
 * coroutineContext父线程所在的线程
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val jobs = arrayListOf<Job>()
    jobs += launch(Unconfined, CoroutineStart.LAZY) {
        // not confined -- will work with main thread
        println("      'Unconfined': I'm working in thread ${Thread.currentThread().name}")
    }
    jobs += launch(coroutineContext) {
        // context of the parent, runBlocking coroutine
        println("'coroutineContext': I'm working in thread ${Thread.currentThread().name}")
    }
    jobs += launch(CommonPool) {
        // will get dispatched to ForkJoinPool.commonPool (or equivalent)
        println("      'CommonPool': I'm working in thread ${Thread.currentThread().name}")
    }
    jobs += launch(newSingleThreadContext("MyOwnThread")) {
        // will get its own new thread
        println("          'newSTC': I'm working in thread ${Thread.currentThread().name}")
    }
    jobs.forEach { it.join() }
}

/**
 * 由运行结果可以看出，
 * Unconfined与coroutineContext都运行在主线程。
 * 但是，它们是有区别的。
 * 将在下一个例子中看出
 */

/**
 * * This function also [starts][Job.start] the corresponding coroutineif the [Job] was still in _new_ state.
 * 这个函数的调用，可以对那些处于_new_ state状态的Job进行[starts]使之进入Active状态。
 * 例如懒启动的协程，通过调用jobLazy.join(),jobLazy将会进入Active状态。
 */
