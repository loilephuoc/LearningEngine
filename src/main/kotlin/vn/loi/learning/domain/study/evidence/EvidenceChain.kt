package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy

enum class PromotionStage(val anchorRating: ReviewRating, val targetRating: ReviewRating?) {
    AGAIN_TO_HARD(ReviewRating.AGAIN, ReviewRating.HARD),
    HARD_TO_GOOD(ReviewRating.HARD, ReviewRating.GOOD),
    GOOD_TO_EASY(ReviewRating.GOOD, ReviewRating.EASY),
    COMPLETE(ReviewRating.EASY, null);

    companion object {
        fun fromAnchorRating(rating: ReviewRating): PromotionStage =
            entries.single { it.anchorRating == rating }
    }
}

enum class ChainAnchorReason {
    INITIAL_RATING,
    PROMOTED,
    NEW_AGAIN,
    NEW_LAPSE,
    MANUAL_RESET,
    DATA_RECOVERY,
    UNDO
}

enum class ChainResetReason {
    PROMOTED,
    NEW_AGAIN,
    NEW_LAPSE,
    MANUAL_RESET,
    DATA_RECOVERY,
    UNDO
}

data class ChainAnchor(
    val rating: ReviewRating,
    val timestamp: Moment,
    val evidence: RecallEvidence,
    val reason: ChainAnchorReason
) {
    init {
        require(evidence.currentRating == rating) {
            "Chain anchor rating must match its evidence rating."
        }
        require(evidence.timestamp == timestamp) {
            "Chain anchor timestamp must match its evidence timestamp."
        }
        require(evidence.sessionPolicy == SessionEvaluationPolicy.EVALUATIVE) {
            "Chain anchor must come from an evaluative session."
        }
        require(evidence.provenance == RatingSource.STANDARD_REVIEW) {
            "Manual rating cannot anchor an evidence chain."
        }
        require(!evidence.wasRevealUsed) {
            "Reveal evidence cannot anchor an evidence chain."
        }
        require(evidence.origin == RecallEvidenceOrigin.EVALUATIVE_RECALL) {
            "Replay evidence cannot anchor an evidence chain."
        }
        require(evidence.commitStatus == RecallEvidenceCommitStatus.COMMITTED) {
            "Undone evidence cannot anchor an evidence chain."
        }
    }
}

enum class NonEvidenceEventKind {
    MANUAL_RATING
}

data class NonEvidenceEvent(
    val contentId: ContentId,
    val timestamp: Moment,
    val sessionId: SessionId,
    val rating: ReviewRating,
    val provenance: RatingSource,
    val kind: NonEvidenceEventKind = NonEvidenceEventKind.MANUAL_RATING
) {
    init {
        require(provenance != RatingSource.STANDARD_REVIEW) {
            "A non-evidence manual event must have manual provenance."
        }
    }
}

sealed interface EvidenceSequenceEntry {
    val timestamp: Moment

    data class Recall(val evidence: RecallEvidence) : EvidenceSequenceEntry {
        override val timestamp: Moment
            get() = evidence.timestamp
    }

    data class NonEvidence(val event: NonEvidenceEvent) : EvidenceSequenceEntry {
        override val timestamp: Moment
            get() = event.timestamp
    }
}

