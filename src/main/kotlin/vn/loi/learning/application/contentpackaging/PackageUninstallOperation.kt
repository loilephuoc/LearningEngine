package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.PackageCatalogRepository

import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

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
        ),
    private val installedPackageRepository: InstalledPackageRepository? = null,
    private val libraryRepository: LibraryRepository? = null,
    private val collectionRepository: CollectionRepository? = null
) {

    fun execute(
        command: UninstallContentPackageCommand
    ) {
        val catalog = packageCatalogRepository.findById(command.catalogId)

        val allInstPkgs = installedPackageRepository?.findAll().orEmpty()

        val matchingInstPkgs = allInstPkgs.filter { instPkg ->
            instPkg.packageId == command.packageId
        }

        val contentPackage = contentPackageRepository.findById(command.packageId)

        if (contentPackage != null) {
            removalDependencyGuard.ensureCanRemove(
                command.packageId
            )
        }

        val packageLibraryIds = mutableSetOf<vn.loi.learning.domain.content.library.model.ContentLibraryId>()

        if (contentPackage != null) {
            packageLibraryIds.addAll(contentPackage.libraryIds)
        }

        val sharedLibraryIds = mutableSetOf<vn.loi.learning.domain.content.library.model.ContentLibraryId>()

        contentPackageRepository
            .findAll()
            .asSequence()
            .filter { otherPackage ->
                otherPackage.id != command.packageId
            }
            .flatMap { otherPackage ->
                otherPackage.libraryIds.asSequence()
            }
            .forEach { sharedLibraryIds.add(it) }

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

        if (catalog != null && catalog.contains(command.packageId)) {
            val updatedCatalog = catalog.remove(command.packageId)
            packageCatalogRepository.save(updatedCatalog)
        }

        contentPackageRepository.deleteById(
            command.packageId
        )

        if (installedPackageRepository != null) {
            for (matchingInstPkg in matchingInstPkgs) {
                installedPackageRepository.delete(matchingInstPkg.id)

                if (libraryRepository != null) {
                    val lib = libraryRepository.findById(matchingInstPkg.libraryId)
                    if (lib != null && lib.hasPackage(matchingInstPkg.id)) {
                        val updatedLib = lib.unregisterEntry(matchingInstPkg.id)
                        libraryRepository.save(updatedLib)
                    }
                }

                if (collectionRepository != null) {
                    val cols = collectionRepository.findAllByLibraryId(matchingInstPkg.libraryId)
                    for (col in cols) {
                        if (col.containsPackage(matchingInstPkg.id)) {
                            val removeResult = col.removePackage(matchingInstPkg.id)
                            collectionRepository.save(removeResult.aggregate)
                        }
                    }
                }
            }
        }
    }
}