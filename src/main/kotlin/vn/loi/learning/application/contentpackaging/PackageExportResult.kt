package vn.loi.learning.application.contentpackaging

/**
 * Kết quả sau khi export package hoàn tất.
 */
data class PackageExportResult(
    val destination: String,
    val exportedContentCount: Int,
    val exportedLearningItemCount: Int
)
