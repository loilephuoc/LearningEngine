package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class StudyQueueProgressTest {

    @Test
    fun `maps new queue to read-only progress model`() {
        val snapshot =
            createSnapshot(
                itemCount = 3
            )

        val progress =
            StudyQueueProgress.from(
                snapshot
            )

        assertEquals(
            expected =
                snapshot.sessionId,
            actual =
                progress.sessionId
        )

        assertEquals(
            expected =
                snapshot.createdAt,
            actual =
                progress.createdAt
        )

        assertEquals(
            expected = 3,
            actual =
                progress.totalItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 3,
            actual =
                progress.remainingItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.currentIndex
        )

        assertEquals(
            expected = itemId(1),
            actual =
                progress
                    .currentLearningItemId
        )

        assertNull(
            progress.previousLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                progress.nextLearningItemId
        )

        assertTrue(
            progress
                .completedLearningItemIds
                .isEmpty()
        )

        assertEquals(
            expected =
                listOf(
                    itemId(1),
                    itemId(2),
                    itemId(3)
                ),
            actual =
                progress
                    .remainingLearningItemIds
        )

        assertEquals(
            expected =
                listOf(
                    itemId(2),
                    itemId(3)
                ),
            actual =
                progress
                    .pendingLearningItemIds
        )

        assertFalse(
            progress.isEmpty
        )

        assertTrue(
            progress.isAtStart
        )

        assertFalse(
            progress.isLastItem
        )

        assertFalse(
            progress.isCompleted
        )

        assertEquals(
            expected = 0.0,
            actual =
                progress.progress
        )

        assertEquals(
            expected = 0,
            actual =
                progress.percentComplete
        )
    }

    @Test
    fun `maps partially completed queue`() {
        val snapshot =
            createSnapshot(
                itemCount = 4
            )
                .advance()
                .advance()

        val progress =
            StudyQueueProgress.from(
                snapshot
            )

        assertEquals(
            expected = 2,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 2,
            actual =
                progress.remainingItemCount
        )

        assertEquals(
            expected = 2,
            actual =
                progress.currentIndex
        )

        assertEquals(
            expected = itemId(3),
            actual =
                progress
                    .currentLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                progress
                    .previousLearningItemId
        )

        assertEquals(
            expected = itemId(4),
            actual =
                progress
                    .nextLearningItemId
        )

        assertEquals(
            expected =
                listOf(
                    itemId(1),
                    itemId(2)
                ),
            actual =
                progress
                    .completedLearningItemIds
        )

        assertEquals(
            expected =
                listOf(
                    itemId(3),
                    itemId(4)
                ),
            actual =
                progress
                    .remainingLearningItemIds
        )

        assertEquals(
            expected =
                listOf(itemId(4)),
            actual =
                progress
                    .pendingLearningItemIds
        )

        assertFalse(
            progress.isAtStart
        )

        assertFalse(
            progress.isLastItem
        )

        assertFalse(
            progress.isCompleted
        )

        assertEquals(
            expected = 0.5,
            actual =
                progress.progress
        )

        assertEquals(
            expected = 50,
            actual =
                progress.percentComplete
        )
    }

    @Test
    fun `maps final current item without marking queue completed`() {
        val snapshot =
            createSnapshot(
                itemCount = 3
            )
                .advance()
                .advance()

        val progress =
            StudyQueueProgress.from(
                snapshot
            )

        assertEquals(
            expected = itemId(3),
            actual =
                progress
                    .currentLearningItemId
        )

        assertEquals(
            expected = 1,
            actual =
                progress.remainingItemCount
        )

        assertTrue(
            progress.isLastItem
        )

        assertFalse(
            progress.isCompleted
        )

        assertEquals(
            expected = 66,
            actual =
                progress.percentComplete
        )
    }

    @Test
    fun `maps completed queue to full progress`() {
        val snapshot =
            createSnapshot(
                itemCount = 2
            )
                .advance()
                .advance()

        val progress =
            StudyQueueProgress.from(
                snapshot
            )

        assertEquals(
            expected = 2,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.remainingItemCount
        )

        assertNull(
            progress.currentLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                progress
                    .previousLearningItemId
        )

        assertNull(
            progress.nextLearningItemId
        )

        assertTrue(
            progress.isCompleted
        )

        assertFalse(
            progress.isLastItem
        )

        assertEquals(
            expected = 1.0,
            actual =
                progress.progress
        )

        assertEquals(
            expected = 100,
            actual =
                progress.percentComplete
        )
    }

    @Test
    fun `maps empty queue to completed progress`() {
        val snapshot =
            createSnapshot(
                itemCount = 0
            )

        val progress =
            StudyQueueProgress.from(
                snapshot
            )

        assertEquals(
            expected = 0,
            actual =
                progress.totalItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.completedItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                progress.remainingItemCount
        )

        assertTrue(
            progress.isEmpty
        )

        assertTrue(
            progress.isCompleted
        )

        assertFalse(
            progress.isAtStart
        )

        assertFalse(
            progress.isLastItem
        )

        assertEquals(
            expected = 1.0,
            actual =
                progress.progress
        )

        assertEquals(
            expected = 100,
            actual =
                progress.percentComplete
        )
    }

    private fun createSnapshot(
        itemCount: Int
    ): StudyQueueSnapshot =
        StudyQueueSnapshot.create(
            sessionId =
                SessionId("session-1"),
            createdAt =
                Moment(1_000L),
            learningItemIds =
                (1..itemCount)
                    .map(::itemId)
        )

    private fun itemId(
        number: Int
    ): LearningItemId =
        LearningItemId(
            "item-$number"
        )
}