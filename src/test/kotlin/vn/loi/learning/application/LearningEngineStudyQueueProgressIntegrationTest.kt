package vn.loi.learning.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
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
import vn.loi.learning.infrastructure.LearningEngineFactory

class LearningEngineStudyQueueProgressIntegrationTest {

    @Test
    fun `engine exposes current queue progress without modifying queue`() {
        val engine =
            createEngineWithItems(
                itemCount = 3
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
                        newItemLimit = 3,
                        reviewItemLimit = 3
                    )
            )
        )

        val firstRead =
            assertNotNull(
                engine.getStudyQueueProgress(
                    sessionId
                )
            )

        val secondRead =
            engine.requireStudyQueueProgress(
                sessionId
            )

        assertEquals(
            expected = firstRead,
            actual = secondRead
        )

        assertEquals(
            expected = 3,
            actual =
                firstRead.totalItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                firstRead.completedItemCount
        )

        assertEquals(
            expected = 3,
            actual =
                firstRead.remainingItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                firstRead.currentIndex
        )

        assertEquals(
            expected = 0,
            actual =
                firstRead.percentComplete
        )

        val persistedQueue =
            assertNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 0,
            actual =
                persistedQueue.currentIndex
        )

        assertEquals(
            expected =
                persistedQueue
                    .currentLearningItemId,
            actual =
                firstRead
                    .currentLearningItemId
        )
    }

    @Test
    fun `engine progress follows successful session review`() {
        val engine =
            createEngineWithItems(
                itemCount = 3
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
                        newItemLimit = 3,
                        reviewItemLimit = 3
                    )
            )
        )

        val initialProgress =
            engine.requireStudyQueueProgress(
                sessionId
            )

        val firstItem =
            assertNotNull(
                engine.getNextSessionItem(
                    sessionId = sessionId,
                    now = startedAt
                )
            )

        val reviewResult = engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-1"),
                learningItemId =
                    firstItem.item
                        .learningItem.id,
                rating =
                    ReviewRating.GOOD,
                reviewedAt =
                    startedAt
            )
        )

        val projected = assertNotNull(reviewResult.progress)
        assertEquals(1, projected.completedItemCount)
        assertEquals(1, projected.reviewedItemCount)
        assertEquals(0, projected.skippedItemCount)
        assertEquals(2, projected.remainingItemCount)

        val progressAfterReview =
            engine.requireStudyQueueProgress(
                sessionId
            )

        assertEquals(
            expected =
                initialProgress.currentIndex + 1,
            actual =
                progressAfterReview
                    .currentIndex
        )

        assertEquals(
            expected = 1,
            actual =
                progressAfterReview
                    .completedItemCount
        )

        assertEquals(
            expected = 2,
            actual =
                progressAfterReview
                    .remainingItemCount
        )

        assertEquals(
            expected = 33,
            actual =
                progressAfterReview
                    .percentComplete
        )

        assertEquals(
            expected =
                initialProgress
                    .currentLearningItemId,
            actual =
                progressAfterReview
                    .previousLearningItemId
        )

        assertEquals(
            expected =
                progressAfterReview
                    .remainingLearningItemIds
                    .first(),
            actual =
                progressAfterReview
                    .currentLearningItemId
        )
    }

    @Test
    fun `engine exposes completed progress before session finish`() {
        val engine =
            createEngineWithItems(
                itemCount = 1
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
                        newItemLimit = 1,
                        reviewItemLimit = 1
                    )
            )
        )

        val item =
            assertNotNull(
                engine.getNextSessionItem(
                    sessionId = sessionId,
                    now = startedAt
                )
            )

        val reviewResult = engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-1"),
                learningItemId =
                    item.item.learningItem.id,
                rating =
                    ReviewRating.GOOD,
                reviewedAt =
                    startedAt
            )
        )

        assertTrue(assertNotNull(reviewResult.progress).isCompleted)

        val progress =
            engine.requireStudyQueueProgress(
                sessionId
            )

        assertTrue(
            progress.isCompleted
        )

        assertEquals(
            expected = 1,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.remainingItemCount
        )

        assertEquals(
            expected = 100,
            actual =
                progress.percentComplete
        )

        assertNull(
            progress.currentLearningItemId
        )
    }

    @Test
    fun `finished session no longer exposes queue progress`() {
        val engine =
            createEngineWithItems(
                itemCount = 1
            )

        val sessionId =
            SessionId("session-1")

        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt =
                    Moment(1_000L),
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 1
                    )
            )
        )

        assertNotNull(
            engine.getStudyQueueProgress(
                sessionId
            )
        )

        engine.finishSession(
            sessionId = sessionId,
            finishedAt =
                Moment(2_000L)
        )

        assertNull(
            engine.getStudyQueueProgress(
                sessionId
            )
        )

        assertFailsWith<
                IllegalArgumentException
                > {
            engine.requireStudyQueueProgress(
                sessionId
            )
        }
    }

    @Test
    fun `missing session returns null progress`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        assertNull(
            engine.getStudyQueueProgress(
                SessionId(
                    "missing-session"
                )
            )
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
