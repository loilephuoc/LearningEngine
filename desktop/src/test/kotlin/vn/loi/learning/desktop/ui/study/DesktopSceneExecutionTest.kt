package vn.loi.learning.desktop.ui.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner

class DesktopSceneExecutionTest {

    private val planner = ProductBrainPlanner()

    @Test
    fun `Desktop UI state encapsulates scene execution flow`() {
        val scene = planner.selectFirstScene(
            promptText = "Prompt text",
            expectedAnswer = "Correct Answer"
        )

        val initialState = StudyUiState(
            studyTitle = "Sample Topic",
            activeScene = scene
        )

        assertNotNull(initialState.activeScene)

        val result = scene.evaluate("Correct Answer", 1000L)
        val evidence = scene.toEvidence(result, "learner-01", "item-01")

        val evaluatedState = initialState.copy(
            lastSceneResult = result,
            lastLearningEvidence = evidence
        )

        assertNotNull(evaluatedState.lastSceneResult)
        assertTrue(evaluatedState.lastSceneResult!!.isExactMatch)
        assertEquals(EvidencePerformance.CORRECT, evaluatedState.lastLearningEvidence?.performance)
    }
}
