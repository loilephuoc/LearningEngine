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
        val resolution = resolvePackage(installedPackageId)
        val rawItems = libraryContents.queryForLibraries(resolution.contentLibraryIds)
        ensurePackageExists(installedPackageId, resolution, rawItems)
        return rawItems
    }

    fun getContentDescriptorsForPackage(installedPackageId: InstalledPackageId): List<LibraryContentItem> {
        val resolution = resolvePackage(installedPackageId)
        val rawItems = libraryContents.queryDescriptorsForLibraries(resolution.contentLibraryIds)
        ensurePackageExists(installedPackageId, resolution, rawItems)
        return rawItems
    }

    fun getContentDescriptorsForPackages(
        installedPackageIds: Collection<InstalledPackageId>
    ): Map<InstalledPackageId, Result<List<LibraryContentItem>>> {
        val distinctIds = installedPackageIds.distinct()
        if (distinctIds.isEmpty()) return emptyMap()
        val defaultLibId = defaultLibraryIdSupplier?.invoke()
        val summaries = if (libraryQuery != null && defaultLibId != null) {
            libraryQuery.getInstalledPackages(defaultLibId).associateBy { it.id }
        } else {
            emptyMap()
        }
        val allInstalled = installedPackages.query()
        val resolutions = distinctIds.associateWith { installedPackageId ->
            runCatching {
                val summary = summaries[installedPackageId]
                val pkgItem = if (summary != null) {
                    allInstalled.firstOrNull { it.id == summary.packageId.value }
                        ?: installedPackages.findById(summary.packageId.value)
                        ?: allInstalled.firstOrNull { it.id == installedPackageId.value }
                } else {
                    allInstalled.firstOrNull { it.id == installedPackageId.value }
                }
                val contentLibraryIds = mutableSetOf<ContentLibraryId>()
                pkgItem?.libraryIds?.mapTo(contentLibraryIds, ::ContentLibraryId)
                contentLibraryIds.add(ContentLibraryId(installedPackageId.value))
                pkgItem?.let { contentLibraryIds.add(ContentLibraryId(it.id)) }
                summary?.let { contentLibraryIds.add(ContentLibraryId(it.packageId.value)) }
                PackageResolution(contentLibraryIds, pkgItem != null || summary != null)
            }
        }
        val descriptorGroups = libraryContents.queryDescriptorGroups(
            resolutions.mapNotNull { (id, result) ->
                result.getOrNull()?.let { id.value to it.contentLibraryIds }
            }.toMap()
        )
        val result = resolutions.mapValues { (installedPackageId, resolutionResult) ->
            resolutionResult.mapCatching { resolution ->
                val items = descriptorGroups[installedPackageId.value].orEmpty()
                ensurePackageExists(installedPackageId, resolution, items)
                items
            }
        }
        return result
    }

    private fun resolvePackage(installedPackageId: InstalledPackageId): PackageResolution {
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

        return PackageResolution(contentLibraryIds, pkgItem != null || summary != null)
    }

    private fun ensurePackageExists(
        installedPackageId: InstalledPackageId,
        resolution: PackageResolution,
        items: List<LibraryContentItem>
    ) {
        if (items.isEmpty() && !resolution.packageExists) {
            throw IllegalArgumentException("Package with id '${installedPackageId.value}' not found.")
        }
    }

    private data class PackageResolution(
        val contentLibraryIds: Set<ContentLibraryId>,
        val packageExists: Boolean
    )
}
