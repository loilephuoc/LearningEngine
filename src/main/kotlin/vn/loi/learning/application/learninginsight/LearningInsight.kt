package vn.loi.learning.application.learninginsight

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

enum class LearningInsightCategory {
    PROMOTION_PROGRESS, DIFFICULTY, RECOVERY, RETENTION_RISK, RECOMMENDATION,
    MASTERY, INSUFFICIENT_DATA, LEARNING_CONTEXT
}

enum class LearningInsightSeverity { INFO, POSITIVE, CAUTION, IMPORTANT }

enum class LearningInsightTitle {
    PROMOTION_READY, PROMOTION_IN_PROGRESS, RECOVERY_NEEDED, DIFFICULT_CONTENT,
    RETENTION_AT_RISK, KEEP_PRACTICING, READY_TO_PROGRESS, STABLE_LEARNING,
    MASTERY_REACHED, MORE_EVIDENCE_NEEDED, PRACTICE_DOES_NOT_CHANGE_RATING,
    MANUAL_RATING_RECORDED
}

enum class LearningInsightSummary {
    PROMOTION_ELIGIBLE, PROMOTION_WAITING_FOR_TIME, PROMOTION_MISSING_RECALLS,
    PROMOTION_EVIDENCE_EXCLUDED, TRAJECTORY_RECOVERING, TRAJECTORY_REGRESSING,
    VERY_DIFFICULT_HIGH_RISK, RETENTION_RISK_HIGH, FOLLOW_RECOMMENDATION,
    STABLE_FOLLOW_SCHEDULE, MASTERED_MONITOR_ONLY, ENGINE_CONFIDENCE_LOW,
    PRACTICE_IS_NOT_PROMOTION_EVIDENCE, MANUAL_RATING_IS_NOT_AUTOMATIC_EVIDENCE
}

enum class LearningInsightMetricKind {
    LIFETIME_LAPSES, LIFETIME_PROMOTIONS, RETENTION_SCORE, STABILITY_SCORE,
    FORGET_RISK, RELAPSE_RISK, PROMOTION_READINESS, MISSING_RECALLS,
    REMAINING_TIME, EVIDENCE_CONFIDENCE
}

enum class MetricDirection { HIGHER_IS_BETTER, LOWER_IS_BETTER, NEUTRAL }

sealed interface LearningInsightMetricValue {
    data class Score(val value: Double) : LearningInsightMetricValue {
        init { require(value in 0.0..1.0) }
    }
    data class Count(val value: Int) : LearningInsightMetricValue {
        init { require(value >= 0) }
    }
    data class Duration(val value: TimeSpan) : LearningInsightMetricValue
}

data class LearningInsightMetric(
    val kind: LearningInsightMetricKind,
    val value: LearningInsightMetricValue,
    val direction: MetricDirection
)

data class LearningInsight(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val category: LearningInsightCategory,
    val severity: LearningInsightSeverity,
    val title: LearningInsightTitle,
    val summary: LearningInsightSummary,
    val difficultyLevel: DifficultyLevel?,
    val trend: DifficultyTrend?,
    val engineConfidence: DifficultyConfidence,
    val recommendedAction: RecommendationAction?,
    val recommendationPriority: RecommendationPriority?,
    val recommendationConfidence: RecommendationConfidence?,
    val recommendationReasons: List<RecommendationReason>,
    val promotionTarget: ReviewRating?,
    val promotionEligibility: Boolean?,
    val promotionReasons: List<PromotionReason>,
    val remainingDuration: TimeSpan,
    val missingRecallCount: Int,
    val supportingMetrics: List<LearningInsightMetric>,
    val generatedAt: Moment
)

data class LearningInsightBundle(
    val primary: LearningInsight,
    val secondary: List<LearningInsight>
) {
    init { require(secondary.none { it == primary }) }
}

enum class LearningInsightMode { EVALUATIVE, PRACTICE_ONLY }

data class LearningInsightContext(
    val mode: LearningInsightMode = LearningInsightMode.EVALUATIVE,
    val latestRatingWasManual: Boolean = false
)
