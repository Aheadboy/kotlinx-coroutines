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

package kotlinx.coroutines.experimental.rx1

import rx.Observable
import rx.Single

fun <T> checkSingleValue(
    observable: Observable<T>,
    checker: (T) -> Unit
) {
    val singleValue = observable.toBlocking().single()
    checker(singleValue)
}

fun checkErroneous(
        observable: Observable<*>,
        checker: (Throwable) -> Unit
) {
    val singleNotification = observable.materialize().toBlocking().single()
    checker(singleNotification.throwable)
}

fun <T> checkSingleValue(
    single: Single<T>,
    checker: (T) -> Unit
) {
    val singleValue = single.toBlocking().value()
    checker(singleValue)
}

fun checkErroneous(
    single: Single<*>,
    checker: (Throwable) -> Unit
) {
    try {
        single.toBlocking().value()
        error("Should have failed")
    } catch (e: Throwable) {
        checker(e)
    }
}

