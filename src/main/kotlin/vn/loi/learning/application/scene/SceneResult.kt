package vn.loi.learning.application.scene

/**
 * Output evaluation result produced by executing a Learning Scene.
 */
data class SceneResult(
    val sceneId: String,
    val userAttempt: String,
    val isExactMatch: Boolean,
    val isNormalizedMatch: Boolean,
    val attemptLatencyMs: Long,
    val editDistance: Int
)
