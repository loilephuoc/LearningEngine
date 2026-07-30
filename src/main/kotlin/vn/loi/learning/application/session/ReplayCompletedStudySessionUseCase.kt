package vn.loi.learning.application.session

import java.nio.charset.StandardCharsets
import java.util.UUID
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

data class ReplayCompletedStudySessionRequest(
    val precedingSessionId: SessionId,
    val learnerId: LearnerId,
    val requestedAt: Moment
)

sealed interface CompletedStudySessionReplayResult {
    data class Accepted(
        val session: StudySession,
        val queue: StudyQueueSnapshot,
        val alreadyAccepted: Boolean
    ) : CompletedStudySessionReplayResult

    data object NoItems : CompletedStudySessionReplayResult

    data class Rejected(
        val reason: CompletedStudySessionReplayRejection
    ) : CompletedStudySessionReplayResult
}

enum class CompletedStudySessionReplayRejection {
    PRECEDING_SESSION_NOT_FOUND,
    PRECEDING_SESSION_NOT_COMPLETED,
    LEARNER_MISMATCH,
    PRECEDING_QUEUE_NOT_FOUND,
    OTHER_ACTIVE_SESSION_EXISTS,
    EXISTING_REPLAY_INCONSISTENT
}

/**
 * Starts one ordinary StudySession from the persisted, completed membership of its predecessor.
 *
 * The predecessor queue remains the durable membership and ordering authority. This use case
 * does not plan additional work and does not alter review, scheduling, or Undo ownership.
 */
class ReplayCompletedStudySessionUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService
) {
    fun execute(
        request: ReplayCompletedStudySessionRequest
    ): CompletedStudySessionReplayResult {
        val preceding =
            sessions.findById(request.precedingSessionId)
                ?: return CompletedStudySessionReplayResult.Rejected(
                    CompletedStudySessionReplayRejection.PRECEDING_SESSION_NOT_FOUND
                )
        if (preceding.status != SessionStatus.FINISHED) {
            return CompletedStudySessionReplayResult.Rejected(
                CompletedStudySessionReplayRejection.PRECEDING_SESSION_NOT_COMPLETED
            )
        }
        if (preceding.learnerId != request.learnerId) {
            return CompletedStudySessionReplayResult.Rejected(
                CompletedStudySessionReplayRejection.LEARNER_MISMATCH
            )
        }

        val precedingQueue =
            queues.get(preceding.id)
                ?: return CompletedStudySessionReplayResult.Rejected(
                    CompletedStudySessionReplayRejection.PRECEDING_QUEUE_NOT_FOUND
                )
        val replayItems =
            precedingQueue.completedLearningItemIds
                .filter { it in preceding.reviewedItemIds }
                .distinct()
        if (replayItems.isEmpty()) return CompletedStudySessionReplayResult.NoItems

        val replaySessionId = replaySessionId(preceding.id)
        sessions.findById(replaySessionId)?.let { existing ->
            val existingQueue = queues.get(replaySessionId)
            return if (
                existing.learnerId == request.learnerId &&
                existing.status == SessionStatus.ACTIVE &&
                existingQueue?.learningItemIds == replayItems
            ) {
                CompletedStudySessionReplayResult.Accepted(
                    session = existing,
                    queue = existingQueue,
                    alreadyAccepted = true
                )
            } else {
                CompletedStudySessionReplayResult.Rejected(
                    CompletedStudySessionReplayRejection.EXISTING_REPLAY_INCONSISTENT
                )
            }
        }

        sessions.findActiveByLearner(request.learnerId)?.let {
            return CompletedStudySessionReplayResult.Rejected(
                CompletedStudySessionReplayRejection.OTHER_ACTIVE_SESSION_EXISTS
            )
        }

        val policy = SessionPolicy(newItemLimit = 0, reviewItemLimit = replayItems.size)
        val replaySession =
            StudySession.start(
                id = replaySessionId,
                learnerId = request.learnerId,
                startedAt = request.requestedAt,
                policy = policy,
                includedContentIds = preceding.includedContentIds,
                topicId = preceding.topicId,
                installedPackageId = preceding.installedPackageId
            )
        sessions.save(replaySession)
        val replayQueue =
            try {
                queues.create(
                    sessionId = replaySessionId,
                    createdAt = request.requestedAt,
                    learningItemIds = replayItems,
                    itemOrigins = replayItems.associateWith { SessionItemOrigin.REVIEW },
                    itemContentIds = precedingQueue.itemContentIds.filterKeys { it in replayItems },
                    configuredReviewTarget = replayItems.size,
                    effectiveReviewWorkload = replayItems.size
                )
            } catch (failure: RuntimeException) {
                sessions.deleteById(replaySessionId)
                throw failure
            }

        return CompletedStudySessionReplayResult.Accepted(
            session = replaySession,
            queue = replayQueue,
            alreadyAccepted = false
        )
    }

    private fun replaySessionId(precedingSessionId: SessionId): SessionId =
        SessionId(
            UUID.nameUUIDFromBytes(
                "completed-study-replay:${precedingSessionId.value}"
                    .toByteArray(StandardCharsets.UTF_8)
            ).toString()
        )
}
