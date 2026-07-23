package vn.loi.learning.application.scene

import java.util.UUID

/**
 * Platform-neutral implementation of LearningScene for Typing Recall interactions.
 */
class TypingRecallScene(
    override val sceneId: String = "typing-recall-" + UUID.randomUUID().toString().take(8)
) : LearningScene {

    override val categoryName: String = "PRACTICE"
    var sceneInput: LearningSceneInput? = null
        private set

    override fun prepare(input: LearningSceneInput) {
        this.sceneInput = input
    }

    override fun evaluate(userAttempt: String, latencyMs: Long): SceneResult {
        val currentInput = requireNotNull(sceneInput) { "Scene must be prepared before evaluation." }
        val expected = currentInput.expectedAnswer

        val exactMatch = userAttempt == expected
        val normAttempt = normalize(userAttempt)
        val normExpected = normalize(expected)
        val normalizedMatch = normAttempt == normExpected
        val distance = computeEditDistance(normAttempt, normExpected)

        return SceneResult(
            sceneId = sceneId,
            userAttempt = userAttempt,
            isExactMatch = exactMatch,
            isNormalizedMatch = normalizedMatch,
            attemptLatencyMs = latencyMs,
            editDistance = distance
        )
    }

    override fun toEvidence(
        result: SceneResult,
        learnerId: String,
        learningItemId: String
    ): LearningEvidence {
        val performance = when {
            result.isNormalizedMatch -> EvidencePerformance.CORRECT
            result.editDistance <= 2 -> EvidencePerformance.PARTIAL
            else -> EvidencePerformance.INCORRECT
        }

        return LearningEvidence(
            evidenceId = "ev-" + UUID.randomUUID().toString().take(8),
            sceneId = result.sceneId,
            learningItemId = learningItemId,
            learnerId = learnerId,
            performance = performance,
            userAttempt = result.userAttempt,
            attemptLatencyMs = result.attemptLatencyMs
        )
    }

    private fun normalize(text: String): String =
        text.trim().lowercase()

    private fun computeEditDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
