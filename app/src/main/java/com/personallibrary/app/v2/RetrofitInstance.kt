package com.personallibrary.app.v2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GoogleBooksApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: GoogleBooksApi by lazy {
        retrofit.create(GoogleBooksApi::class.java)
    }
}
