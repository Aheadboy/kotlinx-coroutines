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
package guide.compose.example01

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import kotlin.system.measureTimeMillis


/**
 * Sequential by default
 * 顺序执行
 *
 * We just use a normal sequential invocation,
 * because the code in the coroutine, just like in the regular code, is sequential by default.
 * 我们只需要顺序调用挂起函数，那么挂起函数就会顺序执行
 * 因为协程里面的代码执行顺序跟普通代码的执行顺序是一样的。
 *
 *  In practise we do this if we use the results of the first function
 *  to make a decision on whether we need to invoke the second one or to decide on how to invoke it.
 *  假如：我们需要doSomethingUsefulOne（）的执行结果，
 *  来决定如何调用doSomethingUsefulTwo（）
 *  那么我们就应该用这种顺序调用挂起函数的方法。
 */
suspend fun doSomethingUsefulOne(): Int {
    println("suspending one")
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    println("suspending two")
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}

fun main(args: Array<String>) = runBlocking<Unit> {
    val time = measureTimeMillis {//测量代码块的执行完成时间
        val one = doSomethingUsefulOne()//第一次遇见挂起函数，协程runBlocking挂起
        val two = doSomethingUsefulTwo()//第二次遇见挂起函数，协程runBlocking挂起
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")
}
