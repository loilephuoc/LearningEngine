package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManualEvaluationDesktopWiringTest {
    @Test
    fun `evaluative action is labeled in fixed chrome and explicit front request reveals before dialog`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt")
        )
        val chrome = source.substringAfter("private fun ActiveSessionChrome(")
            .substringBefore("private fun StudyChromeIconAction(")
        val screen = source.substringBefore("private const val TYPING_SUCCESS_AFTER_AUDIO")

        assertTrue(chrome.contains("ManualEvaluationAvailability.AVAILABLE"))
        assertTrue(chrome.contains("Text(\"Đánh giá thủ công\")"))
        assertFalse(chrome.contains("LEIcons.More"))
        assertTrue(screen.contains("manualEvaluationPendingReveal = true"))
        assertTrue(screen.contains("onRevealAnswer()"))
        assertTrue(screen.contains("uiState.canReview"))
        assertTrue(screen.contains("onManualEvaluation(selected)"))
    }
}
