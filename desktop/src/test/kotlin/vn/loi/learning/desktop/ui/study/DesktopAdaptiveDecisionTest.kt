package vn.loi.learning.desktop.ui.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.decision.AdaptiveAction
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.application.session.bootstrap.SessionPhase
import vn.loi.learning.application.session.bootstrap.SessionPhaseInfo
import vn.loi.learning.application.session.bootstrap.SessionTimeline

class DesktopAdaptiveDecisionTest {

    private val planner = ProductBrainPlanner()

    @Test
    fun `Desktop UI state projects adaptive decision decision trace and updated timeline`() {
        val initialTimeline = SessionTimeline(
            phases = listOf(
                SessionPhaseInfo(SessionPhase.WARM_UP, "Warm Up", 1, 1),
                SessionPhaseInfo(SessionPhase.PRACTICE, "Practice", 6, 5)
            ),
            totalEstimatedMinutes = 7
        )

        val overview = planner.bootstrapSession(
            learnerId = "desktop-learner-01",
            topicId = "Topic: Medical Physics",
            itemCount = 5
        )

        val scene = planner.selectFirstScene("Question 1", "Answer 1")
        val result = scene.evaluate("Answer 1", 1100L)
        val evidence = scene.toEvidence(result, "desktop-learner-01", "item-01")

        val outcome = planner.evaluateAndAdapt(evidence, overview.timeline, currentDifficulty = 1)

        val updatedUiState = StudyUiState(
            studyTitle = "Topic: Medical Physics",
            sessionOverview = overview.copy(timeline = outcome.updatedTimeline),
            lastSceneResult = result,
            lastLearningEvidence = evidence,
            lastAdaptiveDecision = outcome.decision,
            lastDecisionTrace = outcome.trace,
            currentDifficultyLevel = outcome.newDifficultyLevel
        )

        assertNotNull(updatedUiState.lastAdaptiveDecision)
        assertEquals(AdaptiveAction.INCREASE_DIFFICULTY, updatedUiState.lastAdaptiveDecision?.action)
        assertEquals(2, updatedUiState.currentDifficultyLevel)
        assertNotNull(updatedUiState.lastDecisionTrace)
        assertTrue(updatedUiState.lastDecisionTrace!!.rulesTriggered.contains("FAST_CORRECT_MASTERY_BOOST"))
    }
}
