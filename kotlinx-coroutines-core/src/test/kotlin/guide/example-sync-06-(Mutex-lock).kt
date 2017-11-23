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
package guide.sync.example06

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.sync.Mutex
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

suspend fun massiveRun(context: CoroutineContext, action: suspend () -> Unit) {
    val n = 1000 // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        val jobs = List(n) {
            launch(context) {
                repeat(k) { action() }
            }
        }
        jobs.forEach { it.join() }
    }
    println("Completed ${n * k} actions in $time ms")
}

val mutex = Mutex()
var counter = 0

fun main(args: Array<String>) = runBlocking<Unit> {
    massiveRun(CommonPool) {
        //细粒度的加锁，运行比较耗时
        //这段代码，一次只允许一个线程进入
        mutex.lock()
        try {
            counter++
        } finally {
            mutex.unlock()
        }
    }
    println("Counter = $counter")
}

/**
 * Mutual exclusion solution to the problem is to
 * protect all modifications of the shared state with a critical section that is never executed concurrently.
 * Mutual exclusion的解决办法是，通过界定部分代码永远不会被并发执行，来保证多线程的并发安全。
 * 粗暴点说：mutex.lock(),mutex.unlock()之间的代码就是被界定为不可并发执行的代码。
 * In a blocking world you'd typically use synchronized or ReentrantLock for that.
 * 在阻塞的世界里面，例如java，我们就会用synchronized 或 ReentrantLock 来加锁。
 * Coroutine's alternative is called Mutex. It has lock and unlock functions to delimit a critical section.
 * 不同于阻塞世界，在协程里面，我们用Mutex。他通过lock and unlock functions来界定不可并发的代码。
 * The key difference is that Mutex.lock is a suspending function. It does not block a thread.
 * 他们的主要不同是：阻塞与非阻塞。
 */

/**
 * The locking in this example is fine-grained, so it pays the price.
 * However, it is a good choice for some situations where you absolutely must modify some shared state periodically,
 * but there is no natural thread that this state is confined to.
 *
 * 这是细粒度的代码块锁定。所以类似例子4，这里的执行是比较耗时的。（Mutex 互斥）
 * 当然，当有并发问题要保证安全，然而，你又没有普通的线程，让共享变量限定在其中执行。
 * 那么加互斥锁，不失为一种好的选择。
 *
 */
