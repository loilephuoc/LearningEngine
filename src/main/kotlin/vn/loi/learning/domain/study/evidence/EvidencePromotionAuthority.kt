package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy

class EvidencePromotionAuthority(
    private val clock: EvidenceClock,
    private val policy: PromotionPolicy = PromotionPolicy()
) {
    fun evaluatePromotion(candidate: PromotionCandidate): PromotionDecision {
        val target = candidate.currentRating.nextPromotionRating()
            ?: return PromotionDecision(
                eligible = false,
                currentRating = candidate.currentRating,
                targetRating = null,
                reasons = listOf(PromotionReason.ALREADY_AT_HIGHEST_LEVEL)
            )
        val window = requireNotNull(policy.windowFor(candidate.currentRating))
        val classified = classify(candidate.evidence)
        val validEvidence = classified.valid.sortedBy { it.timestamp }
        val anchor = validEvidence.singleOrNull {
            it.reviewEventId == candidate.anchorEvidenceId &&
                it.currentRating == candidate.currentRating
        }
            ?: return PromotionDecision(
                eligible = false,
                currentRating = candidate.currentRating,
                targetRating = target,
                reasons = classified.exclusions + PromotionReason.MISSING_ANCHOR_EVIDENCE,
                missingCorrectRecalls = window.minimumCorrectRecalls
            )
        val now = clock.now()
        require(now >= anchor.timestamp) {
            "Evidence clock must not precede the promotion anchor."
        }
        val elapsed = now - anchor.timestamp
        val afterAnchor = validEvidence.filter {
            it.timestamp > anchor.timestamp && it.timestamp <= now
        }
        val correctTimes = afterAnchor
            .asSequence()
            .filter { it.result == RecallResult.CORRECT }
            .map { it.timestamp }
            .distinct()
            .count()
        val missing = (window.minimumCorrectRecalls - correctTimes).coerceAtLeast(0)
        val reasons = classified.exclusions.toMutableList()
        val remaining = if (elapsed < window.minimumElapsed) {
            reasons += PromotionReason.EVIDENCE_WINDOW_NOT_ELAPSED
            TimeSpan(window.minimumElapsed.millis - elapsed.millis)
        } else {
            TimeSpan.ZERO
        }
        if (missing > 0) reasons += PromotionReason.MISSING_CORRECT_RECALLS
        val laterAgain = afterAnchor.any { it.currentRating == ReviewRating.AGAIN }
        if (candidate.currentRating == ReviewRating.HARD && laterAgain) {
            reasons += PromotionReason.INTERVENING_AGAIN
        }
        if (candidate.currentRating == ReviewRating.GOOD && laterAgain) {
            reasons += PromotionReason.LAPSE_AFTER_ANCHOR
        }
        val blockingReasons = reasons.filterNot { it.isEvidenceExclusion() }
        return if (blockingReasons.isEmpty()) {
            PromotionDecision(
                eligible = true,
                currentRating = candidate.currentRating,
                targetRating = target,
                reasons = listOf(PromotionReason.ELIGIBLE)
            )
        } else {
            PromotionDecision(
                eligible = false,
                currentRating = candidate.currentRating,
                targetRating = target,
                reasons = reasons.distinct(),
                remainingTime = remaining,
                missingCorrectRecalls = missing
            )
        }
    }

    private fun classify(evidence: List<RecallEvidence>): ClassifiedEvidence {
        val seen = mutableSetOf<ReviewEventId>()
        val valid = mutableListOf<RecallEvidence>()
        val exclusions = mutableListOf<PromotionReason>()
        evidence.sortedBy { it.timestamp }.forEach { item ->
            val exclusion = when {
                !seen.add(item.reviewEventId) -> PromotionReason.DUPLICATE_EVIDENCE_EXCLUDED
                item.sessionPolicy != SessionEvaluationPolicy.EVALUATIVE ->
                    PromotionReason.PRACTICE_EVIDENCE_EXCLUDED
                item.wasRevealUsed -> PromotionReason.REVEAL_EVIDENCE_EXCLUDED
                item.provenance == RatingSource.MANUAL_USER_OVERRIDE ->
                    PromotionReason.MANUAL_OVERRIDE_EVIDENCE_EXCLUDED
                item.provenance == RatingSource.MANUAL_USER ->
                    PromotionReason.MANUAL_RATING_EVIDENCE_EXCLUDED
                item.origin == RecallEvidenceOrigin.REPLAY -> PromotionReason.REPLAY_EVIDENCE_EXCLUDED
                item.commitStatus == RecallEvidenceCommitStatus.UNDONE ->
                    PromotionReason.UNDONE_EVIDENCE_EXCLUDED
                else -> null
            }
            if (exclusion == null) valid += item else exclusions += exclusion
        }
        return ClassifiedEvidence(valid, exclusions.distinct())
    }

    private data class ClassifiedEvidence(
        val valid: List<RecallEvidence>,
        val exclusions: List<PromotionReason>
    )
}

private fun ReviewRating.nextPromotionRating(): ReviewRating? =
    when (this) {
        ReviewRating.AGAIN -> ReviewRating.HARD
        ReviewRating.HARD -> ReviewRating.GOOD
        ReviewRating.GOOD -> ReviewRating.EASY
        ReviewRating.EASY -> null
    }

private fun PromotionReason.isEvidenceExclusion(): Boolean =
    this in setOf(
        PromotionReason.PRACTICE_EVIDENCE_EXCLUDED,
        PromotionReason.REVEAL_EVIDENCE_EXCLUDED,
        PromotionReason.MANUAL_OVERRIDE_EVIDENCE_EXCLUDED,
        PromotionReason.MANUAL_RATING_EVIDENCE_EXCLUDED,
        PromotionReason.REPLAY_EVIDENCE_EXCLUDED,
        PromotionReason.UNDONE_EVIDENCE_EXCLUDED,
        PromotionReason.DUPLICATE_EVIDENCE_EXCLUDED
    )
