package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardForecastBucketTest {

    @Test
    fun `bucket preserves its window and due count`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 4
            )

        assertEquals(
            expected = Moment(1_000L),
            actual = bucket.windowStart
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = bucket.windowEnd
        )

        assertEquals(
            expected = 4,
            actual = bucket.dueCount
        )
    }

    @Test
    fun `bucket excludes window start`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 0
            )

        assertFalse(
            actual =
                bucket.contains(
                    Moment(1_000L)
                )
        )
    }

    @Test
    fun `bucket includes moment after window start`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 0
            )

        assertTrue(
            actual =
                bucket.contains(
                    Moment(1_001L)
                )
        )
    }

    @Test
    fun `bucket includes window end`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 0
            )

        assertTrue(
            actual =
                bucket.contains(
                    Moment(2_000L)
                )
        )
    }

    @Test
    fun `bucket excludes moment after window end`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 0
            )

        assertFalse(
            actual =
                bucket.contains(
                    Moment(2_001L)
                )
        )
    }

    @Test
    fun `bucket reports whether it contains due memories`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 3
            )

        assertTrue(
            actual = bucket.hasDueMemories
        )
    }

    @Test
    fun `empty bucket reports no due memories`() {
        val bucket =
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = 0
            )

        assertFalse(
            actual = bucket.hasDueMemories
        )
    }

    @Test
    fun `window end must be after window start`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(1_000L),
                dueCount = 0
            )
        }
    }

    @Test
    fun `window end must not be before window start`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardForecastBucket(
                windowStart = Moment(2_000L),
                windowEnd = Moment(1_000L),
                dueCount = 0
            )
        }
    }

    @Test
    fun `due count must not be negative`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardForecastBucket(
                windowStart = Moment(1_000L),
                windowEnd = Moment(2_000L),
                dueCount = -1
            )
        }
    }
}