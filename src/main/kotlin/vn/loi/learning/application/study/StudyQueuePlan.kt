package vn.loi.learning.application.study

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

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
    learningItemIds: List<LearningItemId>
) {

    val learningItemIds: List<LearningItemId> =
        learningItemIds.toList()

    init {
        require(
            this.learningItemIds.distinct().size ==
                    this.learningItemIds.size
        ) {
            "Study queue plan must not contain duplicate LearningItemIds."
        }
    }

    val totalItemCount: Int
        get() =
            learningItemIds.size

    val isEmpty: Boolean
        get() =
            learningItemIds.isEmpty()

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
                other.learningItemIds
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

        return result
    }

    override fun toString(): String =
        "StudyQueuePlan(" +
                "sessionId=$sessionId, " +
                "plannedAt=$plannedAt, " +
                "learningItemIds=$learningItemIds" +
                ")"

    companion object {

        fun create(
            sessionId: SessionId,
            plannedAt: Moment,
            learningItemIds:
            List<LearningItemId>
        ): StudyQueuePlan =
            StudyQueuePlan(
                sessionId = sessionId,
                plannedAt = plannedAt,
                learningItemIds =
                    learningItemIds
            )
    }
}