package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Retrievability

class LearningDashboardRetentionStatisticsTest {

    @Test
    fun `statistics preserve average retrievability and evaluated count`() {
        val average =
            Retrievability(0.87)

        val statistics =
            LearningDashboardRetentionStatistics(
                averageRetrievability = average,
                evaluatedMemoryCount = 12
            )

        assertEquals(
            expected = average,
            actual = statistics.averageRetrievability
        )

        assertEquals(
            expected = 12,
            actual = statistics.evaluatedMemoryCount
        )

        assertTrue(
            actual = statistics.hasData
        )
    }

    @Test
    fun `empty statistics contain no retention data`() {
        val statistics =
            LearningDashboardRetentionStatistics.EMPTY

        assertNull(
            actual = statistics.averageRetrievability
        )

        assertEquals(
            expected = 0,
            actual = statistics.evaluatedMemoryCount
        )

        assertFalse(
            actual = statistics.hasData
        )
    }

    @Test
    fun `evaluated memory count must not be negative`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardRetentionStatistics(
                averageRetrievability = null,
                evaluatedMemoryCount = -1
            )
        }
    }

    @Test
    fun `average must be absent when evaluated count is zero`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardRetentionStatistics(
                averageRetrievability =
                    Retrievability(0.9),
                evaluatedMemoryCount = 0
            )
        }
    }

    @Test
    fun `average must be present when evaluated count is positive`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardRetentionStatistics(
                averageRetrievability = null,
                evaluatedMemoryCount = 1
            )
        }
    }
}