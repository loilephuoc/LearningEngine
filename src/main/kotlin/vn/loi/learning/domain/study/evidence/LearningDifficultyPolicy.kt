package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.TimeSpan

data class LearningDifficultyPolicy(
    val mediumConfidenceEvidence: Int = 5,
    val highConfidenceEvidence: Int = 12,
    val recentRecallWindow: Int = 6,
    val trendMinimumSample: Int = 4,
    val trendMaterialDelta: Double = 0.20,
    val recoveryRetentionThreshold: Double = 0.70,
    val veryDifficultThreshold: Double = 0.68,
    val difficultThreshold: Double = 0.48,
    val unstableThreshold: Double = 0.30,
    val stableRetentionThreshold: Double = 0.70,
    val masteredRetentionThreshold: Double = 0.85,
    val masteredPromotionCount: Int = 3,
    val freshnessHorizon: TimeSpan = TimeSpan.days(30),
    val retentionDifficultyWeight: Double = 0.35,
    val againDifficultyWeight: Double = 0.40,
    val lapseDifficultyWeight: Double = 0.25,
    val retentionStabilityWeight: Double = 0.55,
    val promotionStabilityWeight: Double = 0.25,
    val freshnessStabilityWeight: Double = 0.20,
    val incorrectForgetRiskWeight: Double = 0.65,
    val stalenessForgetRiskWeight: Double = 0.35,
    val againRelapseRiskWeight: Double = 0.60,
    val lapseRelapseRiskWeight: Double = 0.40
) {
    init {
        require(mediumConfidenceEvidence > 0 && highConfidenceEvidence > mediumConfidenceEvidence)
        require(recentRecallWindow >= trendMinimumSample && trendMinimumSample >= 2)
        require(masteredPromotionCount > 0 && freshnessHorizon.millis > 0)
        listOf(
            trendMaterialDelta, recoveryRetentionThreshold, veryDifficultThreshold,
            difficultThreshold, unstableThreshold, stableRetentionThreshold,
            masteredRetentionThreshold
        ).forEach { require(it in 0.0..1.0) }
        require(veryDifficultThreshold > difficultThreshold && difficultThreshold > unstableThreshold)
        requireWeights(retentionDifficultyWeight, againDifficultyWeight, lapseDifficultyWeight)
        requireWeights(retentionStabilityWeight, promotionStabilityWeight, freshnessStabilityWeight)
        requireWeights(incorrectForgetRiskWeight, stalenessForgetRiskWeight)
        requireWeights(againRelapseRiskWeight, lapseRelapseRiskWeight)
    }

    private fun requireWeights(vararg weights: Double) {
        require(weights.all { it in 0.0..1.0 })
        require(kotlin.math.abs(weights.sum() - 1.0) < WEIGHT_TOLERANCE)
    }

    private companion object { const val WEIGHT_TOLERANCE = 0.000_001 }
}
