package vn.loi.learning.application.contentpackaging

/**
 * Số liệu thống kê của một lần import package.
 *
 * Chỉ chứa dữ liệu tổng hợp,
 * không chứa business logic.
 */
data class PackageImportStatistics(
    val importedLibraryCount: Int = 0,
    val importedContentCount: Int = 0,
    val importedLearningItemCount: Int = 0,
    val skippedRecordCount: Int = 0,
    val duplicateCount: Int = 0
)

