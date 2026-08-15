package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.*
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.model.*

class BatchPartOfSpeechEditServiceTest {
    @Test
    fun `mixed POS including target updates all identities and skips no-op writes`() {
        val repository = RecordingContentRepository()
        listOf("Verb", "Noun", "Adjective").forEachIndexed { index, pos ->
            repository.seed(content("c${index + 1}", pos))
        }
        val service = ContentBrowserEditService(repository, transactionRunner = ImmediateTransactionRunner)

        val result = service.updatePartOfSpeechBatch(
            listOf(ContentId("c1"), ContentId("missing"), ContentId("c2"), ContentId("c3")),
            "Adjective"
        )

        assertEquals(3, result.selectedCount)
        assertEquals(2, result.changedCount)
        assertEquals(1, result.unchangedCount)
        assertEquals(2, repository.commandWrites)
        assertEquals(setOf("Adjective"), repository.findAll().map(::pos).toSet())
    }

    @Test
    fun `first middle and final write failures restore every original POS`() {
        for (failureAt in 1..3) {
            val repository = RecordingContentRepository()
            (1..3).forEach { repository.seed(content("c$it", "Verb")) }
            repository.failAtCommandWrite = failureAt
            val service = ContentBrowserEditService(repository, transactionRunner = ImmediateTransactionRunner)

            assertFails {
                service.updatePartOfSpeechBatch((1..3).map { ContentId("c$it") }, "Adjective")
            }

            assertEquals(listOf("Verb", "Verb", "Verb"), repository.findAll().map(::pos), "failureAt=$failureAt")
        }
    }

    @Test
    fun `batch changes only POS while preserving Content identity text media and metadata`() {
        val repository = RecordingContentRepository()
        val original = content("c1", "Verb")
        repository.seed(original)
        val service = ContentBrowserEditService(repository, transactionRunner = ImmediateTransactionRunner)

        service.updatePartOfSpeechBatch(listOf(original.id), "Adjective")

        val updated = assertNotNull(repository.findById(original.id))
        assertEquals(original.id, updated.id)
        assertEquals(original.type, updated.type)
        assertEquals(original.text, updated.text)
        assertEquals(original.media, updated.media)
        assertEquals(original.metadata, updated.metadata)
    }

    private fun content(id: String, pos: String) = Content(
        id = ContentId(id), type = ContentType.WORD,
        text = ContentText(primaryText = "Question $id", translatedText = "Answer $id"),
        media = ContentMedia(image = "media/$id.png"),
        metadata = ContentMetadata(title = "Title $id"),
        customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("partOfSpeech"), pos)))
    )

    private fun pos(content: Content): String =
        content.customFields[ContentFieldId("partOfSpeech")]?.value.orEmpty()

    private object ImmediateTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T = block()
    }

    private class RecordingContentRepository : ContentRepository {
        private val values = linkedMapOf<ContentId, Content>()
        var commandWrites = 0
        var failAtCommandWrite: Int? = null
        private var failureInjected = false

        fun seed(content: Content) { values[content.id] = content }
        override fun findById(contentId: ContentId): Content? = values[contentId]
        override fun findByIds(contentIds: Collection<ContentId>): List<Content> =
            contentIds.distinct().mapNotNull(values::get)
        override fun findAll(): List<Content> = values.values.toList()
        override fun deleteById(contentId: ContentId) { values.remove(contentId) }
        override fun save(content: Content) {
            commandWrites++
            if (!failureInjected && commandWrites == failAtCommandWrite) {
                failureInjected = true
                throw IllegalStateException("Injected write failure")
            }
            values[content.id] = content
        }
    }
}
