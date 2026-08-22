package vn.loi.learning.application.sync

import java.nio.file.Files
import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContentFieldSyncServiceTest {
    private val account = SyncAccountId("learner")

    private fun change(
        event: String,
        field: ContentField,
        value: String? = "changed",
        operation: DeltaOperation = DeltaOperation.SET,
        device: String = "remote",
        baseRevision: SyncRevision? = null
    ) = OutboundSyncChange(
        account, SyncEventId(event), IdempotencyKey("key-$event"), SyncDeviceId(device),
        SyncEntityId("content-1"), delta = ContentFieldDelta(field, operation, value, baseRevision = baseRevision)
    )

    private fun content() = Content(
        ContentId("content-1"), ContentType.WORD,
        ContentText("Question", "Answer", exampleText = "Example", exampleTranslation = "Translation"),
        customFields = ContentCustomFields(setOf(
            ContentCustomField(ContentFieldId("definition"), "Reminder definition"),
            ContentCustomField(ContentFieldId("unknown-client-field"), "preserve me")
        ))
    )

    @Test
    fun `four canonical fields apply independently and preserve identity and custom fields`() {
        val root = Files.createTempDirectory("content-sync-fields-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val service = context.contentFieldSyncService!!

            val cases = listOf(
                ContentField.QUESTION to "New question",
                ContentField.ANSWER to "New answer",
                ContentField.EXAMPLE to "New example",
                ContentField.TRANSLATION to "New translation"
            )
            cases.forEachIndexed { index, (field, value) ->
                val before = context.contentRepository.findById(ContentId("content-1"))!!
                assertIs<ContentDeltaApplyResult.Applied>(
                    service.applyRemote(RemoteSyncChange(SyncRevision(index + 1L), change("event-$index", field, value)))
                )
                val after = context.contentRepository.findById(ContentId("content-1"))!!
                assertEquals(ContentId("content-1"), after.id)
                assertEquals(before.customFields, after.customFields)
                when (field) {
                    ContentField.QUESTION -> assertEquals(before.text.copy(primaryText = value), after.text)
                    ContentField.ANSWER -> assertEquals(before.text.copy(translatedText = value), after.text)
                    ContentField.EXAMPLE -> assertEquals(before.text.copy(exampleText = value), after.text)
                    ContentField.TRANSLATION -> assertEquals(before.text.copy(exampleTranslation = value), after.text)
                    else -> fail("Unexpected field")
                }
            }
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `local question and remote translation both survive on one Content`() {
        val root = Files.createTempDirectory("content-sync-merge-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val service = context.contentFieldSyncService!!
            service.applyLocal(change("local-question", ContentField.QUESTION, "Local question", device = "desktop"))
            service.applyRemote(RemoteSyncChange(SyncRevision(1), change("remote-translation", ContentField.TRANSLATION, "Remote translation")))

            val actual = context.contentRepository.findById(ContentId("content-1"))!!
            assertEquals("Local question", actual.text.primaryText)
            assertEquals("Remote translation", actual.text.exampleTranslation)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `local answer and remote example both survive`() {
        val root = Files.createTempDirectory("content-sync-answer-example-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val service = context.contentFieldSyncService!!
            service.applyLocal(change("local-answer", ContentField.ANSWER, "Local answer", device = "desktop"))
            service.applyRemote(RemoteSyncChange(SyncRevision(1), change("remote-example", ContentField.EXAMPLE, "Remote example")))
            val actual = context.contentRepository.findById(ContentId("content-1"))!!
            assertEquals("Local answer", actual.text.translatedText)
            assertEquals("Remote example", actual.text.exampleText)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `same-field conflict deterministically preserves pending local value and reports path`() {
        val root = Files.createTempDirectory("content-sync-conflict-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val service = context.contentFieldSyncService!!
            service.applyLocal(change("local-answer", ContentField.ANSWER, "Local answer", device = "desktop"))

            val result = assertIs<ContentDeltaApplyResult.Conflict>(
                service.applyRemote(RemoteSyncChange(SyncRevision(7), change("remote-answer", ContentField.ANSWER, "Remote answer")))
            )
            assertEquals("Local answer", context.contentRepository.findById(ContentId("content-1"))!!.text.translatedText)
            assertEquals("text.translatedText", result.diagnostic.field)
            assertEquals(SyncConflictOutcome.PRESERVED_LOCAL, result.diagnostic.outcome)
            assertEquals(SyncRevision(7), result.diagnostic.remoteRevision)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `duplicate and retry are no-op`() {
        val root = Files.createTempDirectory("content-sync-duplicate-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val remote = RemoteSyncChange(SyncRevision(1), change("remote-question", ContentField.QUESTION, "Once"))
            assertIs<ContentDeltaApplyResult.Applied>(context.contentFieldSyncService!!.applyRemote(remote))
            assertIs<ContentDeltaApplyResult.Duplicate>(context.contentFieldSyncService!!.applyRemote(remote))
            assertEquals("Once", context.contentRepository.findById(ContentId("content-1"))!!.text.primaryText)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `explicit empty and removal follow nullable editor semantics while absent delta changes nothing`() {
        val root = Files.createTempDirectory("content-sync-empty-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val service = context.contentFieldSyncService!!
            service.applyRemote(RemoteSyncChange(SyncRevision(1), change("empty-answer", ContentField.ANSWER, "")))
            assertNull(context.contentRepository.findById(ContentId("content-1"))!!.text.translatedText)
            service.applyRemote(RemoteSyncChange(SyncRevision(2), change("remove-example", ContentField.EXAMPLE, null, DeltaOperation.REMOVE)))
            val actual = context.contentRepository.findById(ContentId("content-1"))!!
            assertNull(actual.text.exampleText)
            assertEquals("Translation", actual.text.exampleTranslation)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `content delta leaves learning memory and review repositories unchanged`() {
        val root = Files.createTempDirectory("content-sync-learning-isolation-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            context.contentRepository!!.save(content())
            val item = LearningItem(LearningItemId("item-1"), ContentId("content-1"), LearningMode.MEANING_RECOGNITION)
            context.learningItemRepository!!.save(item)
            val before = MemoryState.new(LearnerId("learner"), item.id, Moment(1000))
            val after = before.afterReview(LearningStage.LEARNING, Difficulty.of(5.0), Stability.of(1.0), Moment(2000), TimeSpan.days(1.0), false)
            context.memoryStateRepository!!.save(after)
            val event = ReviewEvent(ReviewEventId("review-1"), ReviewRating.GOOD, Moment(2000), null, before, after)
            context.reviewEventRepository!!.append(event)

            context.contentFieldSyncService!!.applyRemote(
                RemoteSyncChange(SyncRevision(1), change("content-only", ContentField.QUESTION, "Changed"))
            )
            assertEquals(listOf(item), context.learningItemRepository!!.findAll())
            assertEquals(listOf(after), context.memoryStateRepository!!.findAll())
            assertEquals(listOf(event), context.reviewEventRepository!!.findAll())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `persistence restart round-trips all four canonical fields and custom fields`() {
        val root = Files.createTempDirectory("content-sync-restart-")
        try {
            val first = LearningApplicationFactory.createPersisted(root, false)
            first.contentRepository!!.save(content())
            val fields = listOf(ContentField.QUESTION, ContentField.ANSWER, ContentField.EXAMPLE, ContentField.TRANSLATION)
            fields.forEachIndexed { index, field ->
                first.contentFieldSyncService!!.applyRemote(
                    RemoteSyncChange(SyncRevision(index + 1L), change("restart-$index", field, "value-$index"))
                )
            }
            val reopened = LearningApplicationFactory.createPersisted(root, false)
            val actual = reopened.contentRepository!!.findById(ContentId("content-1"))!!
            assertEquals(ContentText("value-0", "value-1", exampleText = "value-2", exampleTranslation = "value-3"), actual.text)
            assertEquals(content().customFields, actual.customFields)
        } finally { root.toFile().deleteRecursively() }
    }
}
