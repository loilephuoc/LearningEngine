package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId

enum class RecallResult {
    CORRECT,
    INCORRECT
}

enum class RecallEvidenceOrigin {
    EVALUATIVE_RECALL,
    REPLAY
}

enum class RecallEvidenceCommitStatus {
    COMMITTED,
    UNDONE
}

data class RecallEvidence(
    val reviewEventId: ReviewEventId,
    val contentId: ContentId,
    val timestamp: Moment,
    val currentRating: ReviewRating,
    val result: RecallResult,
    val sessionId: SessionId,
    val sessionPolicy: SessionEvaluationPolicy,
    val provenance: RatingSource,
    val wasRevealUsed: Boolean,
    val schedulerDue: Moment,
    val typingLatency: TimeSpan? = null,
    val origin: RecallEvidenceOrigin = RecallEvidenceOrigin.EVALUATIVE_RECALL,
    val commitStatus: RecallEvidenceCommitStatus = RecallEvidenceCommitStatus.COMMITTED
) {
    val manualOverride: Boolean
        get() = provenance == RatingSource.MANUAL_USER_OVERRIDE
}