@ConsistentCopyVisibility
data class EvidenceChain private constructor(
    val contentId: ContentId,
    val stage: PromotionStage,
    val anchor: ChainAnchor,
    val sequence: List<EvidenceSequenceEntry>,
    val closedBy: ChainResetReason? = null
) {
    init {
        require(anchor.evidence.contentId == contentId) {
            "Chain anchor must belong to the chain Content."
        }
        require(anchor.rating == stage.anchorRating) {
            "Chain anchor rating must match the promotion stage."
        }
        require(sequence.firstOrNull() == EvidenceSequenceEntry.Recall(anchor.evidence)) {
            "Evidence sequence must begin with the chain anchor."
        }
        require(sequence.zipWithNext().all { (first, second) -> first.timestamp <= second.timestamp }) {
            "Evidence sequence must be chronological."
        }
    }

    val recallEvidence: List<RecallEvidence>
        get() = sequence.mapNotNull { (it as? EvidenceSequenceEntry.Recall)?.evidence }

    val nonEvidenceEvents: List<NonEvidenceEvent>
        get() = sequence.mapNotNull { (it as? EvidenceSequenceEntry.NonEvidence)?.event }

    fun advance(evidence: RecallEvidence): EvidenceChain {
        require(closedBy == null) { "A closed evidence chain cannot advance." }
        require(evidence.contentId == contentId) { "Recall evidence must belong to the chain Content." }
        require(evidence.provenance == RatingSource.STANDARD_REVIEW) {
            "Manual rating must be recorded as a non-evidence event."
        }
        require(recallEvidence.none { it.reviewEventId == evidence.reviewEventId }) {
            "Duplicate recall evidence is not allowed in a chain."
        }
        require(evidence.timestamp >= sequence.last().timestamp) {
            "Recall evidence must not precede the current chain sequence."
        }
        return copy(sequence = sequence + EvidenceSequenceEntry.Recall(evidence))
    }

    fun recordNonEvidence(event: NonEvidenceEvent): EvidenceChain {
        require(closedBy == null) { "A closed evidence chain cannot record an event." }
        require(event.contentId == contentId) { "Non-evidence event must belong to the chain Content." }
        require(event.timestamp >= sequence.last().timestamp) {
            "Non-evidence event must not precede the current chain sequence."
        }
        return copy(sequence = sequence + EvidenceSequenceEntry.NonEvidence(event))
    }

    fun reset(
        newAnchor: ChainAnchor,
        reason: ChainResetReason
    ): EvidenceChainReset {
        require(closedBy == null) { "A closed evidence chain cannot reset again." }
        require(newAnchor.evidence.contentId == contentId) {
            "Reset anchor must belong to the chain Content."
        }
        require(newAnchor.timestamp >= sequence.last().timestamp) {
            "Reset anchor must not precede the current chain sequence."
        }
        require(newAnchor.reason == reason.toAnchorReason()) {
            "Reset and anchor reasons must describe the same boundary."
        }
        when (reason) {
            ChainResetReason.PROMOTED -> require(newAnchor.rating == stage.targetRating) {
                "Promotion reset must anchor the next promotion rating."
            }
            ChainResetReason.NEW_AGAIN,
            ChainResetReason.NEW_LAPSE -> require(newAnchor.rating == ReviewRating.AGAIN) {
                "Again or lapse reset must anchor an Again chain."
            }
            ChainResetReason.MANUAL_RESET,
            ChainResetReason.DATA_RECOVERY,
            ChainResetReason.UNDO -> Unit
        }
        return EvidenceChainReset(
            completedChain = copy(closedBy = reason),
            newChain = start(contentId, PromotionStage.fromAnchorRating(newAnchor.rating), newAnchor)
        )
    }

    companion object {
        fun start(
            contentId: ContentId,
            stage: PromotionStage,
            anchor: ChainAnchor
        ): EvidenceChain = EvidenceChain(
            contentId = contentId,
            stage = stage,
            anchor = anchor,
            sequence = listOf(EvidenceSequenceEntry.Recall(anchor.evidence))
        )
    }
}

data class EvidenceChainReset(
    val completedChain: EvidenceChain,
    val newChain: EvidenceChain
)

private fun ChainResetReason.toAnchorReason(): ChainAnchorReason =
    when (this) {
        ChainResetReason.PROMOTED -> ChainAnchorReason.PROMOTED
        ChainResetReason.NEW_AGAIN -> ChainAnchorReason.NEW_AGAIN
        ChainResetReason.NEW_LAPSE -> ChainAnchorReason.NEW_LAPSE
        ChainResetReason.MANUAL_RESET -> ChainAnchorReason.MANUAL_RESET
        ChainResetReason.DATA_RECOVERY -> ChainAnchorReason.DATA_RECOVERY
        ChainResetReason.UNDO -> ChainAnchorReason.UNDO
    }
