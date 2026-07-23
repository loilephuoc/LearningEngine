package vn.loi.learning.application.library.query

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState

/**
 * Immutable DTO đại diện cho bản tóm tắt gói nội dung đã cài đặt trong Library read model.
 */
data class InstalledPackageSummary(
    val id: InstalledPackageId,
    val libraryId: LibraryId,
    val packageId: PackageId,
    val topicId: TopicId,
    val name: String,
    val version: String,
    val state: PackageState,
    val installedAt: Instant,
    val contentCount: Int,
    val learningItemCount: Int
) {
    val isActive: Boolean get() = state == PackageState.ACTIVE
    val isArchived: Boolean get() = state == PackageState.ARCHIVED
    val isRemoved: Boolean get() = state == PackageState.REMOVED
}
