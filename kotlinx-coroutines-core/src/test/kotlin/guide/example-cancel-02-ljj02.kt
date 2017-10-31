package guide

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking<Unit> {
    val job = launch(CommonPool) {
        var nextPrintTime = System.currentTimeMillis()
        var i = 0
        while (i < 10) { // computation loop
            val currentTime = System.currentTimeMillis()
            if (currentTime >= nextPrintTime) {
                println("I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    println(job.cancel()) // 成功调用返回true
    println(job.cancel()) // 重复调用返回false
    delay(6_000L)
    println(job.cancel()) // 协程已经执行完了，调用取消返回false
    println(job.isActive)//未调用job.cancel()，协程执行完毕，返回值也是false
    println("main: Now I can quit.")
}