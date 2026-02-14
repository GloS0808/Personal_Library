package com.personallibrary.app.v2

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): LiveData<List<Book>>

    @Transaction
    @Query("SELECT * FROM books")
    fun getAllBooksWithDetails(): LiveData<List<BookWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM books 
        WHERE title LIKE '%' || :query || '%' 
        OR subtitle LIKE '%' || :query || '%'
    """)
    fun searchBooksWithDetails(query: String): LiveData<List<BookWithDetails>>

    @Transaction
    @Query("SELECT * FROM books WHERE book_id = :bookId")
    suspend fun getBookWithDetails(bookId: Int): BookWithDetails?

    @Query("SELECT * FROM books WHERE isbn_13 = :isbn OR isbn_10 = :isbn LIMIT 1")
    suspend fun getBookByIsbn(isbn: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Delete
    suspend fun deleteBook(book: Book): Int

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%'")
    fun searchBooks(query: String): LiveData<List<Book>>
}
