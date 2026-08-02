package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

enum class RecommendationAction {
    FOCUS_PRACTICE,
    NORMAL_REVIEW,
    READY_FOR_PROMOTION,
    NEEDS_MORE_EVIDENCE,
    MONITOR_ONLY,
    MASTERED,
    RECOVERY_NEEDED
}

enum class RecommendationPriority { LOW, NORMAL, HIGH, CRITICAL }

enum class RecommendationCategory {
    INTERVENTION,
    RECOVERY,
    EVIDENCE_BUILDING,
    PROGRESSION,
    MAINTENANCE
}

enum class RecommendationConfidence { LOW, MEDIUM, HIGH }

enum class RecommendationReason {
    VERY_DIFFICULT_TRAJECTORY,
    DIFFICULT_TRAJECTORY,
    UNSTABLE_RETENTION,
    RECOVERING_TRAJECTORY,
    DECLINING_TRAJECTORY,
    REGRESSING_TRAJECTORY,
    INSUFFICIENT_EVIDENCE,
    LOW_ENGINE_CONFIDENCE,
    HIGH_ENGINE_CONFIDENCE,
    HIGH_FORGET_RISK,
    HIGH_RELAPSE_RISK,
    TOO_MANY_LAPSES,
    PROMOTION_TOO_FAST,
    PROMOTION_READY,
    STABLE_RETENTION,
    MASTERED_TRAJECTORY,
    LONG_MASTERED_HISTORY
}

/** Immutable advice only; it has no command, mutation, scheduling, or persistence semantics. */
data class LearningRecommendation(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val profileCalculatedAt: Moment,
    val action: RecommendationAction,
    val category: RecommendationCategory,
    val priority: RecommendationPriority,
    val confidence: RecommendationConfidence,
    val reasons: List<RecommendationReason>
) {
    init { require(reasons.isNotEmpty()) { "A learning recommendation must explain its decision." } }
}
