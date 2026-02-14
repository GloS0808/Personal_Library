package com.personallibrary.app.v2

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {

    @GET("volumes")
    suspend fun searchBookByIsbn(
        @Query("q") query: String
    ): Response<GoogleBooksResponse>

    companion object {
        const val BASE_URL = "https://www.googleapis.com/books/v1/"
    }
}
