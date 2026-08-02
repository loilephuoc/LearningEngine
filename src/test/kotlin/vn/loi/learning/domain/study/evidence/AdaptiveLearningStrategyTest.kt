package vn.loi.learning.domain.study.evidence

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.*

class AdaptiveLearningStrategyTest {
    private val strategy = AdaptiveLearningStrategy()

    @Test fun `very difficult profile recommends high priority focus practice`() {
        val recommendation = strategy.calculateRecommendation(profile(level = DifficultyLevel.VERY_DIFFICULT))
        assertEquals(RecommendationAction.FOCUS_PRACTICE, recommendation.action)
        assertEquals(RecommendationPriority.HIGH, recommendation.priority)
        assertEquals(RecommendationCategory.INTERVENTION, recommendation.category)
        assertContains(recommendation.reasons, RecommendationReason.VERY_DIFFICULT_TRAJECTORY)
    }

    @Test fun `mastered profile recommends low priority monitoring with typed reasons`() {
        val recommendation = strategy.calculateRecommendation(
            profile(level = DifficultyLevel.MASTERED, confidence = DifficultyConfidence.HIGH, promotions = 3)
        )
        assertEquals(RecommendationAction.MONITOR_ONLY, recommendation.action)
        assertEquals(RecommendationPriority.LOW, recommendation.priority)
        assertContains(recommendation.reasons, RecommendationReason.MASTERED_TRAJECTORY)
        assertContains(recommendation.reasons, RecommendationReason.LONG_MASTERED_HISTORY)
    }

    @Test fun `recovering profile recommends recovery before difficulty action`() {
        val recommendation = strategy.calculateRecommendation(
            profile(level = DifficultyLevel.DIFFICULT, trend = DifficultyTrend.RECOVERING)
        )
        assertEquals(RecommendationAction.RECOVERY_NEEDED, recommendation.action)
        assertContains(recommendation.reasons, RecommendationReason.RECOVERING_TRAJECTORY)
    }

    @Test fun `unstable profile recommends more evidence`() {
        val recommendation = strategy.calculateRecommendation(profile(level = DifficultyLevel.UNSTABLE))
        assertEquals(RecommendationAction.NEEDS_MORE_EVIDENCE, recommendation.action)
        assertEquals(RecommendationCategory.EVIDENCE_BUILDING, recommendation.category)
    }

    @Test fun `stable profile defaults to normal review`() {
        val recommendation = strategy.calculateRecommendation(
            profile(level = DifficultyLevel.STABLE, confidence = DifficultyConfidence.MEDIUM, readiness = 0.50)
        )
        assertEquals(RecommendationAction.NORMAL_REVIEW, recommendation.action)
        assertContains(recommendation.reasons, RecommendationReason.STABLE_RETENTION)
    }

    @Test fun `high confidence stable readiness recommends promotion`() {
        val recommendation = strategy.calculateRecommendation(
            profile(level = DifficultyLevel.STABLE, confidence = DifficultyConfidence.HIGH, readiness = 0.80)
        )
        assertEquals(RecommendationAction.READY_FOR_PROMOTION, recommendation.action)
        assertContains(recommendation.reasons, RecommendationReason.PROMOTION_READY)
    }

    @Test fun `recommendation confidence is engine confidence projection`() {
        assertEquals(RecommendationConfidence.LOW,
            strategy.calculateRecommendation(profile(confidence = DifficultyConfidence.LOW)).confidence)
        assertEquals(RecommendationConfidence.MEDIUM,
            strategy.calculateRecommendation(profile(confidence = DifficultyConfidence.MEDIUM)).confidence)
        assertEquals(RecommendationConfidence.HIGH,
            strategy.calculateRecommendation(profile(confidence = DifficultyConfidence.HIGH)).confidence)
    }

    @Test fun `low confidence explains insufficient evidence without strings`() {
        val reasons = strategy.calculateRecommendation(profile(confidence = DifficultyConfidence.LOW)).reasons
        assertContains(reasons, RecommendationReason.INSUFFICIENT_EVIDENCE)
        assertContains(reasons, RecommendationReason.LOW_ENGINE_CONFIDENCE)
    }

    @Test fun `regressing high relapse profile is critical recovery`() {
        val recommendation = strategy.calculateRecommendation(
            profile(trend = DifficultyTrend.REGRESSING, relapseRisk = 0.75, lapses = 3)
        )
        assertEquals(RecommendationAction.RECOVERY_NEEDED, recommendation.action)
        assertEquals(RecommendationPriority.CRITICAL, recommendation.priority)
        assertContains(recommendation.reasons, RecommendationReason.HIGH_RELAPSE_RISK)
        assertContains(recommendation.reasons, RecommendationReason.TOO_MANY_LAPSES)
    }

    @Test fun `fast promotion and high forget risk remain typed explanation signals`() {
        val recommendation = strategy.calculateRecommendation(
            profile(forgetRisk = 0.80, averagePromotionDuration = TimeSpan.hours(12))
        )
        assertContains(recommendation.reasons, RecommendationReason.HIGH_FORGET_RISK)
        assertContains(recommendation.reasons, RecommendationReason.PROMOTION_TOO_FAST)
    }

    @Test fun `calculation is pure deterministic and preserves learner Content authority`() {
        val input = profile()
        val first = strategy.calculateRecommendation(input)
        val second = strategy.calculateRecommendation(input)
        assertEquals(first, second)
        assertEquals(input.learnerId, first.learnerId)
        assertEquals(input.contentId, first.contentId)
        assertEquals(profile(), input)
    }

    private fun profile(
        level: DifficultyLevel = DifficultyLevel.STABLE,
        trend: DifficultyTrend = DifficultyTrend.PLATEAU,
        confidence: DifficultyConfidence = DifficultyConfidence.MEDIUM,
        readiness: Double = 0.50,
        forgetRisk: Double = 0.20,
        relapseRisk: Double = 0.20,
        lapses: Int = 0,
        promotions: Int = 1,
        averagePromotionDuration: TimeSpan? = TimeSpan.hours(48)
    ) = LearningDifficultyProfile(
        learnerId = LearnerId("learner"),
        contentId = ContentId("content"),
        calculatedAt = Moment(1_000),
        lifetime = LifetimeStatistics(
            Moment(100), Moment(900), 12, 1, 2, 6, 3, promotions, lapses, 12
        ),
        current = CurrentStability(
            RetentionScore(0.80), StabilityScore(0.75), EvidenceConfidenceScore(0.70),
            DifficultyScore(0.20), trend
        ),
        promotionAnalytics = PromotionAnalytics(
            averagePromotionDuration, AverageCount(3.0), AverageCount(2.0), emptyMap()
        ),
        risk = RiskProfile(RiskScore(forgetRisk), RiskScore(relapseRisk), ReadinessScore(readiness)),
        confidence = confidence,
        trend = trend,
        level = level
    )
}
