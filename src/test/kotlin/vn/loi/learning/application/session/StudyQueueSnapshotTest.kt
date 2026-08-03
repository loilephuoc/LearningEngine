package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId

class StudyQueueSnapshotTest {

    @Test
    fun `create preserves stable learning item order`() {
        val firstItemId =
            LearningItemId("item-1")

        val secondItemId =
            LearningItemId("item-2")

        val queue =
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        firstItemId,
                        secondItemId
                    )
            )

        assertEquals(
            listOf(
                firstItemId,
                secondItemId
            ),
            queue.learningItemIds
        )

        assertEquals(
            firstItemId,
            queue.currentLearningItemId
        )

        assertEquals(
            2,
            queue.totalItemCount
        )

        assertEquals(
            0,
            queue.completedItemCount
        )

        assertEquals(
            2,
            queue.remainingItemCount
        )

        assertFalse(queue.isEmpty)
        assertFalse(queue.isCompleted)
    }

    @Test
    fun `advance returns next immutable queue snapshot`() {
        val firstItemId =
            LearningItemId("item-1")

        val secondItemId =
            LearningItemId("item-2")

        val originalQueue =
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        firstItemId,
                        secondItemId
                    )
            )

        val advancedQueue =
            originalQueue.advance()

        assertEquals(
            firstItemId,
            originalQueue
                .currentLearningItemId
        )

        assertEquals(
            0,
            originalQueue.currentIndex
        )

        assertEquals(
            secondItemId,
            advancedQueue
                .currentLearningItemId
        )

        assertEquals(
            1,
            advancedQueue.currentIndex
        )

        assertEquals(
            1,
            advancedQueue
                .completedItemCount
        )

        assertEquals(
            1,
            advancedQueue
                .remainingItemCount
        )
    }

    @Test
    fun `advancing final item completes queue`() {
        val queue =
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        )
                    )
            )

        val completedQueue =
            queue.advance()

        assertTrue(
            completedQueue.isCompleted
        )

        assertNull(
            completedQueue
                .currentLearningItemId
        )

        assertEquals(
            1,
            completedQueue
                .completedItemCount
        )

        assertEquals(
            0,
            completedQueue
                .remainingItemCount
        )

        assertFailsWith<
                IllegalArgumentException
                > {
            completedQueue.advance()
        }
    }

    @Test
    fun `empty queue is immediately completed`() {
        val queue =
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    emptyList()
            )

        assertTrue(queue.isEmpty)
        assertTrue(queue.isCompleted)

        assertNull(
            queue.currentLearningItemId
        )

        assertEquals(
            0,
            queue.totalItemCount
        )

        assertEquals(
            0,
            queue.remainingItemCount
        )
    }

    @Test
    fun `coverage retry preserves fair access and defers a near-end reinforcement`() {
        val first = LearningItemId("item-1")
        val second = LearningItemId("item-2")
        val third = LearningItemId("item-3")
        val queue = StudyQueueSnapshot.create(
            SessionId("session-1"),
            Moment(1_000L),
            listOf(first, second, third)
        )

        val afterAgain = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertEquals(second, afterAgain.currentLearningItemId)
        assertEquals(listOf(first, second, first, third), afterAgain.learningItemIds)

        val afterHard = afterAgain.advanceCoverageReview(ReviewRating.HARD, false)
        assertEquals(first, afterHard.currentLearningItemId)
        assertEquals(listOf(first, second, first, third), afterHard.learningItemIds)
        assertTrue(afterHard.coverageReinforcementStates.getValue(second).deferred)
    }

    @Test
    fun `coverage completion discards pending retries and never exceeds target`() {
        val first = LearningItemId("item-1")
        val second = LearningItemId("item-2")
        val pendingRetry =
            StudyQueueSnapshot.create(
                SessionId("session-1"),
                Moment(1_000L),
                listOf(first, second)
            ).advanceCoverageReview(ReviewRating.AGAIN, false)

        val completed = pendingRetry.advanceCoverageReview(ReviewRating.GOOD, true)

        assertTrue(completed.isCompleted)
        assertEquals(listOf(first, second), completed.learningItemIds)
        assertEquals(2, completed.completedItemCount)
    }

    @Test
    fun `coverage rewind removes retry scheduled by undone review`() {
        val first = LearningItemId("item-1")
        val second = LearningItemId("item-2")
        val reviewed =
            StudyQueueSnapshot.create(
                SessionId("session-1"),
                Moment(1_000L),
                listOf(first, second)
            ).advanceCoverageReview(ReviewRating.AGAIN, false)

        val rewound = reviewed.rewindCoverageReview(first)

        assertEquals(first, rewound.currentLearningItemId)
        assertEquals(listOf(first, second), rewound.learningItemIds)
    }

    @Test
    fun `queue rejects index beyond queue size`() {
        assertFailsWith<
                IllegalArgumentException
                > {
            StudyQueueSnapshot(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId(
                            "item-1"
                        )
                    ),
                currentIndex = 2
            )
        }
    }
}
