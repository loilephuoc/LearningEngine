package vn.loi.learning.application.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.testing.fixtures.ReviewFixtures

class DashboardQueryServiceTest {

    private val repository =
        InMemoryReviewEventRepository()

    private val service =
        DashboardQueryService(
            studyStatisticsQueryService =
                StudyStatisticsQueryService(
                    reviewHistoryQueryService =
                        ReviewHistoryQueryService(
                            reviewEventRepository = repository
                        ),
                    studyStatisticsCalculator =
                        StudyStatisticsCalculator()
                )
        )

    @Test
    fun `query returns empty snapshot when learner has no reviews`() {
        val snapshot =
            service.query(
                dashboardQuery(
                    learnerId =
                        LearnerId("unknown-learner")
                )
            )

        assertFalse(snapshot.today.hasReviews)
        assertFalse(snapshot.currentWeek.hasReviews)
        assertFalse(snapshot.currentMonth.hasReviews)
        assertFalse(snapshot.hasReviewActivity)

        assertEquals(
            expected = 4,
            actual = snapshot.dailyStatistics.size
        )

        assertTrue(
            snapshot.dailyStatistics.all { daily ->
                !daily.hasReviewActivity
            }
        )
    }

    @Test
    fun `query calculates today week and month statistics`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("older-month-review"),
                learnerId = learnerId,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_100L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("earlier-week-review"),
                learnerId = learnerId,
                rating = ReviewRating.HARD,
                reviewedAt = Moment(1_450L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("today-good-review"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_750L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("today-easy-review"),
                learnerId = learnerId,
                rating = ReviewRating.EASY,
                reviewedAt = Moment(1_850L)
            )
        )

        val snapshot =
            service.query(
                dashboardQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 2,
            actual = snapshot.today.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.today.againCount
        )

        assertEquals(
            expected = 0,
            actual = snapshot.today.hardCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.today.goodCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.today.easyCount
        )

        assertEquals(
            expected = 3,
            actual = snapshot.currentWeek.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = snapshot.currentWeek.hardCount
        )

        assertEquals(
            expected = 4,
            actual = snapshot.currentMonth.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = snapshot.currentMonth.againCount
        )

        assertTrue(snapshot.hasReviewActivity)
    }

    @Test
    fun `query calculates daily statistics in requested order`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-one-review"),
                learnerId = learnerId,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_100L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-three-review-one"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_450L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-three-review-two"),
                learnerId = learnerId,
                rating = ReviewRating.EASY,
                reviewedAt = Moment(1_550L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-four-review"),
                learnerId = learnerId,
                rating = ReviewRating.HARD,
                reviewedAt = Moment(1_750L)
            )
        )

        val query =
            dashboardQuery(
                learnerId = learnerId
            )

        val snapshot =
            service.query(query)

        assertEquals(
            expected = query.dailyPeriods.size,
            actual = snapshot.dailyStatistics.size
        )

        assertEquals(
            expected = query.dailyPeriods,
            actual =
                snapshot.dailyStatistics.map { daily ->
                    daily.period
                }
        )

        assertEquals(
            expected = listOf(1, 0, 2, 1),
            actual =
                snapshot.dailyStatistics.map { daily ->
                    daily.totalReviews
                }
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.dailyStatistics[0]
                    .statistics.againCount
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.dailyStatistics[2]
                    .statistics.goodCount
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.dailyStatistics[2]
                    .statistics.easyCount
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.dailyStatistics[3]
                    .statistics.hardCount
        )
    }

    @Test
    fun `query returns no daily statistics when daily periods are empty`() {
        val snapshot =
            service.query(
                DashboardQuery(
                    learnerId = LearnerId("learner-1"),
                    todayPeriod =
                        period(
                            start = 1_700L,
                            end = 1_900L
                        ),
                    currentWeekPeriod =
                        period(
                            start = 1_400L,
                            end = 1_900L
                        ),
                    currentMonthPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    dailyPeriods = emptyList()
                )
            )

        assertTrue(snapshot.dailyStatistics.isEmpty())
    }

    @Test
    fun `query excludes reviews outside current month`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("before-current-month"),
                learnerId = learnerId,
                reviewedAt = Moment(999L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("inside-current-month"),
                learnerId = learnerId,
                reviewedAt = Moment(1_100L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("at-current-month-end"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L)
            )
        )

        val snapshot =
            service.query(
                dashboardQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 0,
            actual = snapshot.today.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.currentWeek.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = snapshot.currentMonth.totalReviews
        )
    }

    @Test
    fun `query isolates dashboard statistics of different learners`() {
        val learnerOne =
            LearnerId("learner-1")

        val learnerTwo =
            LearnerId("learner-2")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-one-review"),
                learnerId = learnerOne,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_750L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-two-review"),
                learnerId = learnerTwo,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_750L)
            )
        )

        val snapshot =
            service.query(
                dashboardQuery(
                    learnerId = learnerOne
                )
            )

        assertEquals(
            expected = 1,
            actual = snapshot.today.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.today.againCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.today.goodCount
        )

        assertEquals(
            expected = 1,
            actual =
                snapshot.dailyStatistics
                    .last()
                    .statistics.goodCount
        )
    }

    private fun dashboardQuery(
        learnerId: LearnerId
    ): DashboardQuery {
        val todayPeriod =
            period(
                start = 1_700L,
                end = 1_900L
            )

        return DashboardQuery(
            learnerId = learnerId,
            todayPeriod = todayPeriod,
            currentWeekPeriod =
                period(
                    start = 1_400L,
                    end = 1_900L
                ),
            currentMonthPeriod =
                period(
                    start = 1_000L,
                    end = 2_000L
                ),
            dailyPeriods =
                listOf(
                    period(
                        start = 1_000L,
                        end = 1_200L
                    ),
                    period(
                        start = 1_200L,
                        end = 1_400L
                    ),
                    period(
                        start = 1_400L,
                        end = 1_700L
                    ),
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
