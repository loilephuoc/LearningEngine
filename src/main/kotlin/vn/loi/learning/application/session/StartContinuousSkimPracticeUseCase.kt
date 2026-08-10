package vn.loi.learning.application.session

import java.util.UUID
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

data class StartContinuousSkimPracticeRequest(
    val completedSessionId: SessionId,
    val requestedAt: Moment
)

sealed interface StartContinuousSkimPracticeResult {
    data class Accepted(val session: StudySession, val queue: StudyQueueSnapshot) :
        StartContinuousSkimPracticeResult
    data object NoItems : StartContinuousSkimPracticeResult
}

/** Transitions completed scheduled coverage into a persisted, practice-only shuffled loop. */
class StartContinuousSkimPracticeUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService
) {
    fun execute(request: StartContinuousSkimPracticeRequest): StartContinuousSkimPracticeResult {
        val predecessor = requireNotNull(sessions.findById(request.completedSessionId))
        require(predecessor.status == SessionStatus.FINISHED)
        val predecessorQueue = queues.require(predecessor.id)
        val membership = predecessorQueue.learningItemIds.distinct()
        if (membership.isEmpty()) return StartContinuousSkimPracticeResult.NoItems

        val uuid = UUID.randomUUID()
        val sessionId = SessionId(uuid.toString())
        val session = StudySession.start(
            id = sessionId,
            learnerId = predecessor.learnerId,
            startedAt = request.requestedAt,
            policy = SessionPolicy(
                newItemLimit = 0,
                reviewItemLimit = membership.size,
                allowRepeatInSameSession = true,
                evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
            ),
            includedContentIds = predecessor.includedContentIds,
            topicId = predecessor.topicId,
            installedPackageId = predecessor.installedPackageId,
            studyMode = predecessor.studyMode
        )
        sessions.save(session)
        val queue = try {
            queues.create(
                sessionId = sessionId,
                createdAt = request.requestedAt,
                learningItemIds = membership,
                itemOrigins = membership.associateWith {
                    predecessorQueue.itemOrigins[it]
                        ?: vn.loi.learning.domain.study.session.model.SessionItemOrigin.REVIEW
                },
                itemContentIds = predecessorQueue.itemContentIds.filterKeys { it in membership },
                configuredReviewTarget = membership.size,
                effectiveReviewWorkload = membership.mapNotNull(predecessorQueue.itemContentIds::get).distinct().size,
                practiceSeed = uuid.mostSignificantBits xor uuid.leastSignificantBits,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
            )
        } catch (failure: RuntimeException) {
            sessions.deleteById(sessionId)
            throw failure
        }
        return StartContinuousSkimPracticeResult.Accepted(session, queue)
    }
}
