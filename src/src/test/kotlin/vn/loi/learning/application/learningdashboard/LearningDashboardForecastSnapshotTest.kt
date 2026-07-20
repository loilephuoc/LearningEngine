package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Moment

class LearningDashboardForecastSnapshotTest {

    @Test
    fun `snapshot preserves composed forecast`() {
        val forecast =
            LearningDashboardForecast(
                buckets =
                    listOf(
                        LearningDashboardForecastBucket(
                            windowStart = Moment(1_000L),
                            windowEnd = Moment(2_000L),
                            dueCount = 2
                        )
                    )
            )

        val snapshot =
            LearningDashboardForecastSnapshot(
                forecast = forecast
            )

        assertSame(
            expected = forecast,
            actual = snapshot.forecast
        )
    }

    @Test
    fun `snapshot reports forecast buckets`() {
        val snapshot =
            LearningDashboardForecastSnapshot(
                forecast =
                    LearningDashboardForecast(
                        buckets =
                            listOf(
                                LearningDashboardForecastBucket(
                                    windowStart = Moment(1_000L),
                                    windowEnd = Moment(2_000L),
                                    dueCount = 0
                                )
                            )
                    )
            )

        assertTrue(
            actual = snapshot.hasBuckets
        )
    }

    @Test
    fun `snapshot reports forecast due memories`() {
        val snapshot =
            LearningDashboardForecastSnapshot(
                forecast =
                    LearningDashboardForecast(
                        buckets =
                            listOf(
                                LearningDashboardForecastBucket(
                                    windowStart = Moment(1_000L),
                                    windowEnd = Moment(2_000L),
                                    dueCount = 3
                                )
                            )
                    )
            )

        assertTrue(
            actual = snapshot.hasDueMemories
        )
    }

    @Test
    fun `snapshot may contain buckets without due memories`() {
        val snapshot =
            LearningDashboardForecastSnapshot(
                forecast =
                    LearningDashboardForecast(
                        buckets =
                            listOf(
                                LearningDashboardForecastBucket(
                                    windowStart = Moment(1_000L),
                                    windowEnd = Moment(2_000L),
                                    dueCount = 0
                                )
                            )
                    )
            )

        assertTrue(
            actual = snapshot.hasBuckets
        )

        assertFalse(
            actual = snapshot.hasDueMemories
        )
    }

    @Test
    fun `empty snapshot contains no forecast data`() {
        val snapshot =
            LearningDashboardForecastSnapshot.EMPTY

        assertEquals(
            expected =
                LearningDashboardForecast.EMPTY,
            actual = snapshot.forecast
        )

        assertFalse(
            actual = snapshot.hasBuckets
        )

        assertFalse(
            actual = snapshot.hasDueMemories
        )
    }
}