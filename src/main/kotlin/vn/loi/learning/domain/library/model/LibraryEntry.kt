package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Entity đại diện cho một mục đăng ký gói nội dung trong Library.
 */
data class LibraryEntry(
    val installedPackageId: InstalledPackageId,
    val packageId: PackageId,
    val state: PackageState,
    val registeredAt: Instant = Instant.now()
) {
    val isActive: Boolean get() = state == PackageState.ACTIVE
}
