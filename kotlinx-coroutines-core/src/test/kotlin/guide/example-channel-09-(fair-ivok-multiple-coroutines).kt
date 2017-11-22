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
package guide.channel.example09

/**
 *
 * Send and receive operations to channels are fair with respect to the order of their invocation from multiple coroutines.
 * They are served in first-in first-out order, e.g. the first coroutine to invoke receive gets the element.
 * In the following example two coroutines "ping" and "pong" are receiving the "ball" object from the shared "table" channel.
 *
 * 对于channel来说，多协程调用（包括send和receive）一个channel的顺序上是公平的。
 * 调用顺序上是公平的，遵循先进先出原则。谁先调用，谁就先得到结果。
 * 最先调用receive的协程，获取到channel里面的元素。
 *
 *
 * 这段代码的故事内容是这样的：
 * 首先：table这个channel，被send了一个Ball,但是呢，这个Ball还没有真的进入到channel，send就被挂起了。
 * 因为这是一个无Buffer的channel，想要元素被成功传递，只有当sender和reciver它俩meet的时候。
 * 接着，调用ping这个挂起函数的协程先于调用pong这个挂起函数的协程。
 * 由于，Channels are fair、并且multiple coroutines are served by channel in first-in first-out order
 * 所有，ping这个协程首先获取到了这个Ball，对其进行自增1，接着将Ball元素send回管道，
 * 同理，这个Ball并没有被send入channel，send被挂起，等待pong来recive
 * pong recive后自增1，又进行了send。
 */
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

data class Ball(var hits: Int)

fun main(args: Array<String>) = runBlocking<Unit> {
    val table = Channel<Ball>() // a shared table
    launch(coroutineContext) { player("ping", table) }
    launch(coroutineContext) { player("pong", table) }
//    repeat(10_000){launch(CommonPool) { player("$it", table) }}
    table.send(Ball(0)) // serve the ball
    delay(1000) // delay 1 second
    table.receive() // game over, grab the ball
}

/**
 * 从channel取元素，
 * 将该元素自增1
 * 放回该元素到channel
 */
suspend fun player(name: String, table: Channel<Ball>) {


    //    对比1、以下代码只有两个如下结果
//    ping Ball(hits=1)
//    pong Ball(hits=2)
    val ball = table.receive() // receive the ball
    ball.hits++
    println("no loop $name $ball")
    delay(300) // wait a bit
    table.send(ball) // send the ball back


    //对比2、以下代码可以不断pingpong，区别在于for循环。
    for (ball in table) { // receive the ball in a loop
        ball.hits++
        println("has loop $name $ball")
        delay(300) // wait a bit
        table.send(ball) // send the ball back
    }


}
//请注意上面这个for (ball in table) { // receive the ball in a loop循环
//由于是在main单线程里面，所以协程的调用是顺序的。
// 因为我们这里想要验证的就是channel对协程调用顺序的公平（FIFO）。
//如果这里是用CommonPool那么两个协程谁先调用就不得而知了。所以这里不能使用CommonPool来验证。
//既然调用是顺序的：
// 那么先调用的是ping这个协程。其次调用pong
//ping协程for loop 去recive，Ball，获取到一个之后，ping协程挂起300/1000秒
//ping协程的挂起点一出现，那么主线程就去执行pong协程，pong协程进入活跃状态。
//pong协程for loop 去recive，Ball，没有获取到，因为ping拿走之后就delay了。还没send回去。
//ping经过300的休息，将Ball,send回去,由于刚刚pong没有recive到，被挂起，此时可以recive到，立刻被pong获取，
//        然后ping进入下一次循环，尝试获取Ball，由于pong进行了delay300，还没有send,Ball
//        所以ping的这次循环获取Ball没有获取到，recive挂起等待。
//        就是这样的循环步骤