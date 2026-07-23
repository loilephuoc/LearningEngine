package vn.loi.learning.application.session.bootstrap

import vn.loi.learning.application.decision.AdaptiveAction

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
) {
    fun updateWithDecision(action: AdaptiveAction): SessionTimeline {
        val updatedPhases = phases.map { phaseInfo ->
            when (action) {
                AdaptiveAction.INCREASE_DIFFICULTY -> {
                    if (phaseInfo.phase == SessionPhase.PRACTICE) {
                        phaseInfo.copy(estimatedMinutes = maxOf(1, phaseInfo.estimatedMinutes - 1))
                    } else phaseInfo
                }
                AdaptiveAction.DECREASE_DIFFICULTY -> {
                    if (phaseInfo.phase == SessionPhase.PRACTICE) {
                        phaseInfo.copy(estimatedMinutes = phaseInfo.estimatedMinutes + 1)
                    } else phaseInfo
                }
                AdaptiveAction.REPEAT_SIMILAR_SCENE -> {
                    if (phaseInfo.phase == SessionPhase.PRACTICE) {
                        phaseInfo.copy(itemCount = phaseInfo.itemCount + 1)
                    } else phaseInfo
                }
                AdaptiveAction.MAINTAIN_PACE -> phaseInfo
            }
        }
        return copy(
            phases = updatedPhases,
            totalEstimatedMinutes = updatedPhases.sumOf { it.estimatedMinutes }
        )
    }
}
