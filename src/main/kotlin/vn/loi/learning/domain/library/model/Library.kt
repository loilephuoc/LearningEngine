package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainMutationResult
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.event.LibraryCreatedEvent
import vn.loi.learning.domain.library.event.PackageInstalledEvent
import vn.loi.learning.domain.library.event.PackageRemovedEvent
import vn.loi.learning.domain.library.event.PackageRemovedFromCollectionEvent

/**
 * Kết quả điều phối việc gỡ bỏ gói nội dung khỏi Library và làm sạch tham chiếu trong các Collection.
 */
data class PackageRemovalResult(
    val libraryMutation: DomainMutationResult<Library, PackageRemovedEvent>,
    val collectionMutations: List<DomainMutationResult<Collection, PackageRemovedFromCollectionEvent>>
)

/**
 * Aggregate Root đại diện cho danh mục Library của người học.
 */
class Library internal constructor(
    val id: LibraryId,
    val name: String,
    val entries: List<LibraryEntry> = emptyList(),
    val createdAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) {
            "Library name must not be blank."
        }
        val ids = entries.map { it.installedPackageId }
        require(ids.size == ids.toSet().size) {
            "Library entries must not contain duplicate InstalledPackageIds."
        }
        val activePackageIds = entries.filter { it.isActive }.map { it.packageId }
        require(activePackageIds.size == activePackageIds.toSet().size) {
            "Library entries contain duplicate active PackageIds."
        }
    }

    fun hasPackage(installedPackageId: InstalledPackageId): Boolean =
        entries.any { it.installedPackageId == installedPackageId }

    fun hasActivePackage(installedPackageId: InstalledPackageId): Boolean =
        entries.any { it.installedPackageId == installedPackageId && it.isActive }

    fun hasActivePackageForPackageId(packageId: PackageId): Boolean =
        entries.any { it.packageId == packageId && it.isActive }

    fun registerPackage(installedPackage: InstalledPackage): DomainMutationResult<Library, PackageInstalledEvent> {
        require(installedPackage.libraryId == id) {
            "Package (${installedPackage.id}) libraryId (${installedPackage.libraryId}) does not match Library ($id)."
        }
        require(installedPackage.isActive) {
            "Cannot register package (${installedPackage.id}) in state ${installedPackage.state}. Package must be ACTIVE."
        }
        check(!hasPackage(installedPackage.id)) {
            "Package (${installedPackage.id}) is already registered in Library ($id)."
        }
        check(!hasActivePackageForPackageId(installedPackage.packageId)) {
            "Library ($id) already has an ACTIVE InstalledPackage for PackageId (${installedPackage.packageId})."
        }

        val entry = LibraryEntry(
            installedPackageId = installedPackage.id,
            packageId = installedPackage.packageId,
            state = installedPackage.state,
            registeredAt = installedPackage.installedAt
        )
        val updated = copy(entries = entries + entry)
        val event = PackageInstalledEvent(
            installedPackageId = installedPackage.id,
            libraryId = id,
            packageId = installedPackage.packageId,
            topicId = installedPackage.topicId,
            version = installedPackage.version,
            occurredAt = installedPackage.installedAt
        )
        return DomainMutationResult(updated, event)
    }

    fun unregisterPackage(installedPackageId: InstalledPackageId): DomainMutationResult<Library, PackageRemovedEvent> {
        val entry = entries.firstOrNull { it.installedPackageId == installedPackageId }
        checkNotNull(entry) {
            "Package ($installedPackageId) is not registered in Library ($id)."
        }

        val updatedEntries = entries.filterNot { it.installedPackageId == installedPackageId }
        val updated = copy(entries = updatedEntries)
        val event = PackageRemovedEvent(
            installedPackageId = installedPackageId,
            libraryId = id,
            packageId = entry.packageId
        )
        return DomainMutationResult(updated, event)
    }

    fun removePackageAndCleanCollections(
        installedPackageId: InstalledPackageId,
        collections: List<Collection>
    ): PackageRemovalResult {
        val libraryMutation = unregisterPackage(installedPackageId)
        val collectionMutations = collections
            .filter { it.libraryId == id && it.containsPackage(installedPackageId) }
            .map { it.removePackage(installedPackageId) }
        return PackageRemovalResult(libraryMutation, collectionMutations)
    }

    fun validateCollectionNameUnique(name: CollectionName, existingCollections: List<Collection>) {
        val duplicateExists = existingCollections.any { it.libraryId == id && it.name == name }
        require(!duplicateExists) {
            "Collection with name '${name.trimmedValue}' already exists in Library ($id)."
        }
    }

    private fun copy(
        name: String = this.name,
        entries: List<LibraryEntry> = this.entries
    ): Library = Library(
        id = id,
        name = name,
        entries = entries,
        createdAt = createdAt
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Library) return false
        return id == other.id &&
                name == other.name &&
                entries == other.entries &&
                createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + entries.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String =
        "Library(id=$id, name='$name', entriesCount=${entries.size})"

    companion object {
        fun create(
            id: LibraryId,
            name: String,
            createdAt: Instant = Instant.now()
        ): DomainMutationResult<Library, LibraryCreatedEvent> {
            val library = Library(
                id = id,
                name = name,
                entries = emptyList(),
                createdAt = createdAt
            )
            val event = LibraryCreatedEvent(
                libraryId = id,
                name = name,
                occurredAt = createdAt
            )
            return DomainMutationResult(library, event)
        }
    }
}
