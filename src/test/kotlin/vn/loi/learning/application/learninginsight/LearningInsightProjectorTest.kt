package vn.loi.learning.application.learninginsight

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*

class LearningInsightProjectorTest {
    private val projector = LearningInsightProjector()
    private val learner = LearnerId("learner")
    private val content = ContentId("content")

    @Test fun `time-blocked promotion preserves typed remaining duration`() {
        val decision = decision(PromotionReason.EVIDENCE_WINDOW_NOT_ELAPSED, remaining = TimeSpan.hours(23))
        val insight = project(promotion = decision).primary
        assertEquals(TimeSpan.hours(23), insight.remainingDuration)
        assertEquals(LearningInsightSummary.PROMOTION_WAITING_FOR_TIME, insight.summary)
        assertIs<LearningInsightMetricValue.Duration>(insight.supportingMetrics.first().value)
    }

    @Test fun `recall-blocked promotion preserves missing count`() {
        val insight = project(promotion = decision(
            PromotionReason.MISSING_CORRECT_RECALLS, missing = 2
        )).primary
        assertEquals(2, insight.missingRecallCount)
        assertEquals(LearningInsightSummary.PROMOTION_MISSING_RECALLS, insight.summary)
    }

    @Test fun `reveal exclusion remains a typed promotion reason`() {
        val insight = project(promotion = decision(PromotionReason.REVEAL_EVIDENCE_EXCLUDED)).primary
        assertContains(insight.promotionReasons, PromotionReason.REVEAL_EVIDENCE_EXCLUDED)
        assertEquals(LearningInsightSummary.PROMOTION_EVIDENCE_EXCLUDED, insight.summary)
    }

    @Test fun `practice context never presents supplied decision as promotion evidence`() {
        val insight = project(
            promotion = decision(PromotionReason.PRACTICE_EVIDENCE_EXCLUDED),
            context = LearningInsightContext(LearningInsightMode.PRACTICE_ONLY)
        ).primary
        assertEquals(LearningInsightTitle.PRACTICE_DOES_NOT_CHANGE_RATING, insight.title)
        assertNull(insight.promotionEligibility)
        assertTrue(insight.promotionReasons.isEmpty())
    }

    @Test fun `manual provenance is described without automatic evidence claim`() {
        val bundle = project(context = LearningInsightContext(latestRatingWasManual = true))
        val manual = (listOf(bundle.primary) + bundle.secondary)
            .single { it.title == LearningInsightTitle.MANUAL_RATING_RECORDED }
        assertEquals(LearningInsightSummary.MANUAL_RATING_IS_NOT_AUTOMATIC_EVIDENCE, manual.summary)
        assertNull(manual.promotionEligibility)
    }

    @Test fun `very difficult high risk selects difficulty before focus recommendation`() {
        val bundle = project(
            profile = profile(level = DifficultyLevel.VERY_DIFFICULT),
            recommendation = recommendation(
                RecommendationAction.FOCUS_PRACTICE,
                RecommendationReason.VERY_DIFFICULT_TRAJECTORY,
                RecommendationReason.HIGH_FORGET_RISK
            )
        )
        assertEquals(LearningInsightCategory.DIFFICULTY, bundle.primary.category)
        assertTrue(bundle.secondary.any { it.category == LearningInsightCategory.RETENTION_RISK })
    }

    @Test fun `recovering trend selects recovery insight`() {
        val bundle = project(profile = profile(trend = DifficultyTrend.RECOVERING),
            recommendation = recommendation(RecommendationAction.RECOVERY_NEEDED,
                RecommendationReason.RECOVERING_TRAJECTORY))
        assertEquals(LearningInsightCategory.RECOVERY, bundle.primary.category)
        assertEquals(LearningInsightSummary.TRAJECTORY_RECOVERING, bundle.primary.summary)
    }

    @Test fun `mastered selects mastery monitor insight`() {
        val bundle = project(profile = profile(level = DifficultyLevel.MASTERED),
            recommendation = recommendation(RecommendationAction.MONITOR_ONLY,
                RecommendationReason.MASTERED_TRAJECTORY))
        assertEquals(LearningInsightCategory.MASTERY, bundle.primary.category)
        assertEquals(LearningInsightTitle.MASTERY_REACHED, bundle.primary.title)
    }

    @Test fun `low confidence suppresses strong difficulty assertion`() {
        val bundle = project(profile = profile(
            level = DifficultyLevel.VERY_DIFFICULT, confidence = DifficultyConfidence.LOW
        ), recommendation = recommendation(RecommendationAction.NEEDS_MORE_EVIDENCE,
            RecommendationReason.INSUFFICIENT_EVIDENCE, confidence = RecommendationConfidence.LOW))
        assertEquals(LearningInsightCategory.INSUFFICIENT_DATA, bundle.primary.category)
        assertEquals(LearningInsightSummary.ENGINE_CONFIDENCE_LOW, bundle.primary.summary)
    }

