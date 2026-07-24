package vn.loi.learning.application.library.query

import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Cấu trúc cây phân cấp chỉ đọc đại diện cho toàn bộ Library Navigation Hierarchy.
 */
data class LibraryNavigationTree(
    val libraryId: LibraryId,
    val libraryName: String,
    val installedPackages: List<InstalledPackageSummary>,
    val collections: List<CollectionNode>,
    val activePackages: List<InstalledPackageSummary>,
    val archivedPackages: List<InstalledPackageSummary>,
    val deletedCollections: List<CollectionSummary>,
    val statistics: LibraryStatistics,
    val activePackageId: InstalledPackageId? = null
)
