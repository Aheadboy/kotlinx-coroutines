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

package kotlinx.coroutines.experimental.selects

import kotlinx.coroutines.experimental.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectJobTest : TestBase() {
    @Test
    fun testSelectCompleted() = runBlocking<Unit> {
        expect(1)
        launch(coroutineContext) { // makes sure we don't yield to it earlier
            expectUnreached() // will terminate before it has a chance to start
        }
        val job = Job()
        job.cancel()
        select<Unit> {
            job.onJoin {
                expect(2)
            }
        }
        finish(3)
    }

    @Test
    fun testSelectIncomplete() = runBlocking<Unit> {
        expect(1)
        val job = Job()
        launch(coroutineContext) { // makes sure we don't yield to it earlier
            expect(3)
            val res = select<String> {
                job.onJoin {
                    expect(6)
                    "OK"
                }
            }
            expect(7)
            assertEquals("OK", res)
        }
        expect(2)
        yield()
        expect(4)
        job.cancel()
        expect(5)
        yield()
        finish(8)
    }

    @Test
    fun testSelectLazy() = runBlocking<Unit> {
        expect(1)
        val job = launch(coroutineContext, CoroutineStart.LAZY) {
            expect(2)
        }
        val res = select<String> {
            job.onJoin {
                expect(3)
                "OK"
            }
        }
        finish(4)
        assertEquals("OK", res)
    }
}