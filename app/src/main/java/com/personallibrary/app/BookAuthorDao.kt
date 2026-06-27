package com.personallibrary.app

import androidx.room.*
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface BookAuthorDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookAuthor(bookAuthor: BookAuthor): Long

    @Query("DELETE FROM book_authors WHERE book_id = :bookId")
    suspend fun deleteBookAuthors(bookId: Int): Int
}
