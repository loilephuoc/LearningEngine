package vn.loi.learning.application.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.scene.LearningEvidence
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.application.session.bootstrap.SessionPhase
import vn.loi.learning.application.session.bootstrap.SessionPhaseInfo
import vn.loi.learning.application.session.bootstrap.SessionTimeline

class InstructionalDecisionEngineTest {

    private val engine = InstructionalDecisionEngine()
    private val planner = ProductBrainPlanner()

    @Test
    fun `Rule 1 CORRECT and fast increases difficulty`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-01",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.CORRECT,
            userAttempt = "correct answer",
            attemptLatencyMs = 1200L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 1)

        assertEquals(AdaptiveAction.INCREASE_DIFFICULTY, decision.action)
        assertEquals(2, decision.newDifficultyLevel)
        assertTrue(trace.rulesTriggered.contains("FAST_CORRECT_MASTERY_BOOST"))
        assertEquals(evidence.evidenceId, trace.evidenceId)
    }

    @Test
    fun `Rule 2 INCORRECT decreases difficulty`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-02",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.INCORRECT,
            userAttempt = "wrong answer",
            attemptLatencyMs = 2500L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 3)

        assertEquals(AdaptiveAction.DECREASE_DIFFICULTY, decision.action)
        assertEquals(2, decision.newDifficultyLevel)
        assertTrue(trace.rulesTriggered.contains("INCORRECT_SCAFFOLDING_SUPPORT"))
    }

    @Test
    fun `Rule 3 PARTIAL repeats similar scene`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-03",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.PARTIAL,
            userAttempt = "typo answer",
            attemptLatencyMs = 1800L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 2)

        assertEquals(AdaptiveAction.REPEAT_SIMILAR_SCENE, decision.action)
        assertEquals(2, decision.newDifficultyLevel)
        assertTrue(trace.rulesTriggered.contains("PARTIAL_CONSOLIDATION_RETRY"))
    }

    @Test
    fun `Rule 4 CORRECT and nominal pace maintains pace`() {
        val evidence = LearningEvidence(
            evidenceId = "ev-04",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.CORRECT,
            userAttempt = "correct answer",
            attemptLatencyMs = 3500L
        )

        val (decision, trace) = engine.evaluate(evidence, currentDifficulty = 2)

        assertEquals(AdaptiveAction.MAINTAIN_PACE, decision.action)
        assertEquals(2, decision.newDifficultyLevel)
        assertTrue(trace.rulesTriggered.contains("NOMINAL_PROGRESSION"))
    }

    @Test
    fun `ProductBrainPlanner evaluateAndAdapt updates session timeline and produces outcome`() {
        val initialTimeline = SessionTimeline(
            phases = listOf(
                SessionPhaseInfo(SessionPhase.WARM_UP, "Warm Up", 1, 1),
                SessionPhaseInfo(SessionPhase.PRACTICE, "Practice", 6, 5)
            ),
            totalEstimatedMinutes = 7
        )

        val evidence = LearningEvidence(
            evidenceId = "ev-05",
            sceneId = "scene-01",
            learningItemId = "item-01",
            learnerId = "learner-01",
            performance = EvidencePerformance.CORRECT,
            userAttempt = "fast answer",
            attemptLatencyMs = 1000L
        )

        val outcome = planner.evaluateAndAdapt(evidence, initialTimeline, currentDifficulty = 1)

        assertNotNull(outcome)
        assertEquals(AdaptiveAction.INCREASE_DIFFICULTY, outcome.decision.action)
        assertEquals(2, outcome.newDifficultyLevel)
        assertTrue(outcome.updatedTimeline.totalEstimatedMinutes < initialTimeline.totalEstimatedMinutes)
    }
}
