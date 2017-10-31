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
package guide.cancel.example03

import kotlinx.coroutines.experimental.*

/**
 * There are two approaches to making computation code cancellable.
 * The first one is to periodically invoke a suspending function.
 *
 * There is a yield function that is a good choice for that purpose.
 * The other one is to explicitly check the cancellation status. Let us try the later approach
 * 有两种取消执行中代码的方法。
 * 这里展示第二种。
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val job = launch(CommonPool) {
        var nextPrintTime = 0L
        var i = 0
        while (isActive) { // cancellable computation loop 可通过 job.cancel()来取消的循环。
            //当job取消了isActive为false，继续循环，条件不满足。
            val currentTime = System.currentTimeMillis()
            if (currentTime >= nextPrintTime) {
                println("I'm sleeping ${i++} ...")
                nextPrintTime = currentTime + 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancel() // cancels the job
    delay(1300L) // delay a bit to see if it was cancelled....
    println("main: Now I can quit.")
}
