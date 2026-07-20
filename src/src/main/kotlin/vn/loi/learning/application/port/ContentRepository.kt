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

    fun findById(contentId: ContentId): Content?

    fun save(content: Content)

    fun deleteById(contentId: ContentId)

    fun findAll(): List<Content>
}
