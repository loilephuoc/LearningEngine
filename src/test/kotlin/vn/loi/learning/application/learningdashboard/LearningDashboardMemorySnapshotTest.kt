package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.application.progress.LearningStageCounts

class LearningDashboardMemorySnapshotTest {

    @Test
    fun `snapshot preserves composed learning stage counts`() {
        val stageCounts =
            LearningStageCounts(
                newCount = 1,
                learningCount = 2,
                reviewCount = 3,
                relearningCount = 4,
                masteredCount = 5,
                suspendedCount = 6
            )

        val snapshot =
            LearningDashboardMemorySnapshot(
                stageCounts = stageCounts
            )

        assertSame(
            expected = stageCounts,
            actual = snapshot.stageCounts
        )
    }

    @Test
    fun `total memories delegate to learning stage counts`() {
        val snapshot =
            LearningDashboardMemorySnapshot(
                stageCounts =
                    LearningStageCounts(
                        newCount = 1,
                        learningCount = 2,
                        reviewCount = 3,
                        relearningCount = 4,
                        masteredCount = 5,
                        suspendedCount = 6
                    )
            )

        assertEquals(
            expected = 21,
            actual = snapshot.totalMemories
        )
    }

    @Test
    fun `active memories exclude suspended memories`() {
        val snapshot =
            LearningDashboardMemorySnapshot(
                stageCounts =
                    LearningStageCounts(
                        newCount = 1,
                        learningCount = 2,
                        reviewCount = 3,
                        relearningCount = 4,
                        masteredCount = 5,
                        suspendedCount = 6
                    )
            )

        assertEquals(
            expected = 15,
            actual = snapshot.activeMemories
        )
    }

    @Test
    fun `snapshot reports whether memories exist`() {
        val snapshot =
            LearningDashboardMemorySnapshot(
                stageCounts =
                    LearningStageCounts(
                        newCount = 1,
                        learningCount = 0,
                        reviewCount = 0,
                        relearningCount = 0,
                        masteredCount = 0,
                        suspendedCount = 0
                    )
            )

        assertTrue(
            actual = snapshot.hasMemories
        )
    }

    @Test
    fun `empty snapshot contains no memories`() {
        val snapshot =
            LearningDashboardMemorySnapshot.EMPTY

        assertEquals(
            expected = LearningStageCounts.EMPTY,
            actual = snapshot.stageCounts
        )

        assertEquals(
            expected = 0,
            actual = snapshot.totalMemories
        )

        assertEquals(
            expected = 0,
            actual = snapshot.activeMemories
        )

        assertFalse(
            actual = snapshot.hasMemories
        )
    }
}