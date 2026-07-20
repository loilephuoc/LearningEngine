package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Lọc candidate đã xuất hiện trong StudySession.
 *
 * Khi session cho phép lặp lại:
 * - giữ nguyên toàn bộ candidate.
 *
 * Khi session không cho phép lặp lại:
 * - loại các LearningItem đã có trong reviewedItemIds.
 */
class RepeatFilter {

    fun filter(
        candidates: List<SelectionCandidate>,
        session: StudySession
    ): List<SelectionCandidate> {

        if (session.policy.allowRepeatInSameSession) {
            return candidates
        }

        return candidates.filterNot { candidate ->
            candidate.learningItemId in session.reviewedItemIds
        }
    }
}