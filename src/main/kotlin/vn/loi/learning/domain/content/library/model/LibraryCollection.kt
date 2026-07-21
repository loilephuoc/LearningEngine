package vn.loi.learning.domain.content.library.model

import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Nhóm nội dung logic nằm trong một Content Library.
 *
 * Collection chỉ lưu tham chiếu PackageId, không nhúng toàn bộ Content Package.
 * Nhờ đó:
 * - collection độc lập với persistence;
 * - package có thể được quản lý và tải riêng;
 * - quan hệ collection-package có thể mở rộng mà không sao chép dữ liệu package.
 */
data class LibraryCollection(
    val id: LibraryCollectionId,
    val libraryId: ContentLibraryId,
    val descriptor: LibraryCollectionDescriptor,
    val packageIds: Set<PackageId> = emptySet()
) {

    val name: String
        get() = descriptor.name

    val packageCount: Int
        get() = packageIds.size

    val isEmpty: Boolean
        get() = packageIds.isEmpty()

    fun contains(
        packageId: PackageId
    ): Boolean =
        packageId in packageIds

    fun rename(
        name: String
    ): LibraryCollection {
        val updatedDescriptor =
            LibraryCollectionDescriptor(
                name = name
            )

        return if (updatedDescriptor == descriptor) {
            this
        } else {
            copy(
                descriptor = updatedDescriptor
            )
        }
    }

    fun attach(
        packageId: PackageId
    ): LibraryCollection =
        if (contains(packageId)) {
            this
        } else {
            copy(
                packageIds = packageIds + packageId
            )
        }

    fun attachAll(
        packageIds: Set<PackageId>
    ): LibraryCollection =
        if (packageIds.isEmpty() || this.packageIds.containsAll(packageIds)) {
            this
        } else {
            copy(
                packageIds = this.packageIds + packageIds
            )
        }

    fun detach(
        packageId: PackageId
    ): LibraryCollection =
        if (!contains(packageId)) {
            this
        } else {
            copy(
                packageIds = packageIds - packageId
            )
        }
}