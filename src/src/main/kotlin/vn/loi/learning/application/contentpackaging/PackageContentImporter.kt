package vn.loi.learning.application.contentpackaging

/**
 * Port dùng để import dữ liệu nội dung từ một package source.
 *
 * Implementation cụ thể có thể đọc OPD3, ZIP, JSON
 * hoặc nguồn package khác ở Infrastructure Layer.
 */
fun interface PackageContentImporter {

    fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent
}
