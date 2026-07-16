package vn.loi.learning.application

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.scheduling.Scheduler

/**
 * API công khai của Learning Engine.
 */
class LearningEngine(
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository,
    private val reviewEventRepository: ReviewEventRepository,
    scheduler: Scheduler
) {

    private val reviewUseCase = ReviewLearningItemUseCase(
        memoryStateRepository = memoryStateRepository,
        reviewEventRepository = reviewEventRepository,
        scheduler = scheduler
    )

    private val getNextLearningItemUseCase =
        GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            memoryStateRepository = memoryStateRepository
        )

    fun registerContent(content: Content) {
        contentRepository.save(content)
    }

    fun registerLearningItem(learningItem: LearningItem) {
        require(
            contentRepository.findById(
                learningItem.contentId
            ) != null
        ) {
            "Cannot register LearningItem ${learningItem.id}: " +
                    "Content ${learningItem.contentId} does not exist."
        }

        learningItemRepository.save(learningItem)
    }

    fun getNextLearningItem(
        learnerId: LearnerId,
        now: Moment
    ): NextLearningItem? =
        getNextLearningItemUseCase.execute(
            GetNextLearningItemQuery(
                learnerId = learnerId,
                now = now
            )
        )

    fun review(command: ReviewCommand): ReviewResult {
        require(
            learningItemRepository.findById(
                command.learningItemId
            ) != null
        ) {
            "LearningItem ${command.learningItemId} does not exist."
        }

        return reviewUseCase.execute(command)
    }

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