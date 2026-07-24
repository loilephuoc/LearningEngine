package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.contentlibrary.LibraryContentItem
import vn.loi.learning.application.contentlibrary.LibraryContentQueryService
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Service có trách nhiệm duy nhất: Resolve danh sách [LibraryContentItem]
 * thuộc về một [InstalledPackageId].
 */
class InstalledPackageContentQueryService(
    private val installedPackages: InstalledPackageQueryService,
    private val libraryContents: LibraryContentQueryService,
    private val libraryQuery: LibraryQueryService? = null,
    private val defaultLibraryIdSupplier: (() -> LibraryId?)? = null
) {

    fun getContentsForPackage(installedPackageId: InstalledPackageId): List<LibraryContentItem> {
        val defaultLibId = defaultLibraryIdSupplier?.invoke()
        val summary = if (libraryQuery != null && defaultLibId != null) {
            libraryQuery.getInstalledPackages(defaultLibId).firstOrNull { it.id == installedPackageId }
        } else null

        val allInstalled = installedPackages.query()
        val pkgItem = if (summary != null) {
            allInstalled.firstOrNull { it.id == summary.packageId.value }
                ?: installedPackages.findById(summary.packageId.value)
                ?: allInstalled.firstOrNull { it.id == installedPackageId.value }
        } else {
            allInstalled.firstOrNull { it.id == installedPackageId.value }
        }

        val contentLibraryIds = mutableSetOf<ContentLibraryId>()
        if (pkgItem != null && pkgItem.libraryIds.isNotEmpty()) {
            contentLibraryIds.addAll(pkgItem.libraryIds.map { ContentLibraryId(it) })
        }
        contentLibraryIds.add(ContentLibraryId(installedPackageId.value))
        if (pkgItem != null) {
            contentLibraryIds.add(ContentLibraryId(pkgItem.id))
        }
        if (summary != null) {
            contentLibraryIds.add(ContentLibraryId(summary.packageId.value))
        }

        val rawItems = libraryContents.queryForLibraries(contentLibraryIds)
        if (rawItems.isEmpty() && pkgItem == null && summary == null) {
            throw IllegalArgumentException("Package with id '${installedPackageId.value}' not found.")
        }

        return rawItems
    }
}
