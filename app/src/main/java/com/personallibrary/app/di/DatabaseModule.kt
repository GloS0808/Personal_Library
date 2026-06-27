package com.personallibrary.app.di

import android.content.Context
import com.personallibrary.app.AuthorDao
import com.personallibrary.app.BookAuthorDao
import com.personallibrary.app.BookDao
import com.personallibrary.app.BookRepository
import com.personallibrary.app.CategoryDao
import com.personallibrary.app.LibraryDatabase
import com.personallibrary.app.UserBookDao
import com.personallibrary.app.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LibraryDatabase {
        return LibraryDatabase.getDatabase(context)
    }

    @Provides
    fun provideBookDao(database: LibraryDatabase): BookDao = database.bookDao()

    @Provides
    fun provideAuthorDao(database: LibraryDatabase): AuthorDao = database.authorDao()

    @Provides
    fun provideCategoryDao(database: LibraryDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideUserDao(database: LibraryDatabase): UserDao = database.userDao()

    @Provides
    fun provideUserBookDao(database: LibraryDatabase): UserBookDao = database.userBookDao()

    @Provides
    fun provideBookAuthorDao(database: LibraryDatabase): BookAuthorDao = database.bookAuthorDao()

    @Provides
    @Singleton
    fun provideBookRepository(database: LibraryDatabase): BookRepository {
        return BookRepository(database)
    }
}
