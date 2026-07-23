package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainMutationResult
import vn.loi.learning.domain.library.event.CollectionCreatedEvent
import vn.loi.learning.domain.library.event.CollectionDeletedEvent
import vn.loi.learning.domain.library.event.CollectionRenamedEvent
import vn.loi.learning.domain.library.event.PackageAssignedToCollectionEvent
import vn.loi.learning.domain.library.event.PackageRemovedFromCollectionEvent

/**
 * Aggregate Root đại diện cho một bộ sưu tập (Collection) do người học định nghĩa.
 */
class Collection internal constructor(
    val id: CollectionId,
    val libraryId: LibraryId,
    val name: CollectionName,
    val description: String = "",
    val assignedPackageIds: Set<InstalledPackageId> = emptySet(),
    val state: CollectionState = CollectionState.ACTIVE,
    val createdAt: Instant = Instant.now()
) {
    val isActive: Boolean get() = state == CollectionState.ACTIVE
    val isDeleted: Boolean get() = state == CollectionState.DELETED

    fun rename(newName: CollectionName): DomainMutationResult<Collection, CollectionRenamedEvent> {
        check(state == CollectionState.ACTIVE) {
            "Cannot rename collection ($id): collection is DELETED."
        }
        check(this.name != newName) {
            "Collection ($id) already has name '$newName'."
        }
        val oldName = this.name
        val updated = copy(name = newName)
        val event = CollectionRenamedEvent(collectionId = id, oldName = oldName, newName = newName)
        return DomainMutationResult(updated, event)
    }

    fun assignPackage(installedPackage: InstalledPackage, library: Library): DomainMutationResult<Collection, PackageAssignedToCollectionEvent> {
        check(state == CollectionState.ACTIVE) {
            "Cannot assign package to collection ($id): collection is DELETED."
        }
        require(installedPackage.libraryId == library.id) {
            "Package (${installedPackage.id}) belongs to library (${installedPackage.libraryId}), not library (${library.id})."
        }
        require(installedPackage.isActive) {
            "Cannot assign package (${installedPackage.id}) to collection: package state is ${installedPackage.state}, expected ACTIVE."
        }
        require(library.hasActivePackage(installedPackage.id, listOf(installedPackage))) {
            "Cannot assign package (${installedPackage.id}) to collection: package is not active in Library (${library.id})."
        }
        check(!assignedPackageIds.contains(installedPackage.id)) {
            "Package (${installedPackage.id}) is already assigned to collection ($id)."
        }

        val updated = copy(assignedPackageIds = assignedPackageIds + installedPackage.id)
        val event = PackageAssignedToCollectionEvent(collectionId = id, installedPackageId = installedPackage.id)
        return DomainMutationResult(updated, event)
    }

    fun removePackage(installedPackageId: InstalledPackageId): DomainMutationResult<Collection, PackageRemovedFromCollectionEvent> {
        check(state == CollectionState.ACTIVE) {
            "Cannot remove package from collection ($id): collection is DELETED."
        }
        check(assignedPackageIds.contains(installedPackageId)) {
            "Package ($installedPackageId) is not assigned to collection ($id)."
        }
        val updated = copy(assignedPackageIds = assignedPackageIds - installedPackageId)
        val event = PackageRemovedFromCollectionEvent(collectionId = id, installedPackageId = installedPackageId)
        return DomainMutationResult(updated, event)
    }

    fun delete(): DomainMutationResult<Collection, CollectionDeletedEvent> {
        check(state == CollectionState.ACTIVE) {
            "Cannot delete collection ($id): collection is already DELETED."
        }
        val updated = copy(state = CollectionState.DELETED)
        val event = CollectionDeletedEvent(collectionId = id, libraryId = libraryId)
        return DomainMutationResult(updated, event)
    }

    fun containsPackage(packageId: InstalledPackageId): Boolean =
        state == CollectionState.ACTIVE && assignedPackageIds.contains(packageId)

    private fun copy(
        name: CollectionName = this.name,
        description: String = this.description,
        assignedPackageIds: Set<InstalledPackageId> = this.assignedPackageIds,
        state: CollectionState = this.state
    ): Collection = Collection(
        id = id,
        libraryId = libraryId,
        name = name,
        description = description,
        assignedPackageIds = assignedPackageIds,
        state = state,
        createdAt = createdAt
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Collection) return false
        return id == other.id &&
                libraryId == other.libraryId &&
                name == other.name &&
                description == other.description &&
                assignedPackageIds == other.assignedPackageIds &&
                state == other.state &&
                createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + libraryId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + assignedPackageIds.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String =
        "Collection(id=$id, libraryId=$libraryId, name=$name, state=$state, packagesCount=${assignedPackageIds.size})"

    companion object {
        internal fun create(
            id: CollectionId,
            libraryId: LibraryId,
            name: CollectionName,
            description: String = "",
            createdAt: Instant = Instant.now()
        ): DomainMutationResult<Collection, CollectionCreatedEvent> {
            val collection = Collection(
                id = id,
                libraryId = libraryId,
                name = name,
                description = description,
                assignedPackageIds = emptySet(),
                state = CollectionState.ACTIVE,
                createdAt = createdAt
            )
            val event = CollectionCreatedEvent(
                collectionId = id,
                libraryId = libraryId,
                name = name,
                occurredAt = createdAt
            )
            return DomainMutationResult(collection, event)
        }

        /**
         * Reconstitution factory dành riêng cho tái tạo Collection từ persistence layer.
         */
        fun reconstitute(
            id: CollectionId,
            libraryId: LibraryId,
            name: CollectionName,
            description: String = "",
            assignedPackageIds: Set<InstalledPackageId> = emptySet(),
            state: CollectionState = CollectionState.ACTIVE,
            createdAt: Instant = Instant.now()
        ): Collection = Collection(
            id = id,
            libraryId = libraryId,
            name = name,
            description = description,
            assignedPackageIds = assignedPackageIds,
            state = state,
            createdAt = createdAt
        )
    }
}
