package com.personallibrary.app.v2

import androidx.room.*
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE category_name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long
}
