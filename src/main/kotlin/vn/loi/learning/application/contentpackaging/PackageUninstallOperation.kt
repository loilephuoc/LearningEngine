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
    private val packageCatalogRepository: PackageCatalogRepository,
    private val removalDependencyGuard:
    PackageRemovalDependencyGuard =
        PackageRemovalDependencyGuard(
            contentPackageRepository
        )
) {

    fun execute(
        command: UninstallContentPackageCommand
    ) {
        val catalog =
            packageCatalogRepository.findById(
                command.catalogId
            ) ?: return

        if (!catalog.contains(command.packageId)) {
            return
        }

        removalDependencyGuard.ensureCanRemove(
            command.packageId
        )

        val contentPackage =
            contentPackageRepository.findById(
                command.packageId
            )

        val packageLibraryIds =
            contentPackage
                ?.libraryIds
                .orEmpty()

        val sharedLibraryIds =
            contentPackageRepository
                .findAll()
                .asSequence()
                .filter { installedPackage ->
                    installedPackage.id !=
                            command.packageId
                }
                .flatMap { installedPackage ->
                    installedPackage.libraryIds.asSequence()
                }
                .toSet()

        val removableLibraryIds =
            packageLibraryIds -
                    sharedLibraryIds

        val allLibraries =
            contentLibraryRepository.findAll()

        val removableLibraries =
            allLibraries.filter { library ->
                library.id in
                        removableLibraryIds
            }

        val candidateContentIds =
            removableLibraries
                .asSequence()
                .flatMap { library ->
                    library.contentIds.asSequence()
                }
                .toSet()

        val preservedContentIds =
            allLibraries
                .asSequence()
                .filter { library ->
                    library.id !in
                            removableLibraryIds
                }
                .flatMap { library ->
                    library.contentIds.asSequence()
                }
                .toSet()

        val removableContentIds =
            candidateContentIds -
                    preservedContentIds

        learningItemRepository.deleteByContentIds(
            removableContentIds
        )

        contentRepository.deleteAllById(
            removableContentIds
        )

        contentLibraryRepository.deleteAllById(
            removableLibraryIds
        )

        val updatedCatalog =
            catalog.remove(
                command.packageId
            )

        packageCatalogRepository.save(
            updatedCatalog
        )

        contentPackageRepository.deleteById(
            command.packageId
        )
    }
}