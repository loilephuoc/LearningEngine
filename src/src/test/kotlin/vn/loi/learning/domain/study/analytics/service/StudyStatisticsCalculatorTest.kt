package vn.loi.learning.domain.study.analytics.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.testing.fixtures.ReviewFixtures
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.model.StudyStatistics



class StudyStatisticsCalculatorTest {

    private val calculator =
        StudyStatisticsCalculator()

    @Test
    fun `calculator returns empty statistics for no reviews`() {
        val statistics =
            calculator.calculate(
                emptyList()
            )

        assertEquals(
            0,
            statistics.totalReviews
        )

        assertFalse(statistics.hasReviews)
        assertFalse(statistics.hasResponseTimeData)

        assertEquals(
            TimeSpan.ZERO,
            statistics.totalResponseTime
        )

        assertNull(
            statistics.averageResponseTimeMillis
        )
    }

    @Test
    fun `calculator counts every review rating`() {
        val events =
            listOf(
                ReviewFixtures.event(
                    id = ReviewEventId("again-review"),
                    rating = ReviewRating.AGAIN,
                    reviewedAt = Moment(1_000L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("hard-review"),
                    rating = ReviewRating.HARD,
                    reviewedAt = Moment(2_000L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("good-review"),
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(3_000L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("easy-review"),
                    rating = ReviewRating.EASY,
                    reviewedAt = Moment(4_000L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("second-good-review"),
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(5_000L)
                )
            )

        val statistics =
            calculator.calculate(events)

        assertEquals(
            5,
            statistics.totalReviews
        )

        assertEquals(
            1,
            statistics.againCount
        )

        assertEquals(
            1,
            statistics.hardCount
        )

        assertEquals(
            2,
            statistics.goodCount
        )

        assertEquals(
            1,
            statistics.easyCount
        )

        assertTrue(statistics.hasReviews)
    }

    @Test
    fun `calculator totals and averages available response times`() {
        val events =
            listOf(
                ReviewFixtures.event(
                    id = ReviewEventId("review-1"),
                    reviewedAt = Moment(1_000L),
                    responseTime = TimeSpan.seconds(2)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("review-2"),
                    reviewedAt = Moment(2_000L),
                    responseTime = null
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("review-3"),
                    reviewedAt = Moment(3_000L),
                    responseTime = TimeSpan.seconds(4)
                )
            )

        val statistics =
            calculator.calculate(events)

        assertEquals(
            3,
            statistics.totalReviews
        )

        assertEquals(
            2,
            statistics.reviewsWithResponseTime
        )

        assertEquals(
            TimeSpan.seconds(6),
            statistics.totalResponseTime
        )

        assertEquals(
            3_000.0,
            statistics.averageResponseTimeMillis
        )

        assertTrue(
            statistics.hasResponseTimeData
        )
    }

    @Test
    fun `calculator leaves average null when reviews have no response time`() {
        val event =
            ReviewFixtures.event(
                responseTime = null
            )

        val statistics =
            calculator.calculate(
                listOf(event)
            )

        assertEquals(
            1,
            statistics.totalReviews
        )

        assertEquals(
            0,
            statistics.reviewsWithResponseTime
        )

        assertEquals(
            TimeSpan.ZERO,
            statistics.totalResponseTime
        )

        assertNull(
            statistics.averageResponseTimeMillis
        )

        assertFalse(
            statistics.hasResponseTimeData
        )
    }


    @Test
    fun `calculator includes only reviews inside period`() {
        val events =
            listOf(
                ReviewFixtures.event(
                    id = ReviewEventId("before-period"),
                    rating = ReviewRating.AGAIN,
                    reviewedAt = Moment(999L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("period-start"),
                    rating = ReviewRating.HARD,
                    reviewedAt = Moment(1_000L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("inside-period"),
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(1_500L)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("period-end"),
                    rating = ReviewRating.EASY,
                    reviewedAt = Moment(2_000L)
                )
            )

        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val statistics =
            calculator.calculate(
                events = events,
                period = period
            )

        assertEquals(
            2,
            statistics.totalReviews
        )

        assertEquals(
            0,
            statistics.againCount
        )

        assertEquals(
            1,
            statistics.hardCount
        )

        assertEquals(
            1,
            statistics.goodCount
        )

        assertEquals(
            0,
            statistics.easyCount
        )
    }

    @Test
    fun `calculator returns empty statistics when period has no reviews`() {
        val events =
            listOf(
                ReviewFixtures.event(
                    reviewedAt = Moment(500L)
                )
            )

        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val statistics =
            calculator.calculate(
                events = events,
                period = period
            )

        assertEquals(
            StudyStatistics.EMPTY,
            statistics
        )
    }

    @Test
    fun `period calculation includes response times only from matching reviews`() {
        val events =
            listOf(
                ReviewFixtures.event(
                    id = ReviewEventId("before-period"),
                    reviewedAt = Moment(500L),
                    responseTime = TimeSpan.seconds(10)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("inside-1"),
                    reviewedAt = Moment(1_000L),
                    responseTime = TimeSpan.seconds(2)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("inside-2"),
                    reviewedAt = Moment(1_500L),
                    responseTime = TimeSpan.seconds(4)
                ),
                ReviewFixtures.event(
                    id = ReviewEventId("at-end"),
                    reviewedAt = Moment(2_000L),
                    responseTime = TimeSpan.seconds(20)
                )
            )

        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val statistics =
            calculator.calculate(
                events = events,
                period = period
            )

        assertEquals(
            2,
            statistics.reviewsWithResponseTime
        )

        assertEquals(
            TimeSpan.seconds(6),
            statistics.totalResponseTime
        )

        assertEquals(
            3_000.0,
            statistics.averageResponseTimeMillis
        )
    }




}