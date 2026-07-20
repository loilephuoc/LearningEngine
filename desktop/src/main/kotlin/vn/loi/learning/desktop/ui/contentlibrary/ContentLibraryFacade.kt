package vn.loi.learning.desktop.ui.contentlibrary

import java.nio.file.Path
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
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

        val libraries =
            applicationContext
                .contentLibraries
                .query()
                .map { library ->
                    ContentLibraryItem(
                        id = library.id,
                        name = library.name,
                        contentCount = library.contentCount,
                        learningItemCount = library.learningItemCount
                    )
                }

        return ContentLibraryUiState(
            packages = packages,
            libraries = libraries
        )
    }

    fun importFromDirectory(
        directory: Path
    ): ContentLibraryImportResult {
        val results =
            applicationContext
                .packageImporter(directory)
                .importAll(
                    PackageCatalogId(DEFAULT_CATALOG_ID)
                )

        return ContentLibraryImportResult(
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
                }
        )
    }

    private companion object {

        const val DEFAULT_CATALOG_ID =
            "desktop-content-library"
    }
}