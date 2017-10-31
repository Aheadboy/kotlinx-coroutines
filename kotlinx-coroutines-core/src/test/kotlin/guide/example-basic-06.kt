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
package guide.basic.example06

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * Coroutines are like daemon threads
 * 协程就像守护线程。
 * 活动的协程并不能使进程保活。
 * Active coroutines do not keep the process alive. They are like daemon threads.
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    launch(CommonPool) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // just quit after delay
}

/**
 *
 * 所谓守护 线程，是指在程序运行的时候在后台提供一种通用服务的线程，
 * 比如垃圾回收线程就是一个很称职的守护者，并且这种线程并不属于程序中不可或缺的部分。
 * 因此，当所有的非守护线程结束时，程序也就终止了，同时会杀死进程中的所有守护线程。
 * 反过来说，只要任何非守护线程还在运行，程序就不会终止。
 *用户线程和守护线程两者几乎没有区别
 * ，唯一的不同之处就在于虚拟机的离开：如果用户线程已经全部退出运行了，只剩下守护线程存在了，虚拟机也就退出了
 * 。 因为没有了被守护者，守护线程也就没有工作可做了，也就没有继续运行程序的必要了。
 */
