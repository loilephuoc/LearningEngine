package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LearningDashboardSchedulingSnapshotTest {

    @Test
    fun `snapshot preserves composed due statistics`() {
        val dueStatistics =
            LearningDashboardDueStatistics(
                dueCount = 5,
                overdueCount = 2
            )

        val snapshot =
            LearningDashboardSchedulingSnapshot(
                dueStatistics = dueStatistics
            )

        assertSame(
            expected = dueStatistics,
            actual = snapshot.dueStatistics
        )
    }

    @Test
    fun `snapshot reports due memories through due statistics`() {
        val snapshot =
            LearningDashboardSchedulingSnapshot(
                dueStatistics =
                    LearningDashboardDueStatistics(
                        dueCount = 3,
                        overdueCount = 0
                    )
            )

        assertTrue(
            actual = snapshot.hasDueMemories
        )

        assertFalse(
            actual = snapshot.hasOverdueMemories
        )
    }

    @Test
    fun `snapshot reports overdue memories through due statistics`() {
        val snapshot =
            LearningDashboardSchedulingSnapshot(
                dueStatistics =
                    LearningDashboardDueStatistics(
                        dueCount = 4,
                        overdueCount = 2
                    )
            )

        assertTrue(
            actual = snapshot.hasDueMemories
        )

        assertTrue(
            actual = snapshot.hasOverdueMemories
        )
    }

    @Test
    fun `empty snapshot contains empty due statistics`() {
        val snapshot =
            LearningDashboardSchedulingSnapshot.EMPTY

        assertEquals(
            expected =
                LearningDashboardDueStatistics.EMPTY,
            actual = snapshot.dueStatistics
        )

        assertFalse(
            actual = snapshot.hasDueMemories
        )

        assertFalse(
            actual = snapshot.hasOverdueMemories
        )
    }
}