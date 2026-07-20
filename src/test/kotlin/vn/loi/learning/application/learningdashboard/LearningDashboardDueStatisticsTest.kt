package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningDashboardDueStatisticsTest {

    @Test
    fun `due now count excludes overdue memories`() {
        val statistics =
            LearningDashboardDueStatistics(
                dueCount = 7,
                overdueCount = 3
            )

        assertEquals(
            expected = 4,
            actual = statistics.dueNowCount
        )
    }

    @Test
    fun `statistics report due and overdue memories`() {
        val statistics =
            LearningDashboardDueStatistics(
                dueCount = 5,
                overdueCount = 2
            )

        assertTrue(
            actual = statistics.hasDueMemories
        )

        assertTrue(
            actual = statistics.hasOverdueMemories
        )
    }

    @Test
    fun `statistics may contain due memories without overdue memories`() {
        val statistics =
            LearningDashboardDueStatistics(
                dueCount = 3,
                overdueCount = 0
            )

        assertEquals(
            expected = 3,
            actual = statistics.dueNowCount
        )

        assertTrue(
            actual = statistics.hasDueMemories
        )

        assertFalse(
            actual = statistics.hasOverdueMemories
        )
    }

    @Test
    fun `empty statistics contain no due memories`() {
        val statistics =
            LearningDashboardDueStatistics.EMPTY

        assertEquals(
            expected = 0,
            actual = statistics.dueCount
        )

        assertEquals(
            expected = 0,
            actual = statistics.overdueCount
        )

        assertEquals(
            expected = 0,
            actual = statistics.dueNowCount
        )

        assertFalse(
            actual = statistics.hasDueMemories
        )

        assertFalse(
            actual = statistics.hasOverdueMemories
        )
    }

    @Test
    fun `due count must not be negative`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardDueStatistics(
                dueCount = -1,
                overdueCount = 0
            )
        }
    }

    @Test
    fun `overdue count must not be negative`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardDueStatistics(
                dueCount = 0,
                overdueCount = -1
            )
        }
    }

    @Test
    fun `overdue count must not exceed due count`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardDueStatistics(
                dueCount = 2,
                overdueCount = 3
            )
        }
    }
}