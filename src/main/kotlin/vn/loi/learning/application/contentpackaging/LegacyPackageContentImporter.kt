package vn.loi.learning.application.contentpackaging

/**
 * Port đọc một cặp package legacy:
 *
 * - JSON metadata/content;
 * - PKG media OPD3.
 *
 * Application không phụ thuộc filesystem hoặc binary parser cụ thể.
 */
fun interface LegacyPackageContentImporter {

    fun importContent(
        candidate: LegacyPackageCandidate,
        progressListener: ((event: PackageImportProgressEvent) -> Unit)?,
        cancellationSignal: PackageImportCancellationSignal?
    ): ImportedPackageContent

    fun importContent(
        candidate: LegacyPackageCandidate
    ): ImportedPackageContent = importContent(candidate, null, null)

    companion object {
        operator fun invoke(block: (LegacyPackageCandidate) -> ImportedPackageContent): LegacyPackageContentImporter =
            LegacyPackageContentImporter { candidate, _, _ -> block(candidate) }
    }
}