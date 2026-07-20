package vn.loi.learning.application.contentlibrary

data class LibraryContentItem(
    val id: String,
    val title: String,
    val type: String,
    val primaryText: String,
    val translatedText: String?,
    val learningItemCount: Int
)