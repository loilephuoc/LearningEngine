package vn.loi.learning.application.session.bootstrap

enum class SessionPhase {
    WARM_UP,
    TEACHING,
    PRACTICE,
    CHALLENGE,
    REVIEW,
    REFLECTION,
    SUMMARY
}

data class SessionPhaseInfo(
    val phase: SessionPhase,
    val name: String,
    val estimatedMinutes: Int,
    val itemCount: Int
)

/**
 * Phase timeline representation of a study session.
 */
data class SessionTimeline(
    val phases: List<SessionPhaseInfo>,
    val totalEstimatedMinutes: Int
)
