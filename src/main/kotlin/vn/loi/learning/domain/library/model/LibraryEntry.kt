package vn.loi.learning.domain.library.model

import java.time.Instant

/**
 * Entity đại diện cho một mục đăng ký gói nội dung trong Library.
 */
data class LibraryEntry(
    val installedPackageId: InstalledPackageId,
    val registeredAt: Instant = Instant.now()
)
