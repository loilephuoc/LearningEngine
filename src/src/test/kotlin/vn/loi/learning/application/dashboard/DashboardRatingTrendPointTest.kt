package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DashboardRatingTrendPointTest {

    @Test
    fun `point exposes rating counts`() {
        val point =
            DashboardRatingTrendPoint(
                period = period(),
                totalReviews = 10,
                againCount = 1,
                hardCount = 2,
                goodCount = 4,
                easyCount = 3
            )

        assertEquals(
            expected = 10,
            actual = point.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = point.againCount
        )

        assertEquals(
            expected = 2,
            actual = point.hardCount
        )

        assertEquals(
            expected = 4,
            actual = point.goodCount
        )

        assertEquals(
            expected = 3,
            actual = point.easyCount
        )

        assertTrue(
            actual = point.hasReviewActivity
        )
    }

    @Test
    fun `point calculates rating proportions`() {
        val point =
            DashboardRatingTrendPoint(
                period = period(),
                totalReviews = 10,
                againCount = 1,
                hardCount = 2,
                goodCount = 4,
                easyCount = 3
            )

        assertEquals(
            expected = 0.1,
            actual = point.againProportion
        )

        assertEquals(
            expected = 0.2,
            actual = point.hardProportion
        )

        assertEquals(
            expected = 0.4,
            actual = point.goodProportion
        )

        assertEquals(
            expected = 0.3,
            actual = point.easyProportion
        )
    }

    @Test
    fun `empty point has no activity and no proportions`() {
        val point =
            DashboardRatingTrendPoint(
                period = period(),
                totalReviews = 0,
                againCount = 0,
                hardCount = 0,
                goodCount = 0,
                easyCount = 0
            )

        assertFalse(
            actual = point.hasReviewActivity
        )

        assertNull(
            actual = point.againProportion
        )

        assertNull(
            actual = point.hardProportion
        )

        assertNull(
            actual = point.goodProportion
        )

        assertNull(
            actual = point.easyProportion
        )
    }

    @Test
    fun `point can be created from daily statistics`() {
        val dailyStatistics =
            DashboardDailyStatistics(
                period = period(),
                statistics =
                    StudyStatistics(
                        totalReviews = 10,
                        againCount = 1,
                        hardCount = 2,
                        goodCount = 4,
                        easyCount = 3,
                        reviewsWithResponseTime = 0,
                        totalResponseTime = TimeSpan.ZERO,
                        averageResponseTimeMillis = null
                    )
            )

        val point =
            DashboardRatingTrendPoint.from(
                dailyStatistics = dailyStatistics
            )

        assertEquals(
            expected = dailyStatistics.period,
            actual = point.period
        )

        assertEquals(
            expected = 10,
            actual = point.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = point.againCount
        )

        assertEquals(
            expected = 2,
            actual = point.hardCount
        )

        assertEquals(
            expected = 4,
            actual = point.goodCount
        )

        assertEquals(
            expected = 3,
            actual = point.easyCount
        )
    }

    @Test
    fun `point rejects negative total reviews`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DashboardRatingTrendPoint(
                    period = period(),
                    totalReviews = -1,
                    againCount = 0,
                    hardCount = 0,
                    goodCount = 0,
                    easyCount = 0
                )
            }

        assertEquals(
            expected =
                "Total reviews must not be negative.",
            actual = exception.message
        )
    }

    @Test
    fun `point rejects negative rating count`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DashboardRatingTrendPoint(
                    period = period(),
                    totalReviews = 1,
                    againCount = -1,
                    hardCount = 0,
                    goodCount = 1,
                    easyCount = 1
                )
            }

        assertEquals(
            expected =
                "Again count must not be negative.",
            actual = exception.message
        )
    }

    @Test
    fun `point rejects rating counts different from total reviews`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                DashboardRatingTrendPoint(
                    period = period(),
                    totalReviews = 5,
                    againCount = 1,
                    hardCount = 1,
                    goodCount = 1,
                    easyCount = 1
                )
            }

        assertEquals(
            expected =
                "Rating counts must equal total reviews.",
            actual = exception.message
        )
    }

    private fun period(): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(1_000L),
            endExclusive = Moment(2_000L)
        )
}