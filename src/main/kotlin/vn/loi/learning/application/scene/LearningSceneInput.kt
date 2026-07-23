package vn.loi.learning.application.scene

import vn.loi.learning.application.session.bootstrap.LearningSessionContext

/**
 * Input contract for initializing a Learning Scene.
 */
data class LearningSceneInput(
    val sceneId: String,
    val objective: String,
    val promptText: String,
    val expectedAnswer: String,
    val learningItemId: String? = null,
    val learnerId: String = "default-learner",
    val sessionContext: LearningSessionContext? = null
)
