package vn.loi.learning.application.contentpackaging

/**
 * Báo cáo kết quả import của một package.
 *
 * Package Import 2.0 sẽ mở rộng dần model này để bổ sung:
 * - duplicate detection;
 * - warning list;
 * - statistics;
 * - progress information.
 *
 * Hiện tại đây là placeholder để ổn định API trước khi bổ sung chức năng.
 */
data class PackageImportReport(
    val duplicateCount: Int = 0,
    val duplicates: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val statistics: PackageImportStatistics = PackageImportStatistics()
)



