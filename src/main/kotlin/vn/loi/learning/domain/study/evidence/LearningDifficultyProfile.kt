package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

@JvmInline value class RetentionScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class StabilityScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class EvidenceConfidenceScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class DifficultyScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class RiskScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class ReadinessScore(val value: Double) { init { require(value in 0.0..1.0) } }
@JvmInline value class AverageCount(val value: Double) { init { require(value >= 0.0) } }

enum class DifficultyLevel { VERY_DIFFICULT, DIFFICULT, UNSTABLE, STABLE, MASTERED }
enum class DifficultyTrend { IMPROVING, RECOVERING, PLATEAU, DECLINING, REGRESSING }
enum class DifficultyConfidence { LOW, MEDIUM, HIGH }

data class LifetimeStatistics(
    val firstSeen: Moment,
    val lastSeen: Moment,
    val totalRecall: Int,
    val totalAgain: Int,
    val totalHard: Int,
    val totalGood: Int,
    val totalEasy: Int,
    val totalPromotion: Int,
    val totalLapse: Int,
    /** Recall entries eligible for engine inference after provenance/reveal/replay/undo filtering. */
    val totalEvidence: Int
)

data class CurrentStability(
    val retentionScore: RetentionScore,
    val stabilityScore: StabilityScore,
    val evidenceConfidence: EvidenceConfidenceScore,
    val currentDifficulty: DifficultyScore,
    val currentTrend: DifficultyTrend
)

data class StagePromotionAnalytics(
    val promotionCount: Int,
    val averageDuration: TimeSpan?,
    val averageRecallCount: AverageCount
)

data class PromotionAnalytics(
    val averagePromotionDuration: TimeSpan?,
    val averageChainLength: AverageCount,
    val averageRecallBetweenPromotions: AverageCount,
    val byStage: Map<PromotionStage, StagePromotionAnalytics>
)

data class RiskProfile(
    val forgetRisk: RiskScore,
    val relapseRisk: RiskScore,
    val promotionReadiness: ReadinessScore
)

data class LearningDifficultyProfile(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val calculatedAt: Moment,
    val lifetime: LifetimeStatistics,
    val current: CurrentStability,
    val promotionAnalytics: PromotionAnalytics,
    val risk: RiskProfile,
    val confidence: DifficultyConfidence,
    val trend: DifficultyTrend,
    val level: DifficultyLevel
)
