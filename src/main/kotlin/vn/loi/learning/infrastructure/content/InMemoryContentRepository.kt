package vn.loi.learning.infrastructure.content

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId

class InMemoryContentRepository : ContentRepository {

    private val contents = linkedMapOf<ContentId, Content>()

    override fun findById(contentId: ContentId): Content? =
        contents[contentId]

    override fun save(content: Content) {
        contents[content.id] = content
    }

    override fun findAll(): List<Content> =
        contents.values.toList()

    fun count(): Int = contents.size

    fun clear() {
        contents.clear()
    }
}