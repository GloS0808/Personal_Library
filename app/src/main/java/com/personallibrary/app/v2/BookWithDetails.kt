package com.personallibrary.app.v2

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// Combined view of book with all its related data
data class BookWithDetails(
    @Embedded val book: Book,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: Category?,

    @Relation(
        parentColumn = "book_id",
        entityColumn = "book_id",
        entity = UserBook::class
    )
    val userBook: UserBook?,

    @Relation(
        parentColumn = "book_id",
        entityColumn = "author_id",
        associateBy = Junction(
            value = BookAuthor::class,
            parentColumn = "book_id",
            entityColumn = "author_id"
        )
    )
    val authors: List<Author>
)
