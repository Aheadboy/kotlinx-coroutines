package guide

import kotlinx.coroutines.experimental.*


//* If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
//* immediately resumes with [CancellationException].
//* 如果调用的协程的Job被取消或者Job完成，并且await()挂起函数处于挂起状态。
//* 那么该挂起函数立刻从挂起状态唤醒（resume）并且抛出异常[CancellationException].
fun main(args: Array<String>) = runBlocking {
    val job = launch(CommonPool) {
        val one = async(CommonPool) { delay(3000L) }
        try {
            one.await()//job所在的协程被挂起，等待one的执行结果
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            println("finally")
        }
    }
    job.cancel()//job取消，await()挂起函数从挂起状态被立刻唤醒(resume),并抛出[CancellationException]异常
    delay(5000L)//为了返回值Unit，无聊地加上这一句
}