    @Test fun `recommendation action priority confidence and reasons are preserved`() {
        val source = recommendation(
            RecommendationAction.READY_FOR_PROMOTION,
            RecommendationReason.PROMOTION_READY,
            priority = RecommendationPriority.HIGH,
            confidence = RecommendationConfidence.HIGH
        )
        val insight = project(recommendation = source).let { listOf(it.primary) + it.secondary }
            .single { it.category == LearningInsightCategory.RECOMMENDATION }
        assertEquals(source.action, insight.recommendedAction)
        assertEquals(source.priority, insight.recommendationPriority)
        assertEquals(source.confidence, insight.recommendationConfidence)
        assertEquals(source.reasons, insight.recommendationReasons)
    }

    @Test fun `primary and secondary ordering is deterministic and bounded`() {
        val first = project(
            profile = profile(level = DifficultyLevel.VERY_DIFFICULT),
            recommendation = recommendation(RecommendationAction.FOCUS_PRACTICE,
                RecommendationReason.HIGH_FORGET_RISK),
            promotion = decision(PromotionReason.MISSING_CORRECT_RECALLS, missing = 1)
        )
        val second = project(
            profile = profile(level = DifficultyLevel.VERY_DIFFICULT),
            recommendation = recommendation(RecommendationAction.FOCUS_PRACTICE,
                RecommendationReason.HIGH_FORGET_RISK),
            promotion = decision(PromotionReason.MISSING_CORRECT_RECALLS, missing = 1)
        )
        assertEquals(first, second)
        assertEquals(LearningInsightCategory.PROMOTION_PROGRESS, first.primary.category)
        assertTrue(first.secondary.size <= 2)
        assertEquals(first.secondary.size, first.secondary.distinctBy { it.category to it.title }.size)
    }

    @Test fun `supporting metrics remain typed and limited`() {
        val insight = project(profile = profile(level = DifficultyLevel.VERY_DIFFICULT)).primary
        assertTrue(insight.supportingMetrics.size <= 3)
        assertTrue(insight.supportingMetrics.all { it.value is LearningInsightMetricValue.Score ||
            it.value is LearningInsightMetricValue.Count || it.value is LearningInsightMetricValue.Duration })
    }

    @Test fun `projection preserves learner Content authority and fake generated time`() {
        val generatedAt = Moment(42_000)
        val insight = projector.project(
            learner, content, generatedAt, profile(), recommendation()
        ).primary
        assertEquals(learner, insight.learnerId)
        assertEquals(content, insight.contentId)
        assertEquals(generatedAt, insight.generatedAt)
    }

    private fun project(
        profile: LearningDifficultyProfile = profile(),
        recommendation: LearningRecommendation = recommendation(),
        promotion: PromotionDecision? = null,
        context: LearningInsightContext = LearningInsightContext()
    ) = projector.project(learner, content, Moment(10_000), profile, recommendation, promotion, context)

    private fun decision(
        reason: PromotionReason,
        remaining: TimeSpan = TimeSpan.ZERO,
        missing: Int = 0
    ) = PromotionDecision(false, ReviewRating.AGAIN, ReviewRating.HARD, listOf(reason), remaining, missing)

    private fun recommendation(
        action: RecommendationAction = RecommendationAction.NORMAL_REVIEW,
        vararg reasons: RecommendationReason,
        priority: RecommendationPriority = RecommendationPriority.NORMAL,
        confidence: RecommendationConfidence = RecommendationConfidence.MEDIUM
    ) = LearningRecommendation(
        learner, content, Moment(9_000), action, RecommendationCategory.MAINTENANCE,
        priority, confidence, reasons.toList().ifEmpty { listOf(RecommendationReason.STABLE_RETENTION) }
    )

    private fun profile(
        level: DifficultyLevel = DifficultyLevel.STABLE,
        trend: DifficultyTrend = DifficultyTrend.PLATEAU,
        confidence: DifficultyConfidence = DifficultyConfidence.MEDIUM
    ) = LearningDifficultyProfile(
        learner, content, Moment(9_000),
        LifetimeStatistics(Moment(1_000), Moment(8_000), 12, 2, 2, 5, 3, 2, 1, 12),
        CurrentStability(RetentionScore(.70), StabilityScore(.65), EvidenceConfidenceScore(.70),
            DifficultyScore(.40), trend),
        PromotionAnalytics(TimeSpan.hours(48), AverageCount(3.0), AverageCount(2.0), emptyMap()),
        RiskProfile(RiskScore(.70), RiskScore(.65), ReadinessScore(.60)),
        confidence, trend, level
    )
}
