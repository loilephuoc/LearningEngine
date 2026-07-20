package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Lọc candidate theo quota còn lại của StudySession.
 *
 * Quy tắc:
 * - Item mới chỉ được giữ khi session còn quota new item.
 * - Item không mới chỉ được giữ khi session còn quota review item.
 *
 * Filter không thay đổi StudySession và không tăng bộ đếm review.
 */
class SessionQuotaFilter {

    fun filter(
        candidates: List<SelectionCandidate>,
        session: StudySession
    ): List<SelectionCandidate> =
        candidates.filter { candidate ->
            if (candidate.isNew) {
                session.canReviewNewItem
            } else {
                session.canReviewDueItem
            }
        }
}