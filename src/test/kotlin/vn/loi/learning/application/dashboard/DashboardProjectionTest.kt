package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class DashboardProjectionTest {

    @Test
    fun `empty projection delegates to empty statistics`() {
        val projection =
            DashboardProjection.EMPTY

        assertEquals(
            expected = 0,
            actual = projection.reviewsToday
        )

        assertEquals(
            expected = 0,
            actual = projection.reviewsThisWeek
        )

        assertEquals(
            expected = 0,
            actual = projection.reviewsThisMonth
        )

        assertNull(
            actual = projection.currentAccuracy
        )

        assertNull(
            actual =
                projection.currentAverageResponseTimeMillis
        )

        assertFalse(
            actual = projection.hasReviewActivity
        )

        assertEquals(
            expected = emptyList(),
            actual = projection.dailyStatistics
        )

        assertEquals(
            expected = 0,
            actual = projection.currentStreak
        )

        assertEquals(
            expected = emptyList(),
            actual = projection.ratingTrend
        )
    }

    @Test
    fun `projection exposes wrapped statistics`() {
        val statistics =
            DashboardStatisticsSnapshot.EMPTY

        val projection =
            DashboardProjection(
                statistics = statistics
            )

        assertEquals(
            expected = statistics,
            actual = projection.statistics
        )
    }

    @Test
    fun `current streak is zero when daily statistics are empty`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts = emptyList()
            )

        assertEquals(
            expected = 0,
            actual = projection.currentStreak
        )
    }

    @Test
    fun `current streak is zero when today has no reviews`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        3,
                        2,
                        0
                    )
            )

        assertEquals(
            expected = 0,
            actual = projection.currentStreak
        )
    }

    @Test
    fun `current streak counts only consecutive active days ending today`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        5,
                        0,
                        3,
                        2,
                        1
                    )
            )

        assertEquals(
            expected = 3,
            actual = projection.currentStreak
        )
    }

    @Test
    fun `current streak includes every day when all days have reviews`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        1,
                        4,
                        2,
                        3
                    )
            )

        assertEquals(
            expected = 4,
            actual = projection.currentStreak
        )
    }

    @Test
    fun `current streak stops at first inactive day from today`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        8,
                        7,
                        0,
                        4,
                        2
                    )
            )

        assertEquals(
            expected = 2,
            actual = projection.currentStreak
        )
    }

    @Test
    fun `rating trend is empty when daily statistics are empty`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts = emptyList()
            )

        assertEquals(
            expected = emptyList(),
            actual = projection.ratingTrend
        )
    }

    @Test
    fun `rating trend preserves daily statistics order`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        1,
                        2,
                        3
                    )
            )

        assertEquals(
            expected =
                projection.dailyStatistics.map { daily ->
                    daily.period
                },
            actual =
                projection.ratingTrend.map { point ->
                    point.period
                }
        )
    }

    @Test
    fun `rating trend maps rating counts from daily statistics`() {
        val projection =
            projectionFromRatingCounts(
                dailyRatingCounts =
                    listOf(
                        DailyRatingCounts(
                            again = 1,
                            hard = 2,
                            good = 3,
                            easy = 4
                        ),
                        DailyRatingCounts(
                            again = 0,
                            hard = 1,
                            good = 2,
                            easy = 1
                        )
                    )
            )

        val firstPoint =
            projection.ratingTrend[0]

        assertEquals(
            expected = 10,
            actual = firstPoint.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = firstPoint.againCount
        )

        assertEquals(
            expected = 2,
            actual = firstPoint.hardCount
        )

        assertEquals(
            expected = 3,
            actual = firstPoint.goodCount
        )

        assertEquals(
            expected = 4,
            actual = firstPoint.easyCount
        )

        val secondPoint =
            projection.ratingTrend[1]

        assertEquals(
            expected = 4,
            actual = secondPoint.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = secondPoint.againCount
        )

        assertEquals(
            expected = 1,
            actual = secondPoint.hardCount
        )

        assertEquals(
            expected = 2,
            actual = secondPoint.goodCount
        )

        assertEquals(
            expected = 1,
            actual = secondPoint.easyCount
        )
    }

    @Test
    fun `rating trend creates inactive point for day without reviews`() {
        val projection =
            projectionFromReviewCounts(
                dailyReviewCounts =
                    listOf(
                        2,
                        0,
                        1
                    )
            )

        val inactivePoint =
            projection.ratingTrend[1]

        assertEquals(
            expected = 0,
            actual = inactivePoint.totalReviews
        )

        assertFalse(
            actual = inactivePoint.hasReviewActivity
        )

        assertNull(
            actual = inactivePoint.againProportion
        )

        assertNull(
            actual = inactivePoint.hardProportion
        )

        assertNull(
            actual = inactivePoint.goodProportion
        )

        assertNull(
            actual = inactivePoint.easyProportion
        )
    }

    private fun projectionFromReviewCounts(
        dailyReviewCounts: List<Int>
    ): DashboardProjection =
        projectionFromRatingCounts(
            dailyRatingCounts =
                dailyReviewCounts.map { reviewCount ->
                    DailyRatingCounts(
                        again = reviewCount,
                        hard = 0,
                        good = 0,
                        easy = 0
                    )
                }
        )

    private fun projectionFromRatingCounts(
        dailyRatingCounts: List<DailyRatingCounts>
    ): DashboardProjection {
        val dailyStatistics =
            dailyRatingCounts.mapIndexed { index, counts ->
                DashboardDailyStatistics(
                    period =
                        period(
                            start =
                                index.toLong() * 100L + 1_000L,
                            end =
                                index.toLong() * 100L + 1_100L
                        ),
                    statistics =
                        statistics(
                            counts = counts
                        )
                )
            }

        return DashboardProjection(
            statistics =
                DashboardStatisticsSnapshot(
                    today = StudyStatistics.EMPTY,
                    currentWeek = StudyStatistics.EMPTY,
                    currentMonth = StudyStatistics.EMPTY,
                    dailyStatistics = dailyStatistics
                )
        )
    }

    private fun statistics(
        counts: DailyRatingCounts
    ): StudyStatistics =
        StudyStatistics(
            totalReviews = counts.totalReviews,
            againCount = counts.again,
            hardCount = counts.hard,
            goodCount = counts.good,
            easyCount = counts.easy,
            reviewsWithResponseTime = 0,
            totalResponseTime = TimeSpan.ZERO,
            averageResponseTimeMillis = null
        )

    private fun period(
        start: Long,
        end: Long
    ): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(start),
            endExclusive = Moment(end)
        )

    private data class DailyRatingCounts(
        val again: Int,
        val hard: Int,
        val good: Int,
        val easy: Int
    ) {

        val totalReviews: Int
            get() =
                again +
                        hard +
                        good +
                        easy
    }
}