package com.personallibrary.app

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    @SerializedName("items")
    val items: List<BookItem>?
) {
    data class BookItem(
        @SerializedName("id")
        val id: String,
        @SerializedName("volumeInfo")
        val volumeInfo: VolumeInfo
    )

    data class VolumeInfo(
        @SerializedName("title")
        val title: String?,
        @SerializedName("subtitle")
        val subtitle: String?,
        @SerializedName("authors")
        val authors: List<String>?,
        @SerializedName("publisher")
        val publisher: String?,
        @SerializedName("publishedDate")
        val publishedDate: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("industryIdentifiers")
        val industryIdentifiers: List<IndustryIdentifier>?,
        @SerializedName("pageCount")
        val pageCount: Int?,
        @SerializedName("categories")
        val categories: List<String>?,
        @SerializedName("averageRating")
        val averageRating: Float?,
        @SerializedName("imageLinks")
        val imageLinks: ImageLinks?
    )

    data class IndustryIdentifier(
        @SerializedName("type")
        val type: String?,
        @SerializedName("identifier")
        val identifier: String?
    )

    data class ImageLinks(
        @SerializedName("smallThumbnail")
        val smallThumbnail: String?,
        @SerializedName("thumbnail")
        val thumbnail: String?
    )
}
