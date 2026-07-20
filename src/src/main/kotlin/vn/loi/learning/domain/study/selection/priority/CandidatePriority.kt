package vn.loi.learning.domain.study.selection.priority

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.policy.SelectionPriority

/**
 * Khóa ưu tiên bất biến của một SelectionCandidate.
 *
 * Priority được so sánh lần lượt theo:
 * 1. Nhóm NEW hoặc REVIEW theo SelectionPriority.
 * 2. dueAt sớm hơn.
 * 3. LearningItemId nhỏ hơn.
 *
 * CandidatePriority:
 * - không giữ hoặc quản lý queue;
 * - không thay đổi SelectionCandidate;
 * - không thay đổi MemoryState;
 * - không truy cập persistence;
 * - không thực hiện scheduling.
 */
data class CandidatePriority(
    val groupRank: Int,
    val dueAt: Moment,
    val learningItemId: LearningItemId
) : Comparable<CandidatePriority> {

    override fun compareTo(
        other: CandidatePriority
    ): Int {

        val groupComparison =
            groupRank.compareTo(other.groupRank)

        if (groupComparison != 0) {
            return groupComparison
        }

        val dueAtComparison =
            dueAt.compareTo(other.dueAt)

        if (dueAtComparison != 0) {
            return dueAtComparison
        }

        return learningItemId.value.compareTo(
            other.learningItemId.value
        )
    }

    companion object {

        fun from(
            candidate: SelectionCandidate,
            priority: SelectionPriority
        ): CandidatePriority {

            return CandidatePriority(
                groupRank = groupRankOf(
                    candidate = candidate,
                    priority = priority
                ),
                dueAt = candidate.memoryState.dueAt,
                learningItemId = candidate.learningItemId
            )
        }

        private fun groupRankOf(
            candidate: SelectionCandidate,
            priority: SelectionPriority
        ): Int =
            when (priority) {
                SelectionPriority.REVIEW_FIRST ->
                    if (candidate.isNew) 1 else 0

                SelectionPriority.NEW_FIRST ->
                    if (candidate.isNew) 0 else 1
            }
    }
}