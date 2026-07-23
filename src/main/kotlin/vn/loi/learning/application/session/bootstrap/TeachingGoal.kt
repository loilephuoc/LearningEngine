package vn.loi.learning.application.session.bootstrap

/**
 * Resolved teaching target formulated by Product Brain.
 */
data class TeachingGoal(
    val objective: String,
    val targetItemCount: Int,
    val topicId: String,
    val description: String
)
