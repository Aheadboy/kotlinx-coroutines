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
package guide.basic.example01

import kotlinx.coroutines.experimental.*

fun main(args: Array<String>) {
    //线程非阻塞延迟1秒
    //线程非阻塞:用于该段代码的线程，不会因为该段代码需要延迟而被阻塞
    //该段代码通过挂起方式进行延迟，而该线程开心得跑了，去执行别的代码或处于休息状态。
    launch(CommonPool) { // create new coroutine in common thread pool
        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)--线程非阻塞延迟1秒
        println("World!") // print after delay
    }
    println("Hello,") // main function continues while coroutine is delayed
    //阻塞线程延迟2秒
    //为了使该段代码不再执行，需要让该线程在此死等。
    //线程作为该延迟的陪葬品。
    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive--阻塞线程延迟2秒（java api的做法）
}
