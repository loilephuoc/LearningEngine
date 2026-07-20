package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

class InstalledPackageQueryService(
    private val contentPackageRepository: ContentPackageRepository,
    private val packageCatalogRepository: PackageCatalogRepository
) {

    fun findAll(
        catalogId: PackageCatalogId
    ): List<InstalledPackageSummary> {
        val catalog =
            packageCatalogRepository.findById(catalogId)
                ?: return emptyList()

        return catalog.packageIds
            .mapNotNull(contentPackageRepository::findById)
            .map { contentPackage ->
                InstalledPackageSummary(
                    id = contentPackage.id,
                    name = contentPackage.name,
                    version = contentPackage.version,
                    format = contentPackage.format,
                    libraryCount = contentPackage.libraryCount
                )
            }
            .sortedWith(
                compareBy<InstalledPackageSummary> { summary ->
                    summary.name.lowercase()
                }.thenBy { summary ->
                    summary.version
                }.thenBy { summary ->
                    summary.id.toString()
                }
            )
    }
}
