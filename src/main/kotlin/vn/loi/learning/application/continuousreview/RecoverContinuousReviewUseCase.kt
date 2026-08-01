package vn.loi.learning.application.continuousreview

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.ContinueGeneralStudyRequest
import vn.loi.learning.application.session.ContinueGeneralStudyUseCase
import vn.loi.learning.application.session.GeneralStudyContinuationResult
import vn.loi.learning.application.session.RecoverActiveStudySessionUseCase
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

sealed interface ContinuousReviewRecoveryResult {
    data object Disabled : ContinuousReviewRecoveryResult
    data object NoEligiblePredecessor : ContinuousReviewRecoveryResult
    data class ScopeInactive(
        val intentPackageId: InstalledPackageId,
        val intentTopicId: TopicId?
    ) : ContinuousReviewRecoveryResult
    data class ResumedExisting(val recovery: ActiveStudySessionRecovery.Resumable) : ContinuousReviewRecoveryResult
    data class ClosedIncomplete(val recovery: ActiveStudySessionRecovery.ClosedIncompleteSession) : ContinuousReviewRecoveryResult
    data class Continued(val accepted: GeneralStudyContinuationResult.Accepted) : ContinuousReviewRecoveryResult
    data object NoWork : ContinuousReviewRecoveryResult
    data class Rejected(val rejection: GeneralStudyContinuationResult.Rejected) : ContinuousReviewRecoveryResult
}

data class ContinuousReviewRecoveryRequest(
    val learnerId: LearnerId,
    val installedPackageId: InstalledPackageId,
    val topicId: TopicId?,
    val recoveredAt: Moment
)

class RecoverContinuousReviewUseCase(
    private val intentService: ContinuousReviewService,
    private val sessions: StudySessionRepository,
    private val recoverActive: RecoverActiveStudySessionUseCase,
    private val continueGeneralStudy: ContinueGeneralStudyUseCase
) {
    fun execute(request: ContinuousReviewRecoveryRequest): ContinuousReviewRecoveryResult {
        val learnerId = request.learnerId
        val recoveredAt = request.recoveredAt
        when (val recovery = recoverActive.execute(learnerId, recoveredAt)) {
            is ActiveStudySessionRecovery.Resumable ->
                return ContinuousReviewRecoveryResult.ResumedExisting(recovery)
            is ActiveStudySessionRecovery.ClosedIncompleteSession ->
                return ContinuousReviewRecoveryResult.ClosedIncomplete(recovery)
            ActiveStudySessionRecovery.NoActiveSession -> Unit
        }

        val intent = intentService.query(learnerId)
            ?.takeIf { it.enabled }
            ?: return ContinuousReviewRecoveryResult.Disabled
        if (intent.installedPackageId != request.installedPackageId ||
            intent.topicId != request.topicId
        ) {
            return ContinuousReviewRecoveryResult.ScopeInactive(
                intent.installedPackageId,
                intent.topicId
            )
        }
        val predecessor = sessions.findAll()
            .asSequence()
            .filter { it.isEligiblePredecessor(intent) }
            .sortedWith(compareBy<StudySession>({ it.finishedAt!!.epochMillis }, { it.id.value }))
            .lastOrNull()
            ?: return ContinuousReviewRecoveryResult.NoEligiblePredecessor
        if (intent.lastNoWorkPredecessorId == predecessor.id) {
            return ContinuousReviewRecoveryResult.NoWork
        }
        return when (val result = continueGeneralStudy.execute(
            ContinueGeneralStudyRequest(
                precedingSessionId = predecessor.id,
                learnerId = learnerId,
                requestedAt = recoveredAt,
                policy = predecessor.policy,
                installedPackageId = intent.installedPackageId,
                topicId = intent.topicId
            )
        )) {
            is GeneralStudyContinuationResult.Accepted -> ContinuousReviewRecoveryResult.Continued(result)
            GeneralStudyContinuationResult.NoWork -> {
                intentService.recordNoWork(intent, predecessor.id, recoveredAt)
                ContinuousReviewRecoveryResult.NoWork
            }
            is GeneralStudyContinuationResult.Rejected -> ContinuousReviewRecoveryResult.Rejected(result)
        }
    }

    private fun StudySession.isEligiblePredecessor(intent: ContinuousReviewIntent): Boolean =
        learnerId == intent.learnerId &&
            status == SessionStatus.FINISHED &&
            completionProvenance == SessionCompletionProvenance.ORDINARY_SUCCESS &&
            includedContentIds.isEmpty() &&
            installedPackageId == intent.installedPackageId &&
            topicId == intent.topicId
}
