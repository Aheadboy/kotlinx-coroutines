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


/**
 * Pipeline is a pattern where one coroutine is producing, possibly infinite, stream of values:
 *
 *And another coroutine or coroutines are consuming that stream, doing some processing
 * , and producing some other results. In the below example the numbers are just squared:
 *
 * 简而言之，多个通道连接，形成管道。
 * 后面的通道可以使用前面通道传来的值，对该值进行处理（消费）之后继续传递给后续的通道。
 * 形成管道（pipeline)的通道(channel)个数应该是不受限制的。
 */
package guide.channel.example04

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking

fun produceNumbers() = produce<Int>(CommonPool) {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}

fun square(numbers: ReceiveChannel<Int>) = produce<Int>(CommonPool) {
    for (x in numbers) {
        send(x * x)
        println("numbers is  ${numbers}")
    }
}

fun main(args: Array<String>) = runBlocking<Unit> {
    val numbers = produceNumbers() // produces integers from 1 and on
    val squares = square(numbers) // squares integers
    for (i in 1..5) println(squares.receive()) // print first five
//    squares.consumeEach { println("$it") }
    val i=0
    println("Done!") // we are done
    squares.cancel() // need to cancel these coroutines in a larger app
    numbers.cancel()
}
