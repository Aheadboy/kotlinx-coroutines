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

package kotlinx.coroutines.experimental

import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test

class CompletableDeferredTest : TestBase() {
    @Test
    fun testFresh() {
        val c = CompletableDeferred<String>()
        assertThat(c.isActive, IsEqual(true))
        assertThat(c.isCancelled, IsEqual(false))
        assertThat(c.isCompleted, IsEqual(false))
        assertThat(c.isCompletedExceptionally, IsEqual(false))
        assertThrows<IllegalStateException> { c.getCompletionException() }
        assertThrows<IllegalStateException> { c.getCompleted() }
    }

    @Test
    fun testComplete() {
        val c = CompletableDeferred<String>()
        assertThat(c.complete("OK"), IsEqual(true))
        checkCompleteOk(c)
        assertThat(c.complete("OK"), IsEqual(false))
        checkCompleteOk(c)
    }

    private fun checkCompleteOk(c: CompletableDeferred<String>) {
        assertThat(c.isActive, IsEqual(false))
        assertThat(c.isCancelled, IsEqual(false))
        assertThat(c.isCompleted, IsEqual(true))
        assertThat(c.isCompletedExceptionally, IsEqual(false))
        assertThat(c.getCompletionException(), IsInstanceOf(CancellationException::class.java))
        assertThat(c.getCompleted(), IsEqual("OK"))
    }

    @Test
    fun testCompleteWithException() {
        val c = CompletableDeferred<String>()
        assertThat(c.completeExceptionally(TestException()), IsEqual(true))
        checkCompleteTestException(c)
        assertThat(c.completeExceptionally(TestException()), IsEqual(false))
        checkCompleteTestException(c)
    }

    private fun checkCompleteTestException(c: CompletableDeferred<String>) {
        assertThat(c.isActive, IsEqual(false))
        assertThat(c.isCancelled, IsEqual(false))
        assertThat(c.isCompleted, IsEqual(true))
        assertThat(c.isCompletedExceptionally, IsEqual(true))
        assertThat(c.getCompletionException(), IsInstanceOf(TestException::class.java))
        assertThrows<TestException> { c.getCompleted() }
    }

    @Test
    fun testCancel() {
        val c = CompletableDeferred<String>()
        assertThat(c.cancel(), IsEqual(true))
        checkCancel(c)
        assertThat(c.cancel(), IsEqual(false))
        checkCancel(c)
    }

    private fun checkCancel(c: CompletableDeferred<String>) {
        assertThat(c.isActive, IsEqual(false))
        assertThat(c.isCancelled, IsEqual(true))
        assertThat(c.isCompleted, IsEqual(true))
        assertThat(c.isCompletedExceptionally, IsEqual(true))
        assertThat(c.getCompletionException(), IsInstanceOf(CancellationException::class.java))
        assertThrows<CancellationException> { c.getCompleted() }
    }

    @Test
    fun testCancelWithException() {
        val c = CompletableDeferred<String>()
        assertThat(c.cancel(TestException()), IsEqual(true))
        checkCancelWithException(c)
        assertThat(c.cancel(TestException()), IsEqual(false))
        checkCancelWithException(c)
    }

    private fun checkCancelWithException(c: CompletableDeferred<String>) {
        assertThat(c.isActive, IsEqual(false))
        assertThat(c.isCancelled, IsEqual(true))
        assertThat(c.isCompleted, IsEqual(true))
        assertThat(c.isCompletedExceptionally, IsEqual(true))
        assertThat(c.getCompletionException(), IsInstanceOf(TestException::class.java))
        assertThrows<TestException> { c.getCompleted() }
    }

    @Test
    fun testAwait() = runBlocking {
        expect(1)
        val c = CompletableDeferred<String>()
        launch(coroutineContext, CoroutineStart.UNDISPATCHED) {
            expect(2)
            assertThat(c.await(), IsEqual("OK")) // suspends
            expect(5)
            assertThat(c.await(), IsEqual("OK")) // does not suspend
            expect(6)
        }
        expect(3)
        c.complete("OK")
        expect(4)
        yield() // to launch
        finish(7)
    }

    private inline fun <reified T: Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
            fail("Should not complete normally")
        } catch (e: Throwable) {
            assertThat(e, IsInstanceOf(T::class.java))
        }
    }

    class TestException : Throwable()
}