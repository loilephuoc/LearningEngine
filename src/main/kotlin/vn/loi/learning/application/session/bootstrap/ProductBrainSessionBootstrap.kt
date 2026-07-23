package vn.loi.learning.application.session.bootstrap

import vn.loi.learning.application.learningcontent.LearningContent
import java.util.UUID

/**
 * Application service for bootstrapping a learning session via Product Brain.
 */
class ProductBrainSessionBootstrap {

    fun bootstrap(
        learnerId: String,
        topicId: String,
        content: LearningContent? = null,
        itemCount: Int = 0,
        availableTimeMinutes: Int = 15,
        sessionId: String = UUID.randomUUID().toString()
    ): SessionOverview {
        val totalItems = when {
            itemCount > 0 -> itemCount
            content != null -> 1
            else -> 0
        }

        val context = LearningSessionContext(
            learnerId = learnerId,
            topicId = topicId,
            availableTimeMinutes = availableTimeMinutes,
            fatigueIndex = 0.0,
            motivationIndex = 1.0,
            learningObjective = "DURABLE_RECALL",
            recentMistakesCount = 0,
            currentMasteryRatio = 0.0
        )

        val goal = TeachingGoal(
            objective = context.learningObjective,
            targetItemCount = totalItems,
            topicId = topicId,
            description = "Master $totalItems learning items in topic '$topicId' with durable recall."
        )

        val phases = mutableListOf<SessionPhaseInfo>()
        if (totalItems > 0) {
            phases.add(SessionPhaseInfo(SessionPhase.WARM_UP, "Warm-up", 1, minOf(2, totalItems)))
            phases.add(SessionPhaseInfo(SessionPhase.TEACHING, "Teaching Phase", 3, totalItems))
            phases.add(SessionPhaseInfo(SessionPhase.PRACTICE, "Practice Phase", 6, totalItems))
            phases.add(SessionPhaseInfo(SessionPhase.REVIEW, "Review Consolidation", 3, totalItems))
            phases.add(SessionPhaseInfo(SessionPhase.REFLECTION, "Reflection & Summary", 2, 0))
        } else {
            phases.add(SessionPhaseInfo(SessionPhase.SUMMARY, "Session Summary", 1, 0))
        }

        val timeline = SessionTimeline(
            phases = phases,
            totalEstimatedMinutes = phases.sumOf { it.estimatedMinutes }
        )

        val initialDecision = InitialDecisionSnapshot(
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            triggerEvent = "SESSION_BOOTSTRAP",
            rulesTriggered = listOf("INITIAL_EXPOSURE", "WARMUP_MOMENTUM_BUILDING", "MULTI_STAGE_PRACTICE"),
            pedagogicalRationale = "Product Brain initialized a ${timeline.totalEstimatedMinutes}-minute session targeting $totalItems items for topic '$topicId'."
        )

        return SessionOverview(
            context = context,
            goal = goal,
            timeline = timeline,
            initialDecision = initialDecision,
            isReadyToStart = true
        )
    }
}
