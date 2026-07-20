package vn.loi.learning.domain.content.packaging.model

/**
 * Aggregate quản lý danh mục các ContentPackage đã được đăng ký.
 *
 * PackageCatalog chỉ giữ PackageId để tránh chứa lồng nhiều aggregate.
 */
data class PackageCatalog(
    val id: PackageCatalogId,
    val packageIds: Set<PackageId> = emptySet()
 ) {

    val packageCount: Int
        get() = packageIds.size

    val isEmpty: Boolean
        get() = packageIds.isEmpty()

    fun contains(
        packageId: PackageId
    ): Boolean =
        packageId in packageIds

    fun register(
        packageId: PackageId
    ): PackageCatalog {
        if (contains(packageId)) {
            return this
        }

        return copy(
            packageIds = packageIds + packageId
        )
    }

    fun registerAll(
        packageIds: Set<PackageId>
    ): PackageCatalog {
        val updatedPackageIds =
            this.packageIds + packageIds

        if (updatedPackageIds == this.packageIds) {
            return this
        }

        return copy(
            packageIds = updatedPackageIds
        )
    }

    fun remove(
        packageId: PackageId
    ): PackageCatalog {
        if (!contains(packageId)) {
            return this
        }

        return copy(
            packageIds = packageIds - packageId
        )
    }
}
