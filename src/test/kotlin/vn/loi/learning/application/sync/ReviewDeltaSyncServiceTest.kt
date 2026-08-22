package vn.loi.learning.application.sync

import java.nio.file.Files
import kotlin.test.*
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ReviewDeltaSyncServiceTest {
    private val account = SyncAccountId("learner-account")
    private val learner = LearnerId("learner")
    private val contentId = ContentId("content-1")
    private val itemId = LearningItemId("item-1")

    @Test
    fun `local canonical review and outbox enqueue are atomic and portable`() {
        withPair("review-sync-local-outbox-") { origin, target ->
            val command = ReviewCommand(
                ReviewEventId("review-local"), learner, itemId, ReviewRating.GOOD,
                Moment(1_000), source = RatingSource.MANUAL_USER
            )
            val result = origin.reviewDeltaSyncService!!.reviewLocalAndEnqueue(
                account, SyncEventId("sync-local"), IdempotencyKey("key-local"),
                SyncDeviceId("desktop"), command
            )
            val outbound = origin.localSyncStateRepository!!.pendingOutbox(account).single()
            assertEquals(result.reviewEvent.id.value, (outbound.delta as ReviewEventDelta).reviewEventId)
            assertIs<ReviewDeltaApplyResult.Applied>(
                target.reviewDeltaSyncService!!.applyRemote(RemoteSyncChange(SyncRevision(1), outbound))
            )
            assertEquals(result.memoryState, target.memoryStateRepository!!.findAll().single())

            val beforeState = origin.memoryStateRepository!!.findAll()
            val beforeOutbox = origin.localSyncStateRepository!!.pendingOutbox(account)
            assertFailsWith<IllegalArgumentException> {
                origin.reviewDeltaSyncService!!.reviewLocalAndEnqueue(
                    account, SyncEventId("sync-failing"), IdempotencyKey("key-failing"),
                    SyncDeviceId("desktop"), command.copy(reviewedAt = Moment(2_000))
                )
            }
            assertEquals(beforeState, origin.memoryStateRepository!!.findAll())
            assertEquals(beforeOutbox, origin.localSyncStateRepository!!.pendingOutbox(account))
            assertEquals(1, origin.reviewEventRepository!!.findAll().size)
        }
    }

    @Test
    fun `Desktop review applies on Android-equivalent repository through canonical scheduler`() {
        withPair("review-sync-cross-device-") { desktop, android ->
            val origin = review(desktop, "review-1", 1_000, ReviewRating.GOOD)
            val remote = remote("sync-1", 1, delta(origin, null))

            val result = assertIs<ReviewDeltaApplyResult.Applied>(android.reviewDeltaSyncService!!.applyRemote(remote))
            assertEquals(ReviewEventId("review-1"), result.reviewEvent.id)
            assertEquals(origin.reviewEvent, android.reviewEventRepository!!.findAll().single())
            assertEquals(origin.memoryState, android.memoryStateRepository!!.findAll().single())
            assertTrue(android.learningTrajectoryRepository!!.findAll().isEmpty())
            assertEquals(content(), android.contentRepository!!.findById(contentId))
        }
    }

    @Test
    fun `Android review applies on Desktop-equivalent repository and sequential timeline remains deterministic`() {
        withPair("review-sync-sequential-") { android, desktop ->
            val first = review(android, "review-1", 1_000, ReviewRating.GOOD)
            assertIs<ReviewDeltaApplyResult.Applied>(
                desktop.reviewDeltaSyncService!!.applyRemote(remote("sync-1", 1, delta(first, null)))
            )
            val second = review(android, "review-2", 2_000, ReviewRating.HARD)
            assertIs<ReviewDeltaApplyResult.Applied>(
                desktop.reviewDeltaSyncService!!.applyRemote(remote("sync-2", 2, delta(second, "review-1")))
            )
            assertEquals(android.reviewEventRepository!!.findAll(), desktop.reviewEventRepository!!.findAll())
            assertEquals(android.memoryStateRepository!!.findAll(), desktop.memoryStateRepository!!.findAll())
        }
    }

    @Test
    fun `duplicate envelope and different envelope with same ReviewEventId never review twice`() {
        withPair("review-sync-duplicate-") { origin, target ->
            val fact = delta(review(origin, "review-1", 1_000, ReviewRating.GOOD), null)
            val first = remote("sync-1", 1, fact)
            assertIs<ReviewDeltaApplyResult.Applied>(target.reviewDeltaSyncService!!.applyRemote(first))
            assertIs<ReviewDeltaApplyResult.Duplicate>(target.reviewDeltaSyncService!!.applyRemote(first))
            assertIs<ReviewDeltaApplyResult.Duplicate>(
                target.reviewDeltaSyncService!!.applyRemote(remote("sync-retry", 2, fact))
            )
            assertEquals(1, target.reviewEventRepository!!.findAll().size)
            assertEquals(1, target.memoryStateRepository!!.findAll().single().reviewCount)
            assertEquals(SyncCursor(2), target.localSyncStateRepository!!.cursor(account))
        }
    }

    @Test
    fun `same-base concurrent review and missing predecessor are durably quarantined`() {
        val root = Files.createTempDirectory("review-sync-quarantine-")
        val originA = Files.createTempDirectory("review-sync-origin-a-")
        val originB = Files.createTempDirectory("review-sync-origin-b-")
        try {
            val target = seeded(root)
            val a = seeded(originA)
            val b = seeded(originB)
            val reviewA = delta(review(a, "review-a", 1_000, ReviewRating.GOOD), null)
            val reviewB = delta(review(b, "review-b", 1_000, ReviewRating.AGAIN), null)
            assertIs<ReviewDeltaApplyResult.Applied>(
                target.reviewDeltaSyncService!!.applyRemote(remote("sync-a", 1, reviewA))
            )
            val concurrent = assertIs<ReviewDeltaApplyResult.Quarantined>(
                target.reviewDeltaSyncService!!.applyRemote(remote("sync-b", 2, reviewB))
            )
            assertEquals("SYNC_REVIEW_CONCURRENT_BRANCH", concurrent.diagnostic.code)

            val gap = reviewB.copy(reviewEventId = "review-gap", predecessorReviewEventId = "missing")
            assertEquals(
                "SYNC_REVIEW_TIMELINE_GAP",
                assertIs<ReviewDeltaApplyResult.Quarantined>(
                    target.reviewDeltaSyncService!!.applyRemote(remote("sync-gap", 3, gap))
                ).diagnostic.code
            )
            assertEquals(1, target.reviewEventRepository!!.findAll().size)
            val reopened = LearningApplicationFactory.createPersisted(root, false)
            assertEquals(2, reopened.localSyncStateRepository!!.quarantines(account).size)
            assertEquals(SyncCursor(3), reopened.localSyncStateRepository!!.cursor(account))
            assertIs<ReviewDeltaApplyResult.Duplicate>(
                reopened.reviewDeltaSyncService!!.applyRemote(remote("sync-b", 2, reviewB))
            )
            assertEquals(2, reopened.localSyncStateRepository!!.quarantines(account).size)
        } finally {
            root.toFile().deleteRecursively(); originA.toFile().deleteRecursively(); originB.toFile().deleteRecursively()
        }
    }

    @Test
    fun `missing disabled and future payloads quarantine without mutating learning or content`() {
        withPair("review-sync-invalid-") { origin, target ->
            val valid = delta(review(origin, "review-1", 1_000, ReviewRating.GOOD), null)
            val missing = valid.copy(reviewEventId = "missing-review", learningItemId = "missing")
            assertEquals("SYNC_REVIEW_ITEM_NOT_FOUND", quarantine(target, remote("missing", 1, missing)))
            target.learningItemRepository!!.save(
                LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION, isEnabled = false)
            )
            assertEquals("SYNC_REVIEW_ITEM_DISABLED", quarantine(target, remote("disabled", 2, valid)))
            assertEquals("SYNC_REVIEW_UNSUPPORTED_PAYLOAD", quarantine(target, remote("future", 3, valid, 2)))
            assertTrue(target.reviewEventRepository!!.findAll().isEmpty())
            assertTrue(target.memoryStateRepository!!.findAll().isEmpty())
            assertEquals(content(), target.contentRepository!!.findById(contentId))
            assertTrue(target.studySessionRepository!!.findAll().isEmpty())
            assertTrue(target.studyQueueRepository!!.findAll().isEmpty())
        }
    }

    @Test
    fun `content delta and review delta on same item both survive`() {
        withPair("review-sync-content-isolation-") { origin, target ->
            val fact = delta(review(origin, "review-1", 1_000, ReviewRating.GOOD), null)
            target.contentFieldSyncService!!.applyRemote(
                RemoteSyncChange(
                    SyncRevision(1), OutboundSyncChange(
                        account, SyncEventId("content-change"), IdempotencyKey("content-key"),
                        SyncDeviceId("desktop"), SyncEntityId(contentId.value),
                        delta = ContentFieldDelta(ContentField.QUESTION, DeltaOperation.SET, "Edited question")
                    )
                )
            )
            assertIs<ReviewDeltaApplyResult.Applied>(
                target.reviewDeltaSyncService!!.applyRemote(remote("review-change", 2, fact))
            )
            assertEquals("Edited question", target.contentRepository!!.findById(contentId)!!.text.primaryText)
            assertEquals(1, target.memoryStateRepository!!.findAll().single().reviewCount)
        }
    }

    private fun quarantine(context: LearningApplicationContext, remote: RemoteSyncChange): String =
        assertIs<ReviewDeltaApplyResult.Quarantined>(context.reviewDeltaSyncService!!.applyRemote(remote)).diagnostic.code

    private fun review(
        context: LearningApplicationContext,
        id: String,
        at: Long,
        rating: ReviewRating
    ): ReviewResult = context.engine.review(
        ReviewCommand(ReviewEventId(id), learner, itemId, rating, Moment(at), source = RatingSource.MANUAL_USER)
    )

    private fun delta(result: ReviewResult, predecessor: String?) = ReviewEventDelta(
        result.reviewEvent.id.value, itemId.value, learner.value,
        contentId = contentId.value,
        rating = result.reviewEvent.rating.name,
        reviewedAtEpochMillis = result.reviewEvent.reviewedAt.epochMillis,
        responseTimeMillis = result.reviewEvent.responseTime?.millis,
        ratingSource = result.reviewEvent.source.name,
        predecessorReviewEventId = predecessor,
        stateBefore = result.reviewEvent.stateBefore.toProof(),
        expectedStateAfter = result.reviewEvent.stateAfter.toProof()
    )

    private fun remote(eventId: String, revision: Long, delta: ReviewEventDelta, version: Int = 1) =
        RemoteSyncChange(
            SyncRevision(revision), OutboundSyncChange(
                account, SyncEventId(eventId), IdempotencyKey("key-$eventId"), SyncDeviceId("remote"),
                SyncEntityId(delta.learningItemId), version, delta
            )
        )

    private fun seeded(root: java.nio.file.Path): LearningApplicationContext =
        LearningApplicationFactory.createPersisted(root, false).also { context ->
            context.contentRepository!!.save(content())
            context.learningItemRepository!!.save(
                LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
            )
        }

    private fun content() = Content(
        contentId, ContentType.WORD,
        ContentText("Question", "Answer", exampleText = "Example", exampleTranslation = "Translation"),
        customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("unknown"), "preserved")))
    )

    private inline fun withPair(prefix: String, block: (LearningApplicationContext, LearningApplicationContext) -> Unit) {
        val firstRoot = Files.createTempDirectory("${prefix}first-")
        val secondRoot = Files.createTempDirectory("${prefix}second-")
        try { block(seeded(firstRoot), seeded(secondRoot)) }
        finally { firstRoot.toFile().deleteRecursively(); secondRoot.toFile().deleteRecursively() }
    }
}
