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
import vn.loi.learning.infrastructure.persistence.mapper.ContentRecordMapper
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.persistence.store.ContentStore
import vn.loi.learning.infrastructure.persistence.store.InMemoryContentStore

class ContentRepositoryPersistentContractTest {

    private fun createRepository(): ContentRepository =
        StoreBackedContentRepository(
            store = InMemoryContentStore()
        )

    @Test
    fun `findById returns null when content does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                ContentId("missing-content")
            )
        )
    }

    @Test
    fun `save stores content and findById returns it`() {
        val repository =
            createRepository()

        val content =
            createContent(
                id = "content-1"
            )

        repository.save(content)

        assertEquals(
            content,
            repository.findById(content.id)
        )
    }

    @Test
    fun `save preserves custom field with stable ID`() {
        val repository =
            createRepository()

        val content =
            Content(
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
        val repository =
            createRepository()

        val id =
            ContentId("content-1")

        val original =
            createContent(
                id = id.value
            )

        val updated =
            Content(
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
    fun `saveAll stores multiple contents`() {
        val repository =
            createRepository()

        val first =
            createContent(
                id = "content-1"
            )

        val second =
            createContent(
                id = "content-2"
            )

        repository.saveAll(
            listOf(
                first,
                second
            )
        )

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `saveAll replaces matching contents and preserves others`() {
        val repository =
            createRepository()

        val original =
            createContent(
                id = "content-1",
                primaryText = "original"
            )

        val preserved =
            createContent(
                id = "content-2",
                primaryText = "preserved"
            )

        val updated =
            createContent(
                id = "content-1",
                primaryText = "updated"
            )

        repository.saveAll(
            listOf(
                original,
                preserved
            )
        )

        repository.saveAll(
            listOf(
                updated
            )
        )

        assertEquals(
            listOf(updated, preserved),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById removes selected contents and preserves others`() {
        val repository =
            createRepository()

        val first =
            createContent(
                id = "content-1"
            )

        val second =
            createContent(
                id = "content-2"
            )

        val preserved =
            createContent(
                id = "content-3"
            )

        repository.saveAll(
            listOf(
                first,
                second,
                preserved
            )
        )

        repository.deleteAllById(
            setOf(
                first.id,
                second.id
            )
        )

        assertNull(
            repository.findById(first.id)
        )

        assertNull(
            repository.findById(second.id)
        )

        assertEquals(
            listOf(preserved),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById accepts empty set without changing repository`() {
        val repository =
            createRepository()

        val content =
            createContent(
                id = "content-1"
            )

        repository.save(content)

        repository.deleteAllById(
            emptySet()
        )

        assertEquals(
            listOf(content),
            repository.findAll()
        )
    }

    @Test
    fun `findAll returns contents in insertion order`() {
        val repository =
            createRepository()

        val first =
            createContent(
                id = "content-1"
            )

        val second =
            createContent(
                id = "content-2"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `findByIds reads a 990 content store once and preserves requested first occurrence order`() {
        val contents =
            (1..990).map { index ->
                createContent(
                    id = "content-$index",
                    primaryText = "Content $index"
                )
            }
        val store =
            CountingContentStore(
                contents.map(ContentRecordMapper::toRecord)
            )
        val repository =
            StoreBackedContentRepository(store)

        val startedAt = System.nanoTime()
        val result =
            repository.findByIds(
                listOf(
                    contents.last().id,
                    ContentId("missing-content"),
                    contents.first().id,
                    contents.last().id
                )
            )
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(1, store.loadCount)
        assertEquals(listOf(contents.last(), contents.first()), result)
        println("ANDROID_UAT_005_METRICS bulk_query_ms=$elapsedMillis store_loads=1 stored_contents=990")
    }

    @Test
    fun `findByIds with empty scope does not read persistent store`() {
        val store = CountingContentStore(emptyList())
        val repository = StoreBackedContentRepository(store)

        assertEquals(emptyList(), repository.findByIds(emptyList()))
        assertEquals(0, store.loadCount)
    }

    @Test
    fun `in-memory findByIds shares ordering missing and duplicate semantics`() {
        val first = createContent("content-1")
        val second = createContent("content-2")
        val repository = InMemoryContentRepository()
        repository.saveAll(listOf(first, second))

        assertEquals(
            listOf(second, first),
            repository.findByIds(
                listOf(second.id, ContentId("missing-content"), first.id, second.id)
            )
        )
    }

    private fun createContent(
        id: String,
        primaryText: String = "hello"
    ): Content =
        Content(
            id = ContentId(id),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = primaryText
            )
        )

    private class CountingContentStore(
        private var records: List<ContentRecord>
    ) : ContentStore {
        var loadCount: Int = 0
            private set

        override fun loadAll(): List<ContentRecord> {
            loadCount += 1
            return records.toList()
        }

        override fun saveAll(records: List<ContentRecord>) {
            this.records = records.toList()
        }
    }
}
