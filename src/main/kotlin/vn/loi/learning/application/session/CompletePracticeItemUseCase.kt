package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.recall.RecallModeHistoryEntry

data class CompletePracticeItemCommand(
    val sessionId: SessionId,
    val learningItemId: LearningItemId,
    val result: PracticeRecallResult,
    val recallModeHistoryEntry: RecallModeHistoryEntry? = null
)

data class CompletePracticeItemResult(
    val session: vn.loi.learning.domain.study.session.model.StudySession,
    val progress: PracticeProgress?
)

/** Advances practice-local navigation without entering any evaluation transaction. */
class CompletePracticeItemUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val transactions: TransactionRunner
) {
    fun execute(command: CompletePracticeItemCommand): CompletePracticeItemResult =
        transactions.runInTransaction {
            val session = requireNotNull(sessions.findById(command.sessionId))
            require(session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY)
            val queue = queues.require(command.sessionId)
            require(queue.currentLearningItemId == command.learningItemId)
            val updatedSession = session.completePracticeItem(command.learningItemId, command.recallModeHistoryEntry)
            sessions.save(updatedSession)
            val advanced = queues.advancePractice(
                command.sessionId,
                command.result,
                graduateCorrectLocally = session.policy.focusedPracticeKind ==
                    vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT
            )
            CompletePracticeItemResult(
                updatedSession,
                advanced.practiceProgress
            )
        }
}
