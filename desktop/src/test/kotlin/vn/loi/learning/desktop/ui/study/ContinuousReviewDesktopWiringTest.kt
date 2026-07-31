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
        assertTrue(source.contains("fun enableContinuousReview() = updateSafely("))
        assertTrue(source.contains("facade.enableContinuousReview()"))
        assertTrue(source.contains("fun disableContinuousReview() = updateSafely("))
        assertTrue(source.contains("facade.disableContinuousReview()"))
    }

    private fun source(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
    )
}
