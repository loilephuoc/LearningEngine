package vn.loi.learning.application.session.bootstrap

/**
 * Structural decision trace capturing Product Brain's reasoning at session initialization.
 */
data class InitialDecisionSnapshot(
    val timestamp: Long,
    val sessionId: String,
    val triggerEvent: String = "SESSION_BOOTSTRAP",
    val rulesTriggered: List<String>,
    val pedagogicalRationale: String
)
