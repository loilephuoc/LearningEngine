package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

enum class ManualEvaluationAvailability {
    AVAILABLE,
    NOT_EVALUATIVE,
    NO_ACTIVE_ITEM,
    SESSION_NOT_ACTIVE
}

object ManualEvaluationAvailabilityResolver {
    fun resolve(session: StudySession): ManualEvaluationAvailability = when {
        session.status != SessionStatus.ACTIVE -> ManualEvaluationAvailability.SESSION_NOT_ACTIVE
        session.policy.evaluationPolicy != SessionEvaluationPolicy.EVALUATIVE ->
            ManualEvaluationAvailability.NOT_EVALUATIVE
        session.currentLearningItemId == null -> ManualEvaluationAvailability.NO_ACTIVE_ITEM
        else -> ManualEvaluationAvailability.AVAILABLE
    }
}
