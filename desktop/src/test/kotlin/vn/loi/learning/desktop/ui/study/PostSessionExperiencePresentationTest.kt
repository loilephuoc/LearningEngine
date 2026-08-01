package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostSessionExperiencePresentationTest {
    @Test
    fun `New and Review targets use distinct existing semantic tokens in both layouts`() {
        val source = source("StudyStatisticsDashboard.kt")
        assertTrue(source.contains("StudyHeaderMetricType.NEW -> colors.metricGreen"))
        assertTrue(source.contains("StudyHeaderMetricType.REVIEW -> colors.metricBlue"))
        assertTrue(source.contains("color = targetColor"))
        assertTrue(source.contains("color = activeColor"))
        assertFalse(source.contains("color = LETheme.colors.metricPurple"))
    }

    @Test
    fun `completion card renders shared learning actions in balanced rows with one guard`() {
        val source = source("SessionCompletionCard.kt")
        assertTrue(source.contains("completionUiState.learningActions.chunked(2)"))
        assertTrue(source.contains("onLearningAction(action.action)"))
        assertTrue(source.contains("enabled = ranked.enabled"))
        assertTrue(source.contains("label = action.label"))
    }

    @Test
    fun `Desktop delegates replay without reading queue persistence`() {
        val facade = source("StudyFacade.kt")
        val viewModel = source("StudyViewModel.kt")
        assertTrue(facade.contains("applicationContext.engine.replayCompletedStudySession("))
        assertTrue(viewModel.contains("facade.replayCompletedStudySession()"))
        assertFalse(facade.substringAfter("fun replayCompletedStudySession()")
            .substringBefore("fun startLessonStudy(").contains("studyQueueRepository"))
    }

    private fun source(name: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
}
