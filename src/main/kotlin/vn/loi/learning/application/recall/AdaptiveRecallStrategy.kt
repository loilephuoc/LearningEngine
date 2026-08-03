package vn.loi.learning.application.recall

import vn.loi.learning.domain.study.evidence.DifficultyConfidence
import vn.loi.learning.domain.study.evidence.DifficultyLevel
import vn.loi.learning.domain.study.evidence.RecommendationAction
import vn.loi.learning.domain.study.recall.*

class AdaptiveRecallStrategy {
    fun select(request: RecallStrategyRequest): RecallStrategyDecisionResult {
        if (request.capabilityProjection.version != RecallContractVersion.CURRENT) {
            return RecallStrategyDecisionResult.UnsupportedVersion(request.capabilityProjection.version)
        }
        val violations = validate(request)
        if (violations.isNotEmpty()) return RecallStrategyDecisionResult.InvalidRequest(violations)
        if (request.capabilityProjection.capabilitySet.isEmpty) {
            return RecallStrategyDecisionResult.NoEligibleMode(listOf(RecallStrategyReason.CAPABILITY_UNAVAILABLE))
        }

        val eligible = request.capabilityProjection.capabilitySet.orderedModes.mapNotNull { mode ->
            val direction = preferredDirection(mode, request.capabilityProjection.supportedDirectionsFor(mode)) ?: return@mapNotNull null
            Ranked(candidate(mode, direction), score(mode, request))
        }.sortedWith(compareByDescending<Ranked> { it.score }.thenBy { seededKey(it.candidate, request.deterministicSeed) }
            .thenBy { it.candidate.mode.wireId }.thenBy { it.candidate.direction.wireId })

        if (eligible.isEmpty()) return RecallStrategyDecisionResult.NoEligibleMode(listOf(RecallStrategyReason.CAPABILITY_UNAVAILABLE))
        val chosen = eligible.first().candidate
        val contextualReasons = decisionReasons(request, chosen)
        val rejected = RecallMode.entries.filterNot(request.capabilityProjection::supports).sortedBy { it.wireId }.map {
            RejectedRecallStrategyCandidate(it, listOf(RecallStrategyReason.CAPABILITY_UNAVAILABLE))
        }
        return RecallStrategyDecisionResult.Selected(
            RecallStrategyDecision(
                chosen.mode, chosen.direction, confidence(request), contextualReasons.first(), contextualReasons.drop(1),
                eligible.drop(1).map(Ranked::candidate), rejected, request.deterministicSeed,
                request.strategyPolicy.version, request.strategyPolicy.decisionVersion, request.contentId,
                request.learnerId, request.generatedAt
            )
        )
    }

    private fun validate(r: RecallStrategyRequest): List<RecallStrategyViolation> = buildList {
        if (r.capabilityProjection.contentId != r.contentId ||
            r.difficultyProfile?.let { it.learnerId != r.learnerId || it.contentId != r.contentId } == true ||
            r.learningRecommendation?.let { it.learnerId != r.learnerId || it.contentId != r.contentId } == true) {
            add(RecallStrategyViolation.IDENTITY_MISMATCH)
        }
        if (r.recentModeHistory.entries.size > r.recentModeHistory.windowSize ||
            r.recentModeHistory.entries.zipWithNext().any { (a, b) -> a.usedAt > b.usedAt }) add(RecallStrategyViolation.INVALID_HISTORY)
    }.distinct().sortedBy { it.wireId }

    private fun score(mode: RecallMode, r: RecallStrategyRequest): Int {
        val strength = strength(mode)
        var score = requireNotNull(r.strategyPolicy.baseScores[strength])
        val action = r.learningRecommendation?.action
        val level = r.difficultyProfile?.level
        val lowConfidence = r.difficultyProfile == null || r.learningRecommendation == null ||
            r.difficultyProfile.confidence == DifficultyConfidence.LOW || r.promotionEvidenceContext.evidenceConfidenceLow
        val strongRequired = action in setOf(RecommendationAction.FOCUS_PRACTICE, RecommendationAction.NEEDS_MORE_EVIDENCE,
            RecommendationAction.READY_FOR_PROMOTION, RecommendationAction.RECOVERY_NEEDED) ||
            level in setOf(DifficultyLevel.VERY_DIFFICULT, DifficultyLevel.DIFFICULT, DifficultyLevel.UNSTABLE) ||
            r.promotionEvidenceContext.needsMoreEvidence || r.promotionEvidenceContext.promotionReady || r.promotionEvidenceContext.recentLapse
        if (strongRequired && strength == RecallModeStrength.STRONG_RECALL) score += r.strategyPolicy.strongRecallBonus
        if (strongRequired && strength == RecallModeStrength.RECOGNITION) score -= r.strategyPolicy.strongRecallBonus
        if (lowConfidence) score += safeFallbackRank(mode)
        if (r.promotionEvidenceContext.strategyContext == RecallStrategyContext.PRACTICE_ONLY &&
            strength in setOf(RecallModeStrength.STANDARD_RECALL, RecallModeStrength.ASSISTED_CONTEXT)) score += r.strategyPolicy.practiceScaffoldBonus
        if (action in setOf(RecommendationAction.MONITOR_ONLY, RecommendationAction.MASTERED) || level == DifficultyLevel.MASTERED) {
            if (mode in setOf(RecallMode.MULTIPLE_CHOICE, RecallMode.IMAGE_RECALL, RecallMode.LISTENING)) score += r.strategyPolicy.masteredEfficiencyBonus
        }
        val history = r.recentModeHistory
        val exempt = strongRequired
        if (!exempt && history.consecutiveSameMode >= r.strategyPolicy.diversity.maxConsecutiveSameMode &&
            history.boundedEntries.lastOrNull()?.mode == mode) score -= r.strategyPolicy.diversity.diversificationPenalty
        if (!exempt && (level in setOf(DifficultyLevel.STABLE, DifficultyLevel.MASTERED) || action == RecommendationAction.MONITOR_ONLY) &&
            history.boundedEntries.lastOrNull()?.mode != mode) score += r.strategyPolicy.stableDiversityBonus
        if (mode == RecallMode.MULTIPLE_CHOICE && r.promotionEvidenceContext.strategyContext == RecallStrategyContext.EVALUATIVE &&
            !r.strategyPolicy.allowRecognitionInEvaluative) score = Int.MIN_VALUE / 2
        return score
    }

