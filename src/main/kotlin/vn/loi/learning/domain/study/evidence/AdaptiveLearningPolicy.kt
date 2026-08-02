package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.TimeSpan

data class AdaptiveLearningPolicy(
    val highForgetRisk: Double = 0.65,
    val highRelapseRisk: Double = 0.60,
    val promotionReadiness: Double = 0.70,
    val excessiveLapseCount: Int = 2,
    val fastPromotionDuration: TimeSpan = TimeSpan.hours(24),
    val longMasteredPromotionCount: Int = 3
) {
    init {
        listOf(highForgetRisk, highRelapseRisk, promotionReadiness).forEach {
            require(it in 0.0..1.0)
        }
        require(excessiveLapseCount > 0)
        require(fastPromotionDuration.millis > 0)
        require(longMasteredPromotionCount > 0)
    }
}
