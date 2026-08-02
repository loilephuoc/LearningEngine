package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

data class PromotionCandidate(
    val chain: EvidenceChain
) {
    val currentRating: ReviewRating
        get() = chain.stage.anchorRating
}

enum class PromotionReason {
    ELIGIBLE,
    ALREADY_AT_HIGHEST_LEVEL,
    MISSING_ANCHOR_EVIDENCE,
    EVIDENCE_WINDOW_NOT_ELAPSED,
    MISSING_CORRECT_RECALLS,
    PRACTICE_EVIDENCE_EXCLUDED,
    REVEAL_EVIDENCE_EXCLUDED,
    MANUAL_OVERRIDE_EVIDENCE_EXCLUDED,
    MANUAL_RATING_EVIDENCE_EXCLUDED,
    REPLAY_EVIDENCE_EXCLUDED,
    UNDONE_EVIDENCE_EXCLUDED,
    DUPLICATE_EVIDENCE_EXCLUDED,
    INTERVENING_AGAIN,
    LAPSE_AFTER_ANCHOR
}

data class PromotionDecision(
    val eligible: Boolean,
    val currentRating: ReviewRating,
    val targetRating: ReviewRating?,
    val reasons: List<PromotionReason>,
    val remainingTime: TimeSpan = TimeSpan.ZERO,
    val missingCorrectRecalls: Int = 0
)
