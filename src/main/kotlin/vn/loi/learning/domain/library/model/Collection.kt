package vn.loi.learning.domain.library.model

import java.time.Instant

/**
 * Aggregate Root đại diện cho một bộ sưu tập (Collection) do người học định nghĩa.
 */
data class Collection(
    val id: CollectionId,
    val libraryId: LibraryId,
    val name: CollectionName,
    val description: String = "",
    val assignedPackageIds: Set<InstalledPackageId> = emptySet(),
    val createdAt: Instant = Instant.now()
) {
    fun rename(newName: CollectionName): Collection {
        if (this.name == newName) return this
        return copy(name = newName)
    }

    fun assignPackage(packageId: InstalledPackageId): Collection {
        if (assignedPackageIds.contains(packageId)) return this
        return copy(assignedPackageIds = assignedPackageIds + packageId)
    }

    fun removePackage(packageId: InstalledPackageId): Collection {
        if (!assignedPackageIds.contains(packageId)) return this
        return copy(assignedPackageIds = assignedPackageIds - packageId)
    }

    fun containsPackage(packageId: InstalledPackageId): Boolean =
        assignedPackageIds.contains(packageId)
}
