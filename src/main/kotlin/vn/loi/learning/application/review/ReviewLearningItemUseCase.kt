package vn.loi.learning.application.review

import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.scheduling.Scheduler

/**
 * Điều phối capability review hoàn chỉnh.
 *
 * Use case:
 * 1. Đọc MemoryState hiện tại.
 * 2. Nếu chưa tồn tại, tạo MemoryState mới.
 * 3. Gọi Scheduler.
 * 4. Tạo ReviewEvent.
 * 5. Lưu state mới và lịch sử.
 * 6. Trả kết quả cho Adapter/UI.
 */
class ReviewLearningItemUseCase(
    private val memoryStateRepository: MemoryStateRepository,
    private val reviewEventRepository: ReviewEventRepository,
    private val scheduler: Scheduler
) {

    fun execute(command: ReviewCommand): ReviewResult {
        val persistedState = memoryStateRepository.find(
                learnerId = command.learnerId,
                learningItemId = command.learningItemId
            )
        val currentState = persistedState ?: MemoryState.new(
                learnerId = command.learnerId,
                learningItemId = command.learningItemId,
                availableAt = command.reviewedAt
            )

        val decision = scheduler.schedule(
            currentState = currentState,
            rating = command.rating,
            reviewedAt = command.reviewedAt
        )

        val event = ReviewEvent(
            id = command.reviewEventId,
            rating = command.rating,
            reviewedAt = command.reviewedAt,
            responseTime = command.responseTime,
            stateBefore = decision.previousState,
            stateAfter = decision.nextState
        )

        memoryStateRepository.save(decision.nextState)
        reviewEventRepository.append(event)

        return ReviewResult(
            memoryState = decision.nextState,
            reviewEvent = event,
            scheduledInterval = decision.scheduledInterval,
            memoryStateExistedBefore = persistedState != null
        )
    }
}
