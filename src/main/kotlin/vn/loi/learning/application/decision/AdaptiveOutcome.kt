package vn.loi.learning.application.decision

import vn.loi.learning.application.session.bootstrap.SessionTimeline

/**
 * Result container returned after Product Brain evaluates evidence and adapts.
 */
data class AdaptiveOutcome(
    val decision: AdaptiveDecision,
    val trace: DecisionTrace,
    val updatedTimeline: SessionTimeline,
    val newDifficultyLevel: Int
)
