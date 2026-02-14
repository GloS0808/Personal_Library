package com.personallibrary.app.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_authors",
    primaryKeys = ["book_id", "author_id"],
    indices = [Index(value = ["author_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["book_id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Author::class,
            parentColumns = ["author_id"],
            childColumns = ["author_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookAuthor(
    @ColumnInfo(name = "book_id")
    val bookId: Int,
    @ColumnInfo(name = "author_id")
    val authorId: Int
)
