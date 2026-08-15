package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*

class BatchContentDeleteServiceTest {
    @Test
    fun `delete three and one undo restore exact content learning identities and media references`() {
        val fixture = fixture(3)
        val originals = fixture.contents.findAll()
        val originalItems = fixture.items.findAll()
        val preflight = fixture.service.preflightDeleteContents(originals.map { it.id }, fixture.items)

        val snapshot = fixture.service.deleteContents(preflight, fixture.items)

        assertTrue(fixture.contents.findAll().isEmpty())
        assertTrue(fixture.items.findAll().isEmpty())
        fixture.service.restoreDeletedContents(snapshot, fixture.items)
        assertEquals(originals, fixture.contents.findAll())
        assertEquals(originalItems, fixture.items.findAll())
        assertEquals(originals.map { it.media }, fixture.contents.findAll().map { it.media })
    }

    @Test
    fun `stale identities are sanitized before confirmation and one blocker blocks whole batch`() {
        val fixture = fixture(3)
        val ids = fixture.contents.findAll().map { it.id }
        val preflight = fixture.service.preflightDeleteContents(ids + ContentId("stale"), fixture.items)
        assertEquals(setOf(ContentId("stale")), preflight.staleContentIds)
        assertEquals(ids.toSet(), preflight.resolvableContentIds)
    }

    @Test
    fun `delete failures at first middle and final write roll back whole batch`() {
        for (failureAt in 1..3) {
            val fixture = fixture(3)
            val originals = fixture.contents.findAll()
            val originalItems = fixture.items.findAll()
            val failingContents = FailingDeleteContentRepository(fixture.contents, failureAt)
            val service = fixture.serviceFor(failingContents, fixture.items)
            val preflight = service.preflightDeleteContents(originals.map { it.id }, fixture.items)

            assertFails { service.deleteContents(preflight, fixture.items) }

            assertEquals(originals.toSet(), fixture.contents.findAll().toSet(), "failureAt=$failureAt")
            assertEquals(originalItems.toSet(), fixture.items.findAll().toSet(), "failureAt=$failureAt")
        }
    }

    @Test
    fun `undo identity conflict rejects entire restore before mutation`() {
        val fixture = fixture(3)
        val originals = fixture.contents.findAll()
        val snapshot = fixture.service.deleteContents(
            fixture.service.preflightDeleteContents(originals.map { it.id }, fixture.items), fixture.items
        )
        fixture.contents.save(content("c2", "conflict"))

        assertFails { fixture.service.restoreDeletedContents(snapshot, fixture.items) }

        assertNull(fixture.contents.findById(ContentId("c1")))
        assertEquals("conflict", fixture.contents.findById(ContentId("c2"))!!.text.primaryText)
        assertNull(fixture.contents.findById(ContentId("c3")))
        assertTrue(fixture.items.findAll().isEmpty())
    }

    @Test
    fun `one transaction handles one hundred selected items`() {
        val fixture = fixture(100)
        val ids = fixture.contents.findAll().map { it.id }
        val snapshot = fixture.service.deleteContents(
            fixture.service.preflightDeleteContents(ids, fixture.items), fixture.items
        )
        assertEquals(100, snapshot.contents.size)
        assertTrue(fixture.contents.findAll().isEmpty())
        fixture.service.restoreDeletedContents(snapshot, fixture.items)
        assertEquals(100, fixture.contents.findAll().size)
        assertEquals(100, fixture.items.findAll().size)
    }

    private fun fixture(count: Int): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val contents = context.contentRepository!!
        val items = context.learningItemRepository!!
        repeat(count) { index ->
            val id = "c${index + 1}"
            contents.save(content(id, "Question $id"))
            items.save(LearningItem(LearningItemId("li-$id"), ContentId(id), LearningMode.MEANING_RECOGNITION))
        }
        val fixture = Fixture(contents, items, context.transactionRunner!!, context)
        return fixture
    }

    private fun content(id: String, question: String) = Content(
        id = ContentId(id), type = ContentType.WORD,
        text = ContentText(primaryText = question, translatedText = "Answer $id"),
        media = ContentMedia(image = "media/shared.png", primaryAudio = "media/$id.mp3"),
        customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("partOfSpeech"), "Verb")))
    )

    private data class Fixture(
        val contents: ContentRepository,
        val items: LearningItemRepository,
        val transaction: vn.loi.learning.application.port.TransactionRunner,
        val context: LearningApplicationContext
    ) {
        val service = serviceFor(contents, items)
        fun serviceFor(contents: ContentRepository, items: LearningItemRepository) = ContentBrowserEditService(
            contentRepository = contents,
            contentLibraryRepository = context.contentLibraryRepository,
            installedPackageRepository = context.installedPackageRepository,
            transactionRunner = transaction,
            studySessionRepository = context.studySessionRepository
        )
    }

    private class FailingDeleteContentRepository(
        private val delegate: ContentRepository,
        private val failureAt: Int
    ) : ContentRepository by delegate {
        private var deletes = 0
        override fun deleteAllById(contentIds: Set<ContentId>) = contentIds.forEach(::deleteById)
        override fun deleteById(contentId: ContentId) {
            deletes++
            if (deletes == failureAt) throw IllegalStateException("Injected delete failure $failureAt")
            delegate.deleteById(contentId)
        }
    }
}
