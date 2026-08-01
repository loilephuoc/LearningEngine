package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContinuousReviewDesktopWiringTest {
    @Test
    fun `desktop startup consumes typed application recovery without scanning persistence`() {
        val facade = source("StudyFacade.kt")
        val start = facade.indexOf("private fun restoreActiveSession(")
        val end = facade.indexOf("private fun restoreLatestUndoableCompletion", start)
        val recovery = facade.substring(start, end)

        assertTrue(recovery.contains("engine.recoverContinuousReview("))
        assertTrue(recovery.contains("ContinuousReviewRecoveryResult.ResumedExisting"))
        assertTrue(recovery.contains("ContinuousReviewRecoveryResult.Continued"))
        assertTrue(recovery.contains("ContinuousReviewRecoveryResult.NoWork"))
        assertTrue(recovery.contains("ContinuousReviewRecoveryResult.Rejected"))
        assertFalse(recovery.contains("studySessionRepository"))
        assertFalse(recovery.contains("studyQueueRepository"))
    }

    @Test
    fun `view model exposes guarded enable and disable actions through facade`() {
        val source = source("StudyViewModel.kt")
        val screen = source("StudyScreen.kt")
        val completion = source("SessionCompletionCard.kt")
        val strings = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
        )
        assertTrue(source.contains("fun enableContinuousReview() = updateSafely("))
        assertTrue(source.contains("facade.enableContinuousReview()"))
        assertTrue(source.contains("fun disableContinuousReview() = updateSafely("))
        assertTrue(source.contains("facade.disableContinuousReview()"))
        assertTrue(screen.contains("continuousReviewEnabled = uiState.continuousReviewEnabled"))
        assertTrue(screen.contains("onContinuousReviewChanged = { enabled ->"))
        assertTrue(screen.contains("continuousReviewAvailable = !uiState.isLessonStudy"))
        assertTrue(completion.contains("checked = continuousReviewEnabled"))
        assertTrue(completion.contains("workspaceStrings.continuousReviewAccessibility"))
        assertTrue(completion.contains("onCheckedChange = onContinuousReviewChanged"))
        assertTrue(strings.contains("continuousReviewLabel"))
        assertTrue(strings.contains("continuousReviewAccessibility"))
    }

    private fun source(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
    )
}
