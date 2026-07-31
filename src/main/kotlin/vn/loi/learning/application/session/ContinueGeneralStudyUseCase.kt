package vn.loi.learning.application.session

import java.nio.charset.StandardCharsets
import java.util.UUID
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

data class ContinueGeneralStudyRequest(
    val precedingSessionId: SessionId,
    val learnerId: LearnerId,
    val requestedAt: Moment,
    val policy: SessionPolicy,
    val installedPackageId: InstalledPackageId,
    val topicId: TopicId?
)

sealed interface GeneralStudyContinuationResult {
    data class Accepted(
        val session: StudySession,
        val queue: StudyQueueSnapshot,
        val alreadyAccepted: Boolean
    ) : GeneralStudyContinuationResult

    data object NoWork : GeneralStudyContinuationResult

    data class Rejected(
        val reason: GeneralStudyContinuationRejection
    ) : GeneralStudyContinuationResult
}

enum class GeneralStudyContinuationRejection {
    PRECEDING_SESSION_NOT_FOUND,
    PRECEDING_SESSION_NOT_COMPLETED,
    LEARNER_MISMATCH,
    SCOPE_MISMATCH,
    NOT_GENERAL_STUDY,
    OTHER_ACTIVE_SESSION_EXISTS,
    EXISTING_CONTINUATION_INCONSISTENT
}

/**
 * Coordinates one ordinary general-Study continuation.
 *
 * Planner and ordinary Study remain authoritative for queue planning, Session creation,
 * acceptance, and completion. The deterministic next SessionId gives sequential and
 * restart-visible repeated requests one persisted acceptance identity. Durable Continuous
 * Review delegates here and does not change this ordinary one-time continuation contract.
 */
class ContinueGeneralStudyUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val startSession: StartStudySessionUseCase
) {
    fun execute(
        request: ContinueGeneralStudyRequest
    ): GeneralStudyContinuationResult {
        val preceding =
            sessions.findById(request.precedingSessionId)
                ?: return GeneralStudyContinuationResult.Rejected(
                    GeneralStudyContinuationRejection.PRECEDING_SESSION_NOT_FOUND
                )

        validate(preceding, request)?.let { reason ->
            return GeneralStudyContinuationResult.Rejected(reason)
        }

        val nextSessionId = continuationSessionId(preceding.id)
        sessions.findById(nextSessionId)?.let { existing ->
            return existingContinuation(existing, request)
        }

        sessions.findActiveByLearner(request.learnerId)?.let {
            return GeneralStudyContinuationResult.Rejected(
                GeneralStudyContinuationRejection.OTHER_ACTIVE_SESSION_EXISTS
            )
        }

        val nextSession =
            startSession.execute(
                StartStudySessionCommand(
                    sessionId = nextSessionId,
                    learnerId = request.learnerId,
                    startedAt = request.requestedAt,
                    policy = request.policy,
                    topicId = request.topicId,
                    installedPackageId = request.installedPackageId
                )
            )
        val queue = queues.require(nextSessionId)

        if (queue.isEmpty) {
            queues.delete(nextSessionId)
            sessions.deleteById(nextSessionId)
            return GeneralStudyContinuationResult.NoWork
        }

        return GeneralStudyContinuationResult.Accepted(
            session = nextSession,
            queue = queue,
            alreadyAccepted = false
        )
    }

    private fun validate(
        preceding: StudySession,
        request: ContinueGeneralStudyRequest
    ): GeneralStudyContinuationRejection? =
        when {
            preceding.status != SessionStatus.FINISHED ->
                GeneralStudyContinuationRejection.PRECEDING_SESSION_NOT_COMPLETED

            preceding.learnerId != request.learnerId ->
                GeneralStudyContinuationRejection.LEARNER_MISMATCH

            preceding.includedContentIds.isNotEmpty() ->
                GeneralStudyContinuationRejection.NOT_GENERAL_STUDY

            preceding.installedPackageId != request.installedPackageId ||
                preceding.topicId != request.topicId ->
                GeneralStudyContinuationRejection.SCOPE_MISMATCH

            else -> null
        }

    private fun existingContinuation(
        existing: StudySession,
        request: ContinueGeneralStudyRequest
    ): GeneralStudyContinuationResult {
        val queue = queues.get(existing.id)
        val consistent =
            existing.learnerId == request.learnerId &&
                existing.includedContentIds.isEmpty() &&
                existing.installedPackageId == request.installedPackageId &&
                existing.topicId == request.topicId &&
                queue != null &&
                !queue.isEmpty

        return if (consistent) {
            GeneralStudyContinuationResult.Accepted(
                session = existing,
                queue = requireNotNull(queue),
                alreadyAccepted = true
            )
        } else {
            GeneralStudyContinuationResult.Rejected(
                GeneralStudyContinuationRejection.EXISTING_CONTINUATION_INCONSISTENT
            )
        }
    }

    internal fun continuationSessionId(
        precedingSessionId: SessionId
    ): SessionId =
        SessionId(
            UUID.nameUUIDFromBytes(
                "general-study-continuation:${precedingSessionId.value}"
                    .toByteArray(StandardCharsets.UTF_8)
            ).toString()
        )
}
