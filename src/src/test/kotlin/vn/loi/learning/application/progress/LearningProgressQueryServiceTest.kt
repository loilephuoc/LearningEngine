package vn.loi.learning.application.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

class LearningProgressQueryServiceTest {

    private val repository =
        InMemoryReviewEventRepository()

    private val service =
        LearningProgressQueryService(
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
                progressQuery(
                    learnerId =
                        LearnerId("unknown-learner")
                )
            )

        assertEquals(
            expected = 0,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.activeDays
        )

        assertEquals(
            expected = 0,
            actual = snapshot.againCount
        )

        assertEquals(
            expected = 0,
            actual = snapshot.hardCount
        )

        assertEquals(
            expected = 0,
            actual = snapshot.goodCount
        )

        assertEquals(
            expected = 0,
            actual = snapshot.easyCount
        )

        assertFalse(
            actual = snapshot.hasReviewActivity
        )

        assertNull(
            actual =
                snapshot.averageReviewsPerActiveDay
        )

        assertNull(
            actual = snapshot.accuracy
        )
    }

    @Test
    fun `query calculates review counts for activity period`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("again-review"),
                learnerId = learnerId,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_050L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("hard-review"),
                learnerId = learnerId,
                rating = ReviewRating.HARD,
                reviewedAt = Moment(1_250L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("good-review"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_450L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("easy-review"),
                learnerId = learnerId,
                rating = ReviewRating.EASY,
                reviewedAt = Moment(1_650L)
            )
        )

        val snapshot =
            service.query(
                progressQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 4,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = snapshot.againCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.hardCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.goodCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.easyCount
        )

        assertEquals(
            expected = 0.75,
            actual = snapshot.accuracy
        )

        assertTrue(
            actual = snapshot.hasReviewActivity
        )
    }

    @Test
    fun `query counts only daily periods containing reviews as active`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-one-review"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_050L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("day-three-review-one"),
                learnerId = learnerId,
                rating = ReviewRating.HARD,
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

        val snapshot =
            service.query(
                progressQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 3,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 2,
            actual = snapshot.activeDays
        )

        assertEquals(
            expected = 1.5,
            actual =
                snapshot.averageReviewsPerActiveDay
        )
    }

    @Test
    fun `query returns zero active days when daily periods are empty`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("review"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_500L)
            )
        )

        val snapshot =
            service.query(
                LearningProgressQuery(
                    learnerId = learnerId,
                    activityPeriod =
                        period(
                            start = 1_000L,
                            end = 2_000L
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods = emptyList()
                )
            )

        assertEquals(
            expected = 1,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.activeDays
        )

        assertNull(
            actual =
                snapshot.averageReviewsPerActiveDay
        )
    }

    @Test
    fun `query excludes reviews outside activity period`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("before-period"),
                learnerId = learnerId,
                reviewedAt = Moment(999L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("inside-period"),
                learnerId = learnerId,
                reviewedAt = Moment(1_500L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("at-period-end"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L)
            )
        )

        val snapshot =
            service.query(
                progressQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 1,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = snapshot.activeDays
        )
    }

    @Test
    fun `query isolates progress of different learners`() {
        val learnerOne =
            LearnerId("learner-1")

        val learnerTwo =
            LearnerId("learner-2")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-one-review"),
                learnerId = learnerOne,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_450L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-two-review"),
                learnerId = learnerTwo,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_450L)
            )
        )

        val snapshot =
            service.query(
                progressQuery(
                    learnerId = learnerOne
                )
            )

        assertEquals(
            expected = 1,
            actual = snapshot.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = snapshot.againCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.goodCount
        )

        assertEquals(
            expected = 1,
            actual = snapshot.activeDays
        )
    }

    private fun progressQuery(
        learnerId: LearnerId
    ): LearningProgressQuery =
        LearningProgressQuery(
            learnerId = learnerId,
            activityPeriod =
                period(
                    start = 1_000L,
                    end = 2_000L
                ),
            evaluatedAt =
                Moment(2_000L),
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
                        end = 1_600L
                    ),
                    period(
                        start = 1_600L,
                        end = 1_800L
                    ),
                    period(
                        start = 1_800L,
                        end = 2_000L
                    )
                )
        )

    private fun period(
        start: Long,
        end: Long
    ): StudyPeriod =
        StudyPeriod(
            startInclusive = Moment(start),
            endExclusive = Moment(end)
        )
}
