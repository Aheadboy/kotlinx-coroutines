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
package guide.cancel.example02

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * Cancellation is cooperative
 * 协程的取消是协同的
 * 在cancel-01的例子中，我们通过job.cancel()取消了一个协程。
 * 这很容易让我们想当然地认为，每当需要取消协程时，调用job.cancel()总能奏效。事实并非如此；
 * *******job.cancel()取消的只是挂起函数的调用，而不是一个正在执行的协程；*********
 * 在本例中，协程并未涉及挂起函数的调用，取而代之的是在协程里面进行一个循环；
 * 在循环进行的过程中，协程处于执行状态。job.cancel()并*******不能使协程中正在执行的内容立刻停止，但协程还是被取消了*******
 * ***********这点务必注意，虽然没有立刻终止正在执行的内容，但是协程还是被取消了。isActive为false************。
 * 在上例中，通过repeat函数多次（尝试调用1000次）调用挂起函数 delay(500L)
 * 当协程被挂起之后，还没有重新唤醒（resume）之际，调用 job.cancel()，那么协程将会被成功取消
 * 关于这一点，请看更清晰的例子example-cancel-02-ljj01.kt
 *那么能否取消正在执行的协程呢？
 * 答案是：可以；并且有两种方式；
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val job = launch(CommonPool) {
        var nextPrintTime = System.currentTimeMillis()
        var i = 0
        while (i < 10) { // computation loop
            val currentTime = System.currentTimeMillis()
            if (currentTime >= nextPrintTime) {
                println("I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    println(job.cancel()) // cancels the job
    delay(1300L) // delay a bit to see if it was cancelled....
    println("main: Now I can quit.")
}
