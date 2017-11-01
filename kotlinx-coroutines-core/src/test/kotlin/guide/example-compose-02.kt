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
package guide.compose.example02

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlin.system.measureTimeMillis


/**
 * Concurrent using async
 * 并发执行挂起函数
 *
 * What if there are no dependencies between invocation of doSomethingUsefulOne and doSomethingUsefulTwo
 * and we want to get the answer faster, by doing both concurrently?
 *
 *  假如doSomethingUsefulOne（）doSomethingUsefulTwo（）两个函数之间没有依赖
 *  并且希望他们并发执行，以最快的速度获取答案。我们该怎么做？
 *
 *  下面的代码清晰地展示了做法。
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
        val one = async(CommonPool) { doSomethingUsefulOne() }//协程1
        val two = async(CommonPool) { doSomethingUsefulTwo() }//协程2
        println("The answer is ${one.await() + two.await()}")//子协程one,two类似守护线程，如果没有这两个await，让runBlocking等待，则无法保活进程。
        //通过这段代码，直观的感觉Deferred中的await有点类似Job中的join。
        //是否真是如此，我们稍后加以了解。
    }
    println("Completed in $time ms")
}
