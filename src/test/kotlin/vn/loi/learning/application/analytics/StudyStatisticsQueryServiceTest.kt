package vn.loi.learning.application.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.testing.fixtures.ReviewFixtures

class StudyStatisticsQueryServiceTest {

    private val repository =
        InMemoryReviewEventRepository()

    private val service =
        StudyStatisticsQueryService(
            reviewHistoryQueryService =
                ReviewHistoryQueryService(
                    reviewEventRepository = repository
                ),
            studyStatisticsCalculator =
                StudyStatisticsCalculator()
        )

    @Test
    fun `query returns empty statistics when learner has no reviews`() {
        val statistics =
            service.query(
                ReviewHistoryQuery(
                    learnerId =
                        LearnerId("unknown-learner")
                )
            )

        assertEquals(
            expected = 0,
            actual = statistics.totalReviews
        )

        assertFalse(statistics.hasReviews)
    }

    @Test
    fun `query calculates statistics from learner review history`() {
        val learnerId =
            LearnerId("learner-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("review-again"),
                learnerId = learnerId,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_000L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("review-good"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(2_000L)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("review-easy"),
                learnerId = learnerId,
                rating = ReviewRating.EASY,
                reviewedAt = Moment(3_000L)
            )
        )

        val statistics =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = 3,
            actual = statistics.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = statistics.againCount
        )

        assertEquals(
            expected = 0,
            actual = statistics.hardCount
        )

        assertEquals(
            expected = 1,
            actual = statistics.goodCount
        )

        assertEquals(
            expected = 1,
            actual = statistics.easyCount
        )

        assertTrue(statistics.hasReviews)
    }

    @Test
    fun `query applies review history conditions before calculating statistics`() {
        val learnerId =
            LearnerId("learner-1")

        val selectedItemId =
            LearningItemId("item-1")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("matching-review"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_500L),
                responseTime = TimeSpan.seconds(2)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("wrong-rating"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                rating = ReviewRating.AGAIN,
                reviewedAt = Moment(1_600L),
                responseTime = TimeSpan.seconds(4)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("wrong-item"),
                learnerId = learnerId,
                learningItemId =
                    LearningItemId("item-2"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_700L),
                responseTime = TimeSpan.seconds(6)
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("outside-period"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(2_000L),
                responseTime = TimeSpan.seconds(8)
            )
        )

        val statistics =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    learningItemId = selectedItemId,
                    period = StudyPeriod(
                        startInclusive = Moment(1_000L),
                        endExclusive = Moment(2_000L)
                    ),
                    ratings = setOf(
                        ReviewRating.GOOD
                    )
                )
            )

        assertEquals(
            expected = 1,
            actual = statistics.totalReviews
        )

        assertEquals(
            expected = 0,
            actual = statistics.againCount
        )

        assertEquals(
            expected = 1,
            actual = statistics.goodCount
        )

        assertEquals(
            expected = 1,
            actual =
                statistics.reviewsWithResponseTime
        )

        assertEquals(
            expected = TimeSpan.seconds(2),
            actual = statistics.totalResponseTime
        )

        assertEquals(
            expected = 2_000.0,
            actual =
                statistics.averageResponseTimeMillis
        )
    }

    @Test
    fun `query isolates statistics of different learners`() {
        val learnerOne =
            LearnerId("learner-1")

        val learnerTwo =
            LearnerId("learner-2")

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-one-review"),
                learnerId = learnerOne,
                rating = ReviewRating.HARD
            )
        )

        repository.append(
            ReviewFixtures.event(
                id = ReviewEventId("learner-two-review"),
                learnerId = learnerTwo,
                rating = ReviewRating.EASY
            )
        )

        val statistics =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerOne
                )
            )

        assertEquals(
            expected = 1,
            actual = statistics.totalReviews
        )

        assertEquals(
            expected = 1,
            actual = statistics.hardCount
        )

        assertEquals(
            expected = 0,
            actual = statistics.easyCount
        )
    }
}
