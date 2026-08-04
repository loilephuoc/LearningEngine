package vn.loi.learning.application.port

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId

/**
 * Port dùng để lưu và truy xuất Content.
 *
 * Application chỉ biết interface này, không biết dữ liệu được lưu
 * trong RAM, JSON, SQLite, server hay cloud.
 */
interface ContentRepository {

    fun findById(
        contentId: ContentId
    ): Content?

    /**
     * Returns requested contents in first-occurrence input order.
     * Missing IDs are omitted and duplicate IDs produce one result.
     */
    fun findByIds(
        contentIds: Collection<ContentId>
    ): List<Content> {
        if (contentIds.isEmpty()) {
            return emptyList()
        }

        val contentsById =
            findAll().associateBy(Content::id)

        return contentIds
            .distinct()
            .mapNotNull(contentsById::get)
    }

    fun save(
        content: Content
    )

    fun saveAll(
        contents: List<Content>
    ) {
        contents.forEach(::save)
    }

    fun deleteById(
        contentId: ContentId
    )

    fun deleteAllById(
        contentIds: Set<ContentId>
    ) {
        contentIds.forEach(::deleteById)
    }

    fun findAll(): List<Content>
}
