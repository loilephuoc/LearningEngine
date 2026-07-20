package vn.loi.learning.domain.content.library.model

import vn.loi.learning.domain.content.model.ContentId

/**
 * Aggregate Root đại diện cho một thư viện nội dung đã được đăng ký.
 *
 * ContentLibrary quản lý quan hệ sở hữu logic giữa Library và Content.
 * Nó không biết:
 * - dữ liệu nằm trong file JSON, OPD3, thư mục hay cloud;
 * - media được đọc hoặc phát như thế nào;
 * - Content và LearningItem được lưu bằng persistence nào.
 */
data class ContentLibrary(
    val id: ContentLibraryId,
    val descriptor: LibraryDescriptor,
    val contentIds: Set<ContentId> = emptySet()
) {

    val name: String
        get() = descriptor.name

    val contentCount: Int
        get() = contentIds.size

    val isEmpty: Boolean
        get() = contentIds.isEmpty()

    fun contains(
        contentId: ContentId
    ): Boolean =
        contentId in contentIds

    fun register(
        contentId: ContentId
    ): ContentLibrary =
        if (contains(contentId)) {
            this
        } else {
            copy(
                contentIds = contentIds + contentId
            )
        }

    fun registerAll(
        contentIds: Set<ContentId>
    ): ContentLibrary =
        if (contentIds.isEmpty() || this.contentIds.containsAll(contentIds)) {
            this
        } else {
            copy(
                contentIds = this.contentIds + contentIds
            )
        }

    fun remove(
        contentId: ContentId
    ): ContentLibrary =
        if (!contains(contentId)) {
            this
        } else {
            copy(
                contentIds = contentIds - contentId
            )
        }
}
