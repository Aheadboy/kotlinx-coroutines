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
package guide.channel.example02

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * Unlike a queue, a channel can be closed to indicate that no more elements are coming.
 * On the receiver side it is convenient to use a regular for loop to receive elements from the channel.
 * 与队列所不同的是：
 * 通道可以被关闭，来指示通道中已经没有元素可接收。
 * Conceptually, a close is like sending a special close token to the channel.
 * The iteration stops as soon as this close token is received,
 * so there is a guarantee that all previously sent elements before the close are received:
 *
 * 概念上来说，关闭好比一个特殊的通道元素，
 * 通道接收端一旦接收到这个元素，即刻停止for循环。不再向通道取元素。
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val channel = Channel<Int>(Channel.UNLIMITED)
    launch(CommonPool) {
        for (x in 1..5) {
            channel.send(x * x)
        }
//        channel.close() // we're done sending
        println(channel.isClosedForSend)
        try {
//            channel.send(6)//照理应该报异常的，因为channel已经关闭。但是时而报，时而不包？？？？？
        } catch (e: Exception) {
            println(e.printStackTrace())
        }
    }
    // here we print received values using `for` loop (until the channel is closed)
//    for (y in channel) println(y)
    println("poll element using ${channel.poll()}")
//    channel.receive()
//    channel.send(7)
    println("Done!")
    println(channel.isClosedForReceive)
    println(channel.isClosedForSend)
    println("is empty ${channel.isEmpty}")
}
