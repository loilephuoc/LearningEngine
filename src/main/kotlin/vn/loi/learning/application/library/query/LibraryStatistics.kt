package vn.loi.learning.application.library.query

import vn.loi.learning.domain.library.model.LibraryId

/**
 * Immutable DTO chứa thông tin thống kê chỉ đọc của một Library.
 */
data class LibraryStatistics(
    val libraryId: LibraryId,
    val totalInstalledPackagesCount: Int,
    val activePackagesCount: Int,
    val archivedPackagesCount: Int,
    val removedPackagesCount: Int,
    val totalCollectionsCount: Int,
    val activeCollectionsCount: Int,
    val deletedCollectionsCount: Int,
    val totalActiveContentCount: Int,
    val totalActiveLearningItemCount: Int
)
