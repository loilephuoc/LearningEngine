package vn.loi.learning.domain.content.packaging.model

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Aggregate đại diện cho một gói phân phối nội dung.
 *
 * ContentPackage quản lý quan hệ giữa package và các ContentLibrary
 * mà package đó cung cấp.
 */
data class ContentPackage(
    val id: PackageId,
    val descriptor: PackageDescriptor,
    val libraryIds: Set<ContentLibraryId> = emptySet(),
    val topicId: TopicId =
        TopicId.deriveForLegacyPackage(
            packageName = descriptor.name,
            packageFormat = descriptor.format
        )
 ) {

    val name: String
        get() = descriptor.name

    val version: String
        get() = descriptor.version

    val format: String
        get() = descriptor.format

    val libraryCount: Int
        get() = libraryIds.size

    val isEmpty: Boolean
        get() = libraryIds.isEmpty()

    fun contains(
        libraryId: ContentLibraryId
    ): Boolean =
        libraryId in libraryIds

    fun register(
        libraryId: ContentLibraryId
    ): ContentPackage {
        if (contains(libraryId)) {
            return this
        }

        return copy(
            libraryIds = libraryIds + libraryId
        )
    }

    fun registerAll(
        libraryIds: Set<ContentLibraryId>
    ): ContentPackage {
        val updatedLibraryIds =
            this.libraryIds + libraryIds

        if (updatedLibraryIds == this.libraryIds) {
            return this
        }

        return copy(
            libraryIds = updatedLibraryIds
        )
    }

    fun remove(
        libraryId: ContentLibraryId
    ): ContentPackage {
        if (!contains(libraryId)) {
            return this
        }

        return copy(
            libraryIds = libraryIds - libraryId
        )
    }
}
