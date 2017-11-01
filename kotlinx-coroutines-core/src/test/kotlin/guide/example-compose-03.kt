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
package guide.compose.example03

import kotlinx.coroutines.experimental.*
import kotlin.system.measureTimeMillis

/**
 * Lazily started async
 * 协程懒启动
 *
 *  There is a laziness option to async with CoroutineStart.LAZY parameter.
 *  It starts coroutine only when its result is needed by some **await** or if a **start** function is invoked.
 *
 *  通过参数CoroutineStart.LAZY可以将协程设置为懒启动。
 *  懒启动的协程只有在结果被需要时，
 *  也就是一些await(),或者一个start()函数被调用时。
 *  协程才会启动
 *
 *
 *  So, we are back to sequential execution, because we first start and await for one, and then start and await for two.
 *  It is not the intended use-case for laziness.
 *  It is designed as a replacement for the standard lazy function in cases when computation of the value involves suspending functions.
 *
 *  正如实验结果显示的
 *  我们又顺序执行了两个挂起函数：
 *  首先我们通过调用one.await()来启动并等待one，
 *  接着，我们通过two.await()来启动并等待two。
 *
 *  当然了，这个例子不是懒启动协程的设计目的。
 *  懒启动协程的设计目的是：
 *  在值的计算结果调用挂起函数的场景下，
 *  替代标准的lazy function
 */
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}

fun main(args: Array<String>) = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = async(CommonPool, CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(CommonPool, CoroutineStart.LAZY) { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
//        * Awaits for completion of this value without blocking a thread and resumes when deferred computation is complete,
//        * //ljj1,功能：调用await()的协程非阻塞线程等待Deferred的值，一旦Deferred的值获取到，立刻运行该协程。
//        * returning the resulting value or throwing the corresponding exception if the deferred had completed exceptionally.
//        * //ljj1,返回值：await()返回Deferred的值，或者Deferred的异常
    }
    println("Completed in $time ms")
}
