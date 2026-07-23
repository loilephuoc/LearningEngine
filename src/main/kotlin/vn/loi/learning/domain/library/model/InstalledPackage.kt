package vn.loi.learning.domain.library.model

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainMutationResult
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.event.PackageArchivedEvent
import vn.loi.learning.domain.library.event.PackageRemovedEvent
import vn.loi.learning.domain.library.event.PackageRestoredEvent
import vn.loi.learning.domain.library.service.CoordinatorToken

/**
 * Aggregate Root đại diện cho tệp gói nội dung OPD3 đã cài đặt trong hệ thống.
 */
class InstalledPackage internal constructor(
    val id: InstalledPackageId,
    val libraryId: LibraryId,
    val packageId: PackageId,
    val topicId: TopicId,
    val name: PackageName,
    val version: PackageVersion,
    val state: PackageState = PackageState.ACTIVE,
    val installedAt: Instant = Instant.now(),
    val contentCount: Int,
    val learningItemCount: Int,
    /**
     * Canonical content fingerprint / checksum (nullable for backward compatibility with legacy records).
     * When present, used as authoritative evidence for deterministic identical-package comparison.
     * Null means the record was created before LP-004R or by a workflow that did not supply a checksum.
     */
    val contentChecksum: String? = null
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

    /**
     * Chuyển trạng thái sang ARCHIVED.
     * Thao tác này là aggregate-local (không yêu cầu kiểm tra cross-aggregate trong Library).
     */
    fun archive(): DomainMutationResult<InstalledPackage, PackageArchivedEvent> {
        check(state != PackageState.REMOVED) {
            "Cannot archive a removed package ($id)."
        }
        check(state == PackageState.ACTIVE) {
            "Cannot archive package ($id): package is already in state $state."
        }
        val updated = copy(state = PackageState.ARCHIVED)
        val event = PackageArchivedEvent(installedPackageId = id, packageId = packageId)
        return DomainMutationResult(updated, event)
    }

    /**
     * Khôi phục trạng thái từ ARCHIVED về ACTIVE.
     * Thao tác này yêu cầu kiểm tra Single Active Version cross-aggregate trong Library,
     * do đó chỉ có thể được gọi thông qua LibraryDomainCoordinator.
     */
    fun restore(@Suppress("UNUSED_PARAMETER") token: CoordinatorToken): DomainMutationResult<InstalledPackage, PackageRestoredEvent> {
        check(state != PackageState.REMOVED) {
            "Cannot restore a removed package ($id)."
        }
        check(state == PackageState.ARCHIVED) {
            "Cannot restore package ($id): package is already in state $state."
        }
        val updated = copy(state = PackageState.ACTIVE)
        val event = PackageRestoredEvent(installedPackageId = id, packageId = packageId)
        return DomainMutationResult(updated, event)
    }

    /**
     * Gỡ bỏ gói nội dung (REMOVED).
     * Thao tác này yêu cầu unregister khỏi Library và dọn dẹp Collection references,
     * do đó chỉ có thể được gọi thông qua LibraryDomainCoordinator.
     */
    fun remove(@Suppress("UNUSED_PARAMETER") token: CoordinatorToken): DomainMutationResult<InstalledPackage, PackageRemovedEvent> {
        check(state != PackageState.REMOVED) {
            "Package ($id) is already removed."
        }
        val updated = copy(state = PackageState.REMOVED)
        val event = PackageRemovedEvent(installedPackageId = id, libraryId = libraryId, packageId = packageId)
        return DomainMutationResult(updated, event)
    }

    private fun copy(
        state: PackageState = this.state
    ): InstalledPackage = InstalledPackage(
        id = id,
        libraryId = libraryId,
        packageId = packageId,
        topicId = topicId,
        name = name,
        version = version,
        state = state,
        installedAt = installedAt,
        contentCount = contentCount,
        learningItemCount = learningItemCount,
        contentChecksum = contentChecksum
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstalledPackage) return false
        return id == other.id &&
                libraryId == other.libraryId &&
                packageId == other.packageId &&
                topicId == other.topicId &&
                name == other.name &&
                version == other.version &&
                state == other.state &&
                installedAt == other.installedAt &&
                contentCount == other.contentCount &&
                learningItemCount == other.learningItemCount &&
                contentChecksum == other.contentChecksum
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + libraryId.hashCode()
        result = 31 * result + packageId.hashCode()
        result = 31 * result + topicId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + installedAt.hashCode()
        result = 31 * result + contentCount
        result = 31 * result + learningItemCount
        result = 31 * result + contentChecksum.hashCode()
        return result
    }

    override fun toString(): String =
        "InstalledPackage(id=$id, packageId=$packageId, state=$state, version=$version)"

    companion object {
        /**
         * Reconstitution factory dành riêng cho việc tải/tái tạo aggregate từ lớp lưu trữ (persistence rehydration).
         * Kiểm tra toàn bộ aggregate-local invariants nhưng không sinh ra Domain Events.
         */
        fun reconstitute(
            id: InstalledPackageId,
            libraryId: LibraryId,
            packageId: PackageId,
            topicId: TopicId,
            name: PackageName,
            version: PackageVersion,
            state: PackageState,
            installedAt: Instant,
            contentCount: Int,
            learningItemCount: Int,
            contentChecksum: String? = null
        ): InstalledPackage = InstalledPackage(
            id = id,
            libraryId = libraryId,
            packageId = packageId,
            topicId = topicId,
            name = name,
            version = version,
            state = state,
            installedAt = installedAt,
            contentCount = contentCount,
            learningItemCount = learningItemCount,
            contentChecksum = contentChecksum
        )
    }
}
