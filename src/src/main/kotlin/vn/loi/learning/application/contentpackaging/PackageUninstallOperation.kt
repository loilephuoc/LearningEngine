package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.PackageCatalogRepository

class PackageUninstallOperation(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val contentPackageRepository: ContentPackageRepository,
    private val packageCatalogRepository: PackageCatalogRepository
) {

    fun execute(
        command: UninstallContentPackageCommand
    ) {
        val catalog =
            packageCatalogRepository.findById(command.catalogId)
                ?: return

        if (!catalog.contains(command.packageId)) {
            return
        }

        val contentPackage =
            contentPackageRepository.findById(command.packageId)

        val libraryIds =
            contentPackage
                ?.libraryIds
                .orEmpty()
                .toSet()

        val contentIds =
            libraryIds
                .asSequence()
                .mapNotNull(contentLibraryRepository::findById)
                .flatMap { library ->
                    library.contentIds.asSequence()
                }
                .toSet()

        learningItemRepository.deleteByContentIds(
            contentIds
        )

        contentRepository.deleteAllById(
            contentIds
        )

        contentLibraryRepository.deleteAllById(
            libraryIds
        )

        val updatedCatalog =
            catalog.remove(command.packageId)

        packageCatalogRepository.save(
            updatedCatalog
        )

        contentPackageRepository.deleteById(
            command.packageId
        )
    }
}