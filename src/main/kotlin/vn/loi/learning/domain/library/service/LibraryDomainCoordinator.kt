package vn.loi.learning.domain.library.service

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.common.event.DomainMutationResult
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.event.CollectionCreatedEvent
import vn.loi.learning.domain.library.event.CollectionRenamedEvent
import vn.loi.learning.domain.library.event.PackageInstalledEvent
import vn.loi.learning.domain.library.event.PackageRemovedFromCollectionEvent
import vn.loi.learning.domain.library.event.PackageRestoredEvent
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion

/**
 * Kết quả của thao tác cài đặt tệp gói mới vào Library.
 */
data class PackageInstallationResult(
    val library: Library,
    val installedPackage: InstalledPackage,
    val event: PackageInstalledEvent
)

/**
 * Kết quả của thao tác khôi phục gói nội dung từ trạng thái ARCHIVED về ACTIVE.
 */
data class PackageRestoreResult(
    val library: Library,
    val installedPackage: InstalledPackage,
    val event: PackageRestoredEvent
)

/**
 * Kết quả của thao tác gỡ bỏ gói nội dung khỏi Library và tự động dọn dẹp các tham chiếu trong các Collection liên quan.
 */
data class PackageRemovalResult(
    val library: Library,
    val installedPackage: InstalledPackage,
    val updatedCollections: List<Collection>,
    val events: List<DomainEvent>
)

/**
 * Pure Domain Coordinator điều phối các nghiệp vụ phức tạp liên quan đến nhiều Aggregate Root
 * (Library, InstalledPackage, Collection) mà không vi phạm ranh giới giao dịch và không sở hữu persistence/infrastructure.
 */
object LibraryDomainCoordinator {

    private val token = CoordinatorToken()

    /**
     * Điều phối quy trình cài đặt gói nội dung mới:
     * 1. Kiểm tra Single Active Version invariant trong Library.
     * 2. Tái tạo InstalledPackage với trạng thái ACTIVE.
     * 3. Đăng ký mục entry trong Library.
     * 4. Sinh ra duy nhất 1 PackageInstalledEvent chính danh.
     */
    fun installPackage(
        library: Library,
        id: InstalledPackageId,
        packageId: PackageId,
        topicId: TopicId,
        name: PackageName,
        version: PackageVersion,
        contentCount: Int,
        learningItemCount: Int,
        installedPackagesInLibrary: List<InstalledPackage>,
        installedAt: Instant = Instant.now()
    ): PackageInstallationResult {
        // Enforce Single Active Version Invariant
        library.validateSingleActiveVersion(packageId, installedPackagesInLibrary)

        // Instantiate InstalledPackage aggregate
        val installedPackage = InstalledPackage.reconstitute(
            id = id,
            libraryId = library.id,
            packageId = packageId,
            topicId = topicId,
            name = name,
            version = version,
            state = PackageState.ACTIVE,
            installedAt = installedAt,
            contentCount = contentCount,
            learningItemCount = learningItemCount
        )

        // Register entry in Library
        val updatedLibrary = library.registerEntry(id, packageId, installedAt)

        // Single canonical PackageInstalledEvent
        val event = PackageInstalledEvent(
            installedPackageId = id,
            libraryId = library.id,
            packageId = packageId,
            topicId = topicId,
            version = version,
            occurredAt = installedAt
        )

        return PackageInstallationResult(updatedLibrary, installedPackage, event)
    }

    /**
     * Điều phối quy trình khôi phục gói đã ARCHIVED về ACTIVE:
     * 1. Kiểm tra xem có gói nào khác cùng PackageId đang ACTIVE hay không.
     * 2. Chuyển trạng thái InstalledPackage sang ACTIVE.
     */
    fun restorePackage(
        library: Library,
        installedPackage: InstalledPackage,
        installedPackagesInLibrary: List<InstalledPackage>
    ): PackageRestoreResult {
        require(installedPackage.libraryId == library.id) {
            "Package (${installedPackage.id}) libraryId (${installedPackage.libraryId}) does not match Library (${library.id})."
        }
        check(library.hasPackage(installedPackage.id)) {
            "Package (${installedPackage.id}) is not registered in Library (${library.id})."
        }

        // Enforce Single Active Version Invariant before restoring
        val otherPackages = installedPackagesInLibrary.filterNot { it.id == installedPackage.id }
        library.validateSingleActiveVersion(installedPackage.packageId, otherPackages)

        val restoreMutation = installedPackage.restore(token)
        return PackageRestoreResult(
            library = library,
            installedPackage = restoreMutation.aggregate,
            event = restoreMutation.event
        )
    }

    /**
     * Điều phối quy trình gỡ bỏ gói nội dung:
     * 1. Chuyển trạng thái InstalledPackage sang REMOVED.
     * 2. Gỡ bỏ entry trong Library.
     * 3. Dọn dẹp tham chiếu gói bị gỡ trong tất cả các Collection active liên quan.
     */
    fun removePackage(
        library: Library,
        installedPackage: InstalledPackage,
        collections: List<Collection>
    ): PackageRemovalResult {
        require(installedPackage.libraryId == library.id) {
            "Package (${installedPackage.id}) libraryId (${installedPackage.libraryId}) does not match Library (${library.id})."
        }
        check(library.hasPackage(installedPackage.id)) {
            "Package (${installedPackage.id}) is not registered in Library (${library.id})."
        }

        val removePackageMutation = installedPackage.remove(token)
        val updatedLibrary = library.unregisterEntry(installedPackage.id)

        val updatedCollections = mutableListOf<Collection>()
        val events = mutableListOf<DomainEvent>()

        events.add(removePackageMutation.event)

        collections.filter { it.libraryId == library.id && it.isActive && it.containsPackage(installedPackage.id) }
            .forEach { collection ->
                val removeMutation = collection.removePackage(installedPackage.id)
                updatedCollections.add(removeMutation.aggregate)
                events.add(removeMutation.event)
            }

        return PackageRemovalResult(
            library = updatedLibrary,
            installedPackage = removePackageMutation.aggregate,
            updatedCollections = updatedCollections,
            events = events
        )
    }

    /**
     * Điều phối tạo Collection mới với kiểm tra tính duy nhất của tên bộ sưu tập trong Library.
     */
    fun createCollection(
        library: Library,
        id: CollectionId,
        name: CollectionName,
        existingCollections: List<Collection> = emptyList(),
        description: String = "",
        createdAt: Instant = Instant.now()
    ): DomainMutationResult<Collection, CollectionCreatedEvent> {
        library.validateCollectionNameUnique(name, existingCollections)
        return Collection.create(id, library.id, name, description, createdAt, token)
    }

    /**
     * Điều phối đổi tên Collection với kiểm tra tính duy nhất của tên bộ sưu tập trong Library.
     */
    fun renameCollection(
        library: Library,
        collection: Collection,
        newName: CollectionName,
        existingCollections: List<Collection>
    ): DomainMutationResult<Collection, CollectionRenamedEvent> {
        require(collection.libraryId == library.id) {
            "Collection (${collection.id}) belongs to library (${collection.libraryId}), not library (${library.id})."
        }
        val otherCollections = existingCollections.filterNot { it.id == collection.id }
        library.validateCollectionNameUnique(newName, otherCollections)
        return collection.rename(newName, token)
    }
}
