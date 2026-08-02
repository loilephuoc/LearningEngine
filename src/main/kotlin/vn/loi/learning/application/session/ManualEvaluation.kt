package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

enum class EvaluativeRatingAvailability {
    AVAILABLE,
    NOT_EVALUATIVE,
    NO_ACTIVE_ITEM,
    SESSION_NOT_ACTIVE
}

object EvaluativeRatingAvailabilityResolver {
    fun resolve(session: StudySession): EvaluativeRatingAvailability = when {
        session.status != SessionStatus.ACTIVE -> EvaluativeRatingAvailability.SESSION_NOT_ACTIVE
        session.policy.evaluationPolicy != SessionEvaluationPolicy.EVALUATIVE ->
            EvaluativeRatingAvailability.NOT_EVALUATIVE
        session.currentLearningItemId == null -> EvaluativeRatingAvailability.NO_ACTIVE_ITEM
        else -> EvaluativeRatingAvailability.AVAILABLE
    }
}
