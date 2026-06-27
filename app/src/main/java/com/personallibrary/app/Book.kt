package com.personallibrary.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["category_id"])]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "book_id")
    val bookId: Int = 0,
    @ColumnInfo(name = "isbn_13")
    val isbn13: String?,
    @ColumnInfo(name = "isbn_10")
    val isbn10: String?,
    val title: String,
    val subtitle: String?,
    val publisher: String?,
    @ColumnInfo(name = "published_date")
    val publishedDate: String?,
    val description: String?,
    @ColumnInfo(name = "page_count")
    val pageCount: Int?,
    @ColumnInfo(name = "average_rating")
    val averageRating: Float?,
    val thumbnail: String?,
    @ColumnInfo(name = "category_id")
    val categoryId: Int?
)
