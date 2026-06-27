package com.personallibrary.app

import androidx.room.*
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface UserBookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBook(userBook: UserBook): Long

    @Update
    suspend fun updateUserBook(userBook: UserBook): Int

    @Query("SELECT * FROM user_books WHERE book_id = :bookId AND user_id = :userId LIMIT 1")
    suspend fun getUserBook(bookId: Int, userId: Int): UserBook?

    @Delete
    suspend fun deleteUserBook(userBook: UserBook): Int
}
