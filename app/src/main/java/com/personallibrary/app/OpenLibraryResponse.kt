package com.personallibrary.app

import com.google.gson.annotations.SerializedName

data class OpenLibraryResponse(
    val title: String?,
    val authors: List<Author>?,
    val publishers: List<Publisher>?,
    @SerializedName("publish_date")
    val publishDate: String?,
    @SerializedName("number_of_pages")
    val pageCount: Int?,
    val cover: Cover?,
    val description: Any?,
    val identifiers: Identifiers?
) {
    data class Author(val name: String)
    data class Publisher(val name: String)
    data class Cover(val small: String?, val medium: String?, val large: String?)
    data class Identifiers(
        @SerializedName("isbn_10")
        val isbn10: List<String>?,
        @SerializedName("isbn_13")
        val isbn13: List<String>?
    )
}
