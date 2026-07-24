package vn.loi.learning.application.contentpackaging

/**
 * Port dùng để import dữ liệu nội dung từ một package source.
 *
 * Implementation cụ thể có thể đọc OPD3, ZIP, JSON
 * hoặc nguồn package khác ở Infrastructure Layer.
 */
fun interface PackageContentImporter {

    fun importContent(
        candidate: PackageScanCandidate,
        progressListener: ((event: PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): ImportedPackageContent

    fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent = importContent(candidate, null, null)

    companion object {
        operator fun invoke(block: (PackageScanCandidate) -> ImportedPackageContent): PackageContentImporter =
            PackageContentImporter { candidate, _, _ -> block(candidate) }
    }
}
