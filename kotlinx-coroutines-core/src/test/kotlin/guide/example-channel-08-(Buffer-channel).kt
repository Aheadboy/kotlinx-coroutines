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
package guide.channel.example08

/**
 * Buffered channels
 *
 * The channels shown so far had no buffer.
 * Unbuffered channels transfer elements when sender and receiver meet each other (aka rendezvous).
 * If send is invoked first, then it is suspended until receive is invoked,
 * if receive is invoked first, it is suspended until send is invoked.
 *
 * 没有缓存的channel，只有在sender和receiver相遇时元素才能传递成功。（例如：rendezvous）
 * 如果发送先被调用，那么发送端被挂起直到接收端被调用
 * 反向，同理。
 *
 * 缓存满了之后，也依据以上规则进行挂起。
 */
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking<Unit> {
    val channel = Channel<Int>(5) // create buffered channel
    launch(coroutineContext) {
        // launch sender coroutine
        repeat(10) {
            println("Sending $it") // print before sending each element
            channel.send(it) // will suspend when buffer is full
//            当缓存满了，这个send会被挂起。
        }
    }
    // don't receive anything... just wait....
    delay(1000)
}
