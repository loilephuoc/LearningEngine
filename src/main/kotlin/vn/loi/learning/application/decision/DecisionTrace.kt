package vn.loi.learning.application.decision

/**
 * Structural reasoning log generated alongside AdaptiveDecision.
 */
data class DecisionTrace(
    val traceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val evidenceId: String,
    val rulesTriggered: List<String>,
    val decision: AdaptiveDecision,
    val explanation: String
)
