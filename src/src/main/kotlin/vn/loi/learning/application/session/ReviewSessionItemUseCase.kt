package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.study.session.model.SessionStatus

class ReviewSessionItemUseCase(
    private val sessionRepository: StudySessionRepository,
    private val learningItemRepository: LearningItemRepository,
    private val reviewLearningItemUseCase: ReviewLearningItemUseCase,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: ReviewSessionItemCommand
    ): ReviewSessionItemResult {
        val session = requireNotNull(
            sessionRepository.findById(command.sessionId)
        ) {
            "Session ${command.sessionId} does not exist."
        }

        require(session.status == SessionStatus.ACTIVE) {
            "Cannot review an item in a finished session."
        }

        val learningItem = requireNotNull(
            learningItemRepository.findById(
                command.learningItemId
            )
        ) {
            "LearningItem ${command.learningItemId} does not exist."
        }

        return transactionRunner.runInTransaction {
            val reviewResult = reviewLearningItemUseCase.execute(
                ReviewCommand(
                    reviewEventId = command.reviewEventId,
                    learnerId = session.learnerId,
                    learningItemId = command.learningItemId,
                    rating = command.rating,
                    reviewedAt = command.reviewedAt,
                    responseTime = command.responseTime
                )
            )

            val wasNewItem =
                reviewResult.reviewEvent.stateBefore.reviewCount == 0

            val updatedSession = session.recordReview(
                learningItemId = command.learningItemId,
                contentId = learningItem.contentId,
                wasNewItem = wasNewItem
            )

            sessionRepository.save(updatedSession)

            ReviewSessionItemResult(
                session = updatedSession,
                reviewResult = reviewResult
            )
        }
    }
}