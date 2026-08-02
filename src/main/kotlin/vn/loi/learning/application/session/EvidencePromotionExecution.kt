package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId

data class EvidenceExecutionResult(
    val committedRating: ReviewRating,
    val promotionDecision: PromotionDecision?,
    val trajectoryBefore: LearningTrajectory?,
    val trajectoryAfter: LearningTrajectory?
)

class EvidencePromotionExecution(
    private val trajectories: LearningTrajectoryRepository
) {
    fun execute(
        learnerId: LearnerId,
        contentId: ContentId,
        sessionId: SessionId,
        eventId: ReviewEventId,
        proposedRating: ReviewRating,
        reviewedAt: Moment,
        schedulerDue: Moment,
        source: RatingSource,
        sessionPolicy: SessionEvaluationPolicy,
        recall: AutomaticRecallEvidenceInput?
    ): EvidenceExecutionResult {
        if (source != RatingSource.STANDARD_REVIEW || recall == null ||
            sessionPolicy != SessionEvaluationPolicy.EVALUATIVE || recall.wasRevealUsed
        ) return EvidenceExecutionResult(proposedRating, null, null, null)

        val before = trajectories.find(learnerId, contentId)
        if (before?.chains?.any { chain -> chain.recallEvidence.any { it.reviewEventId == eventId } } == true) {
            return EvidenceExecutionResult(proposedRating, null, before, before)
        }
        val chainRating = before?.currentChain()?.stage?.anchorRating ?: proposedRating
        val evidence = RecallEvidence(
            eventId, contentId, reviewedAt, chainRating, recall.result, sessionId,
            sessionPolicy, source, false, schedulerDue, recall.typingLatency
        )
        var trajectory = if (before == null) {
            LearningTrajectory.start(newChain(contentId, evidence, ChainAnchorReason.INITIAL_RATING))
        } else if (proposedRating == ReviewRating.AGAIN && chainRating != ReviewRating.AGAIN) {
            before.reset(anchor(evidence.copy(currentRating = ReviewRating.AGAIN), ChainAnchorReason.NEW_LAPSE), ChainResetReason.NEW_LAPSE)
        } else if (proposedRating == ReviewRating.AGAIN && chainRating == ReviewRating.AGAIN) {
            before.reset(anchor(evidence, ChainAnchorReason.NEW_AGAIN), ChainResetReason.NEW_AGAIN)
        } else {
            before.advance(evidence)
        }

        val decision = EvidencePromotionAuthority(EvidenceClock { reviewedAt })
            .evaluatePromotion(PromotionCandidate(trajectory.currentChain()))
        val allowed = if (decision.eligible) requireNotNull(decision.targetRating) else decision.currentRating
        val committed = lowerOf(proposedRating, allowed)
        if (decision.eligible && committed == decision.targetRating) {
            val promotedEvidence = evidence.copy(currentRating = committed)
            trajectory = requireNotNull(before).reset(
                anchor(promotedEvidence, ChainAnchorReason.PROMOTED),
                ChainResetReason.PROMOTED
            )
        }
        trajectories.save(learnerId, trajectory)
        return EvidenceExecutionResult(committed, decision, before, trajectory)
    }

    private fun newChain(contentId: ContentId, evidence: RecallEvidence, reason: ChainAnchorReason) =
        EvidenceChain.start(contentId, PromotionStage.fromAnchorRating(evidence.currentRating), anchor(evidence, reason))

    private fun anchor(evidence: RecallEvidence, reason: ChainAnchorReason) =
        ChainAnchor(evidence.currentRating, evidence.timestamp, evidence, reason)

    private fun lowerOf(first: ReviewRating, second: ReviewRating): ReviewRating =
        if (first.ordinal <= second.ordinal) first else second
}
