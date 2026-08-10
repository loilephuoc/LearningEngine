package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypedAnswerStagesCompositionTest {
    private val modes = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
    )
    private val screen = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
    )

    @Test
    fun `typed modes reuse one input and feedback foundation`() {
        assertTrue(modes.contains("internal fun TypingStudyStage("))
        assertTrue(modes.contains("internal fun ListeningStudyStage("))
        assertEquals(2, Regex("StudyAnswerInput\\(").findAll(modes).count())
        assertTrue(modes.contains("TypedAnswerStageFrame"))
        assertTrue(screen.contains("StudyAnswerSection("))
    }

    @Test
    fun `typing is IME aware and keeps media subordinate`() {
        assertTrue(modes.contains("WindowInsets.ime.getBottom"))
        assertTrue(modes.contains("typedModeMediaRole(false, feedbackVisible)"))
        assertTrue(modes.contains("availableMediaHeightDp"))
        assertTrue(modes.contains("BringIntoView") || screen.contains("bringIntoViewRequester"))
    }

    @Test
    fun `listening has one replay route and restrained audio state`() {
        val listeningPrompt = modes.substringAfter("internal fun StudyListeningAudioPrompt(")
            .substringBefore("private fun TypedInputActions(")
        assertTrue(modes.contains("StudyListeningAudioPrompt("))
        assertEquals(1, Regex("playAudio\\(AudioRole\\.PROMPT").findAll(
            modes.substringAfter("internal fun ListeningStudyStage(").substringBefore("private fun TypedAnswerStageFrame(")
        ).count())
        assertTrue(listeningPrompt.contains("StudyMotionRole.AUDIO_PULSE"))
        assertTrue(listeningPrompt.contains("if (isPlaying && !reducedMotion)"))
        assertFalse(modes.contains("Snackbar"))
        assertFalse(modes.contains("Dialog("))
    }

    @Test
    fun `StudyScreen delegates typed mode presentation`() {
        assertTrue(screen.contains("TypingStudyStage("))
        assertTrue(screen.contains("ListeningStudyStage("))
        assertFalse(screen.contains("private fun StudyModeInputArea("))
        assertFalse(screen.contains("private fun StudyPromptHeader("))
    }
}
