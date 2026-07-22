package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Lấy item hiện tại của StudySession.
 *
 * Khi queue dependencies được cung cấp, persisted StudyQueue là nguồn
 * xác định thứ tự item. GetNextLearningItemUseCase chỉ còn chịu trách nhiệm
 * dựng NextLearningItem đầy đủ cho đúng LearningItemId hiện tại.
 *
 * Nếu item hiện tại trong queue không còn hợp lệ theo trạng thái session,
 * item đó được bỏ qua và queue tiếp tục advance.
 *
 * Khi queue dependencies không được cung cấp, use case giữ hành vi legacy
 * để các test hoặc adapter cũ vẫn tương thích.
 */
class GetNextSessionItemUseCase(
    private val sessionRepository: StudySessionRepository,
    private val getNextLearningItemUseCase:
    GetNextLearningItemUseCase,
    private val learningItemRepository:
    LearningItemRepository? = null,
    private val studyQueueService:
    StudyQueueService? = null
) {

    init {
        require(
            (learningItemRepository == null) ==
                    (studyQueueService == null)
        ) {
            "LearningItemRepository and StudyQueueService " +
                    "must either both be provided or both be absent."
        }
    }

    fun execute(
        sessionId: SessionId,
        now: Moment
    ): NextSessionItem? {
        val session =
            requireActiveSession(
                sessionId
            )

        if (
            !session.canReviewNewItem &&
            !session.canReviewDueItem
        ) {
            return null
        }

        val result = if (
            studyQueueService == null
        ) {
            findLegacyNextItem(
                session = session,
                now = now
            )
        } else {
            findQueuedNextItem(
                session = session,
                now = now
            )
        }

        if (result != null) {
            val presented = session.presentItem(
                learningItemId = result.item.learningItem.id,
                presentedAt = now
            )
            if (presented != session) sessionRepository.save(presented)
            return result.copy(session = presented)
        }
        return null
    }

    private fun findQueuedNextItem(
        session: StudySession,
        now: Moment
    ): NextSessionItem? {
        val queueService =
            requireNotNull(
                studyQueueService
            )

        while (true) {
            val queue =
                queueService.require(
                    session.id
                )

            val currentItemId =
                queue.currentLearningItemId
                    ?: return null

            val nextItem =
                findExactQueuedItem(
                    session = session,
                    now = now,
                    currentItemId =
                        currentItemId
                )

            if (nextItem != null) {
                return NextSessionItem(
                    session = session,
                    item = nextItem
                )
            }

            /*
             * Item hiện tại không còn hợp lệ, ví dụ:
             * - limit của loại item đã đạt;
             * - item đã bị disable;
             * - item không còn due;
             * - LearningItem đã được review;
             * - Content sibling đã xuất hiện trong session;
             * - dữ liệu nguồn đã thay đổi.
             *
             * Bỏ qua item đó để queue không bị kẹt.
             */
            queueService.advance(
                session.id
            )
        }
    }

    private fun findExactQueuedItem(
        session: StudySession,
        now: Moment,
        currentItemId: LearningItemId
    ): NextLearningItem? {
        val itemRepository =
            requireNotNull(
                learningItemRepository
            )

        val allOtherEnabledItemIds =
            itemRepository
                .findAllEnabled()
                .asSequence()
                .map { learningItem ->
                    learningItem.id
                }
                .filter { learningItemId ->
                    learningItemId !=
                            currentItemId
                }
                .toSet()

        val excludedItemIds =
            if (
                session.policy
                    .allowRepeatInSameSession
            ) {
                allOtherEnabledItemIds
            } else {
                allOtherEnabledItemIds +
                        session.reviewedItemIds
            }

        val excludedContentIds =
            if (
                session.policy
                    .allowRepeatInSameSession
            ) {
                emptySet()
            } else {
                session.reviewedContentIds
            }

        return getNextLearningItemUseCase
            .execute(
                GetNextLearningItemQuery(
                    learnerId =
                        session.learnerId,
                    now = now,
                    excludedItemIds =
                        excludedItemIds,
                    excludedContentIds =
                        excludedContentIds,
                    includedContentIds =
                        session.includedContentIds,
                    includeNewItems =
                        session.canReviewNewItem,
                    includeReviewItems =
                        session.canReviewDueItem
                )
            )
            ?.takeIf { nextItem ->
                nextItem.learningItem.id ==
                        currentItemId
            }
    }

    private fun findLegacyNextItem(
        session: StudySession,
        now: Moment
    ): NextSessionItem? {
        val excludedItemIds =
            if (
                session.policy
                    .allowRepeatInSameSession
            ) {
                emptySet()
            } else {
                session.reviewedItemIds
            }

        val excludedContentIds =
            if (
                session.policy
                    .allowRepeatInSameSession
            ) {
                emptySet()
            } else {
                session.reviewedContentIds
            }

        val nextItem =
            getNextLearningItemUseCase
                .execute(
                    GetNextLearningItemQuery(
                        learnerId =
                            session.learnerId,
                        now = now,
                        excludedItemIds =
                            excludedItemIds,
                        excludedContentIds =
                            excludedContentIds,
                        includedContentIds =
                            session.includedContentIds,
                        includeNewItems =
                            session.canReviewNewItem,
                        includeReviewItems =
                            session.canReviewDueItem
                    )
                )
                ?: return null

        return NextSessionItem(
            session = session,
            item = nextItem
        )
    }

    private fun requireActiveSession(
        sessionId: SessionId
    ): StudySession {
        val session =
            requireNotNull(
                sessionRepository.findById(
                    sessionId
                )
            ) {
                "Session $sessionId does not exist."
            }

        require(
            session.status ==
                    SessionStatus.ACTIVE
        ) {
            "Cannot get an item from a finished session."
        }

        return session
    }
}
