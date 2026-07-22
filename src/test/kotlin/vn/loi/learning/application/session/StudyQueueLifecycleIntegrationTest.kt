package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
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
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.LearningEngineFactory

class StudyQueueLifecycleIntegrationTest {

    @Test
    fun `session uses persisted queue from start through finish`() {
        val engine =
            createEngineWithItems(
                itemCount = 2
            )

        val sessionId =
            SessionId("session-1")

        val learnerId =
            LearnerId("learner-1")

        val startedAt =
            Moment(1_000L)

        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = startedAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 2
                    )
            )
        )

        val createdQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 2,
            actual =
                createdQueue.totalItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                createdQueue.currentIndex
        )

        val first =
            assertNotNull(
                engine.getNextSessionItem(
                    sessionId = sessionId,
                    now = startedAt
                )
            )

        assertEquals(
            expected =
                createdQueue.currentLearningItemId,
            actual =
                first.item.learningItem.id
        )

        engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-1"),
                learningItemId =
                    first.item.learningItem.id,
                rating =
                    ReviewRating.GOOD,
                reviewedAt =
                    startedAt
            )
        )

        val advancedQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 1,
            actual =
                advancedQueue.currentIndex
        )

        assertEquals(
            expected = 1,
            actual =
                advancedQueue.remainingItemCount
        )

        val second =
            assertNotNull(
                engine.getNextSessionItem(
                    sessionId = sessionId,
                    now = startedAt
                )
            )

        assertEquals(
            expected =
                advancedQueue.currentLearningItemId,
            actual =
                second.item.learningItem.id
        )

        engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-2"),
                learningItemId =
                    second.item.learningItem.id,
                rating =
                    ReviewRating.GOOD,
                reviewedAt =
                    startedAt
            )
        )

        val completedQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertTrue(
            completedQueue.isCompleted
        )

        assertNull(
            engine.getNextSessionItem(
                sessionId = sessionId,
                now = startedAt
            )
        )

        val finished =
            engine.finishSession(
                sessionId = sessionId,
                finishedAt =
                    Moment(2_000L)
            )

        assertEquals(
            expected =
                SessionStatus.FINISHED,
            actual =
                finished.status
        )

        assertNotNull(
            engine.getStudyQueue(
                sessionId
            )
        )
    }

    @Test
    fun `review rejects item that is not current queue item`() {
        val engine =
            createEngineWithItems(
                itemCount = 2
            )

        val sessionId =
            SessionId("session-1")

        val startedAt =
            Moment(1_000L)

        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt = startedAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 2
                    )
            )
        )

        val queue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 2,
            actual =
                queue.learningItemIds.size
        )

        val currentItemId =
            assertNotNull(
                queue.currentLearningItemId
            )

        val nonCurrentItemId =
            queue.learningItemIds
                .first { learningItemId ->
                    learningItemId !=
                            currentItemId
                }

        assertFailsWith<
                IllegalArgumentException
                > {
            engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId =
                        ReviewEventId(
                            "invalid-review"
                        ),
                    learningItemId =
                        nonCurrentItemId,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt =
                        startedAt
                )
            )
        }

        val unchangedQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 0,
            actual =
                unchangedQueue.currentIndex
        )

        assertEquals(
            expected =
                currentItemId,
            actual =
                unchangedQueue
                    .currentLearningItemId
        )

        val unchangedSession =
            assertNotNull(
                engine.getSession(
                    sessionId
                )
            )

        assertEquals(
            expected = 0,
            actual =
                unchangedSession.totalReviews
        )
    }

    @Test
    fun `session with no eligible items creates completed empty queue`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val sessionId =
            SessionId(
                "session-empty-queue"
            )

        val startedAt =
            Moment(1_000L)

        val session =
            engine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId =
                        LearnerId("learner-1"),
                    startedAt = startedAt,
                    policy =
                        SessionPolicy(
                            newItemLimit = 1,
                            reviewItemLimit = 1
                        )
                )
            )

        assertEquals(
            expected = 0,
            actual =
                session.totalReviews
        )

        val queue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertTrue(
            queue.isEmpty
        )

        assertTrue(
            queue.isCompleted
        )

        assertEquals(
            expected = 0,
            actual =
                queue.totalItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                queue.remainingItemCount
        )

        assertNull(
            queue.currentLearningItemId
        )

        assertNull(
            engine.getNextSessionItem(
                sessionId = sessionId,
                now = startedAt
            )
        )
    }

    @Test
    fun `finish removes queue and finished session rejects next item`() {
        val engine =
            createEngineWithItems(
                itemCount = 1
            )

        val sessionId =
            SessionId("session-finished")

        val startedAt =
            Moment(1_000L)

        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt = startedAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 1
                    )
            )
        )

        assertNotNull(
            engine.getStudyQueue(
                sessionId
            )
        )

        val finished =
            engine.finishSession(
                sessionId = sessionId,
                finishedAt =
                    Moment(2_000L)
            )

        assertEquals(
            expected =
                SessionStatus.FINISHED,
            actual =
                finished.status
        )

        assertNull(
            engine.getStudyQueue(
                sessionId
            )
        )

        assertFailsWith<
                IllegalArgumentException
                > {
            engine.getNextSessionItem(
                sessionId = sessionId,
                now =
                    Moment(2_000L)
            )
        }
    }

    @Test
    fun `successful review advances queue exactly once`() {
        val engine =
            createEngineWithItems(
                itemCount = 3
            )

        val sessionId =
            SessionId(
                "session-single-advance"
            )

        val startedAt =
            Moment(1_000L)

        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt = startedAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 3,
                        reviewItemLimit = 3
                    )
            )
        )

        val initialQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        val currentItemId =
            assertNotNull(
                initialQueue
                    .currentLearningItemId
            )

        engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-1"),
                learningItemId =
                    currentItemId,
                rating =
                    ReviewRating.GOOD,
                reviewedAt =
                    startedAt
            )
        )

        val advancedQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected =
                initialQueue.currentIndex + 1,
            actual =
                advancedQueue.currentIndex
        )

        assertEquals(
            expected =
                initialQueue.totalItemCount,
            actual =
                advancedQueue.totalItemCount
        )
    }

    private fun createEngineWithItems(
        itemCount: Int
    ): LearningEngine {
        val engine =
            LearningEngineFactory
                .createInMemory()

        repeat(itemCount) { index ->
            registerItem(
                engine = engine,
                number = index + 1
            )
        }

        return engine
    }

    private fun registerItem(
        engine: LearningEngine,
        number: Int
    ) {
        val content =
            Content(
                id =
                    ContentId(
                        "content-$number"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Sentence $number"
                    )
            )

        engine.registerContent(
            content
        )

        engine.registerLearningItem(
            LearningItem(
                id =
                    LearningItemId(
                        "item-$number"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )
    }
}
