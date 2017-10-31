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

import kotlin.coroutines.experimental.CoroutineContext

/**
 * @suppress **Deprecated**: `Deferred` incorporates functionality of `LazyDeferred`. See [Deferred].
 */
@Deprecated(message = "`Deferred` incorporates functionality of `LazyDeferred`", level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("Deferred"))
typealias LazyDeferred<T> = Deferred<T>

/**
 * @suppress **Deprecated**: Replace with `async(context, start = false) { ... }`. See [async].
 */
@Suppress("DEPRECATION")
@Deprecated(message = "This functionality is incorporated into `async", level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("async(context, start = false, block = block)"))
public fun <T> lazyDefer(context: CoroutineContext, block: suspend CoroutineScope.() -> T) : Deferred<T> =
    async(context, start = false, block = block)
