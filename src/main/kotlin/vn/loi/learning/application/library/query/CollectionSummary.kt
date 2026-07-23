package vn.loi.learning.application.library.query

import java.time.Instant
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Immutable DTO đại diện cho bản tóm tắt Collection trong Library read model.
 */
data class CollectionSummary(
    val id: CollectionId,
    val libraryId: LibraryId,
    val name: String,
    val description: String,
    val state: CollectionState,
    val createdAt: Instant,
    val assignedPackageIds: Set<InstalledPackageId>,
    val assignedPackagesCount: Int
) {
    val isActive: Boolean get() = state == CollectionState.ACTIVE
    val isDeleted: Boolean get() = state == CollectionState.DELETED
}
