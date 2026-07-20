package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.policy.SelectionPriority
import vn.loi.learning.domain.study.selection.priority.CandidatePriority

/**
 * Sắp xếp candidate theo mức ưu tiên của session selection.
 *
 * PriorityOrdering chỉ thực hiện việc sắp xếp.
 * Quy tắc priority được biểu diễn bởi CandidatePriority.
 *
 * Ordering luôn deterministic để test và hành vi runtime ổn định.
 */
class PriorityOrdering {

    fun order(
        candidates: List<SelectionCandidate>,
        priority: SelectionPriority
    ): List<SelectionCandidate> =
        candidates.sortedBy { candidate ->
            CandidatePriority.from(
                candidate = candidate,
                priority = priority
            )
        }
}