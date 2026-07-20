package vn.loi.learning.application.contentlibrary

data class ContentLibrarySummary(
    val id: String,
    val name: String,
    val contentCount: Int,
    val learningItemCount: Int
)