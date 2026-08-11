package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper

class FocusedPracticeSessionPersistenceTest {
    @Test
    fun `focused practice kind survives session record round trip`() {
        val session = StudySession.start(
            SessionId("focused-persisted"),
            LearnerId("learner"),
            Moment(1),
            SessionPolicy(
                evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED,
                focusedPracticeKind = FocusedPracticeKind.LATEST_SESSION
            )
        )

        val restored = StudySessionRecordMapper.toDomain(StudySessionRecordMapper.toRecord(session))

        assertEquals(FocusedPracticeKind.LATEST_SESSION, restored.policy.focusedPracticeKind)
        assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, restored.policy.evaluationPolicy)
    }
}
