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

class StudyQueueSnapshotNavigationTest {

    @Test
    fun `new queue exposes current next remaining and zero progress`() {
        val snapshot =
            createSnapshot(
                itemCount = 4
            )

        assertEquals(
            expected = itemId(1),
            actual =
                snapshot.currentLearningItemId
        )

        assertNull(
            snapshot.previousLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                snapshot.nextLearningItemId
        )

        assertEquals(
            expected =
                listOf(
                    itemId(1),
                    itemId(2),
                    itemId(3),
                    itemId(4)
                ),
            actual =
                snapshot
                    .remainingLearningItemIds
        )

        assertEquals(
            expected =
                listOf(
                    itemId(2),
                    itemId(3),
                    itemId(4)
                ),
            actual =
                snapshot
                    .pendingLearningItemIds
        )

        assertTrue(
            snapshot
                .completedLearningItemIds
                .isEmpty()
        )

        assertEquals(
            expected = 0,
            actual =
                snapshot.completedItemCount
        )

        assertEquals(
            expected = 4,
            actual =
                snapshot.remainingItemCount
        )

        assertEquals(
            expected = 0.0,
            actual =
                snapshot.progress
        )

        assertEquals(
            expected = 0,
            actual =
                snapshot.percentComplete
        )

        assertTrue(
            snapshot.isAtStart
        )

        assertFalse(
            snapshot.isLastItem
        )

        assertFalse(
            snapshot.isCompleted
        )
    }

    @Test
    fun `advanced queue exposes completed current previous and pending items`() {
        val snapshot =
            createSnapshot(
                itemCount = 4
            )
                .advance()
                .advance()

        assertEquals(
            expected = itemId(3),
            actual =
                snapshot.currentLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                snapshot.previousLearningItemId
        )

        assertEquals(
            expected = itemId(4),
            actual =
                snapshot.nextLearningItemId
        )

        assertEquals(
            expected =
                listOf(
                    itemId(1),
                    itemId(2)
                ),
            actual =
                snapshot
                    .completedLearningItemIds
        )

        assertEquals(
            expected =
                listOf(
                    itemId(3),
                    itemId(4)
                ),
            actual =
                snapshot
                    .remainingLearningItemIds
        )

        assertEquals(
            expected =
                listOf(
                    itemId(4)
                ),
            actual =
                snapshot
                    .pendingLearningItemIds
        )

        assertEquals(
            expected = 2,
            actual =
                snapshot.completedItemCount
        )

        assertEquals(
            expected = 2,
            actual =
                snapshot.remainingItemCount
        )

        assertEquals(
            expected = 0.5,
            actual =
                snapshot.progress
        )

        assertEquals(
            expected = 50,
            actual =
                snapshot.percentComplete
        )

        assertFalse(
            snapshot.isAtStart
        )

        assertFalse(
            snapshot.isLastItem
        )
    }

    @Test
    fun `last current item is reported before queue completes`() {
        val snapshot =
            createSnapshot(
                itemCount = 3
            )
                .advance()
                .advance()

        assertEquals(
            expected = itemId(3),
            actual =
                snapshot.currentLearningItemId
        )

        assertEquals(
            expected = itemId(2),
            actual =
                snapshot.previousLearningItemId
        )

        assertNull(
            snapshot.nextLearningItemId
        )

        assertTrue(
            snapshot.isLastItem
        )

        assertFalse(
            snapshot.isCompleted
        )

        assertEquals(
            expected = 2,
            actual =
                snapshot.completedItemCount
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.remainingItemCount
        )

        assertEquals(
            expected =
                listOf(itemId(3)),
            actual =
                snapshot
                    .remainingLearningItemIds
        )

        assertTrue(
            snapshot
                .pendingLearningItemIds
                .isEmpty()
        )

        assertEquals(
            expected = 66,
            actual =
                snapshot.percentComplete
        )
    }

    @Test
    fun `completed queue exposes full completion state`() {
        val snapshot =
            createSnapshot(
                itemCount = 3
            )
                .advance()
                .advance()
                .advance()

        assertTrue(
            snapshot.isCompleted
        )

        assertFalse(
            snapshot.isLastItem
        )

        assertFalse(
            snapshot.isAtStart
        )

        assertNull(
            snapshot.currentLearningItemId
        )

        assertEquals(
            expected = itemId(3),
            actual =
                snapshot.previousLearningItemId
        )

        assertNull(
            snapshot.nextLearningItemId
        )

        assertEquals(
            expected =
                listOf(
                    itemId(1),
                    itemId(2),
                    itemId(3)
                ),
            actual =
                snapshot
                    .completedLearningItemIds
        )

        assertTrue(
            snapshot
                .remainingLearningItemIds
                .isEmpty()
        )

        assertTrue(
            snapshot
                .pendingLearningItemIds
                .isEmpty()
        )

        assertEquals(
            expected = 3,
            actual =
                snapshot.completedItemCount
        )

        assertEquals(
            expected = 0,
            actual =
                snapshot.remainingItemCount
        )

        assertEquals(
            expected = 1.0,
            actual =
                snapshot.progress
        )

        assertEquals(
            expected = 100,
            actual =
                snapshot.percentComplete
        )
    }

    @Test
    fun `empty queue is completed with full progress`() {
        val snapshot =
            createSnapshot(
                itemCount = 0
            )

        assertTrue(
            snapshot.isEmpty
        )

        assertTrue(
            snapshot.isCompleted
        )

        assertFalse(
            snapshot.isAtStart
        )

        assertFalse(
            snapshot.isLastItem
        )

        assertNull(
            snapshot.currentLearningItemId
        )

        assertNull(
            snapshot.previousLearningItemId
        )

        assertNull(
            snapshot.nextLearningItemId
        )

        assertTrue(
            snapshot
                .completedLearningItemIds
                .isEmpty()
        )

        assertTrue(
            snapshot
                .remainingLearningItemIds
                .isEmpty()
        )

        assertTrue(
            snapshot
                .pendingLearningItemIds
                .isEmpty()
        )

        assertEquals(
            expected = 1.0,
            actual =
                snapshot.progress
        )

        assertEquals(
            expected = 100,
            actual =
                snapshot.percentComplete
        )
    }

    @Test
    fun `peek next reads relative positions without changing queue`() {
        val snapshot =
            createSnapshot(
                itemCount = 4
            )
                .advance()

        assertEquals(
            expected = itemId(2),
            actual =
                snapshot.peekNext(
                    offset = 0
                )
        )

        assertEquals(
            expected = itemId(3),
            actual =
                snapshot.peekNext(
                    offset = 1
                )
        )

        assertEquals(
            expected = itemId(4),
            actual =
                snapshot.peekNext(
                    offset = 2
                )
        )

        assertNull(
            snapshot.peekNext(
                offset = 3
            )
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.currentIndex
        )

        assertEquals(
            expected = itemId(2),
            actual =
                snapshot.currentLearningItemId
        )
    }

    @Test
    fun `peek next rejects negative offset`() {
        val snapshot =
            createSnapshot(
                itemCount = 2
            )

        assertFailsWith<
                IllegalArgumentException
                > {
            snapshot.peekNext(
                offset = -1
            )
        }
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