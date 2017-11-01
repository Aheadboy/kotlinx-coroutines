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
package guide.context.example06

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * When coroutineContext of a coroutine is used to launch another coroutine,
 * the Job of the new coroutine becomes a child of the parent coroutine's job.
 * When the parent coroutine is cancelled, all its children are recursively cancelled, too.
 *
 * 使用coroutineContext协程的Job将成为其父协程Job的子Job
 * 父Job被取消，其子Job将被递归取消。
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    // start a coroutine to process some kind of incoming request
    val request = launch(CommonPool) {
        // it spawns two other jobs, one with its separate context
        //这个job1独立于request的context
        //当request取消，该job1不受影响。
        val job1 = launch(CommonPool) {
            println("job1: I have my own context and execute independently!")
            delay(1000)
            println("job1: I am not affected by cancellation of the request")
        }
        // and the other inherits the parent context
        //这个job2继承了request的context，使job2成为request的子Job
        //当父Job取消，其子Job将递归取消
        val job2 = launch(coroutineContext) {
            println("job2: I am a child of the request coroutine")
            delay(1000)
            println("job2: I will not execute this line if my parent request is cancelled")
        }
        // request completes when both its sub-jobs complete:
        //request非阻塞等待job1，job2完成。
        job1.join()
        job2.join()
    }
    delay(500)
    request.cancel() // cancel processing of the request
    delay(1000) // delay a second to see what happens
    println("main: Who has survived request cancellation?")
}
