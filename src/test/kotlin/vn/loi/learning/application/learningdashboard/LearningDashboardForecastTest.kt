package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardForecastTest {

    @Test
    fun `forecast preserves composed buckets`() {
        val first =
            bucket(
                start = 1_000L,
                end = 2_000L,
                dueCount = 2
            )

        val second =
            bucket(
                start = 2_000L,
                end = 3_000L,
                dueCount = 3
            )

        val buckets =
            listOf(
                first,
                second
            )

        val forecast =
            LearningDashboardForecast(
                buckets = buckets
            )

        assertSame(
            expected = buckets,
            actual = forecast.buckets
        )
    }

    @Test
    fun `forecast calculates total due count from buckets`() {
        val forecast =
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 1_000L,
                            end = 2_000L,
                            dueCount = 2
                        ),
                        bucket(
                            start = 2_000L,
                            end = 3_000L,
                            dueCount = 3
                        ),
                        bucket(
                            start = 3_000L,
                            end = 4_000L,
                            dueCount = 4
                        )
                    )
            )

        assertEquals(
            expected = 9,
            actual = forecast.totalDueCount
        )
    }

    @Test
    fun `forecast reports whether buckets exist`() {
        val forecast =
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 1_000L,
                            end = 2_000L,
                            dueCount = 0
                        )
                    )
            )

        assertTrue(
            actual = forecast.hasBuckets
        )
    }

    @Test
    fun `forecast reports due memories across buckets`() {
        val forecast =
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 1_000L,
                            end = 2_000L,
                            dueCount = 0
                        ),
                        bucket(
                            start = 2_000L,
                            end = 3_000L,
                            dueCount = 1
                        )
                    )
            )

        assertTrue(
            actual = forecast.hasDueMemories
        )
    }

    @Test
    fun `forecast may contain buckets without due memories`() {
        val forecast =
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 1_000L,
                            end = 2_000L,
                            dueCount = 0
                        ),
                        bucket(
                            start = 2_000L,
                            end = 3_000L,
                            dueCount = 0
                        )
                    )
            )

        assertFalse(
            actual = forecast.hasDueMemories
        )
    }

    @Test
    fun `empty forecast contains no buckets or due memories`() {
        val forecast =
            LearningDashboardForecast.EMPTY

        assertEquals(
            expected = emptyList(),
            actual = forecast.buckets
        )

        assertEquals(
            expected = 0,
            actual = forecast.totalDueCount
        )

        assertFalse(
            actual = forecast.hasBuckets
        )

        assertFalse(
            actual = forecast.hasDueMemories
        )
    }

    @Test
    fun `forecast accepts adjacent buckets`() {
        LearningDashboardForecast(
            buckets =
                listOf(
                    bucket(
                        start = 1_000L,
                        end = 2_000L,
                        dueCount = 1
                    ),
                    bucket(
                        start = 2_000L,
                        end = 3_000L,
                        dueCount = 1
                    )
                )
        )
    }

    @Test
    fun `forecast accepts gaps between buckets`() {
        LearningDashboardForecast(
            buckets =
                listOf(
                    bucket(
                        start = 1_000L,
                        end = 2_000L,
                        dueCount = 1
                    ),
                    bucket(
                        start = 3_000L,
                        end = 4_000L,
                        dueCount = 1
                    )
                )
        )
    }

    @Test
    fun `forecast rejects overlapping buckets`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 1_000L,
                            end = 3_000L,
                            dueCount = 1
                        ),
                        bucket(
                            start = 2_000L,
                            end = 4_000L,
                            dueCount = 1
                        )
                    )
            )
        }
    }

    @Test
    fun `forecast rejects buckets in descending order`() {
        assertFailsWith<IllegalArgumentException> {
            LearningDashboardForecast(
                buckets =
                    listOf(
                        bucket(
                            start = 3_000L,
                            end = 4_000L,
                            dueCount = 1
                        ),
                        bucket(
                            start = 1_000L,
                            end = 2_000L,
                            dueCount = 1
                        )
                    )
            )
        }
    }

    private fun bucket(
        start: Long,
        end: Long,
        dueCount: Int
    ): LearningDashboardForecastBucket =
        LearningDashboardForecastBucket(
            windowStart = Moment(start),
            windowEnd = Moment(end),
            dueCount = dueCount
        )
}