package com.personallibrary.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_books",
    primaryKeys = ["user_id", "book_id"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Book::class,
            parentColumns = ["book_id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"])]
)
data class UserBook(
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "book_id")
    val bookId: Int,
    val status: String?, // "to_read", "reading", "read"
    @ColumnInfo(name = "user_rating")
    val userRating: Float?,
    @ColumnInfo(name = "current_page")
    val currentPage: Int?,
    val notes: String?,
    @ColumnInfo(name = "started_date")
    val startedDate: String?,
    @ColumnInfo(name = "read_date")
    val readDate: String?
)
