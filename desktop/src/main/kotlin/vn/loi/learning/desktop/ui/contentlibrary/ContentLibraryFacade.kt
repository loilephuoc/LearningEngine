package vn.loi.learning.desktop.ui.contentlibrary

import java.nio.file.Path
import java.util.UUID
import vn.loi.learning.application.contentlibrary.AttachPackageToLibraryCollectionCommand
import vn.loi.learning.application.contentlibrary.CreateLibraryCollectionCommand
import vn.loi.learning.application.contentlibrary.DeleteLibraryCollectionCommand
import vn.loi.learning.application.contentlibrary.DetachPackageFromLibraryCollectionCommand
import vn.loi.learning.application.contentlibrary.RenameLibraryCollectionCommand
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.application.contentpackaging.PackageImportProgressListener
import vn.loi.learning.application.contentpackaging.PackageImportOutcome
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Presentation facade dành cho Content Library.
 *
 * Facade:
 * - chỉ gọi Application Service;
 * - chuyển Application DTO thành Presentation model;
 * - không truy cập repository trực tiếp;
 * - không chứa Compose code.
 */
class ContentLibraryFacade(
    private val applicationContext: LearningApplicationContext
) {

    fun load(): ContentLibraryUiState {
        val packages =
            applicationContext
                .installedPackages
                .query()
                .map { installedPackage ->
                    ContentLibraryPackageItem(
                        id = installedPackage.id,
                        name = installedPackage.name,
                        version = installedPackage.version,
                        format = installedPackage.format,
                        libraryCount =
                            installedPackage.libraryCount
                    )
                }

        val packagesById =
            packages.associateBy { contentPackage ->
                contentPackage.id
            }

        val libraries =
            applicationContext
                .contentLibraries
                .query()
                .map { library ->
                    val collections =
                        applicationContext
                            .libraryCollections
                            .query(
                                ContentLibraryId(
                                    library.id
                                )
                            )
                            .map { collection ->
                                ContentLibraryCollectionItem(
                                    id = collection.id,
                                    libraryId =
                                        collection.libraryId,
                                    name = collection.name,
                                    attachedPackages =
                                        collection.packageIds
                                            .map { packageId ->
                                                val contentPackage =
                                                    packagesById[
                                                        packageId
                                                    ]

                                                ContentLibraryAttachedPackageItem(
                                                    id = packageId,
                                                    name =
                                                        contentPackage
                                                            ?.name
                                                            ?: packageId,
                                                    version =
                                                        contentPackage
                                                            ?.version
                                                            ?: "",
                                                    format =
                                                        contentPackage
                                                            ?.format
                                                            ?: ""
                                                )
                                            }
                                            .sortedWith(
                                                compareBy(
                                                    { attachedPackage ->
                                                        attachedPackage
                                                            .name
                                                            .lowercase()
                                                    },
                                                    { attachedPackage ->
                                                        attachedPackage.id
                                                    }
                                                )
                                            )
                                )
                            }

                    ContentLibraryItem(
                        id = library.id,
                        name = library.name,
                        contentCount = library.contentCount,
                        learningItemCount =
                            library.learningItemCount,
                        collections = collections
                    )
                }

        return ContentLibraryUiState(
            packages = packages,
            libraries = libraries
        )
    }

    fun createCollection(
        libraryId: String,
        name: String
    ): ContentLibraryCollectionItem {
        val collection =
            applicationContext
                .createLibraryCollection
                .execute(
                    CreateLibraryCollectionCommand(
                        collectionId =
                            LibraryCollectionId(
                                UUID.randomUUID()
                                    .toString()
                            ),
                        libraryId =
                            ContentLibraryId(
                                libraryId
                            ),
                        name = name
                    )
                )

        return collection.toUiItem()
    }

    fun renameCollection(
        collectionId: String,
        name: String
    ): ContentLibraryCollectionItem {
        val collection =
            applicationContext
                .renameLibraryCollection
                .execute(
                    RenameLibraryCollectionCommand(
                        collectionId =
                            LibraryCollectionId(
                                collectionId
                            ),
                        name = name
                    )
                )

        return collection.toUiItem()
    }

    fun attachPackageToCollection(
        collectionId: String,
        packageId: String
    ): ContentLibraryCollectionItem {
        val collection =
            applicationContext
                .attachPackageToLibraryCollection
                .execute(
                    AttachPackageToLibraryCollectionCommand(
                        collectionId =
                            LibraryCollectionId(
                                collectionId
                            ),
                        packageId =
                            PackageId(
                                packageId
                            )
                    )
                )

        return collection.toUiItem()
    }

    fun detachPackageFromCollection(
        collectionId: String,
        packageId: String
    ): ContentLibraryCollectionItem {
        val collection =
            applicationContext
                .detachPackageFromLibraryCollection
                .execute(
                    DetachPackageFromLibraryCollectionCommand(
                        collectionId =
                            LibraryCollectionId(
                                collectionId
                            ),
                        packageId =
                            PackageId(
                                packageId
                            )
                    )
                )

        return collection.toUiItem()
    }

    fun deleteCollection(
        collectionId: String
    ) {
        applicationContext
            .deleteLibraryCollection
            .execute(
                DeleteLibraryCollectionCommand(
                    collectionId =
                        LibraryCollectionId(
                            collectionId
                        )
                )
            )
    }

    fun importFromDirectory(
        directory: Path,
        progressListener: PackageImportProgressListener? = null,
        cancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal? = null
    ): ContentLibraryImportResult {
        val service = if (progressListener == null) {
            applicationContext.packageImporter(directory)
        } else {
            applicationContext.packageImporterWithProgress(directory, progressListener)
        }
        val batchResult = service.importAllDetailed(
            catalogId = PackageCatalogId(DEFAULT_CATALOG_ID),
            cancellationSignal = cancellationSignal
        )

        val alreadyInstalled = batchResult.successfulImports.filter { result ->
            result.lifecycleOutcome is PackageImportOutcome.AlreadyInstalledIdentical
        }
        val results = batchResult.successfulImports - alreadyInstalled.toSet()

        return ContentLibraryImportResult(
            discoveredPackageCount =
                batchResult.discoveredPackageCount,
            importedPackageCount = results.size,
            importedLibraryCount =
                results.sumOf { result ->
                    result.importedLibraryCount
                },
            importedContentCount =
                results.sumOf { result ->
                    result.importedContentCount
                },
            importedLearningItemCount =
                results.sumOf { result ->
                    result.importedLearningItemCount
                },
            failures =
                batchResult.failures.map { failure ->
                    ContentLibraryImportFailure(
                        source = failure.source,
                        message = failure.message
                    )
                } + alreadyInstalled.map { result ->
                    ContentLibraryImportFailure(
                        source = result.source ?: result.contentPackage.descriptor.name,
                        message = "CONTENT_ID_ALREADY_INSTALLED: Package identity is already installed."
                    )
                }
        )
    }

    fun removeInstalledPackage(packageId: String) {
        val catalogId = vn.loi.learning.domain.content.packaging.model.PackageCatalogId(DEFAULT_CATALOG_ID)
        val pkgId = vn.loi.learning.domain.content.packaging.model.PackageId(packageId)
        val uninstallUseCase = applicationContext.uninstallContentPackage
        uninstallUseCase?.execute(
            vn.loi.learning.application.contentpackaging.UninstallContentPackageCommand(
                catalogId = catalogId,
                packageId = pkgId
            )
        )
    }

    fun getPackageLifecycleState(sourceName: String): String? {
        val defaultLibId = applicationContext.defaultLibraryId ?: return null
        val tree = applicationContext.libraryQuery?.getNavigationTree(defaultLibId) ?: return null
        val norm = sourceName.replace(" ", "").lowercase()
        if (norm.isBlank()) return null

        val archivedMatch = tree.archivedPackages.firstOrNull { pkg ->
            val pNorm = pkg.name.replace(" ", "").lowercase()
            pNorm == norm || pNorm.contains(norm) || norm.contains(pNorm)
        }
        if (archivedMatch != null) return "ARCHIVED"

        val activeMatch = tree.activePackages.firstOrNull { pkg ->
            val pNorm = pkg.name.replace(" ", "").lowercase()
            pNorm == norm || pNorm.contains(norm) || norm.contains(pNorm)
        } ?: tree.installedPackages.firstOrNull { pkg ->
            val pNorm = pkg.name.replace(" ", "").lowercase()
            pNorm == norm || pNorm.contains(norm) || norm.contains(pNorm)
        }
        if (activeMatch != null) return "ACTIVE"

        return null
    }

    fun exportPackage(
        installedPackageId: String,
        destinationPath: Path,
        progressListener: vn.loi.learning.application.contentpackaging.export.PackageExportProgressListener? = null
    ): vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult {
        val useCase = applicationContext.exportContentPackage
            ?: throw IllegalStateException("ExportContentPackageUseCase is not configured in application context.")

        return useCase.execute(
            vn.loi.learning.application.contentpackaging.export.ExportContentPackageCommand(
                installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(installedPackageId),
                destinationPath = destinationPath,
                overwrite = true,
                progressListener = progressListener
            )
        )
    }

    private fun vn.loi.learning.domain.content.library.model.LibraryCollection
            .toUiItem(): ContentLibraryCollectionItem =
        ContentLibraryCollectionItem(
            id = id.value,
            libraryId = libraryId.value,
            name = name,
            attachedPackages = emptyList()
        )

    private companion object {

        const val DEFAULT_CATALOG_ID =
            "desktop-content-library"
    }
}
