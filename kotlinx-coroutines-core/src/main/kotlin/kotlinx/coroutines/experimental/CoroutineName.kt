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

import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User-specified name of coroutine. This name is used in debugging mode.
 * See [newCoroutineContext] for the description of coroutine debugging facilities.
 */
public data class CoroutineName(
    /**
     * User-defined coroutine name.
     */
    val name: String
) : AbstractCoroutineContextElement(CoroutineName) {
    /**
     * Key for [CoroutineName] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<CoroutineName>

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String = "CoroutineName($name)"
}
