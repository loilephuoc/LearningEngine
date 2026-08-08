package vn.loi.learning.application.session

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.evidence.AutomaticRecallEvidenceInput
import vn.loi.learning.domain.study.recall.RecallModeHistoryEntry

data class ReviewSessionItemCommand(
    val sessionId: SessionId,
    val reviewEventId: ReviewEventId,
    val learningItemId: LearningItemId,
    val rating: ReviewRating,
    val reviewedAt: Moment,
    val responseTime: TimeSpan? = null,
    val ratingSource: RatingSource = RatingSource.STANDARD_REVIEW,
    val automaticRecall: AutomaticRecallEvidenceInput? = null,
    val recallModeHistoryEntry: RecallModeHistoryEntry? = null
)
