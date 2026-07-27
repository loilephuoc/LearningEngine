package vn.loi.learning.application.packageprogress

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*

class PackageLatestRatingQueryServiceTest {
    private val learnerId = LearnerId("learner")

    @Test
    fun `unrated items are excluded and each item contributes only its authoritative latest rating`() {
        val first = LearningItemId("first")
        val second = LearningItemId("second")
        val unrated = LearningItemId("unrated")
        val events = listOf(
            event(first, ReviewRating.GOOD, 100, 1),
            event(second, ReviewRating.EASY, 150, 1),
            event(first, ReviewRating.HARD, 200, 2)
        )

        val result = latestRatingDistribution(setOf(first, second, unrated), events)

        assertEquals(PackageLatestRatingDistribution(0, 1, 0, 1), result)
        assertEquals(2, result.ratedItemCount)
    }

    @Test
    fun `package ownership filters other and removed learning items`() {
        val packageA = LearningItemId("package-a")
        val removedFromA = LearningItemId("removed-a")
        val packageB = LearningItemId("package-b")
        val events = listOf(
            event(packageA, ReviewRating.AGAIN, 100, 1),
            event(removedFromA, ReviewRating.GOOD, 110, 1),
            event(packageB, ReviewRating.EASY, 120, 1)
        )

        assertEquals(
            PackageLatestRatingDistribution(1, 0, 0, 0),
            latestRatingDistribution(setOf(packageA), events)
        )
        assertEquals(
            PackageLatestRatingDistribution(0, 0, 0, 1),
            latestRatingDistribution(setOf(packageB), events)
        )
    }

    @Test
    fun `legacy or reset history with no events is an empty distribution`() {
        assertEquals(
            PackageLatestRatingDistribution.EMPTY,
            latestRatingDistribution(setOf(LearningItemId("legacy")), emptyList())
        )
    }

    private fun event(
        itemId: LearningItemId,
        rating: ReviewRating,
        reviewedAt: Long,
        reviewCount: Int
    ): ReviewEvent {
        val before = MemoryState(
            learnerId = learnerId,
            learningItemId = itemId,
            stage = if (reviewCount == 1) LearningStage.NEW else LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = 1.0,
            dueAt = Moment(0),
            lastReviewedAt = if (reviewCount == 1) null else Moment(reviewedAt - 1),
            reviewCount = reviewCount - 1,
            lapseCount = 0
        )
        val after = before.copy(
            stage = LearningStage.REVIEW,
            dueAt = Moment(reviewedAt + 1),
            lastReviewedAt = Moment(reviewedAt),
            reviewCount = reviewCount
        )
        return ReviewEvent(
            id = ReviewEventId("$itemId-$reviewedAt"),
            rating = rating,
            reviewedAt = Moment(reviewedAt),
            responseTime = null,
            stateBefore = before,
            stateAfter = after
        )
    }
}
