package vn.loi.learning.domain.study.evidence

import kotlin.math.roundToLong
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy

class LearningDifficultyProfileCalculator(
    private val clock: EvidenceClock,
    private val policy: LearningDifficultyPolicy = LearningDifficultyPolicy()
) {
    fun calculateDifficultyProfile(
        learnerId: LearnerId,
        trajectory: LearningTrajectory
    ): LearningDifficultyProfile {
        val recalls = trajectory.chains.flatMap(EvidenceChain::recallEvidence).sortedBy { it.timestamp }
        val validEvidence = recalls.filter(::isInferenceEvidence)
        val allEntries = trajectory.chains.flatMap(EvidenceChain::sequence).sortedBy { it.timestamp }
        val now = clock.now()
        val firstSeen = requireNotNull(allEntries.firstOrNull()).timestamp
        val lastSeen = requireNotNull(allEntries.lastOrNull()).timestamp
        require(now >= lastSeen) { "Difficulty profile clock must not precede trajectory evidence." }

        val lifetime = lifetime(recalls, validEvidence, firstSeen, lastSeen, trajectory)
        val confidence = confidence(validEvidence.size)
        val confidenceScore = confidenceScore(validEvidence.size)
        val currentEvidence = validEvidence.takeLast(policy.recentRecallWindow)
        val retention = ratio(currentEvidence.count { it.result == RecallResult.CORRECT }, currentEvidence.size)
        val againRate = ratio(recalls.count { it.currentRating == ReviewRating.AGAIN }, recalls.size)
        val lapseRate = ratio(lifetime.totalLapse, trajectory.chains.size)
        val difficulty = bounded(
            (1.0 - retention) * policy.retentionDifficultyWeight +
                againRate * policy.againDifficultyWeight + lapseRate * policy.lapseDifficultyWeight
        )
        val trend = trend(validEvidence, trajectory, retention)
        val freshness = freshness(now - lastSeen)
        val promotionRatio = ratio(lifetime.totalPromotion, policy.masteredPromotionCount)
        val stability = bounded(
            retention * policy.retentionStabilityWeight +
                promotionRatio * policy.promotionStabilityWeight + freshness * policy.freshnessStabilityWeight
        )
        val analytics = promotionAnalytics(trajectory)
        val relapseRisk = bounded(
            againRate * policy.againRelapseRiskWeight + lapseRate * policy.lapseRelapseRiskWeight
        )
        val forgetRisk = bounded(
            (1.0 - retention) * policy.incorrectForgetRiskWeight +
                (1.0 - freshness) * policy.stalenessForgetRiskWeight
        )
        val readiness = bounded(retention * confidenceScore * (1.0 - relapseRisk))
        val level = level(difficulty, retention, lifetime.totalPromotion, confidence)

        return LearningDifficultyProfile(
            learnerId, trajectory.contentId, now, lifetime,
            CurrentStability(
                RetentionScore(retention), StabilityScore(stability),
                EvidenceConfidenceScore(confidenceScore), DifficultyScore(difficulty), trend
            ),
            analytics,
            RiskProfile(RiskScore(forgetRisk), RiskScore(relapseRisk), ReadinessScore(readiness)),
            confidence, trend, level
        )
    }

    private fun lifetime(
        recalls: List<RecallEvidence>,
        validEvidence: List<RecallEvidence>,
        firstSeen: vn.loi.learning.domain.study.memory.model.Moment,
        lastSeen: vn.loi.learning.domain.study.memory.model.Moment,
        trajectory: LearningTrajectory
    ) = LifetimeStatistics(
        firstSeen, lastSeen, recalls.size,
        recalls.count { it.currentRating == ReviewRating.AGAIN },
        recalls.count { it.currentRating == ReviewRating.HARD },
        recalls.count { it.currentRating == ReviewRating.GOOD },
        recalls.count { it.currentRating == ReviewRating.EASY },
        trajectory.chains.count { it.closedBy == ChainResetReason.PROMOTED },
        trajectory.chains.count { it.closedBy in setOf(ChainResetReason.NEW_AGAIN, ChainResetReason.NEW_LAPSE) },
        validEvidence.size
    )

    private fun promotionAnalytics(trajectory: LearningTrajectory): PromotionAnalytics {
        val promoted = trajectory.chains.mapIndexedNotNull { index, chain ->
            if (chain.closedBy != ChainResetReason.PROMOTED) null else {
                val nextAnchor = trajectory.chains.getOrNull(index + 1)?.anchor ?: return@mapIndexedNotNull null
                PromotionSample(chain.stage, nextAnchor.timestamp - chain.anchor.timestamp, chain.recallEvidence.size)
            }
        }
        val byStage = promoted.groupBy(PromotionSample::stage).mapValues { (_, samples) ->
            StagePromotionAnalytics(
                samples.size,
                averageDuration(samples.map(PromotionSample::duration)),
                AverageCount(samples.map(PromotionSample::recallCount).average())
            )
        }.toSortedMap(compareBy(PromotionStage::ordinal))
        return PromotionAnalytics(
            averageDuration(promoted.map(PromotionSample::duration)),
            AverageCount(trajectory.chains.map { it.recallEvidence.size }.average()),
            AverageCount(promoted.map(PromotionSample::recallCount).averageOrZero()),
            byStage
        )
    }

    private fun trend(
        evidence: List<RecallEvidence>,
        trajectory: LearningTrajectory,
        currentRetention: Double
    ): DifficultyTrend {
        val latestClosed = trajectory.chains.dropLast(1).lastOrNull()?.closedBy
        if (latestClosed in setOf(ChainResetReason.NEW_AGAIN, ChainResetReason.NEW_LAPSE) &&
            currentRetention >= policy.recoveryRetentionThreshold
        ) return DifficultyTrend.RECOVERING
        val recent = evidence.takeLast(policy.recentRecallWindow)
        if (recent.size < policy.trendMinimumSample) return DifficultyTrend.PLATEAU
        val midpoint = recent.size / 2
        val earlier = ratio(recent.take(midpoint).count { it.result == RecallResult.CORRECT }, midpoint)
        val laterItems = recent.drop(midpoint)
        val later = ratio(laterItems.count { it.result == RecallResult.CORRECT }, laterItems.size)
        val delta = later - earlier
        return when {
            delta >= policy.trendMaterialDelta -> DifficultyTrend.IMPROVING
            delta <= -policy.trendMaterialDelta && latestClosed in
                setOf(ChainResetReason.NEW_AGAIN, ChainResetReason.NEW_LAPSE) -> DifficultyTrend.REGRESSING
            delta <= -policy.trendMaterialDelta -> DifficultyTrend.DECLINING
            else -> DifficultyTrend.PLATEAU
        }
    }

    private fun level(
        difficulty: Double,
        retention: Double,
        promotions: Int,
        confidence: DifficultyConfidence
    ): DifficultyLevel = when {
        difficulty >= policy.veryDifficultThreshold -> DifficultyLevel.VERY_DIFFICULT
        difficulty >= policy.difficultThreshold -> DifficultyLevel.DIFFICULT
        difficulty >= policy.unstableThreshold -> DifficultyLevel.UNSTABLE
        retention >= policy.masteredRetentionThreshold &&
            promotions >= policy.masteredPromotionCount && confidence == DifficultyConfidence.HIGH ->
            DifficultyLevel.MASTERED
        retention >= policy.stableRetentionThreshold -> DifficultyLevel.STABLE
        else -> DifficultyLevel.UNSTABLE
    }

    private fun confidence(evidenceCount: Int): DifficultyConfidence = when {
        evidenceCount >= policy.highConfidenceEvidence -> DifficultyConfidence.HIGH
        evidenceCount >= policy.mediumConfidenceEvidence -> DifficultyConfidence.MEDIUM
        else -> DifficultyConfidence.LOW
    }

    private fun confidenceScore(evidenceCount: Int): Double =
        bounded(evidenceCount.toDouble() / policy.highConfidenceEvidence)

    private fun freshness(age: TimeSpan): Double =
        bounded(1.0 - age.millis.toDouble() / policy.freshnessHorizon.millis)

    private fun averageDuration(values: List<TimeSpan>): TimeSpan? = values.takeIf { it.isNotEmpty() }
        ?.let { durations -> TimeSpan(durations.map { it.millis }.average().roundToLong()) }

    private fun isInferenceEvidence(evidence: RecallEvidence): Boolean =
        evidence.sessionPolicy == SessionEvaluationPolicy.EVALUATIVE &&
            evidence.provenance == RatingSource.STANDARD_REVIEW && !evidence.wasRevealUsed &&
            evidence.origin == RecallEvidenceOrigin.EVALUATIVE_RECALL &&
            evidence.commitStatus == RecallEvidenceCommitStatus.COMMITTED

    private fun ratio(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else bounded(numerator.toDouble() / denominator)

    private fun bounded(value: Double): Double = value.coerceIn(0.0, 1.0)

    private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private data class PromotionSample(
        val stage: PromotionStage,
        val duration: TimeSpan,
        val recallCount: Int
    )
}
