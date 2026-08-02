package vn.loi.learning.domain.study.session.model

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.evidence.AutomaticRecallEvidenceInput

/** Durable intent used to complete exactly one interrupted session review. */
data class PendingSessionReview(
    val reviewEventId: ReviewEventId,
    val learningItemId: LearningItemId,
    val rating: ReviewRating,
    val reviewedAt: Moment,
    val responseTime: TimeSpan?,
    val ratingSource: RatingSource = RatingSource.STANDARD_REVIEW,
    val automaticRecall: AutomaticRecallEvidenceInput? = null
)
