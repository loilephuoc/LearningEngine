package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.study.session.model.SessionStatus

/**
 * Review item trong phạm vi một StudySession.
 *
 * Khi StudyQueueService được cung cấp:
 * - chỉ item hiện tại của queue mới được review;
 * - queue được advance sau khi review và session đều lưu thành công.
 */
class ReviewSessionItemUseCase(
    private val sessionRepository: StudySessionRepository,
    private val learningItemRepository: LearningItemRepository,
    private val reviewLearningItemUseCase:
    ReviewLearningItemUseCase,
    private val transactionRunner: TransactionRunner,
    private val studyQueueService:
    StudyQueueService? = null
) {

    fun execute(
        command: ReviewSessionItemCommand
    ): ReviewSessionItemResult {
        val session =
            requireNotNull(
                sessionRepository.findById(
                    command.sessionId
                )
            ) {
                "Session ${command.sessionId} does not exist."
            }

        require(
            session.status ==
                    SessionStatus.ACTIVE
        ) {
            "Cannot review an item in a finished session."
        }

        requireCurrentQueueItemWhenEnabled(
            command
        )

        val learningItem =
            requireNotNull(
                learningItemRepository.findById(
                    command.learningItemId
                )
            ) {
                "LearningItem ${command.learningItemId} does not exist."
            }

        return transactionRunner
            .runInTransaction {
                val reviewResult =
                    reviewLearningItemUseCase
                        .execute(
                            ReviewCommand(
                                reviewEventId =
                                    command.reviewEventId,
                                learnerId =
                                    session.learnerId,
                                learningItemId =
                                    command.learningItemId,
                                rating =
                                    command.rating,
                                reviewedAt =
                                    command.reviewedAt,
                                responseTime =
                                    command.responseTime
                            )
                        )

                val wasNewItem =
                    reviewResult
                        .reviewEvent
                        .stateBefore
                        .reviewCount == 0

                val updatedSession =
                    session.recordReview(
                        learningItemId =
                            command.learningItemId,
                        contentId =
                            learningItem.contentId,
                        wasNewItem =
                            wasNewItem
                    )

                sessionRepository.save(
                    updatedSession
                )

                advanceQueueWhenEnabled(
                    command
                )

                ReviewSessionItemResult(
                    session = updatedSession,
                    reviewResult = reviewResult
                )
            }
    }

    private fun requireCurrentQueueItemWhenEnabled(
        command: ReviewSessionItemCommand
    ) {
        val queueService =
            studyQueueService ?: return

        val queue =
            queueService.require(
                command.sessionId
            )

        require(!queue.isCompleted) {
            "Cannot review an item from a completed study queue."
        }

        require(
            queue.currentLearningItemId ==
                    command.learningItemId
        ) {
            "LearningItem ${command.learningItemId} " +
                    "is not the current item of study queue " +
                    "for session ${command.sessionId}."
        }
    }

    private fun advanceQueueWhenEnabled(
        command: ReviewSessionItemCommand
    ) {
        val queueService =
            studyQueueService ?: return

        queueService.advance(
            command.sessionId
        )
    }
}