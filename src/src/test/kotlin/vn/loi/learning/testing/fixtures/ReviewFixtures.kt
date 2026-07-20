package vn.loi.learning.testing.fixtures

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

object ReviewFixtures {

    fun event(
        id: ReviewEventId = ReviewEventId("review-1"),
        learnerId: LearnerId = LearnerId("learner-1"),
        learningItemId: LearningItemId =
            LearningItemId("item-1"),
        rating: ReviewRating = ReviewRating.GOOD,
        reviewedAt: Moment = Moment(1_000L),
        responseTime: TimeSpan? = TimeSpan.seconds(3),
        previousReviewCount: Int = 0
    ): ReviewEvent {
        require(previousReviewCount >= 0) {
            "Previous review count must not be negative."
        }

        val stateBefore = createStateBefore(
            learnerId = learnerId,
            learningItemId = learningItemId,
            reviewedAt = reviewedAt,
            previousReviewCount = previousReviewCount
        )

        val stateAfter = createStateAfter(
            stateBefore = stateBefore,
            rating = rating,
            reviewedAt = reviewedAt
        )

        return ReviewEvent(
            id = id,
            rating = rating,
            reviewedAt = reviewedAt,
            responseTime = responseTime,
            stateBefore = stateBefore,
            stateAfter = stateAfter
        )
    }

    private fun createStateBefore(
        learnerId: LearnerId,
        learningItemId: LearningItemId,
        reviewedAt: Moment,
        previousReviewCount: Int
    ): MemoryState {
        if (previousReviewCount == 0) {
            return MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )
        }

        return MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = previousReviewCount.toDouble(),
            dueAt = reviewedAt,
            lastReviewedAt = Moment(
                reviewedAt.epochMillis - 1L
            ),
            reviewCount = previousReviewCount,
            lapseCount = 0
        )
    }

    private fun createStateAfter(
        stateBefore: MemoryState,
        rating: ReviewRating,
        reviewedAt: Moment
    ): MemoryState {
        val nextStage =
            when (rating) {
                ReviewRating.AGAIN ->
                    LearningStage.RELEARNING

                ReviewRating.HARD,
                ReviewRating.GOOD,
                ReviewRating.EASY ->
                    LearningStage.REVIEW
            }

        val nextLapseCount =
            if (rating == ReviewRating.AGAIN) {
                stateBefore.lapseCount + 1
            } else {
                stateBefore.lapseCount
            }

        return MemoryState(
            learnerId = stateBefore.learnerId,
            learningItemId = stateBefore.learningItemId,
            stage = nextStage,
            difficulty = stateBefore.difficulty,
            stabilityDays = stateBefore.stabilityDays + 1.0,
            dueAt = reviewedAt,
            lastReviewedAt = reviewedAt,
            reviewCount = stateBefore.reviewCount + 1,
            lapseCount = nextLapseCount
        )
    }
}