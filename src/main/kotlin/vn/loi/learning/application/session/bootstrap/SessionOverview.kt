package vn.loi.learning.application.session.bootstrap

/**
 * Overview of a bootstrapped session ready to be presented to the learner.
 */
data class SessionOverview(
    val context: LearningSessionContext,
    val goal: TeachingGoal,
    val timeline: SessionTimeline,
    val initialDecision: InitialDecisionSnapshot,
    val isReadyToStart: Boolean = true
)
