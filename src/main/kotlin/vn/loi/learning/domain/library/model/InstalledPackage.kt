package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Aggregate Root đại diện cho tệp gói nội dung OPD3 đã cài đặt trong hệ thống.
 */
data class InstalledPackage(
    val id: InstalledPackageId,
    val libraryId: LibraryId,
    val packageId: PackageId,
    val topicId: TopicId,
    val name: PackageName,
    val version: PackageVersion,
    val state: PackageState = PackageState.ACTIVE,
    val installedAt: Instant = Instant.now(),
    val contentCount: Int,
    val learningItemCount: Int
) {
    init {
        require(contentCount >= 0) {
            "Content count must not be negative: $contentCount."
        }
        require(learningItemCount >= 0) {
            "Learning item count must not be negative: $learningItemCount."
        }
    }

    val isActive: Boolean get() = state == PackageState.ACTIVE
    val isArchived: Boolean get() = state == PackageState.ARCHIVED
    val isRemoved: Boolean get() = state == PackageState.REMOVED

    fun archive(): InstalledPackage {
        check(state != PackageState.REMOVED) {
            "Cannot archive a removed package ($id)."
        }
        if (state == PackageState.ARCHIVED) return this
        return copy(state = PackageState.ARCHIVED)
    }

    fun restore(): InstalledPackage {
        check(state != PackageState.REMOVED) {
            "Cannot restore a removed package ($id)."
        }
        if (state == PackageState.ACTIVE) return this
        return copy(state = PackageState.ACTIVE)
    }

    fun remove(): InstalledPackage {
        if (state == PackageState.REMOVED) return this
        return copy(state = PackageState.REMOVED)
    }
}
