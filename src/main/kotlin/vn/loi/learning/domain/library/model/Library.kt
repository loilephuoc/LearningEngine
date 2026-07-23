package vn.loi.learning.domain.library.model

import java.time.Instant

/**
 * Aggregate Root đại diện cho danh mục Library của người học.
 */
data class Library(
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

    fun registerPackage(installedPackageId: InstalledPackageId, registeredAt: Instant = Instant.now()): Library {
        require(!hasPackage(installedPackageId)) {
            "Package ($installedPackageId) is already registered in Library ($id)."
        }
        return copy(entries = entries + LibraryEntry(installedPackageId, registeredAt))
    }

    fun unregisterPackage(installedPackageId: InstalledPackageId): Library {
        if (!hasPackage(installedPackageId)) return this
        return copy(entries = entries.filterNot { it.installedPackageId == installedPackageId })
    }
}
