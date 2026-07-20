package vn.loi.learning.testing.fixtures

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

object MemoryFixtures {

    fun newState(
        learnerId: LearnerId = LearnerId("learner-1"),
        learningItemId: LearningItemId =
            LearningItemId("item-1"),
        availableAt: Moment = Moment(0L)
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId = learningItemId,
            availableAt = availableAt
        )

    fun reviewedState(
        learnerId: LearnerId = LearnerId("learner-1"),
        learningItemId: LearningItemId =
            LearningItemId("item-1"),
        reviewedAt: Moment = Moment(1_000L)
    ): MemoryState =
        MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = 1.0,
            dueAt = reviewedAt,
            lastReviewedAt = reviewedAt,
            reviewCount = 1,
            lapseCount = 0
        )

    fun masteredState(
        learnerId: LearnerId = LearnerId("learner-1"),
        learningItemId: LearningItemId =
            LearningItemId("item-1"),
        reviewedAt: Moment = Moment(10_000L)
    ): MemoryState =
        MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.MASTERED,
            difficulty = 2.0,
            stabilityDays = 365.0,
            dueAt = reviewedAt,
            lastReviewedAt = reviewedAt,
            reviewCount = 100,
            lapseCount = 0
        )
}