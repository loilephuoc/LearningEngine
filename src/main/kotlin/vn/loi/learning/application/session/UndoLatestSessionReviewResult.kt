package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.StudySession

sealed interface UndoLatestSessionReviewResult {
    data object NothingToUndo : UndoLatestSessionReviewResult
    data class Undone(
        val session: StudySession,
        val progress: LearningSessionProgress
    ) : UndoLatestSessionReviewResult
}
