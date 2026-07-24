package vn.loi.learning.application.library.query

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

/**
 * Application Read-Side Query Service cho Library Platform (LP-002).
 *
 * Cho phép truy vấn cây điều hướng (Navigation Tree), các gói đã cài đặt (Installed Packages),
 * các gói active/archived, các bộ sưu tập active/deleted, số liệu thống kê (Statistics),
 * mà KHÔNG bao giờ làm biến đổi (mutate) trạng thái các aggregates.
 */
class LibraryQueryService(
    private val libraryRepository: LibraryRepository,
    private val installedPackageRepository: InstalledPackageRepository,
    private val collectionRepository: CollectionRepository
) {

    /**
     * Lấy toàn bộ cây điều hướng LibraryNavigationTree cho một Library.
     */
    fun getNavigationTree(libraryId: LibraryId): LibraryNavigationTree? {
        val library = libraryRepository.findById(libraryId) ?: return null
        val entryOrderMap = library.entries.mapIndexed { index, entry -> entry.installedPackageId to index }.toMap()
        val packageComparator = compareBy<InstalledPackageSummary>(
            { entryOrderMap[it.id] ?: Int.MAX_VALUE },
            { it.name.lowercase() },
            { it.id.value }
        )

        val allPackages = installedPackageRepository.findAllByLibraryId(libraryId)
            .map { it.toSummary() }
            .sortedWith(packageComparator)

        val activePackages = allPackages.filter { it.isActive }
        val archivedPackages = allPackages.filter { it.isArchived }
        val installedPackages = allPackages.filterNot { it.isRemoved }

        val activePackagesMap = activePackages.associateBy { it.id }

        val collections = collectionRepository.findAllByLibraryId(libraryId)
        val activeCollections = collections
            .filter { it.isActive }
            .sortedWith(compareBy({ it.name.value.lowercase() }, { it.id.value }))
            .map { col ->
                val summary = col.toSummary()
                val assigned = summary.assignedPackageIds
                    .mapNotNull { activePackagesMap[it] }
                    .sortedWith(packageComparator)
                CollectionNode(collection = summary, assignedPackages = assigned)
            }

        val deletedCollections = collections
            .filter { it.isDeleted }
            .map { it.toSummary() }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value }))

        val stats = computeStatistics(libraryId, allPackages, collections)

        val sanitizedActivePackageId = library.activePackageId?.takeIf { candidateId ->
            activePackages.any { it.id == candidateId }
        }

        return LibraryNavigationTree(
            libraryId = library.id,
            libraryName = library.name,
            installedPackages = installedPackages,
            collections = activeCollections,
            activePackages = activePackages,
            archivedPackages = archivedPackages,
            deletedCollections = deletedCollections,
            statistics = stats,
            activePackageId = sanitizedActivePackageId
        )
    }

    /**
     * Lấy tất cả các gói đã cài đặt (không bị REMOVED) thuộc Library.
     */
    fun getInstalledPackages(libraryId: LibraryId): List<InstalledPackageSummary> {
        val library = libraryRepository.findById(libraryId)
        val entryOrderMap = library?.entries?.mapIndexed { index, entry -> entry.installedPackageId to index }?.toMap() ?: emptyMap()
        val packageComparator = compareBy<InstalledPackageSummary>(
            { entryOrderMap[it.id] ?: Int.MAX_VALUE },
            { it.name.lowercase() },
            { it.id.value }
        )
        return installedPackageRepository.findAllByLibraryId(libraryId)
            .filterNot { it.isRemoved }
            .map { it.toSummary() }
            .sortedWith(packageComparator)
    }

    /**
     * Lấy tất cả các gói có trạng thái ACTIVE thuộc Library.
     */
    fun getActivePackages(libraryId: LibraryId): List<InstalledPackageSummary> {
        val library = libraryRepository.findById(libraryId)
        val entryOrderMap = library?.entries?.mapIndexed { index, entry -> entry.installedPackageId to index }?.toMap() ?: emptyMap()
        val packageComparator = compareBy<InstalledPackageSummary>(
            { entryOrderMap[it.id] ?: Int.MAX_VALUE },
            { it.name.lowercase() },
            { it.id.value }
        )
        return installedPackageRepository.findAllByLibraryIdAndState(libraryId, PackageState.ACTIVE)
            .map { it.toSummary() }
            .sortedWith(packageComparator)
    }

    /**
     * Lấy tất cả các gói có trạng thái ARCHIVED thuộc Library.
     */
    fun getArchivedPackages(libraryId: LibraryId): List<InstalledPackageSummary> {
        val library = libraryRepository.findById(libraryId)
        val entryOrderMap = library?.entries?.mapIndexed { index, entry -> entry.installedPackageId to index }?.toMap() ?: emptyMap()
        val packageComparator = compareBy<InstalledPackageSummary>(
            { entryOrderMap[it.id] ?: Int.MAX_VALUE },
            { it.name.lowercase() },
            { it.id.value }
        )
        return installedPackageRepository.findAllByLibraryIdAndState(libraryId, PackageState.ARCHIVED)
            .map { it.toSummary() }
            .sortedWith(packageComparator)
    }

    /**
     * Lấy danh sách Collection (ACTIVE) dưới dạng CollectionNode chứa danh sách các gói active.
     */
    fun getActiveCollections(libraryId: LibraryId): List<CollectionNode> {
        val activePackagesMap = getActivePackages(libraryId).associateBy { it.id }
        return collectionRepository.findAllByLibraryId(libraryId)
            .filter { it.isActive }
            .sortedWith(compareBy({ it.name.value.lowercase() }, { it.id.value }))
            .map { col ->
                val summary = col.toSummary()
                val assigned = summary.assignedPackageIds
                    .mapNotNull { activePackagesMap[it] }
                    .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value }))
                CollectionNode(collection = summary, assignedPackages = assigned)
            }
    }

    /**
     * Lấy danh sách Collection đã bị xoá (DELETED) thuộc Library.
     */
    fun getDeletedCollections(libraryId: LibraryId): List<CollectionSummary> =
        collectionRepository.findAllByLibraryId(libraryId)
            .filter { it.isDeleted }
            .map { it.toSummary() }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value }))

    /**
     * Lấy số liệu thống kê chỉ đọc (Statistics) của Library.
     */
    fun getStatistics(libraryId: LibraryId): LibraryStatistics? {
        if (!libraryRepository.existsById(libraryId)) return null
        val allPackages = installedPackageRepository.findAllByLibraryId(libraryId).map { it.toSummary() }
        val collections = collectionRepository.findAllByLibraryId(libraryId)
        return computeStatistics(libraryId, allPackages, collections)
    }

    /**
     * Lấy bản tóm tắt của một InstalledPackage dựa theo InstalledPackageId.
     */
    fun getPackageSummary(installedPackageId: InstalledPackageId): InstalledPackageSummary? =
        installedPackageRepository.findById(installedPackageId)?.toSummary()

    /**
     * Lấy thông tin CollectionNode dựa theo CollectionId.
     */
    fun getCollectionNode(collectionId: CollectionId): CollectionNode? {
        val col = collectionRepository.findById(collectionId) ?: return null
        val summary = col.toSummary()
        val activePackagesMap = getActivePackages(col.libraryId).associateBy { it.id }
        val assigned = summary.assignedPackageIds
            .mapNotNull { activePackagesMap[it] }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value }))
        return CollectionNode(collection = summary, assignedPackages = assigned)
    }

    private fun computeStatistics(
        libraryId: LibraryId,
        allPackages: List<InstalledPackageSummary>,
        collections: List<Collection>
    ): LibraryStatistics {
        val activePkgs = allPackages.filter { it.isActive }
        val archivedPkgs = allPackages.filter { it.isArchived }
        val removedPkgs = allPackages.filter { it.isRemoved }

        val activeCols = collections.filter { it.isActive }
        val deletedCols = collections.filter { it.isDeleted }

        return LibraryStatistics(
            libraryId = libraryId,
            totalInstalledPackagesCount = allPackages.size - removedPkgs.size,
            activePackagesCount = activePkgs.size,
            archivedPackagesCount = archivedPkgs.size,
            removedPackagesCount = removedPkgs.size,
            totalCollectionsCount = collections.size,
            activeCollectionsCount = activeCols.size,
            deletedCollectionsCount = deletedCols.size,
            totalActiveContentCount = activePkgs.sumOf { it.contentCount },
            totalActiveLearningItemCount = activePkgs.sumOf { it.learningItemCount }
        )
    }
}
