package vn.loi.learning.domain.study.recall

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningDifficultyProfile
import vn.loi.learning.domain.study.evidence.LearningRecommendation
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

enum class RecallModeStrength(override val wireId: String) : StableWireValue {
    STRONG_RECALL("strong-recall"), STANDARD_RECALL("standard-recall"),
    RECOGNITION("recognition"), ASSISTED_CONTEXT("assisted-context"), UNSUPPORTED("unsupported")
}

enum class RecallStrategyContext(override val wireId: String) : StableWireValue {
    EVALUATIVE("evaluative"), PRACTICE_ONLY("practice-only")
}

enum class RecallStrategyReason(override val wireId: String) : StableWireValue {
    CAPABILITY_AVAILABLE("capability-available"), CAPABILITY_UNAVAILABLE("capability-unavailable"),
    VERY_DIFFICULT_CONTENT("very-difficult-content"), UNSTABLE_TRAJECTORY("unstable-trajectory"),
    RECOVERY_NEEDED("recovery-needed"), NEEDS_MORE_EVIDENCE("needs-more-evidence"),
    READY_FOR_PROMOTION("ready-for-promotion"), MASTERED_OR_STABLE("mastered-or-stable"),
    LOW_ENGINE_CONFIDENCE("low-engine-confidence"), RECENT_LAPSE("recent-lapse"),
    RECENT_MODE_REPETITION("recent-mode-repetition"), MODE_DIVERSIFICATION("mode-diversification"),
    STRONG_RECALL_REQUIRED("strong-recall-required"), RECOGNITION_CHECK_APPROPRIATE("recognition-check-appropriate"),
    AUDIO_SKILL_REINFORCEMENT("audio-skill-reinforcement"), IMAGE_ASSOCIATION_REINFORCEMENT("image-association-reinforcement"),
    CONTEXT_RECALL_REINFORCEMENT("context-recall-reinforcement"), SAFE_FALLBACK("safe-fallback"),
    POLICY_DISABLED("policy-disabled"), MISSING_REQUIRED_CONTEXT("missing-required-context")
}

enum class RecallStrategyConfidence(override val wireId: String) : StableWireValue {
    LOW("low"), MEDIUM("medium"), HIGH("high")
}

enum class RecallStrategyViolation(override val wireId: String) : StableWireValue {
    IDENTITY_MISMATCH("identity-mismatch"), INVALID_HISTORY("invalid-history"),
    INVALID_POLICY("invalid-policy"), UNSUPPORTED_PROJECTION_VERSION("unsupported-projection-version")
}

data class RecallModeHistoryEntry(
    val mode: RecallMode,
    val direction: RecallDirection,
    val outcome: RecallOutcome? = null,
    val usedAt: Moment,
    val assistanceUsed: Boolean = false
)

data class RecallModeHistory(val entries: List<RecallModeHistoryEntry>, val windowSize: Int) {
    init { require(windowSize > 0) }
    val boundedEntries: List<RecallModeHistoryEntry> get() = entries.takeLast(windowSize)
    val consecutiveSameMode: Int get() {
        val last = boundedEntries.lastOrNull()?.mode ?: return 0
        return boundedEntries.asReversed().takeWhile { it.mode == last }.size
    }
    companion object { fun empty(windowSize: Int = 8) = RecallModeHistory(emptyList(), windowSize) }
}

data class RecallPromotionEvidenceContext(
    val strategyContext: RecallStrategyContext,
    val needsMoreEvidence: Boolean = false,
    val promotionReady: Boolean = false,
    val recentLapse: Boolean = false,
    val evidenceConfidenceLow: Boolean = false
)

data class RecallModeDiversityPolicy(
    val maxConsecutiveSameMode: Int = 2,
    val recentWindowSize: Int = 8,
    val diversificationPenalty: Int = 35
) { init { require(maxConsecutiveSameMode > 0); require(recentWindowSize > 0); require(diversificationPenalty >= 0) } }

data class AdaptiveRecallStrategyPolicy(
    val version: String = "adaptive-recall-v1",
    val decisionVersion: RecallContractVersion = RecallContractVersion.CURRENT,
    val allowRecognitionInEvaluative: Boolean = true,
    val diversity: RecallModeDiversityPolicy = RecallModeDiversityPolicy(),
    val baseScores: Map<RecallModeStrength, Int> = mapOf(
        RecallModeStrength.STRONG_RECALL to 100,
        RecallModeStrength.STANDARD_RECALL to 75,
        RecallModeStrength.ASSISTED_CONTEXT to 55,
        RecallModeStrength.RECOGNITION to 35,
        RecallModeStrength.UNSUPPORTED to -1000
    ),
    val strongRecallBonus: Int = 50,
    val stableDiversityBonus: Int = 45,
    val practiceScaffoldBonus: Int = 30,
    val masteredEfficiencyBonus: Int = 80
) {
    init {
        require(version.isNotBlank())
        require(baseScores.keys == RecallModeStrength.entries.toSet())
        require(listOf(strongRecallBonus, stableDiversityBonus, practiceScaffoldBonus, masteredEfficiencyBonus).all { it >= 0 })
    }
}

data class RecallStrategyRequest(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val capabilityProjection: RecallCapabilityProjection,
    val difficultyProfile: LearningDifficultyProfile?,
    val learningRecommendation: LearningRecommendation?,
    val promotionEvidenceContext: RecallPromotionEvidenceContext,
    val recentModeHistory: RecallModeHistory,
    val strategyPolicy: AdaptiveRecallStrategyPolicy,
    val deterministicSeed: RecallDeterministicSeed,
    val generatedAt: Moment
)

data class RecallStrategyCandidate(
    val mode: RecallMode,
    val direction: RecallDirection,
    val strength: RecallModeStrength,
    val reasons: List<RecallStrategyReason>
)

data class RejectedRecallStrategyCandidate(
    val mode: RecallMode,
    val reasons: List<RecallStrategyReason>
)

data class RecallStrategyDecision(
    val selectedMode: RecallMode,
    val selectedDirection: RecallDirection,
    val confidence: RecallStrategyConfidence,
    val primaryReason: RecallStrategyReason,
    val secondaryReasons: List<RecallStrategyReason>,
    val fallbackCandidates: List<RecallStrategyCandidate>,
    val rejectedCandidates: List<RejectedRecallStrategyCandidate>,
    val deterministicSeed: RecallDeterministicSeed,
    val policyVersion: String,
    val decisionVersion: RecallContractVersion,
    val contentId: ContentId,
    val learnerId: LearnerId,
    val generatedAt: Moment
)

sealed interface RecallStrategyDecisionResult {
    data class Selected(val decision: RecallStrategyDecision) : RecallStrategyDecisionResult
    data class NoEligibleMode(val reasons: List<RecallStrategyReason>) : RecallStrategyDecisionResult
    data class InvalidRequest(val violations: List<RecallStrategyViolation>) : RecallStrategyDecisionResult
    data class UnsupportedVersion(val actual: RecallContractVersion) : RecallStrategyDecisionResult
}
