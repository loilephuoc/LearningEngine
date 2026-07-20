package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Lọc các candidate đủ điều kiện tham gia Session Selection.
 *
 * Một candidate được xem là eligible khi:
 * - LearningItem đang enabled;
 * - MemoryState đã đến hạn tại thời điểm xét.
 */
class EligibilityFilter {

    fun filter(
        candidates: List<SelectionCandidate>,
        at: Moment
    ): List<SelectionCandidate> =
        candidates.filter { candidate ->
            candidate.isEligibleAt(at)
        }
}