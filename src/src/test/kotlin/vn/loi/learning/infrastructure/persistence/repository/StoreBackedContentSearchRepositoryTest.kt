package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.search.model.ContentSearchQuery
import vn.loi.learning.infrastructure.persistence.store.InMemoryContentStore

class StoreBackedContentSearchRepositoryTest {

    private val contentRepository: ContentRepository =
        StoreBackedContentRepository(
            store = InMemoryContentStore()
        )

    private val searchRepository =
        StoreBackedContentSearchRepository(
            contentRepository = contentRepository
        )

    private fun saveContent(
        id: String,
        primaryText: String
    ) {
        contentRepository.save(
            Content(
                id = ContentId(id),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = primaryText
                )
            )
        )
    }

    @Test
    fun `search finds content by primary text`() {
        saveContent(
            id = "content-1",
            primaryText = "hello world"
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "world"
            )
        )

        assertEquals(
            listOf("content-1"),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `search ignores letter case`() {
        saveContent(
            id = "content-1",
            primaryText = "Hello World"
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "hello"
            )
        )

        assertEquals(1, result.count)
    }

    @Test
    fun `search returns empty result when nothing matches`() {
        saveContent(
            id = "content-1",
            primaryText = "hello world"
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "missing"
            )
        )

        assertTrue(result.isEmpty)
    }

    @Test
    fun `search finds content by group`() {
        contentRepository.save(
            Content(
                id = ContentId("content-1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "hello world"
                ),
                metadata = ContentMetadata(
                    group = "Daily English"
                )
            )
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "daily"
            )
        )

        assertEquals(
            listOf("content-1"),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `search finds content by section`() {
        contentRepository.save(
            Content(
                id = ContentId("content-2"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "good morning"
                ),
                metadata = ContentMetadata(
                    section = "Morning Conversations"
                )
            )
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "conversations"
            )
        )

        assertEquals(
            listOf("content-2"),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `search finds content by lesson`() {
        contentRepository.save(
            Content(
                id = ContentId("content-3"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "see you later"
                ),
                metadata = ContentMetadata(
                    lesson = "Farewell Expressions"
                )
            )
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "farewell"
            )
        )

        assertEquals(
            listOf("content-3"),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `search finds content by custom field value`() {
        contentRepository.save(
            Content(
                id = ContentId("content-4"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "appointment"
                ),
                customFields = ContentCustomFields(
                    fields = setOf(
                        ContentCustomField(
                            id = ContentFieldId("topic"),
                            value = "Hospital Vocabulary"
                        )
                    )
                )
            )
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "hospital"
            )
        )

        assertEquals(
            listOf("content-4"),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `search returns contents in ranking order`() {
        contentRepository.save(Content(id = ContentId("custom-field-match"), type = ContentType.WORD, text = ContentText(primaryText = "custom content"), customFields = ContentCustomFields(fields = setOf(ContentCustomField(id = ContentFieldId("topic"), value = "target vocabulary")))))
        contentRepository.save(Content(id = ContentId("lesson-match"), type = ContentType.WORD, text = ContentText(primaryText = "lesson content"), metadata = ContentMetadata(lesson = "target lesson")))
        contentRepository.save(Content(id = ContentId("section-match"), type = ContentType.WORD, text = ContentText(primaryText = "section content"), metadata = ContentMetadata(section = "target section")))
        contentRepository.save(Content(id = ContentId("group-match"), type = ContentType.WORD, text = ContentText(primaryText = "group content"), metadata = ContentMetadata(group = "target group")))
        contentRepository.save(Content(id = ContentId("translated-text-match"), type = ContentType.WORD, text = ContentText(primaryText = "translated content", translatedText = "target translation")))
        contentRepository.save(Content(id = ContentId("primary-text-match"), type = ContentType.WORD, text = ContentText(primaryText = "target primary text")))

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "target"
            )
        )

        assertEquals(
            listOf(
                "primary-text-match",
                "translated-text-match",
                "group-match",
                "section-match",
                "lesson-match",
                "custom-field-match"
            ),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `phrase match ranks above token match in primary text`() {
        saveContent(
            id = "token-match",
            primaryText = "hospital booking and appointment"
        )

        saveContent(
            id = "phrase-match",
            primaryText = "hospital appointment"
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "hospital appointment"
            )
        )

        assertEquals(
            listOf(
                "phrase-match",
                "token-match"
            ),
            result.contents.map { it.id.value }
        )
    }

    @Test
    fun `equal ranking scores are ordered by primary text`() {
        saveContent(
            id = "zebra-content",
            primaryText = "target Zebra"
        )

        saveContent(
            id = "apple-content",
            primaryText = "target Apple"
        )

        val result = searchRepository.search(
            ContentSearchQuery(
                keyword = "target"
            )
        )

        assertEquals(
            listOf(
                "apple-content",
                "zebra-content"
            ),
            result.contents.map { it.id.value }
        )
    }
}

