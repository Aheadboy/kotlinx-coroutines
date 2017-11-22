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
package guide.channel.example03

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

/**
 * The pattern where a coroutine is producing a sequence of elements is quite common.
 * This is a part of producer-consumer pattern that is often found in concurrent code.
 * You could abstract such a producer into a function that takes channel as its parameter,
 * but this goes contrary to common sense that results must be returned from functions.
 * 从一个协程里面持续性得发出一系列数据流是非常常见的模式
 * 这种模式其实是并发代码中非常常见的， 生产者消费者模式,中的一部分（生产者部分）
 *
 * There is a convenience coroutine builder named produce that makes it easy to do it right on producer side,
 * and an extension function consumeEach, that can replace a for loop on the consumer side、
 * 有一个很方便的方法来完成生产者部分。
 * 那就是使用produce来构建一个协程。
 *
 */
fun produceSquares() =
        /**
         * 创建一个生产者端的协程。用来作为channel的发送端。发送数据流
         */
        produce<Int>(CommonPool) {
            for (x in 1..5) {
                send(x * x)
                delay(1000L)
            }
        }

fun main(args: Array<String>) = runBlocking<Unit> {
    val squares = produceSquares()
    squares.consumeEach {
        println(it)
        delay(10_000L)
    }
    println("Done!")
}