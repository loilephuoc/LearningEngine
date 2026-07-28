package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.study.session.model.PendingSessionReview
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.UndoableSessionReview
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

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
        var session =
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
        val admittedOrigin = studyQueueService
            ?.require(command.sessionId)
            ?.currentItemOrigin

        val learningItem =
            requireNotNull(
                learningItemRepository.findById(
                    command.learningItemId
                )
            ) {
                "LearningItem ${command.learningItemId} does not exist."
            }

        val intent = PendingSessionReview(
            reviewEventId = command.reviewEventId,
            learningItemId = command.learningItemId,
            rating = command.rating,
            reviewedAt = command.reviewedAt,
            responseTime = command.responseTime
        )
        if (session.currentLearningItemId == null) {
            session = session.presentItem(command.learningItemId, command.reviewedAt)
        }
        session = session.stageReview(intent)
        sessionRepository.save(session)

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

                val wasNewItem = when (admittedOrigin) {
                    SessionItemOrigin.NEW -> true
                    SessionItemOrigin.REVIEW -> false
                    null -> reviewResult.reviewEvent.stateBefore.isNew
                }

                val updatedSession =
                    session.recordReview(
                        learningItemId =
                            command.learningItemId,
                        contentId =
                            learningItem.contentId,
                        wasNewItem = wasNewItem,
                        undoableReview = UndoableSessionReview(
                            reviewEventId = reviewResult.reviewEvent.id,
                            learningItemId = command.learningItemId,
                            contentId = learningItem.contentId,
                            memoryStateBefore = reviewResult.reviewEvent.stateBefore,
                            memoryStateExistedBefore = reviewResult.memoryStateExistedBefore,
                            reviewedItemIdsBefore = session.reviewedItemIds,
                            reviewedContentIdsBefore = session.reviewedContentIds,
                            newItemsReviewedBefore = session.newItemsReviewed,
                            reviewItemsReviewedBefore = session.reviewItemsReviewed,
                            currentItemPresentedAtBefore = session.currentItemPresentedAt,
                            answerRevealedBefore = session.answerRevealed
                        )
                    )

                sessionRepository.save(
                    updatedSession
                )

                val queueProgress = advanceQueueWhenEnabled(
                    command
                )

                ReviewSessionItemResult(
                    session = updatedSession,
                    reviewResult = reviewResult,
                    progress = queueProgress?.let { LearningSessionProgress.from(updatedSession, it) }
                )
            }
    }

    fun resumePending(
        sessionId: SessionId
    ): ReviewSessionItemResult? {
        val session = sessionRepository.findById(sessionId) ?: return null
        val pending = session.pendingReview ?: return null
        return execute(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId = pending.reviewEventId,
                learningItemId = pending.learningItemId,
                rating = pending.rating,
                reviewedAt = pending.reviewedAt,
                responseTime = pending.responseTime
            )
        )
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
    ): StudyQueueProgress? {
        val queueService =
            studyQueueService ?: return null

        return StudyQueueProgress.from(queueService.advance(command.sessionId))
    }
}
