package vn.loi.learning.application.scene

enum class EvidencePerformance {
    CORRECT,
    PARTIAL,
    INCORRECT
}

/**
 * Canonical evidence payload returned to Product Brain after scene evaluation.
 */
data class LearningEvidence(
    val evidenceId: String,
    val sceneId: String,
    val learningItemId: String,
    val learnerId: String,
    val performance: EvidencePerformance,
    val userAttempt: String,
    val attemptLatencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Receipt returned by Product Brain acknowledging received LearningEvidence.
 */
data class EvidenceReceipt(
    val evidenceId: String,
    val status: String = "ACCEPTED",
    val timestamp: Long = System.currentTimeMillis()
)
