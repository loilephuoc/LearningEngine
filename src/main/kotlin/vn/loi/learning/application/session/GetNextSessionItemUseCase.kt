package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.application.study.ContentLearningStateQueryService
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
    StudyQueueService? = null,
    private val contentLearningStateQuery:
    ContentLearningStateQueryService? = null
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

        if (studyQueueService == null &&
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
            val queue = studyQueueService?.require(session.id)
            val progress = if (queue?.practiceLoopPolicy ==
                vn.loi.learning.domain.study.session.model.PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
            ) {
                LearningSessionProgress.unknown(presented)
            } else {
                queue
                    ?.let(StudyQueueProgress::from)
                    ?.let { LearningSessionProgress.from(presented, it) }
                    ?: LearningSessionProgress.unknown(presented)
            }
            return result.copy(session = presented, progress = progress)
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

        repairQueueIfMismatched(session, queueService)

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
                    item = nextItem,
                    origin = contentLearningStateQuery
                        ?.resolve(session.learnerId, nextItem.learningItem.contentId)
                        ?.let {
                            if (it.isLearned) {
                                vn.loi.learning.domain.study.session.model.SessionItemOrigin.REVIEW
                            } else {
                                vn.loi.learning.domain.study.session.model.SessionItemOrigin.NEW
                            }
                        }
                        ?: queue.currentItemOrigin
                        ?: if (nextItem.isNew) {
                            vn.loi.learning.domain.study.session.model.SessionItemOrigin.NEW
                        } else {
                            vn.loi.learning.domain.study.session.model.SessionItemOrigin.REVIEW
                        }
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
        val itemRepository = requireNotNull(learningItemRepository)

        /*
         * The persisted StudyQueue has already selected the exact item that must be
         * presented. The old implementation rebuilt a full GetNextLearningItemQuery,
         * called findAllEnabled(), created a huge exclusion set containing every item
         * except currentItemId, and then ran the global StudyQueuePlanner again.
         *
         * That turns an O(1)-style queue read into an O(N) package scan on every card
         * transition. With large learned-item review queues this was measured at
         * roughly 650-800 ms per next item.
         *
         * GetNextLearningItemUseCase.getById() exists specifically for this case: an
         * external mechanism (the persisted queue) has already selected the ID, so we
         * only hydrate LearningItem + Content + MemoryState without replanning.
         */
        val learningItem = itemRepository.findById(currentItemId) ?: return null
        if (!learningItem.isEnabled) return null

        if (!session.policy.allowRepeatInSameSession) {
            if (currentItemId in session.reviewedItemIds) return null
            if (learningItem.contentId in session.reviewedContentIds) return null
        }

        val hydrationTime =
            if (session.includedContentIds.isNotEmpty() || session.installedPackageId != null) {
                Moment(Long.MAX_VALUE / 2)
            } else {
                now
            }

        return getNextLearningItemUseCase.getById(
            learnerId = session.learnerId,
            learningItemId = currentItemId,
            now = hydrationTime
        )
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

    private fun repairQueueIfMismatched(
        session: StudySession,
        queueService: StudyQueueService
    ) {
        val queue = queueService.get(session.id) ?: return
        if (queue.practiceLoopPolicy != vn.loi.learning.domain.study.session.model.PracticeLoopPolicy.NONE) return
        if (queue.completedItemCount < session.totalReviews && session.reviewedItemIds.isNotEmpty()) {
            val missingCompleted = session.reviewedItemIds.filterNot { it in queue.learningItemIds.take(queue.currentIndex) }
            if (missingCompleted.isNotEmpty() || queue.currentIndex < session.totalReviews) {
                val remainingItems = queue.learningItemIds.drop(queue.currentIndex).filterNot { it in session.reviewedItemIds }
                val repairedIds = (session.reviewedItemIds + remainingItems).toList()
                val repairedOrigins = queue.itemOrigins.toMutableMap()
                val repairedContentIds = queue.itemContentIds.toMutableMap()
                val repaired = queue.copy(
                    learningItemIds = repairedIds,
                    currentIndex = minOf(session.totalReviews, repairedIds.size),
                    itemOrigins = repairedOrigins,
                    itemContentIds = repairedContentIds,
                    effectiveNewWorkload = maxOf(queue.effectiveNewWorkload, session.newItemsReviewed),
                    effectiveReviewWorkload = maxOf(queue.effectiveReviewWorkload, session.reviewItemsReviewed),
                    configuredNewTarget = maxOf(queue.configuredNewTarget, session.policy.newItemLimit),
                    configuredReviewTarget = maxOf(queue.configuredReviewTarget, session.policy.reviewItemLimit)
                )
                queueService.save(repaired)
            }
        }
    }
}
