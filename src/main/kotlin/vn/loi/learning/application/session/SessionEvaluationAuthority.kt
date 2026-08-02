package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.StudySession

/** Shared authority guarding every durable evaluation transaction. */
object SessionEvaluationAuthority {
    fun requireEvaluationAllowed(session: StudySession) {
        require(session.policy.evaluationPolicy == SessionEvaluationPolicy.EVALUATIVE) {
            "Practice-only sessions cannot mutate learning evaluation state."
        }
    }
}
