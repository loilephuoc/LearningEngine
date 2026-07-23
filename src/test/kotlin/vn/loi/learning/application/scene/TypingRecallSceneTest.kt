package vn.loi.learning.application.scene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner

class TypingRecallSceneTest {

    private val planner = ProductBrainPlanner()

    @Test
    fun `TypingRecallScene evaluates exact and normalized matches correctly`() {
        val scene = TypingRecallScene(sceneId = "test-scene-01")
        val input = LearningSceneInput(
            sceneId = "test-scene-01",
            objective = "DURABLE_RECALL",
            promptText = "translate: cat",
            expectedAnswer = "neko",
            learningItemId = "item-cat"
        )

        scene.prepare(input)

        val exactResult = scene.evaluate(userAttempt = "neko", latencyMs = 1200L)
        assertTrue(exactResult.isExactMatch)
        assertTrue(exactResult.isNormalizedMatch)
        assertEquals(0, exactResult.editDistance)

        val exactEvidence = scene.toEvidence(exactResult, "learner-1", "item-cat")
        assertEquals(EvidencePerformance.CORRECT, exactEvidence.performance)

        val normalizedResult = scene.evaluate(userAttempt = "  Neko  ", latencyMs = 1500L)
        assertFalse(normalizedResult.isExactMatch)
        assertTrue(normalizedResult.isNormalizedMatch)

        val normEvidence = scene.toEvidence(normalizedResult, "learner-1", "item-cat")
        assertEquals(EvidencePerformance.CORRECT, normEvidence.performance)
    }

    @Test
    fun `TypingRecallScene calculates edit distance and partial evidence`() {
        val scene = TypingRecallScene(sceneId = "test-scene-02")
        val input = LearningSceneInput(
            sceneId = "test-scene-02",
            objective = "DURABLE_RECALL",
            promptText = "translate: elephant",
            expectedAnswer = "elephant",
            learningItemId = "item-elephant"
        )

        scene.prepare(input)

        val typoResult = scene.evaluate(userAttempt = "elefant", latencyMs = 2000L)
        assertFalse(typoResult.isNormalizedMatch)
        assertEquals(2, typoResult.editDistance)

        val partialEvidence = scene.toEvidence(typoResult, "learner-1", "item-elephant")
        assertEquals(EvidencePerformance.PARTIAL, partialEvidence.performance)
    }

    @Test
    fun `ProductBrainPlanner selects first scene and accepts evidence`() {
        val scene = planner.selectFirstScene(
            promptText = "What is the capital of Japan?",
            expectedAnswer = "Tokyo",
            learningItemId = "item-japan"
        )

        assertNotNull(scene)
        assertEquals("PRACTICE", scene.categoryName)

        val result = scene.evaluate("Tokyo", 900L)
        val evidence = scene.toEvidence(result, "learner-1", "item-japan")
        val receipt = planner.processEvidence(evidence)

        assertNotNull(receipt)
        assertEquals("ACCEPTED", receipt.status)
        assertEquals(evidence.evidenceId, receipt.evidenceId)
    }
}
