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
package guide.channel.example01

/**
 * Deferred values provide a convenient way to transfer a single value between coroutines.
 * Channels provide a way to transfer a stream of values.
 * Deferred 提供了一种在协程之间传递值的方式。
 * 通道提供了一种流的形式在协程间传递值
 * 简单说：
 * 协程间传递数据流用什么？
 * 用通道（channel）
 */
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * A Channel is conceptually very similar to BlockingQueue. One key difference is that
 * instead of a blocking put operation it has a suspending send,
 * and instead of a blocking take operation it has a suspending receive.
 *
 * 通道好比一个阻塞队列。
 * 不同点在于，
 * 通道是挂起发送和挂起接收，而非阻塞。
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val channel = Channel<Int>()
    launch(CommonPool) {
        // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
        for (x in 1..5) channel.send(x * x)
    }
    // here we print five received integers:
    repeat(5) { println(channel.receive()) }
    println("Done!")
}
