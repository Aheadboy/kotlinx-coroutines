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
package guide.sync.example05

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
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
    massiveRun(counterContext) {
        // run each coroutine in the single-threaded context
        counter++
    }
    println("Counter = $counter")
}
/**
 * 组粒度的线程限制，
 * 将一个协程限制在一个线程里面。
 *In practice, thread confinement is performed in large chunks,
 * e.g. big pieces of state-updating business logic are confined to the single thread.
 * The following example does it like that, running each coroutine in the single-threaded context to start with.
 * You can get full code here
 * This now works much faster and produces correct result.
 */