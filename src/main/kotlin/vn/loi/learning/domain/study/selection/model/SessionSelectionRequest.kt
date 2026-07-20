package vn.loi.learning.domain.study.selection.model

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.policy.SessionSelectionPolicy
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Toàn bộ dữ liệu cần thiết cho một lần lựa chọn LearningItem tiếp theo.
 *
 * Request không truy cập repository và không thay đổi StudySession.
 */
data class SessionSelectionRequest(
    val candidates: List<SelectionCandidate>,
    val session: StudySession,
    val selectionPolicy: SessionSelectionPolicy,
    val at: Moment,
    val previousContentId: ContentId? = null
)