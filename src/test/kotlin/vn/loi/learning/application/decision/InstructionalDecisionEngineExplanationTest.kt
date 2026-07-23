package vn.loi.learning.application.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.scene.LearningEvidence

class InstructionalDecisionEngineExplanationTest {

    private val engine = InstructionalDecisionEngine()

    @Test
    fun `generateExplanation produces valid explanation for INCREASE_DIFFICULTY without technical leakage`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-01",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.CORRECT,
            userAttempt = "fast response",
            attemptLatencyMs = 1100L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 1)
        val explanation = engine.generateExplanation(decision, evidence)

        assertNotNull(explanation)
        assertEquals(decision.decisionId, explanation.decisionId)
        assertTrue(explanation.observation.contains("1100ms"))
        assertTrue(explanation.decisionSummary.contains("increasing"))
        assertTrue(explanation.pedagogicalReason.contains("fluency"))
        assertTrue(explanation.nextStep.contains("higher difficulty"))

        assertFalse(explanation.observation.contains("FAST_CORRECT_MASTERY_BOOST"))
        assertFalse(explanation.pedagogicalReason.contains("InstructionalDecisionEngine"))
    }

    @Test
    fun `generateExplanation produces valid explanation for DECREASE_DIFFICULTY`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-02",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.INCORRECT,
            userAttempt = "wrong response",
            attemptLatencyMs = 2500L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 3)
        val explanation = engine.generateExplanation(decision, evidence)

        assertNotNull(explanation)
        assertTrue(explanation.observation.contains("incorrect"))
        assertTrue(explanation.decisionSummary.contains("easing"))
        assertTrue(explanation.pedagogicalReason.contains("scaffolding"))
        assertTrue(explanation.nextStep.contains("clearer prompt"))
    }

    @Test
    fun `generateExplanation produces valid explanation for REPEAT_SIMILAR_SCENE`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-03",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.PARTIAL,
            userAttempt = "near response",
            attemptLatencyMs = 1800L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 2)
        val explanation = engine.generateExplanation(decision, evidence)

        assertNotNull(explanation)
        assertTrue(explanation.observation.contains("typo"))
        assertTrue(explanation.decisionSummary.contains("similar practice scene"))
        assertTrue(explanation.pedagogicalReason.contains("consolidation"))
        assertTrue(explanation.nextStep.contains("similar recall exercise"))
    }

    @Test
    fun `generateExplanation produces valid explanation for MAINTAIN_PACE`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-04",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.CORRECT,
            userAttempt = "steady response",
            attemptLatencyMs = 3500L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 2)
        val explanation = engine.generateExplanation(decision, evidence)

        assertNotNull(explanation)
        assertTrue(explanation.observation.contains("steady pace"))
        assertTrue(explanation.decisionSummary.contains("maintaining"))
        assertTrue(explanation.pedagogicalReason.contains("appropriate learning pace"))
        assertTrue(explanation.nextStep.contains("Continuing practice"))
    }
}
