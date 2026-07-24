package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainMutationResult
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.event.LibraryCreatedEvent

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
    }

    fun hasPackage(installedPackageId: InstalledPackageId): Boolean =
        entries.any { it.installedPackageId == installedPackageId }

    fun hasActivePackage(installedPackageId: InstalledPackageId, installedPackages: List<InstalledPackage>): Boolean {
        if (!hasPackage(installedPackageId)) return false
        val pkg = installedPackages.firstOrNull { it.id == installedPackageId } ?: return false
        return pkg.isActive
    }

    fun hasActivePackageForPackageId(packageId: PackageId, installedPackages: List<InstalledPackage>): Boolean {
        val registeredPackageIds = entries.map { it.installedPackageId }.toSet()
        return installedPackages.any {
            registeredPackageIds.contains(it.id) && it.packageId == packageId && it.isActive
        }
    }

    fun validateSingleActiveVersion(packageId: PackageId, installedPackages: List<InstalledPackage>) {
        val activeExists = hasActivePackageForPackageId(packageId, installedPackages)
        check(!activeExists) {
            "Library ($id) already has an ACTIVE InstalledPackage for PackageId ($packageId)."
        }
    }

    fun validateCollectionNameUnique(name: CollectionName, existingCollections: List<Collection>) {
        val duplicateExists = existingCollections.any {
            it.libraryId == id && it.isActive && it.name == name
        }
        require(!duplicateExists) {
            "Collection with name '${name.trimmedValue}' already exists in Library ($id)."
        }
    }

    fun registerEntry(
        installedPackageId: InstalledPackageId,
        packageId: PackageId,
        registeredAt: Instant = Instant.now()
    ): Library {
        check(!hasPackage(installedPackageId)) {
            "Package ($installedPackageId) is already registered in Library ($id)."
        }
        val entry = LibraryEntry(
            installedPackageId = installedPackageId,
            packageId = packageId,
            registeredAt = registeredAt
        )
        return copy(entries = entries + entry)
    }

    internal fun unregisterEntry(installedPackageId: InstalledPackageId): Library {
        check(hasPackage(installedPackageId)) {
            "Package ($installedPackageId) is not registered in Library ($id)."
        }
        return copy(entries = entries.filterNot { it.installedPackageId == installedPackageId })
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

        /**
         * Reconstitution factory dành riêng cho việc tái tạo Library từ persistence layer.
         */
        fun reconstitute(
            id: LibraryId,
            name: String,
            entries: List<LibraryEntry> = emptyList(),
            createdAt: Instant = Instant.now()
        ): Library = Library(
            id = id,
            name = name,
            entries = entries,
            createdAt = createdAt
        )
    }
}
