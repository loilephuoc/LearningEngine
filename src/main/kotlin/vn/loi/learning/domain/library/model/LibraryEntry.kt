package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Entity đại diện cho một mục đăng ký gói nội dung trong Library.
 * Không chứa PackageState snapshot để đảm bảo Single Source of Truth thuộc về InstalledPackage.
 */
data class LibraryEntry(
    val installedPackageId: InstalledPackageId,
    val packageId: PackageId,
    val registeredAt: Instant = Instant.now()
)
