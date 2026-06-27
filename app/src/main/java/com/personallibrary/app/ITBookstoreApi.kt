package com.personallibrary.app

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ITBookstoreApi {
    @GET("books/{isbn}")
    suspend fun getBookByIsbn(@Path("isbn") isbn: String): Response<ITBookstoreResponse>

    companion object {
        const val BASE_URL = "https://api.itbook.store/1.0/"
    }
}

data class ITBookstoreResponse(
    val error: String,
    val title: String?,
    val subtitle: String?,
    val authors: String?,
    val publisher: String?,
    val isbn10: String?,
    val isbn13: String?,
    val pages: String?,
    val year: String?,
    val rating: String?,
    val desc: String?,
    val image: String?
)
