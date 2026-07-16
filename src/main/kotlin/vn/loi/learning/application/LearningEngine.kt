package vn.loi.learning.application

import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.scheduling.Scheduler

/**
 * API công khai chính của Learning Engine.
 *
 * Android, Desktop, Server hoặc CLI chỉ cần làm việc với class này,
 * không cần trực tiếp điều phối Scheduler và Repository.
 */
class LearningEngine(
    private val memoryStateRepository: MemoryStateRepository,
    private val reviewEventRepository: ReviewEventRepository,
    scheduler: Scheduler
) {

    private val reviewUseCase = ReviewLearningItemUseCase(
        memoryStateRepository = memoryStateRepository,
        reviewEventRepository = reviewEventRepository,
        scheduler = scheduler
    )

    fun review(command: ReviewCommand): ReviewResult =
        reviewUseCase.execute(command)

    fun getMemoryState(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        memoryStateRepository.find(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

    fun getReviewHistory(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        reviewEventRepository.findAll(
            learnerId = learnerId,
            learningItemId = learningItemId
        )
}