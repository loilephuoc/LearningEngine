package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class DashboardProjectionQueryServiceTest {

    @Test
    fun `query returns projection containing statistics snapshot`() {
        val expectedQuery =
            dashboardQuery()

        val expectedSnapshot =
            DashboardStatisticsSnapshot.EMPTY

        var receivedQuery: DashboardQuery? =
            null

        val statisticsQuery =
            DashboardStatisticsQuery { query ->
                receivedQuery = query
                expectedSnapshot
            }

        val service =
            DashboardProjectionQueryService(
                dashboardStatisticsQuery =
                    statisticsQuery
            )

        val result =
            service.query(
                query = expectedQuery
            )

        assertEquals(
            expected = expectedQuery,
            actual = receivedQuery
        )

        assertSame(
            expected = expectedSnapshot,
            actual = result.statistics
        )
    }

    @Test
    fun `query exposes projection values derived from snapshot`() {
        val service =
            DashboardProjectionQueryService(
                dashboardStatisticsQuery =
                    DashboardStatisticsQuery {
                        DashboardStatisticsSnapshot.EMPTY
                    }
            )

        val result =
            service.query(
                query = dashboardQuery()
            )

        assertEquals(
            expected = 0,
            actual = result.reviewsToday
        )

        assertEquals(
            expected = 0,
            actual = result.reviewsThisWeek
        )

        assertEquals(
            expected = 0,
            actual = result.reviewsThisMonth
        )

        assertEquals(
            expected = 0,
            actual = result.currentStreak
        )

        assertEquals(
            expected = emptyList(),
            actual = result.ratingTrend
        )
    }

    private fun dashboardQuery(): DashboardQuery {
        val todayPeriod =
            period(
                start = 300L,
                end = 400L
            )

        return DashboardQuery(
            learnerId =
                LearnerId(
                    value = "learner-1"
                ),
            todayPeriod = todayPeriod,
            currentWeekPeriod =
                period(
                    start = 100L,
                    end = 500L
                ),
            currentMonthPeriod =
                period(
                    start = 0L,
                    end = 1_000L
                ),
            dailyPeriods =
                listOf(
                    todayPeriod
                )
        )
    }

    private fun period(
        start: Long,
        end: Long
    ): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(start),
            endExclusive = Moment(end)
        )
}