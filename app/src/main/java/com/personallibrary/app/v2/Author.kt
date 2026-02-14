package com.personallibrary.app.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class Author(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "author_id")
    val authorId: Int = 0,
    val name: String
)
