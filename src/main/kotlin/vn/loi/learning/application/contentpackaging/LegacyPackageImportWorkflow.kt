package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Workflow cấp cao import toàn bộ package legacy tìm thấy
 * bởi LegacyPackageScanner.
 */
class LegacyPackageImportWorkflow(
    private val packageScanner: LegacyPackageScanner,
    private val packageImportService: LegacyPackageImportService
) {

    fun execute(
        catalogId: PackageCatalogId
    ): LegacyPackageImportWorkflowResult {
        val candidates =
            packageScanner.scan()

        val results =
            packageImportService.importAll(
                catalogId = catalogId,
                candidates = candidates
            )

        return LegacyPackageImportWorkflowResult(
            scannedCandidateCount =
                candidates.size,
            importedPackages =
                results
        )
    }
}

data class LegacyPackageImportWorkflowResult(
    val scannedCandidateCount: Int,
    val importedPackages: List<PackageImportResult>
) {

    init {
        require(
            scannedCandidateCount >= 0
        ) {
            "Scanned candidate count must not be negative."
        }

        require(
            importedPackages.size <=
                    scannedCandidateCount
        ) {
            "Imported package count must not exceed scanned candidate count."
        }
    }

    val importedPackageCount: Int
        get() =
            importedPackages.size

    val importedContentCount: Int
        get() =
            importedPackages.sumOf { result ->
                result.importedContentCount
            }

    val importedLearningItemCount: Int
        get() =
            importedPackages.sumOf { result ->
                result.importedLearningItemCount
            }

    val warnings: List<String>
        get() =
            importedPackages.flatMap { result ->
                result.warnings
            }
}