package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningEngineFactory

class UpdateActiveStudySessionLimitsIntegrationTest {

    private val learnerId = LearnerId("learner-uat-1")

    @Test
    fun `changing new limit from 100 to 15 after reviews preserves completed items and invariants`() {
        val fixture = createFixture(newCount = 30, reviewCount = 0)
        val sessionId = SessionId("limit-new-100-to-15")

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = fixture.now,
                policy = SessionPolicy(newItemLimit = 100, reviewItemLimit = 100)
            )
        )

        // Review 3 new items
        for (i in 1..3) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.now
                )
            )
        }

        val sessionBefore = assertNotNull(fixture.engine.getSession(sessionId))
        assertEquals(3, sessionBefore.totalReviews)
        assertEquals(3, sessionBefore.newItemsReviewed)

        // Fetch current presented item before limit change
        val currentBefore = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))

        // Change New limit 100 -> 15
        val updatedSession = fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 15, reviewLimit = 100)
        assertEquals(15, updatedSession.policy.newItemLimit)

        val queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(15, queue.configuredNewTarget)
        assertEquals(15, queue.effectiveNewWorkload)
        assertEquals(15, queue.learningItemIds.size)
        assertEquals(3, queue.currentIndex)
        assertEquals(3, queue.completedItemCount)
        assertEquals(12, queue.remainingItemCount)

        // Progress invariants must hold
        val next = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
        assertEquals(currentBefore.item.learningItem.id, next.item.learningItem.id)
        assertNotNull(next.progress)
        assertEquals(3, next.progress!!.completedItemCount)
        assertEquals(3, next.progress!!.reviewedItemCount)
        assertEquals(15, next.progress!!.totalItemCount)
        assertEquals(12, next.progress!!.remainingItemCount)
        assertEquals(4, next.progress!!.currentPosition)
    }

    @Test
    fun `changing review limit after reviews preserves completed items and progress`() {
        val fixture = createFixture(newCount = 0, reviewCount = 30)
        val sessionId = SessionId("limit-review-100-to-15")

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = fixture.dueAt,
                policy = SessionPolicy(newItemLimit = 0, reviewItemLimit = 100)
            )
        )

        // Review 4 review items
        for (i in 1..4) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.dueAt))
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-review-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.dueAt
                )
            )
        }

        val sessionBefore = assertNotNull(fixture.engine.getSession(sessionId))
        assertEquals(4, sessionBefore.totalReviews)
        assertEquals(4, sessionBefore.reviewItemsReviewed)

        // Update Review limit from 100 to 15
        val updated = fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 0, reviewLimit = 15)
        assertEquals(15, updated.policy.reviewItemLimit)

        val queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(15, queue.configuredReviewTarget)
        assertEquals(15, queue.effectiveReviewWorkload)
        assertEquals(15, queue.learningItemIds.size)
        assertEquals(4, queue.currentIndex)
        assertEquals(11, queue.remainingItemCount)

        val next = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.dueAt))
        assertNotNull(next.progress)
        assertEquals(4, next.progress!!.completedItemCount)
        assertEquals(4, next.progress!!.reviewedItemCount)
        assertEquals(15, next.progress!!.totalItemCount)
    }

    @Test
    fun `multiple limit increases and decreases within same session maintain correct state`() {
        val fixture = createFixture(newCount = 30, reviewCount = 0)
        val sessionId = SessionId("multiple-limit-changes")

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = fixture.now,
                policy = SessionPolicy(newItemLimit = 20, reviewItemLimit = 0)
            )
        )

        // Review 2 items
        for (i in 1..2) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-multi-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.now
                )
            )
        }

        // Lower limit 20 -> 10
        fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 10, reviewLimit = 0)
        var queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(10, queue.learningItemIds.size)
        assertEquals(2, queue.currentIndex)
        assertEquals(8, queue.remainingItemCount)

        // Review 2 more items (total 4 reviewed)
        for (i in 3..4) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-multi-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.now
                )
            )
        }

        // Lower limit 10 -> 5
        fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 5, reviewLimit = 0)
        queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(5, queue.learningItemIds.size)
        assertEquals(4, queue.currentIndex)
        assertEquals(1, queue.remainingItemCount)

        // Increase limit 5 -> 25
        fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 25, reviewLimit = 0)
        queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(25, queue.learningItemIds.size)
        assertEquals(4, queue.currentIndex)
        assertEquals(21, queue.remainingItemCount)
        assertEquals(queue.learningItemIds.distinct().size, queue.learningItemIds.size)

        // Verify next item opens without exception
        val next = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
        assertEquals(4, next.progress!!.completedItemCount)
        assertEquals(25, next.progress!!.totalItemCount)
    }

    @Test
    fun `replan failure rolls back atomically leaving previous session and queue intact`() {
        val fixture = createFixture(newCount = 10, reviewCount = 0)
        val sessionId = SessionId("rollback-session")

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = fixture.now,
                policy = SessionPolicy(newItemLimit = 10, reviewItemLimit = 0)
            )
        )

        // Review 3 items
        for (i in 1..3) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-rb-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.now
                )
            )
        }

        // Attempting to set limit < completed items (e.g. 2 < 3) must throw
        assertFailsWith<IllegalArgumentException> {
            fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 2, reviewLimit = 0)
        }

        // Previous session policy and queue must remain completely untouched
        val session = assertNotNull(fixture.engine.getSession(sessionId))
        assertEquals(10, session.policy.newItemLimit)
        val queue = assertNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(10, queue.configuredNewTarget)
        assertEquals(3, queue.currentIndex)
        assertEquals(7, queue.remainingItemCount)

        // Session continues normally
        val next = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
        assertEquals(3, next.progress!!.completedItemCount)
    }

    @Test
    fun `persisted queue count mismatch is recovered self-healingly on reopen`() {
        val fixture = createFixture(newCount = 10, reviewCount = 0)
        val sessionId = SessionId("mismatched-recovery-session")

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = fixture.now,
                policy = SessionPolicy(newItemLimit = 10, reviewItemLimit = 0)
            )
        )

        // Review 3 items
        val reviewedIds = mutableListOf<LearningItemId>()
        for (i in 1..3) {
            val item = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
            reviewedIds.add(item.item.learningItem.id)
            fixture.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("rev-heal-$i"),
                    learningItemId = item.item.learningItem.id,
                    rating = ReviewRating.GOOD,
                    reviewedAt = fixture.now
                )
            )
        }

        // Simulate corrupted legacy state: queue replaced with only 5 remaining items and currentIndex = 0
        val corruptedRemaining = fixture.newItemIds.filterNot { it in reviewedIds }.take(5).toList()
        val corruptedSnapshot = StudyQueueSnapshot(
            sessionId = sessionId,
            createdAt = fixture.now,
            learningItemIds = corruptedRemaining,
            currentIndex = 0,
            itemOrigins = corruptedRemaining.associateWith { SessionItemOrigin.NEW },
            configuredNewTarget = 5,
            effectiveNewWorkload = 5
        )
        fixture.context.studyQueue.save(corruptedSnapshot)

        // Opening next item must self-heal the queue without throwing invariant exception
        val next = assertNotNull(fixture.engine.getNextSessionItem(sessionId, fixture.now))
        assertNotNull(next.progress)
        assertTrue(next.progress!!.completedItemCount >= 3)
        assertEquals(3, next.progress!!.reviewedItemCount)
    }

    private fun createFixture(newCount: Int, reviewCount: Int): Fixture {
        val context = vn.loi.learning.infrastructure.LearningApplicationFactory.createInMemory()
        val engine = context.engine
        val now = Moment(1_000_000L)

        val dueTimes = mutableListOf<Moment>()
        val reviewItemIds = (1..reviewCount).map { i ->
            val cid = ContentId("content-rev-$i")
            val lid = LearningItemId("item-rev-$i")
            engine.registerContent(Content(cid, ContentType.SENTENCE, ContentText("Sentence rev $i")))
            engine.registerLearningItem(LearningItem(lid, cid, LearningMode.MEANING_RECOGNITION))
            val res = engine.review(
                ReviewCommand(
                    reviewEventId = ReviewEventId("prep-rev-$i"),
                    learnerId = learnerId,
                    learningItemId = lid,
                    rating = ReviewRating.GOOD,
                    reviewedAt = now
                )
            )
            dueTimes.add(res.memoryState.dueAt)
            lid
        }.toSet()

        val newItemIds = (1..newCount).map { i ->
            val cid = ContentId("content-new-$i")
            val lid = LearningItemId("item-new-$i")
            engine.registerContent(Content(cid, ContentType.SENTENCE, ContentText("Sentence new $i")))
            engine.registerLearningItem(LearningItem(lid, cid, LearningMode.MEANING_RECOGNITION))
            lid
        }.toSet()

        val dueAt = dueTimes.maxOrNull() ?: now
        return Fixture(context, engine, now, dueAt, newItemIds, reviewItemIds)
    }

    private data class Fixture(
        val context: vn.loi.learning.infrastructure.LearningApplicationContext,
        val engine: LearningEngine,
        val now: Moment,
        val dueAt: Moment,
        val newItemIds: Set<LearningItemId>,
        val reviewItemIds: Set<LearningItemId>
    )
}
