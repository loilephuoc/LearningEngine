package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

enum class StudyQueueUnderfillReason {
    NONE,
    ELIGIBLE_INVENTORY_EXHAUSTED
}

/**
 * Kết quả bất biến của một lần lập kế hoạch StudyQueue.
 *
 * StudyQueuePlan chỉ mô tả:
 * - queue thuộc session nào;
 * - queue được lập tại thời điểm nào;
 * - thứ tự LearningItem đã được lựa chọn.
 *
 * Plan không:
 * - persist queue;
 * - thay đổi StudySession;
 * - advance queue;
 * - thay đổi MemoryState;
 * - thực hiện review.
 */
class StudyQueuePlan private constructor(
    val sessionId: SessionId,
    val plannedAt: Moment,
    learningItemIds: List<LearningItemId>,
    itemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
    itemContentIds: Map<LearningItemId, ContentId> = emptyMap(),
    val configuredNewTarget: Int = 0,
    val effectiveNewWorkload: Int = 0,
    val configuredReviewTarget: Int = 0,
    val effectiveReviewWorkload: Int = 0
) {

    val learningItemIds: List<LearningItemId> =
        learningItemIds.toList()
    val itemOrigins: Map<LearningItemId, SessionItemOrigin> = itemOrigins.toMap()
    val itemContentIds: Map<LearningItemId, ContentId> = itemContentIds.toMap()

    init {
        require(
            this.learningItemIds.distinct().size ==
                    this.learningItemIds.size
        ) {
            "Study queue plan must not contain duplicate LearningItemIds."
        }
        require(this.itemOrigins.keys.all { it in this.learningItemIds }) {
            "Study queue origins must reference planned LearningItemIds."
        }
        require(this.itemContentIds.keys.all { it in this.learningItemIds }) {
            "Study queue content identities must reference planned LearningItemIds."
        }
        require(effectiveNewWorkload in 0..configuredNewTarget)
        require(effectiveReviewWorkload in 0..configuredReviewTarget)
    }

    val totalItemCount: Int
        get() =
            learningItemIds.size

    val isEmpty: Boolean
        get() =
            learningItemIds.isEmpty()

    val newUnderfillReason: StudyQueueUnderfillReason
        get() = if (effectiveNewWorkload < configuredNewTarget) {
            StudyQueueUnderfillReason.ELIGIBLE_INVENTORY_EXHAUSTED
        } else {
            StudyQueueUnderfillReason.NONE
        }

    val reviewUnderfillReason: StudyQueueUnderfillReason
        get() = if (effectiveReviewWorkload < configuredReviewTarget) {
            StudyQueueUnderfillReason.ELIGIBLE_INVENTORY_EXHAUSTED
        } else {
            StudyQueueUnderfillReason.NONE
        }

    operator fun contains(
        learningItemId: LearningItemId
    ): Boolean =
        learningItemId in learningItemIds

    override fun equals(
        other: Any?
    ): Boolean {
        if (this === other) {
            return true
        }

        if (
            other !is StudyQueuePlan
        ) {
            return false
        }

        return sessionId ==
                other.sessionId &&
                plannedAt ==
                other.plannedAt &&
                learningItemIds ==
                other.learningItemIds &&
                itemOrigins == other.itemOrigins &&
                itemContentIds == other.itemContentIds &&
                configuredNewTarget == other.configuredNewTarget &&
                effectiveNewWorkload == other.effectiveNewWorkload &&
                configuredReviewTarget == other.configuredReviewTarget &&
                effectiveReviewWorkload == other.effectiveReviewWorkload
    }

    override fun hashCode(): Int {
        var result =
            sessionId.hashCode()

        result =
            31 * result +
                    plannedAt.hashCode()

        result =
            31 * result +
                    learningItemIds.hashCode()
        result = 31 * result + itemOrigins.hashCode()
        result = 31 * result + itemContentIds.hashCode()
        result = 31 * result + configuredNewTarget
        result = 31 * result + effectiveNewWorkload
        result = 31 * result + configuredReviewTarget
        result = 31 * result + effectiveReviewWorkload

        return result
    }

    override fun toString(): String =
        "StudyQueuePlan(" +
                "sessionId=$sessionId, " +
                "plannedAt=$plannedAt, " +
                "learningItemIds=$learningItemIds, " +
                "itemOrigins=$itemOrigins, " +
                "itemContentIds=$itemContentIds" +
                ")"

    companion object {

        fun create(
            sessionId: SessionId,
            plannedAt: Moment,
            learningItemIds:
            List<LearningItemId>,
            itemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
            itemContentIds: Map<LearningItemId, ContentId> = emptyMap(),
            configuredNewTarget: Int = 0,
            effectiveNewWorkload: Int = 0,
            configuredReviewTarget: Int = 0,
            effectiveReviewWorkload: Int = 0
        ): StudyQueuePlan =
            StudyQueuePlan(
                sessionId = sessionId,
                plannedAt = plannedAt,
                learningItemIds =
                    learningItemIds,
                itemOrigins = itemOrigins,
                itemContentIds = itemContentIds,
                configuredNewTarget = configuredNewTarget,
                effectiveNewWorkload = effectiveNewWorkload,
                configuredReviewTarget = configuredReviewTarget,
                effectiveReviewWorkload = effectiveReviewWorkload
            )
    }
}
