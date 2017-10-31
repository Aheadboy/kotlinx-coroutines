package guide

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking<Unit> {
    val job = launch(CommonPool) {
        sFun1()
        sFun2()
        sFun3()
    }
    delay(600L)
    //原计划在1秒后执行挂起函数2的打印；
    //但是0.6秒的时候协作性地取消了sFun2挂起函数，
    //所以只执行了sFun1()
    job.cancel()
    delay(1600);//延迟，观察协程是否被取消了。挂起函数2,3是否被调用了。
}

suspend fun sFun1() {
    delay(500L)
    println("sFun1")
}

suspend fun sFun2() {
    delay(500L)
    println("sFun2")
}

suspend fun sFun3() {

    delay(500L)
    println("sFun3")
}

