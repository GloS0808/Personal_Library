package com.personallibrary.app

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApi {

    @GET("api/books")
    suspend fun searchBookByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data"
    ): Response<Map<String, OpenLibraryResponse>>

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
    }
}
