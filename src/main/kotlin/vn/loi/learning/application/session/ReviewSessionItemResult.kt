package vn.loi.learning.application.session

import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.domain.study.session.model.StudySession

data class ReviewSessionItemResult(
    val session: StudySession,
    val reviewResult: ReviewResult,
    val progress: LearningSessionProgress? = null
)
