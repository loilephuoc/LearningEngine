package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage


/**
 * Kết quả của một lần import package.
 *
 * Chỉ là DTO của Application Layer,
 * không chứa business logic.
 */
data class PackageImportResult(
    val contentPackage: ContentPackage,
    val importedLibraryCount: Int,
    val importedContentCount: Int,
    val importedLearningItemCount: Int,
    val report: PackageImportReport = PackageImportReport(),
    val warnings: List<String> = emptyList()
)


