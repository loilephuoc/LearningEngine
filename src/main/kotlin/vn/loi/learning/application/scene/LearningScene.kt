package vn.loi.learning.application.scene

/**
 * Platform-neutral interface contract for all Learning Scenes.
 */
interface LearningScene {
    val sceneId: String
    val categoryName: String
    fun prepare(input: LearningSceneInput)
    fun evaluate(userAttempt: String, latencyMs: Long): SceneResult
    fun toEvidence(result: SceneResult, learnerId: String, learningItemId: String): LearningEvidence
}
