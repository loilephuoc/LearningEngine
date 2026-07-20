package vn.loi.learning.domain.study.analytics.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

class StudyStatisticsTest {

    @Test
    fun `empty statistics contains no reviews`() {
        val statistics =
            StudyStatistics.EMPTY

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
    fun `empty statistics has null rating proportions`() {
        val statistics =
            StudyStatistics.EMPTY

        ReviewRating.entries.forEach { rating ->
            assertEquals(
                0,
                statistics.countFor(rating)
            )

            assertNull(
                statistics.proportionFor(rating)
            )
        }
    }

    @Test
    fun `statistics returns counts and proportions for each rating`() {
        val statistics =
            StudyStatistics(
                totalReviews = 10,
                againCount = 1,
                hardCount = 2,
                goodCount = 5,
                easyCount = 2,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )

        assertEquals(
            1,
            statistics.countFor(
                ReviewRating.AGAIN
            )
        )

        assertEquals(
            2,
            statistics.countFor(
                ReviewRating.HARD
            )
        )

        assertEquals(
            5,
            statistics.countFor(
                ReviewRating.GOOD
            )
        )

        assertEquals(
            2,
            statistics.countFor(
                ReviewRating.EASY
            )
        )

        assertEquals(
            0.1,
            statistics.proportionFor(
                ReviewRating.AGAIN
            )
        )

        assertEquals(
            0.2,
            statistics.proportionFor(
                ReviewRating.HARD
            )
        )

        assertEquals(
            0.5,
            statistics.proportionFor(
                ReviewRating.GOOD
            )
        )

        assertEquals(
            0.2,
            statistics.proportionFor(
                ReviewRating.EASY
            )
        )
    }

    @Test
    fun `rating proportions sum to one`() {
        val statistics =
            StudyStatistics(
                totalReviews = 7,
                againCount = 1,
                hardCount = 1,
                goodCount = 3,
                easyCount = 2,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )

        val totalProportion =
            ReviewRating.entries.sumOf { rating ->
                statistics.proportionFor(rating)
                    ?: error("Expected a proportion.")
            }

        assertEquals(
            1.0,
            totalProportion,
            absoluteTolerance = 0.000000001
        )
    }

    @Test
    fun `statistics rejects rating counts different from total reviews`() {
        assertFailsWith<IllegalArgumentException> {
            StudyStatistics(
                totalReviews = 2,
                againCount = 1,
                hardCount = 0,
                goodCount = 0,
                easyCount = 0,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )
        }
    }

    @Test
    fun `statistics rejects response count greater than total reviews`() {
        assertFailsWith<IllegalArgumentException> {
            StudyStatistics(
                totalReviews = 1,
                againCount = 0,
                hardCount = 0,
                goodCount = 1,
                easyCount = 0,
                reviewsWithResponseTime = 2,
                totalResponseTime = TimeSpan.seconds(4),
                averageResponseTimeMillis = 2_000.0
            )
        }
    }

    @Test
    fun `statistics rejects average when response time data is absent`() {
        assertFailsWith<IllegalArgumentException> {
            StudyStatistics(
                totalReviews = 1,
                againCount = 0,
                hardCount = 0,
                goodCount = 1,
                easyCount = 0,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = 1_000.0
            )
        }
    }


    @Test
    fun `empty statistics has no review outcome proportions`() {
        val statistics =
            StudyStatistics.EMPTY

        assertEquals(
            0,
            statistics.successfulReviewCount
        )

        assertEquals(
            0,
            statistics.unsuccessfulReviewCount
        )

        assertNull(
            statistics.successfulReviewProportion
        )

        assertNull(
            statistics.unsuccessfulReviewProportion
        )
    }

    @Test
    fun `again reviews are unsuccessful and other ratings are successful`() {
        val statistics =
            StudyStatistics(
                totalReviews = 10,
                againCount = 2,
                hardCount = 1,
                goodCount = 5,
                easyCount = 2,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )

        assertEquals(
            8,
            statistics.successfulReviewCount
        )

        assertEquals(
            2,
            statistics.unsuccessfulReviewCount
        )

        assertEquals(
            0.8,
            statistics.successfulReviewProportion
        )

        assertEquals(
            0.2,
            statistics.unsuccessfulReviewProportion
        )
    }

    @Test
    fun `successful and unsuccessful outcomes cover all reviews`() {
        val statistics =
            StudyStatistics(
                totalReviews = 7,
                againCount = 1,
                hardCount = 2,
                goodCount = 3,
                easyCount = 1,
                reviewsWithResponseTime = 0,
                totalResponseTime = TimeSpan.ZERO,
                averageResponseTimeMillis = null
            )

        assertEquals(
            statistics.totalReviews,
            statistics.successfulReviewCount +
                    statistics.unsuccessfulReviewCount
        )

        val combinedProportion =
            requireNotNull(
                statistics.successfulReviewProportion
            ) +
                    requireNotNull(
                        statistics.unsuccessfulReviewProportion
                    )

        assertEquals(
            1.0,
            combinedProportion,
            absoluteTolerance = 0.000000001
        )
    }
}