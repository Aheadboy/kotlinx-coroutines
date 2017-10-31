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

package kotlinx.coroutines.experimental.channels

import kotlinx.coroutines.experimental.TestBase
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.yield
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertThat
import org.junit.Test

class ActorTest : TestBase() {
    @Test
    fun testEmpty() = runBlocking<Unit> {
        expect(1)
        val actor = actor<String>(coroutineContext) {
            expect(3)
        }
        assertThat(actor.isActive, IsEqual(true))
        assertThat(actor.isCompleted, IsEqual(false))
        assertThat(actor.channel.isClosedForSend, IsEqual(false))
        expect(2)
        yield() // to actor code
        assertThat(actor.isActive, IsEqual(false))
        assertThat(actor.isCompleted, IsEqual(true))
        assertThat(actor.channel.isClosedForSend, IsEqual(true))
        finish(4)
    }

    @Test
    fun testOne() = runBlocking<Unit> {
        expect(1)
        val actor = actor<String>(coroutineContext) {
            expect(3)
            assertThat(receive(), IsEqual("OK"))
            expect(6)
        }
        assertThat(actor.isActive, IsEqual(true))
        assertThat(actor.isCompleted, IsEqual(false))
        assertThat(actor.channel.isClosedForSend, IsEqual(false))
        expect(2)
        yield() // to actor code
        assertThat(actor.isActive, IsEqual(true))
        assertThat(actor.isCompleted, IsEqual(false))
        assertThat(actor.channel.isClosedForSend, IsEqual(false))
        expect(4)
        // send message to actor
        actor.send("OK")
        expect(5)
        yield() // to actor code
        assertThat(actor.isActive, IsEqual(false))
        assertThat(actor.isCompleted, IsEqual(true))
        assertThat(actor.channel.isClosedForSend, IsEqual(true))
        finish(7)
    }
}