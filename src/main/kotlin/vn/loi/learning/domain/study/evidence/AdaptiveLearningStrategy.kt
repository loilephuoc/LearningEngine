package vn.loi.learning.domain.study.evidence

class AdaptiveLearningStrategy(
    private val policy: AdaptiveLearningPolicy = AdaptiveLearningPolicy()
) {
    fun calculateRecommendation(profile: LearningDifficultyProfile): LearningRecommendation {
        val action = action(profile)
        val reasons = reasons(profile, action).distinct().sortedBy(RecommendationReason::ordinal)
        return LearningRecommendation(
            learnerId = profile.learnerId,
            contentId = profile.contentId,
            profileCalculatedAt = profile.calculatedAt,
            action = action,
            category = category(action),
            priority = priority(profile, action),
            confidence = confidence(profile.confidence),
            reasons = reasons
        )
    }

    private fun action(profile: LearningDifficultyProfile): RecommendationAction = when {
        profile.trend in setOf(DifficultyTrend.RECOVERING, DifficultyTrend.REGRESSING) ->
            RecommendationAction.RECOVERY_NEEDED
        profile.level == DifficultyLevel.VERY_DIFFICULT -> RecommendationAction.FOCUS_PRACTICE
        profile.level == DifficultyLevel.DIFFICULT -> RecommendationAction.FOCUS_PRACTICE
        profile.level == DifficultyLevel.MASTERED -> RecommendationAction.MONITOR_ONLY
        profile.level == DifficultyLevel.UNSTABLE -> RecommendationAction.NEEDS_MORE_EVIDENCE
        profile.level == DifficultyLevel.STABLE &&
            profile.risk.promotionReadiness.value >= policy.promotionReadiness &&
            profile.confidence == DifficultyConfidence.HIGH -> RecommendationAction.READY_FOR_PROMOTION
        else -> RecommendationAction.NORMAL_REVIEW
    }

    private fun priority(
        profile: LearningDifficultyProfile,
        action: RecommendationAction
    ): RecommendationPriority = when {
        profile.trend == DifficultyTrend.REGRESSING &&
            profile.risk.relapseRisk.value >= policy.highRelapseRisk -> RecommendationPriority.CRITICAL
        action in setOf(RecommendationAction.FOCUS_PRACTICE, RecommendationAction.RECOVERY_NEEDED) ->
            RecommendationPriority.HIGH
        action == RecommendationAction.MONITOR_ONLY -> RecommendationPriority.LOW
        else -> RecommendationPriority.NORMAL
    }

    private fun reasons(
        profile: LearningDifficultyProfile,
        action: RecommendationAction
    ): List<RecommendationReason> = buildList {
        when (profile.level) {
            DifficultyLevel.VERY_DIFFICULT -> add(RecommendationReason.VERY_DIFFICULT_TRAJECTORY)
            DifficultyLevel.DIFFICULT -> add(RecommendationReason.DIFFICULT_TRAJECTORY)
            DifficultyLevel.UNSTABLE -> add(RecommendationReason.UNSTABLE_RETENTION)
            DifficultyLevel.STABLE -> add(RecommendationReason.STABLE_RETENTION)
            DifficultyLevel.MASTERED -> add(RecommendationReason.MASTERED_TRAJECTORY)
        }
        when (profile.trend) {
            DifficultyTrend.RECOVERING -> add(RecommendationReason.RECOVERING_TRAJECTORY)
            DifficultyTrend.DECLINING -> add(RecommendationReason.DECLINING_TRAJECTORY)
            DifficultyTrend.REGRESSING -> add(RecommendationReason.REGRESSING_TRAJECTORY)
            DifficultyTrend.IMPROVING,
            DifficultyTrend.PLATEAU -> Unit
        }
        when (profile.confidence) {
            DifficultyConfidence.LOW -> {
                add(RecommendationReason.INSUFFICIENT_EVIDENCE)
                add(RecommendationReason.LOW_ENGINE_CONFIDENCE)
            }
            DifficultyConfidence.HIGH -> add(RecommendationReason.HIGH_ENGINE_CONFIDENCE)
            DifficultyConfidence.MEDIUM -> Unit
        }
        if (profile.risk.forgetRisk.value >= policy.highForgetRisk) add(RecommendationReason.HIGH_FORGET_RISK)
        if (profile.risk.relapseRisk.value >= policy.highRelapseRisk) add(RecommendationReason.HIGH_RELAPSE_RISK)
        if (profile.lifetime.totalLapse >= policy.excessiveLapseCount) add(RecommendationReason.TOO_MANY_LAPSES)
        if (profile.promotionAnalytics.averagePromotionDuration?.let {
                it.millis < policy.fastPromotionDuration.millis
            } == true
        ) add(RecommendationReason.PROMOTION_TOO_FAST)
        if (action == RecommendationAction.READY_FOR_PROMOTION) add(RecommendationReason.PROMOTION_READY)
        if (profile.level == DifficultyLevel.MASTERED &&
            profile.lifetime.totalPromotion >= policy.longMasteredPromotionCount
        ) add(RecommendationReason.LONG_MASTERED_HISTORY)
    }

    private fun category(action: RecommendationAction): RecommendationCategory = when (action) {
        RecommendationAction.FOCUS_PRACTICE -> RecommendationCategory.INTERVENTION
        RecommendationAction.RECOVERY_NEEDED -> RecommendationCategory.RECOVERY
        RecommendationAction.NEEDS_MORE_EVIDENCE -> RecommendationCategory.EVIDENCE_BUILDING
        RecommendationAction.READY_FOR_PROMOTION -> RecommendationCategory.PROGRESSION
        RecommendationAction.NORMAL_REVIEW,
        RecommendationAction.MONITOR_ONLY,
        RecommendationAction.MASTERED -> RecommendationCategory.MAINTENANCE
    }

    private fun confidence(confidence: DifficultyConfidence): RecommendationConfidence = when (confidence) {
        DifficultyConfidence.LOW -> RecommendationConfidence.LOW
        DifficultyConfidence.MEDIUM -> RecommendationConfidence.MEDIUM
        DifficultyConfidence.HIGH -> RecommendationConfidence.HIGH
    }
}
