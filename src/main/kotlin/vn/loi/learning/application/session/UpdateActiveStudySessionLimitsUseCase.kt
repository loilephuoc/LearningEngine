package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionStatus

class UpdateActiveStudySessionLimitsUseCase(
    private val sessions: StudySessionRepository,
    private val planning: StudyQueuePlanningService,
    private val queues: StudyQueueService,
    private val transactions: TransactionRunner
) {
    fun execute(sessionId: SessionId, newLimit: Int, reviewLimit: Int) = transactions.runInTransaction {
        val current = requireNotNull(sessions.findById(sessionId)) { "Phiên học không tồn tại." }
        require(current.status == SessionStatus.ACTIVE) { "Chỉ có thể thay đổi giới hạn cho phiên học đang hoạt động." }
        val effectiveNewLimit = when (current.studyMode) {
            vn.loi.learning.domain.study.recall.StudyMode.TYPING -> 0
            else -> newLimit
        }
        val effectiveReviewLimit = when (current.studyMode) {
            vn.loi.learning.domain.study.recall.StudyMode.LEARN_NEW -> 0
            else -> reviewLimit
        }
        require(effectiveNewLimit >= 0 && effectiveReviewLimit >= 0 && effectiveNewLimit + effectiveReviewLimit > 0) {
            "Giới hạn phiên học phải không âm và tổng phải lớn hơn 0."
        }
        require(effectiveNewLimit >= current.newItemsReviewed) {
            "Giới hạn từ mới ($effectiveNewLimit) không thể nhỏ hơn số từ mới đã hoàn thành (${current.newItemsReviewed})."
        }
        require(effectiveReviewLimit >= current.reviewItemsReviewed) {
            "Giới hạn ôn tập ($effectiveReviewLimit) không thể nhỏ hơn số từ ôn tập đã hoàn thành (${current.reviewItemsReviewed})."
        }

        val updated = current.copy(policy = current.policy.copy(
            newItemLimit = effectiveNewLimit,
            reviewItemLimit = effectiveReviewLimit
        ))

        val existingQueue = queues.get(sessionId)
        if (existingQueue == null) {
            val plan = planning.plan(updated)
            sessions.save(updated)
            queues.replace(plan)
            return@runInTransaction updated
        }

        val completedItemIds = existingQueue.completedLearningItemIds
        val completedOrigins = existingQueue.itemOrigins.filterKeys { it in completedItemIds }
        val completedContentIds = existingQueue.itemContentIds.filterKeys { it in completedItemIds }

        val completedNewCount = current.newItemsReviewed
        val completedReviewCount = current.reviewItemsReviewed

        val remainingNewQuota = maxOf(0, newLimit - completedNewCount)
        val remainingReviewQuota = maxOf(0, reviewLimit - completedReviewCount)

        val currentUncompletedItem = if (!existingQueue.isCompleted) existingQueue.currentLearningItemId else null
        val currentOrigin = currentUncompletedItem?.let { existingQueue.originOf(it) ?: SessionItemOrigin.NEW }
        val currentContentId = currentUncompletedItem?.let { existingQueue.contentIdOf(it) }

        val canKeepCurrent = when {
            currentUncompletedItem == null -> false
            currentOrigin == SessionItemOrigin.NEW && remainingNewQuota > 0 -> true
            currentOrigin == SessionItemOrigin.REVIEW && remainingReviewQuota > 0 -> true
            else -> false
        }

        val preservedCurrentItem = if (canKeepCurrent) currentUncompletedItem else null
        val preservedCurrentOrigin = if (canKeepCurrent) currentOrigin else null
        val preservedCurrentContentId = if (canKeepCurrent) currentContentId else null

        val additionalNewQuota = if (canKeepCurrent && currentOrigin == SessionItemOrigin.NEW) {
            maxOf(0, remainingNewQuota - 1)
        } else {
            remainingNewQuota
        }

        val additionalReviewQuota = if (canKeepCurrent && currentOrigin == SessionItemOrigin.REVIEW) {
            maxOf(0, remainingReviewQuota - 1)
        } else {
            remainingReviewQuota
        }

        val remainingPlan = if (additionalNewQuota > 0 || additionalReviewQuota > 0) {
            val excludedItems = current.reviewedItemIds + completedItemIds +
                (if (preservedCurrentItem != null) setOf(preservedCurrentItem) else emptySet())
            val excludedContents = current.reviewedContentIds +
                completedContentIds.values +
                (if (preservedCurrentContentId != null) setOf(preservedCurrentContentId) else emptySet())

            val planningSession = updated.copy(
                policy = updated.policy.copy(
                    newItemLimit = additionalNewQuota,
                    reviewItemLimit = additionalReviewQuota
                ),
                newItemsReviewed = 0,
                reviewItemsReviewed = 0,
                reviewedItemIds = excludedItems,
                reviewedContentIds = excludedContents
            )
            planning.plan(planningSession)
        } else {
            null
        }

        val additionalItems = remainingPlan?.learningItemIds
            ?.filterNot { it in completedItemIds || it == preservedCurrentItem }
            .orEmpty()

        val nextLearningItemIds = completedItemIds +
            (if (preservedCurrentItem != null) listOf(preservedCurrentItem) else emptyList()) +
            additionalItems

        val nextOrigins = completedOrigins.toMutableMap()
        if (preservedCurrentItem != null && preservedCurrentOrigin != null) {
            nextOrigins[preservedCurrentItem] = preservedCurrentOrigin
        }
        remainingPlan?.itemOrigins?.forEach { (k, v) ->
            if (k in nextLearningItemIds) nextOrigins[k] = v
        }

        val nextContentIds = completedContentIds.toMutableMap()
        if (preservedCurrentItem != null && preservedCurrentContentId != null) {
            nextContentIds[preservedCurrentItem] = preservedCurrentContentId
        }
        remainingPlan?.itemContentIds?.forEach { (k, v) ->
            if (k in nextLearningItemIds) nextContentIds[k] = v
        }

        val plannedNewCount = (if (preservedCurrentItem != null && preservedCurrentOrigin == SessionItemOrigin.NEW) 1 else 0) +
            (remainingPlan?.effectiveNewWorkload ?: 0)
        val plannedReviewCount = (if (preservedCurrentItem != null && preservedCurrentOrigin == SessionItemOrigin.REVIEW) 1 else 0) +
            (remainingPlan?.effectiveReviewWorkload ?: 0)

        val effectiveNewWorkload = minOf(effectiveNewLimit, completedNewCount + plannedNewCount)
        val effectiveReviewWorkload = minOf(effectiveReviewLimit, completedReviewCount + plannedReviewCount)

        val replannedSnapshot = StudyQueueSnapshot(
            sessionId = sessionId,
            createdAt = current.startedAt,
            learningItemIds = nextLearningItemIds,
            currentIndex = completedItemIds.size,
            itemOrigins = nextOrigins,
            itemContentIds = nextContentIds,
            configuredNewTarget = effectiveNewLimit,
            effectiveNewWorkload = effectiveNewWorkload,
            configuredReviewTarget = effectiveReviewLimit,
            effectiveReviewWorkload = effectiveReviewWorkload,
            fixedPracticeMembership = if (existingQueue.practiceLoopPolicy != PracticeLoopPolicy.NONE) {
                existingQueue.fixedPracticeMembership
            } else emptyList(),
            practiceSeed = existingQueue.practiceSeed,
            practiceRound = existingQueue.practiceRound,
            practiceLoopPolicy = existingQueue.practiceLoopPolicy,
            practiceReinforcementStates = existingQueue.practiceReinforcementStates,
            practiceExposureSequence = existingQueue.practiceExposureSequence,
            practiceMembershipUndo = existingQueue.practiceMembershipUndo,
            coverageReinforcementStates = existingQueue.coverageReinforcementStates,
            coverageReinforcementUndo = existingQueue.coverageReinforcementUndo
        )

        sessions.save(updated)
        queues.save(replannedSnapshot)
        updated
    }
}
