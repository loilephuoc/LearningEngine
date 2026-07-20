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