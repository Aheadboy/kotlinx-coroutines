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
package guide.sync.example04

import kotlinx.coroutines.experimental.*
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

val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main(args: Array<String>) = runBlocking<Unit> {
    //经过实验，好像这种（fine-grained）写法，运行起来比较耗时。
    //原因有待分析
    //好像细粒度的线程指定总是会这么耗时，java也有类似的情况，这种写法应该避免。
    //java里面好像是通过加锁，细粒度的加锁也会有类似的耗时情况。
    //看来要做就做粗粒度（coarse-grained）的。
    //java的锁加到类
    //kotlin的线程指定，指定到协程级别，例如下一个例子。
    massiveRun(CommonPool) {
        // run each coroutine in CommonPool
        run(counterContext) {
            // but confine each increment to the single-threaded context
            counter++//只允许指定线程，运行该段代码
        }
    }
    println("Counter = $counter")
}
/**
 * Thread confinement is an approach to the problem of shared mutable state
 * where all access to the particular shared state is confined to a single thread.
 * 线程限制是解决线程并发问题的一种途径，
 * 它是通过限制共享变量只能在指定的一个线程里面执行，来解决的。
 * It is typically used in UI applications,
 * where all UI state is confined to the single event-dispatch/application thread.
 * 这种做法在UI应用里面很常见。
 * It is easy to apply with coroutines by using a single-threaded context:
 * You can get full code here
 *
 *  This code works very slowly, because it does fine-grained thread-confinement.
 *  Each individual increment switches from multi-threaded CommonPool context to the single-threaded context using run block.
 *  这段代码执行得比较缓慢，因为它是细粒度的线程限制。
 *  每一次独立增加共享变量的值，都要从线程池切换到指定的单线程。
 */
