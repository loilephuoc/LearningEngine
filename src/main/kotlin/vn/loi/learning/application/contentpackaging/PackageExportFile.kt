package vn.loi.learning.application.contentpackaging

/**
 * File logic sẽ được ghi vào package export.
 *
 * Tất cả các thành phần (manifest, metadata, contents, learning-items...)
 * đều được biểu diễn thống nhất bằng PackageExportFile trước khi Infrastructure
 * ghi ra ZIP hoặc thư mục.
 */
data class PackageExportFile(
    val relativePath: String,
    val content: String
)
