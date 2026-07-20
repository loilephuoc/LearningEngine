package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DashboardDailyStatisticsTest {

    @Test
    fun `daily statistics exposes period and study statistics`() {
        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val statistics =
            statistics(
                totalReviews = 4,
                againCount = 1,
                hardCount = 1,
                goodCount = 1,
                easyCount = 1
            )

        val dailyStatistics =
            DashboardDailyStatistics(
                period = period,
                statistics = statistics
            )

        assertSame(
            expected = period,
            actual = dailyStatistics.period
        )

        assertSame(
            expected = statistics,
            actual = dailyStatistics.statistics
        )
    }

    @Test
    fun `daily statistics exposes dashboard ready values`() {
        val dailyStatistics =
            DashboardDailyStatistics(
                period =
                    StudyPeriod(
                        startInclusive = Moment(1_000L),
                        endExclusive = Moment(2_000L)
                    ),
                statistics =
                    statistics(
                        totalReviews = 4,
                        againCount = 1,
                        hardCount = 1,
                        goodCount = 1,
                        easyCount = 1,
                        reviewsWithResponseTime = 2,
                        totalResponseTimeMillis = 1_000L,
                        averageResponseTimeMillis = 500.0
                    )
            )

        assertEquals(
            expected = Moment(1_000L),
            actual = dailyStatistics.dayStart
        )

        assertEquals(
            expected = 4,
            actual = dailyStatistics.totalReviews
        )

        assertEquals(
            expected = 0.75,
            actual = dailyStatistics.accuracy
        )

        assertEquals(
            expected = 500.0,
            actual =
                dailyStatistics.averageResponseTimeMillis
        )

        assertTrue(dailyStatistics.hasReviewActivity)
    }

    @Test
    fun `empty day exposes empty dashboard values`() {
        val dailyStatistics =
            DashboardDailyStatistics(
                period =
                    StudyPeriod(
                        startInclusive = Moment(1_000L),
                        endExclusive = Moment(2_000L)
                    ),
                statistics = StudyStatistics.EMPTY
            )

        assertEquals(
            expected = 0,
            actual = dailyStatistics.totalReviews
        )

        assertNull(dailyStatistics.accuracy)

        assertNull(
            dailyStatistics.averageResponseTimeMillis
        )

        assertFalse(dailyStatistics.hasReviewActivity)
    }

    private fun statistics(
        totalReviews: Int,
        againCount: Int = 0,
        hardCount: Int = 0,
        goodCount: Int = 0,
        easyCount: Int = 0,
        reviewsWithResponseTime: Int = 0,
        totalResponseTimeMillis: Long = 0L,
        averageResponseTimeMillis: Double? = null
    ): StudyStatistics =
        StudyStatistics(
            totalReviews = totalReviews,
            againCount = againCount,
            hardCount = hardCount,
            goodCount = goodCount,
            easyCount = easyCount,
            reviewsWithResponseTime =
                reviewsWithResponseTime,
            totalResponseTime =
                TimeSpan(totalResponseTimeMillis),
            averageResponseTimeMillis =
                averageResponseTimeMillis
        )
}