    private fun safeFallbackRank(mode: RecallMode): Int = when (mode) {
        RecallMode.TYPING -> 40; RecallMode.REVERSE_TRANSLATION -> 30
        RecallMode.IMAGE_RECALL, RecallMode.LISTENING, RecallMode.DICTATION -> 20
        RecallMode.EXAMPLE_COMPLETION -> 10; RecallMode.MULTIPLE_CHOICE -> 0
    }

    private fun strength(mode: RecallMode): RecallModeStrength = when (mode) {
        RecallMode.TYPING, RecallMode.DICTATION, RecallMode.LISTENING -> RecallModeStrength.STRONG_RECALL
        RecallMode.REVERSE_TRANSLATION, RecallMode.IMAGE_RECALL -> RecallModeStrength.STANDARD_RECALL
        RecallMode.EXAMPLE_COMPLETION -> RecallModeStrength.ASSISTED_CONTEXT
        RecallMode.MULTIPLE_CHOICE -> RecallModeStrength.RECOGNITION
    }

    private fun preferredDirection(mode: RecallMode, directions: List<RecallDirection>): RecallDirection? {
        val preferred = when (mode) {
            RecallMode.TYPING, RecallMode.MULTIPLE_CHOICE -> RecallDirection.SOURCE_TO_TARGET
            RecallMode.REVERSE_TRANSLATION -> RecallDirection.TARGET_TO_SOURCE
            RecallMode.LISTENING, RecallMode.DICTATION -> RecallDirection.AUDIO_TO_TEXT
            RecallMode.IMAGE_RECALL -> RecallDirection.IMAGE_TO_TEXT
            RecallMode.EXAMPLE_COMPLETION -> RecallDirection.CONTEXT_TO_TEXT
        }
        return preferred.takeIf { it in directions } ?: directions.firstOrNull()
    }

    private fun candidate(mode: RecallMode, direction: RecallDirection) = RecallStrategyCandidate(
        mode, direction, strength(mode), listOf(RecallStrategyReason.CAPABILITY_AVAILABLE)
    )

    private fun decisionReasons(r: RecallStrategyRequest, selected: RecallStrategyCandidate): List<RecallStrategyReason> = buildList {
        val action = r.learningRecommendation?.action
        when {
            r.difficultyProfile == null || r.learningRecommendation == null || r.difficultyProfile.confidence == DifficultyConfidence.LOW -> add(RecallStrategyReason.SAFE_FALLBACK)
            action == RecommendationAction.RECOVERY_NEEDED -> add(RecallStrategyReason.RECOVERY_NEEDED)
            action == RecommendationAction.NEEDS_MORE_EVIDENCE -> add(RecallStrategyReason.NEEDS_MORE_EVIDENCE)
            action == RecommendationAction.READY_FOR_PROMOTION -> add(RecallStrategyReason.READY_FOR_PROMOTION)
            r.difficultyProfile.level == DifficultyLevel.VERY_DIFFICULT -> add(RecallStrategyReason.VERY_DIFFICULT_CONTENT)
            r.difficultyProfile.level == DifficultyLevel.UNSTABLE -> add(RecallStrategyReason.UNSTABLE_TRAJECTORY)
            action in setOf(RecommendationAction.MONITOR_ONLY, RecommendationAction.MASTERED) || r.difficultyProfile.level in setOf(DifficultyLevel.STABLE, DifficultyLevel.MASTERED) -> add(RecallStrategyReason.MASTERED_OR_STABLE)
            else -> add(RecallStrategyReason.CAPABILITY_AVAILABLE)
        }
        if (r.promotionEvidenceContext.recentLapse) add(RecallStrategyReason.RECENT_LAPSE)
        if (selected.strength == RecallModeStrength.STRONG_RECALL) add(RecallStrategyReason.STRONG_RECALL_REQUIRED)
        if (r.recentModeHistory.consecutiveSameMode >= r.strategyPolicy.diversity.maxConsecutiveSameMode) add(RecallStrategyReason.MODE_DIVERSIFICATION)
    }.distinct()

    private fun confidence(r: RecallStrategyRequest) = when {
        r.difficultyProfile == null || r.learningRecommendation == null -> RecallStrategyConfidence.LOW
        r.difficultyProfile.confidence == DifficultyConfidence.HIGH -> RecallStrategyConfidence.HIGH
        r.difficultyProfile.confidence == DifficultyConfidence.MEDIUM -> RecallStrategyConfidence.MEDIUM
        else -> RecallStrategyConfidence.LOW
    }

    private fun seededKey(candidate: RecallStrategyCandidate, seed: RecallDeterministicSeed): Long {
        var hash = seed.value xor -7046029254386353131L
        (candidate.mode.wireId + ":" + candidate.direction.wireId).forEach { hash = (hash xor it.code.toLong()) * 1099511628211L }
        return hash
    }

    private data class Ranked(val candidate: RecallStrategyCandidate, val score: Int)
}
