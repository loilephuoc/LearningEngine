package vn.loi.learning.application.learninginsight

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class LearningInsightProjector(
    private val policy: LearningInsightPresentationPolicy = LearningInsightPresentationPolicy()
) {
    fun project(
        learnerId: LearnerId,
        contentId: ContentId,
        generatedAt: Moment,
        profile: LearningDifficultyProfile?,
        recommendation: LearningRecommendation?,
        promotion: PromotionDecision? = null,
        context: LearningInsightContext = LearningInsightContext()
    ): LearningInsightBundle {
        require(profile == null || profile.learnerId == learnerId && profile.contentId == contentId)
        require(recommendation == null ||
            recommendation.learnerId == learnerId && recommendation.contentId == contentId)

        val candidates = buildList {
            if (context.mode == LearningInsightMode.PRACTICE_ONLY) {
                add(candidate(-1, base(
                    learnerId, contentId, generatedAt, profile, recommendation,
                    LearningInsightCategory.LEARNING_CONTEXT, LearningInsightSeverity.INFO,
                    LearningInsightTitle.PRACTICE_DOES_NOT_CHANGE_RATING,
                    LearningInsightSummary.PRACTICE_IS_NOT_PROMOTION_EVIDENCE
                )))
            } else if (promotion != null) {
                add(promotionCandidate(learnerId, contentId, generatedAt, profile, recommendation, promotion))
            }

            if (profile != null && recommendation != null) {
                val confident = profile.confidence != DifficultyConfidence.LOW
                if (confident && profile.trend in setOf(DifficultyTrend.RECOVERING, DifficultyTrend.REGRESSING)) {
                    add(candidate(0, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.RECOVERY,
                        if (profile.trend == DifficultyTrend.REGRESSING) LearningInsightSeverity.IMPORTANT
                        else LearningInsightSeverity.CAUTION,
                        LearningInsightTitle.RECOVERY_NEEDED,
                        if (profile.trend == DifficultyTrend.REGRESSING)
                            LearningInsightSummary.TRAJECTORY_REGRESSING
                        else LearningInsightSummary.TRAJECTORY_RECOVERING
                    )))
                }
                if (confident && profile.level in setOf(DifficultyLevel.VERY_DIFFICULT, DifficultyLevel.DIFFICULT)) {
                    add(candidate(2, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.DIFFICULTY,
                        LearningInsightSeverity.IMPORTANT, LearningInsightTitle.DIFFICULT_CONTENT,
                        LearningInsightSummary.VERY_DIFFICULT_HIGH_RISK
                    )))
                }
                if (confident && recommendation.reasons.any {
                        it in setOf(RecommendationReason.HIGH_FORGET_RISK, RecommendationReason.HIGH_RELAPSE_RISK)
                    }) {
                    add(candidate(2, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.RETENTION_RISK,
                        LearningInsightSeverity.CAUTION, LearningInsightTitle.RETENTION_AT_RISK,
                        LearningInsightSummary.RETENTION_RISK_HIGH
                    )))
                }
                if (confident) add(candidate(recommendationRank(recommendation), profileInsight(
                    profile, recommendation, generatedAt, LearningInsightCategory.RECOMMENDATION,
                    recommendationSeverity(recommendation), recommendationTitle(recommendation),
                    LearningInsightSummary.FOLLOW_RECOMMENDATION,
                    includeRecommendation = true
                )))
                if (confident && profile.level == DifficultyLevel.MASTERED) {
                    add(candidate(5, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.MASTERY,
                        LearningInsightSeverity.POSITIVE, LearningInsightTitle.MASTERY_REACHED,
                        LearningInsightSummary.MASTERED_MONITOR_ONLY
                    )))
                } else if (confident && profile.level == DifficultyLevel.STABLE) {
                    add(candidate(5, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.DIFFICULTY,
                        LearningInsightSeverity.POSITIVE, LearningInsightTitle.STABLE_LEARNING,
                        LearningInsightSummary.STABLE_FOLLOW_SCHEDULE
                    )))
                }
                if (profile.confidence == DifficultyConfidence.LOW) {
                    add(candidate(6, profileInsight(
                        profile, recommendation, generatedAt, LearningInsightCategory.INSUFFICIENT_DATA,
                        LearningInsightSeverity.INFO, LearningInsightTitle.MORE_EVIDENCE_NEEDED,
                        LearningInsightSummary.ENGINE_CONFIDENCE_LOW,
                        includeRecommendation = true
                    )))
                }
            }

            if (context.latestRatingWasManual) {
                add(candidate(5, base(
                    learnerId, contentId, generatedAt, profile, recommendation,
                    LearningInsightCategory.LEARNING_CONTEXT, LearningInsightSeverity.INFO,
                    LearningInsightTitle.MANUAL_RATING_RECORDED,
                    LearningInsightSummary.MANUAL_RATING_IS_NOT_AUTOMATIC_EVIDENCE
                )))
            }
            if (isEmpty()) add(candidate(6, base(
                learnerId, contentId, generatedAt, profile, recommendation,
                LearningInsightCategory.INSUFFICIENT_DATA, LearningInsightSeverity.INFO,
                LearningInsightTitle.MORE_EVIDENCE_NEEDED, LearningInsightSummary.ENGINE_CONFIDENCE_LOW
            )))
        }.sortedWith(compareBy<Candidate>({ it.rank }, { it.insight.category.ordinal }, { it.insight.title.ordinal }))

        val primary = candidates.first().insight.withMetricLimit(policy.maximumPrimaryMetrics)
        val secondary = candidates.drop(1)
            .map(Candidate::insight)
            .distinctBy { it.category to it.title }
            .filterNot { it.category == primary.category && it.title == primary.title }
            .take(policy.maximumSecondaryInsights)
            .map { it.withMetricLimit(policy.maximumSecondaryMetrics) }
        return LearningInsightBundle(primary, secondary)
    }

    private fun promotionCandidate(
        learnerId: LearnerId,
        contentId: ContentId,
        generatedAt: Moment,
        profile: LearningDifficultyProfile?,
        recommendation: LearningRecommendation?,
        decision: PromotionDecision
    ): Candidate {
        val blocked = !decision.eligible
        val summary = when {
            decision.eligible -> LearningInsightSummary.PROMOTION_ELIGIBLE
            PromotionReason.EVIDENCE_WINDOW_NOT_ELAPSED in decision.reasons ->
                LearningInsightSummary.PROMOTION_WAITING_FOR_TIME
            PromotionReason.MISSING_CORRECT_RECALLS in decision.reasons ->
                LearningInsightSummary.PROMOTION_MISSING_RECALLS
            else -> LearningInsightSummary.PROMOTION_EVIDENCE_EXCLUDED
        }
        val insight = base(
            learnerId, contentId, generatedAt, profile, recommendation,
            LearningInsightCategory.PROMOTION_PROGRESS,
            if (decision.eligible) LearningInsightSeverity.POSITIVE else LearningInsightSeverity.CAUTION,
            if (decision.eligible) LearningInsightTitle.PROMOTION_READY else LearningInsightTitle.PROMOTION_IN_PROGRESS,
            summary
        ).copy(
            promotionTarget = decision.targetRating,
            promotionEligibility = decision.eligible,
            promotionReasons = decision.reasons,
            remainingDuration = decision.remainingTime,
            missingRecallCount = decision.missingCorrectRecalls,
            supportingMetrics = buildList {
                if (decision.remainingTime != TimeSpan.ZERO) add(metricDuration(
                    LearningInsightMetricKind.REMAINING_TIME, decision.remainingTime))
                if (decision.missingCorrectRecalls > 0) add(metricCount(
                    LearningInsightMetricKind.MISSING_RECALLS, decision.missingCorrectRecalls))
                profile?.let { add(metricScore(
                    LearningInsightMetricKind.EVIDENCE_CONFIDENCE,
                    it.current.evidenceConfidence.value, MetricDirection.HIGHER_IS_BETTER)) }
            }
        )
        return candidate(if (blocked) 1 else 4, insight)
    }

    private fun profileInsight(
        profile: LearningDifficultyProfile,
        recommendation: LearningRecommendation,
        generatedAt: Moment,
        category: LearningInsightCategory,
        severity: LearningInsightSeverity,
        title: LearningInsightTitle,
        summary: LearningInsightSummary,
        includeRecommendation: Boolean = false
    ): LearningInsight {
        val metrics = metrics(category, profile)
        return base(
            profile.learnerId, profile.contentId, generatedAt, profile, recommendation,
            category, severity, title, summary
        ).copy(
            recommendedAction = recommendation.action.takeIf { includeRecommendation },
            recommendationPriority = recommendation.priority.takeIf { includeRecommendation },
            recommendationConfidence = recommendation.confidence.takeIf { includeRecommendation },
            recommendationReasons = recommendation.reasons.takeIf { includeRecommendation }.orEmpty(),
            supportingMetrics = metrics
        )
    }

    private fun base(
        learnerId: LearnerId,
        contentId: ContentId,
        generatedAt: Moment,
        profile: LearningDifficultyProfile?,
        recommendation: LearningRecommendation?,
        category: LearningInsightCategory,
        severity: LearningInsightSeverity,
        title: LearningInsightTitle,
        summary: LearningInsightSummary
    ) = LearningInsight(
        learnerId, contentId, category, severity, title, summary,
        profile?.level, profile?.trend, profile?.confidence ?: DifficultyConfidence.LOW,
        null, null, recommendation?.confidence, emptyList(), null, null, emptyList(),
        TimeSpan.ZERO, 0, emptyList(), generatedAt
    )

    private fun metrics(category: LearningInsightCategory, profile: LearningDifficultyProfile) = when (category) {
        LearningInsightCategory.DIFFICULTY -> listOf(
            metricScore(LearningInsightMetricKind.RETENTION_SCORE, profile.current.retentionScore.value, MetricDirection.HIGHER_IS_BETTER),
            metricScore(LearningInsightMetricKind.STABILITY_SCORE, profile.current.stabilityScore.value, MetricDirection.HIGHER_IS_BETTER),
            metricCount(LearningInsightMetricKind.LIFETIME_LAPSES, profile.lifetime.totalLapse)
        )
        LearningInsightCategory.RECOVERY -> listOf(
            metricScore(LearningInsightMetricKind.RETENTION_SCORE, profile.current.retentionScore.value, MetricDirection.HIGHER_IS_BETTER),
            metricScore(LearningInsightMetricKind.RELAPSE_RISK, profile.risk.relapseRisk.value, MetricDirection.LOWER_IS_BETTER),
            metricCount(LearningInsightMetricKind.LIFETIME_LAPSES, profile.lifetime.totalLapse)
        )
        LearningInsightCategory.RETENTION_RISK -> listOf(
            metricScore(LearningInsightMetricKind.FORGET_RISK, profile.risk.forgetRisk.value, MetricDirection.LOWER_IS_BETTER),
            metricScore(LearningInsightMetricKind.RELAPSE_RISK, profile.risk.relapseRisk.value, MetricDirection.LOWER_IS_BETTER)
        )
        LearningInsightCategory.RECOMMENDATION -> listOf(
            metricScore(LearningInsightMetricKind.PROMOTION_READINESS, profile.risk.promotionReadiness.value, MetricDirection.HIGHER_IS_BETTER),
            metricScore(LearningInsightMetricKind.EVIDENCE_CONFIDENCE, profile.current.evidenceConfidence.value, MetricDirection.HIGHER_IS_BETTER)
        )
        LearningInsightCategory.MASTERY -> listOf(
            metricCount(LearningInsightMetricKind.LIFETIME_PROMOTIONS, profile.lifetime.totalPromotion),
            metricScore(LearningInsightMetricKind.RETENTION_SCORE, profile.current.retentionScore.value, MetricDirection.HIGHER_IS_BETTER)
        )
        LearningInsightCategory.INSUFFICIENT_DATA -> listOf(
            metricScore(LearningInsightMetricKind.EVIDENCE_CONFIDENCE, profile.current.evidenceConfidence.value, MetricDirection.HIGHER_IS_BETTER)
        )
        LearningInsightCategory.PROMOTION_PROGRESS,
        LearningInsightCategory.LEARNING_CONTEXT -> emptyList()
    }

    private fun recommendationRank(recommendation: LearningRecommendation): Int = when (recommendation.action) {
        RecommendationAction.RECOVERY_NEEDED -> 0
        RecommendationAction.NEEDS_MORE_EVIDENCE -> 1
        RecommendationAction.FOCUS_PRACTICE -> 3
        RecommendationAction.READY_FOR_PROMOTION -> 4
        RecommendationAction.NORMAL_REVIEW -> 5
        RecommendationAction.MONITOR_ONLY,
        RecommendationAction.MASTERED -> 6
    }

    private fun recommendationTitle(recommendation: LearningRecommendation) = when (recommendation.action) {
        RecommendationAction.FOCUS_PRACTICE -> LearningInsightTitle.KEEP_PRACTICING
        RecommendationAction.READY_FOR_PROMOTION -> LearningInsightTitle.READY_TO_PROGRESS
        RecommendationAction.RECOVERY_NEEDED -> LearningInsightTitle.RECOVERY_NEEDED
        RecommendationAction.NEEDS_MORE_EVIDENCE -> LearningInsightTitle.MORE_EVIDENCE_NEEDED
        RecommendationAction.MASTERED -> LearningInsightTitle.MASTERY_REACHED
        RecommendationAction.NORMAL_REVIEW,
        RecommendationAction.MONITOR_ONLY -> LearningInsightTitle.STABLE_LEARNING
    }

    private fun recommendationSeverity(recommendation: LearningRecommendation) = when (recommendation.priority) {
        RecommendationPriority.CRITICAL -> LearningInsightSeverity.IMPORTANT
        RecommendationPriority.HIGH -> LearningInsightSeverity.CAUTION
        RecommendationPriority.NORMAL -> LearningInsightSeverity.INFO
        RecommendationPriority.LOW -> LearningInsightSeverity.POSITIVE
    }

    private fun LearningInsight.withMetricLimit(limit: Int) = copy(supportingMetrics = supportingMetrics.take(limit))
    private fun candidate(rank: Int, insight: LearningInsight) = Candidate(rank, insight)
    private fun metricScore(kind: LearningInsightMetricKind, value: Double, direction: MetricDirection) =
        LearningInsightMetric(kind, LearningInsightMetricValue.Score(value), direction)
    private fun metricCount(kind: LearningInsightMetricKind, value: Int) =
        LearningInsightMetric(kind, LearningInsightMetricValue.Count(value), MetricDirection.NEUTRAL)
    private fun metricDuration(kind: LearningInsightMetricKind, value: TimeSpan) =
        LearningInsightMetric(kind, LearningInsightMetricValue.Duration(value), MetricDirection.NEUTRAL)

    private data class Candidate(val rank: Int, val insight: LearningInsight)
}
