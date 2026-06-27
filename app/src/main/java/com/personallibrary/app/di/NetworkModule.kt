package com.personallibrary.app.di

import com.personallibrary.app.GoogleBooksApi
import com.personallibrary.app.ITBookstoreApi
import com.personallibrary.app.OpenLibraryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApi(okHttpClient: OkHttpClient): GoogleBooksApi {
        return Retrofit.Builder()
            .baseUrl(GoogleBooksApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenLibraryApi(okHttpClient: OkHttpClient): OpenLibraryApi {
        return Retrofit.Builder()
            .baseUrl(OpenLibraryApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideITBookstoreApi(okHttpClient: OkHttpClient): ITBookstoreApi {
        return Retrofit.Builder()
            .baseUrl(ITBookstoreApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITBookstoreApi::class.java)
    }
}
