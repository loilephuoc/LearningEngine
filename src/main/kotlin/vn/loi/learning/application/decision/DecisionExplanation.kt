package vn.loi.learning.application.decision

/**
 * Learner-facing explanation of an adaptive teaching decision made by Product Brain.
 */
data class DecisionExplanation(
    val explanationId: String,
    val decisionId: String,
    val observation: String,
    val decisionSummary: String,
    val pedagogicalReason: String,
    val nextStep: String
)
