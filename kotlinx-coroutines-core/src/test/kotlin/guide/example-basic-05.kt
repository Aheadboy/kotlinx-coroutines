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
package guide.basic.example05

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * Coroutines ARE light-weight
 * 协程是轻量的
 */
fun main(args: Array<String>) = runBlocking<Unit> {
    val jobs = List(100_000) {
        // create a lot of coroutines and list their jobs
        launch(CommonPool) {
            delay(1000L)
            println(it)
        }
    }//list包含了100_000个launch协程的返回值--job；
    //list存放的内容就是list闭包的返回值。
    //100_000个协程并不是从0到99999顺序执行完的。而是杂乱的，例如2,1,0，3.....这样无规律的顺序。
    //所以应该等待所有的job执行完成。
    jobs.forEach { it.join() } // wait for all jobs to complete
}
