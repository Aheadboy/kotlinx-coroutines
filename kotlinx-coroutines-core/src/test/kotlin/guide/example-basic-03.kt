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
package guide.basic.example03

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking<Unit> {
    val job = launch(CommonPool) {//launch与async的区别是返回值的区别。
        // create new coroutine and keep a reference to its Job
        delay(1000L)//该协程挂起1秒后唤醒
        println("World!")
    }
    println("Hello,")
    job.join() // wait until child coroutine completes--join()是挂起函数：在挂起函数出现的地方，协程将被挂起
             //该协程挂起，直到job完成，该协程唤醒。
    //join()为挂起函数，使该协程非阻塞挂起。
    //但是，注意，这里的runBlocking是一个特殊的协程，他是个阻塞当前线程的协程。
    //主要使用场景为main函数，以及一些测试场景。
}
