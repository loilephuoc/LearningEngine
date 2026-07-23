package vn.loi.learning.application.session.bootstrap

/**
 * Information available to Product Brain before and during a session.
 */
data class LearningSessionContext(
    val learnerId: String,
    val topicId: String,
    val availableTimeMinutes: Int = 15,
    val fatigueIndex: Double = 0.0,
    val motivationIndex: Double = 1.0,
    val learningObjective: String = "DURABLE_RECALL",
    val recentMistakesCount: Int = 0,
    val currentMasteryRatio: Double = 0.0
)
