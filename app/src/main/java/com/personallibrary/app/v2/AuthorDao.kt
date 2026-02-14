package com.personallibrary.app.v2

import androidx.room.*
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface AuthorDao {

    @Query("SELECT * FROM authors WHERE name = :name LIMIT 1")
    suspend fun getAuthorByName(name: String): Author?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuthor(author: Author): Long
}
