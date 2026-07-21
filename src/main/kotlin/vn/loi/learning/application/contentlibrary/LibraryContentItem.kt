package vn.loi.learning.application.contentlibrary

data class LibraryContentItem(
    val id: String,
    val title: String,
    val type: String,
    val group: String? = null,
    val section: String? = null,
    val lesson: String? = null,
    val primaryText: String,
    val translatedText: String?,
    val learningItemCount: Int
)