package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedContentRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryContentStore

class ContentRepositoryPersistentContractTest {

    private val repository: ContentRepository =
        StoreBackedContentRepository(
            store = InMemoryContentStore()
        )

    private fun createContent(
        id: String
    ): Content =
        Content(
            id = ContentId(id),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "hello"
            )
        )

    @Test
    fun `findById returns null when content does not exist`() {
        assertNull(
            repository.findById(
                ContentId("missing-content")
            )
        )
    }

    @Test
    fun `save stores content and findById returns it`() {
        val content = createContent("content-1")

        repository.save(content)

        assertEquals(
            content,
            repository.findById(content.id)
        )
    }

    @Test
    fun `save preserves custom field with stable ID`() {
        val content = Content(
            id = ContentId("content-custom-field"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "aunt"
            ),
            customFields = ContentCustomFields(
                fields = setOf(
                    ContentCustomField(
                        id = ContentFieldId("oxford.level"),
                        value = "A1"
                    )
                )
            )
        )

        repository.save(content)

        assertEquals(
            content,
            repository.findById(content.id)
        )
    }

    @Test
    fun `save replaces content with same ID`() {
        val id = ContentId("content-1")

        val original = createContent(id.value)

        val updated = Content(
            id = id,
            type = ContentType.SENTENCE,
            text = ContentText(
                primaryText = "updated"
            )
        )

        repository.save(original)
        repository.save(updated)

        assertEquals(
            updated,
            repository.findById(id)
        )
    }

    @Test
    fun `findAll returns contents in insertion order`() {
        val first = createContent("content-1")
        val second = createContent("content-2")

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }
}
