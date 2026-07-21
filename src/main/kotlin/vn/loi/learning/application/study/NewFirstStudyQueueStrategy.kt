package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.pipeline.PriorityOrdering
import vn.loi.learning.domain.study.selection.policy.SelectionPriority

/**
 * Chiến lược ưu tiên item NEW trước item REVIEW.
 *
 * Strategy chỉ chọn SelectionPriority.NEW_FIRST.
 * Toàn bộ quy tắc so sánh và deterministic ordering vẫn được
 * giao cho PriorityOrdering của domain.
 *
 * Strategy không:
 * - đọc repository;
 * - lọc candidate;
 * - áp dụng SessionPolicy;
 * - thay đổi candidate;
 * - tạo hoặc persist StudyQueue.
 */
class NewFirstStudyQueueStrategy(
    private val priorityOrdering:
    PriorityOrdering =
        PriorityOrdering()
) : StudyQueueStrategy {

    override fun order(
        candidates: List<SelectionCandidate>
    ): List<SelectionCandidate> =
        priorityOrdering.order(
            candidates = candidates,
            priority =
                SelectionPriority.NEW_FIRST
        )
}