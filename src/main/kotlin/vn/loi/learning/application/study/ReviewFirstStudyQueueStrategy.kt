package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.pipeline.PriorityOrdering
import vn.loi.learning.domain.study.selection.policy.SelectionPriority

/**
 * Chiến lược mặc định ưu tiên REVIEW trước NEW.
 *
 * Việc diễn giải SelectionPriority được giao cho PriorityOrdering
 * để không lặp lại quy tắc ordering trong application layer.
 */
class ReviewFirstStudyQueueStrategy(
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
                SelectionPriority.REVIEW_FIRST
        )
}