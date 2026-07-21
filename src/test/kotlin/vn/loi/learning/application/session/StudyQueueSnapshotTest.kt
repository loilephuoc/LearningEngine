package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
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
    fun `queue rejects duplicate learning item ids`() {
        val duplicatedItemId =
            LearningItemId("item-1")

        assertFailsWith<
                IllegalArgumentException
                > {
            StudyQueueSnapshot.create(
                sessionId =
                    SessionId("session-1"),
                createdAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        duplicatedItemId,
                        duplicatedItemId
                    )
            )
        }
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