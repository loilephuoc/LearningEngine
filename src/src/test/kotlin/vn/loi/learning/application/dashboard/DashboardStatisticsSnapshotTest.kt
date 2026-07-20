package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DashboardStatisticsSnapshotTest {

    @Test
    fun `snapshot exposes today week and month statistics`() {
        val today =
            statistics(
                totalReviews = 2,
                againCount = 1,
                goodCount = 1
            )

        val currentWeek =
            statistics(
                totalReviews = 5,
                againCount = 1,
                hardCount = 1,
                goodCount = 2,
                easyCount = 1
            )

        val currentMonth =
            statistics(
                totalReviews = 10,
                againCount = 2,
                hardCount = 2,
                goodCount = 4,
                easyCount = 2
            )

        val snapshot =
            DashboardStatisticsSnapshot(
                today = today,
                currentWeek = currentWeek,
                currentMonth = currentMonth
            )

        assertSame(
            expected = today,
            actual = snapshot.today
        )

        assertSame(
            expected = currentWeek,
            actual = snapshot.currentWeek
        )

        assertSame(
            expected = currentMonth,
            actual = snapshot.currentMonth
        )
    }

    @Test
    fun `snapshot exposes dashboard review totals`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today =
                    statistics(
                        totalReviews = 2,
                        goodCount = 2
                    ),
                currentWeek =
                    statistics(
                        totalReviews = 5,
                        hardCount = 1,
                        goodCount = 4
                    ),
                currentMonth =
                    statistics(
                        totalReviews = 10,
                        againCount = 2,
                        hardCount = 2,
                        goodCount = 4,
                        easyCount = 2
                    )
            )

        assertEquals(
            expected = 2,
            actual = snapshot.reviewsToday
        )

        assertEquals(
            expected = 5,
            actual = snapshot.reviewsThisWeek
        )

        assertEquals(
            expected = 10,
            actual = snapshot.reviewsThisMonth
        )
    }

    @Test
    fun `current accuracy reuses successful review proportion of current month`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth =
                    statistics(
                        totalReviews = 10,
                        againCount = 2,
                        hardCount = 2,
                        goodCount = 4,
                        easyCount = 2
                    )
            )

        assertEquals(
            expected = 0.8,
            actual = snapshot.currentAccuracy
        )
    }

    @Test
    fun `current accuracy is null when current month has no reviews`() {
        val snapshot =
            DashboardStatisticsSnapshot.EMPTY

        assertNull(snapshot.currentAccuracy)
    }

    @Test
    fun `current average response time reuses current month statistics`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth =
                    statistics(
                        totalReviews = 2,
                        goodCount = 2,
                        reviewsWithResponseTime = 2,
                        totalResponseTimeMillis = 1_000L,
                        averageResponseTimeMillis = 500.0
                    )
            )

        assertEquals(
            expected = 500.0,
            actual =
                snapshot.currentAverageResponseTimeMillis
        )
    }

    @Test
    fun `current average response time is null without response time data`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth =
                    statistics(
                        totalReviews = 1,
                        goodCount = 1
                    )
            )

        assertNull(
            snapshot.currentAverageResponseTimeMillis
        )
    }

    @Test
    fun `snapshot reports review activity when current month has reviews`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth =
                    statistics(
                        totalReviews = 1,
                        goodCount = 1
                    )
            )

        assertTrue(snapshot.hasReviewActivity)
    }

    @Test
    fun `snapshot reports no review activity when current month is empty`() {
        val snapshot =
            DashboardStatisticsSnapshot(
                today = StudyStatistics.EMPTY,
                currentWeek = StudyStatistics.EMPTY,
                currentMonth = StudyStatistics.EMPTY
            )

        assertFalse(snapshot.hasReviewActivity)
    }

    @Test
    fun `empty snapshot exposes empty dashboard values`() {
        val snapshot =
            DashboardStatisticsSnapshot.EMPTY

        assertEquals(
            expected = StudyStatistics.EMPTY,
            actual = snapshot.today
        )

        assertEquals(
            expected = StudyStatistics.EMPTY,
            actual = snapshot.currentWeek
        )

        assertEquals(
            expected = StudyStatistics.EMPTY,
            actual = snapshot.currentMonth
        )

        assertEquals(
            expected = 0,
            actual = snapshot.reviewsToday
        )

        assertEquals(
            expected = 0,
            actual = snapshot.reviewsThisWeek
        )

        assertEquals(
            expected = 0,
            actual = snapshot.reviewsThisMonth
        )

        assertNull(snapshot.currentAccuracy)

        assertNull(
            snapshot.currentAverageResponseTimeMillis
        )

        assertFalse(snapshot.hasReviewActivity)
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