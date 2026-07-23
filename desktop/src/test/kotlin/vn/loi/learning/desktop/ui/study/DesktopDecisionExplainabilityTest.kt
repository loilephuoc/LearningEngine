package vn.loi.learning.desktop.ui.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopDecisionExplainabilityTest {

    @Test
    fun `Facade through ViewModel preserves explanation while learner hides and shows it`() {
        val facade = StudyFacade(LearningApplicationFactory.createInMemory())
        val viewModel = StudyViewModel(facade)

        viewModel.bootstrapSessionOverview("Topic: Medical Physics")
        viewModel.startFirstScene("Question 1", "Answer 1")
        viewModel.submitSceneAttempt("Answer 1", 1100L)

        val explanation = viewModel.uiState.lastDecisionExplanation
        assertNotNull(explanation)
        assertTrue(viewModel.uiState.isDecisionExplanationVisible)
        assertEquals(
            viewModel.uiState.lastAdaptiveDecision?.decisionId,
            explanation?.decisionId
        )
        assertTrue(explanation!!.observation.contains("1100ms"))

        viewModel.hideDecisionExplanation()
        assertFalse(viewModel.uiState.isDecisionExplanationVisible)
        assertEquals(explanation, viewModel.uiState.lastDecisionExplanation)

        viewModel.showDecisionExplanation()
        assertTrue(viewModel.uiState.isDecisionExplanationVisible)
        assertEquals(explanation, viewModel.uiState.lastDecisionExplanation)

        viewModel.toggleDecisionExplanationVisibility()
        assertFalse(viewModel.uiState.isDecisionExplanationVisible)
        assertEquals(explanation, viewModel.uiState.lastDecisionExplanation)

        viewModel.toggleDecisionExplanationVisibility()
        assertTrue(viewModel.uiState.isDecisionExplanationVisible)
        assertEquals(explanation, viewModel.uiState.lastDecisionExplanation)
    }
}
