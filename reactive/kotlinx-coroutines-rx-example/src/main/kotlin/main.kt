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


import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.rx1.awaitSingle
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import rx.Observable

interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    fun contributors(
            @Path("owner") owner: String,
            @Path("repo") repo: String
    ): Observable<List<Contributor>>

    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Observable<List<Repo>>
}

data class Contributor(val login: String, val contributions: Int)
data class Repo(val name: String)

fun main(args: Array<String>) {
    val retrofit = Retrofit.Builder().apply {
        baseUrl("https://api.github.com")
        addConverterFactory(GsonConverterFactory.create())
        addCallAdapterFactory(RxJavaCallAdapterFactory.create())
    }.build()

    val github = retrofit.create(GitHub::class.java)

    launch(CommonPool) {
        val contributors =
                github.contributors("JetBrains", "Kotlin")
                      .awaitSingle().take(10)

        for ((name, contributions) in contributors) {
            println("$name has $contributions contributions, other repos: ")

            val otherRepos =
                    github.listRepos(name).awaitSingle()
                          .map(Repo::name).joinToString(", ")

            println(otherRepos)
        }
    }
}
