package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingFirstPresentationTest {
    @Test
    fun `typing input presentation stays large and responsive`() {
        val wide = TypingPresentationResolver.input(StudyViewportClass.WIDE)
        val standard = TypingPresentationResolver.input(StudyViewportClass.STANDARD)
        val compact = TypingPresentationResolver.input(StudyViewportClass.COMPACT)

        assertTrue(wide.minimumHeightDp in 72..96)
        assertTrue(standard.minimumHeightDp in 72..96)
        assertTrue(compact.minimumHeightDp in 72..96)
        assertTrue(wide.fontSizeSp > compact.fontSizeSp)
        assertEquals(1f, compact.revealWidthFraction)
        assertTrue(wide.revealWidthFraction < compact.revealWidthFraction)
    }

    @Test
    fun `success overlay scales long canonical English without changing its text`() {
        val short = "to sign"
        val long = "Where is the nearest hospital?"
        val shortPresentation =
            TypingPresentationResolver.successOverlay(StudyViewportClass.WIDE, short)
        val longPresentation =
            TypingPresentationResolver.successOverlay(StudyViewportClass.WIDE, long)
        val compact =
            TypingPresentationResolver.successOverlay(StudyViewportClass.COMPACT, long)

        assertTrue(longPresentation.answerFontSizeSp < shortPresentation.answerFontSizeSp)
        assertTrue(compact.answerFontSizeSp <= longPresentation.answerFontSizeSp)
        assertEquals("Where is the nearest hospital?", long)
        assertTrue(compact.horizontalMarginDp > 0)
    }

    @Test
    fun `focus is requested only while typing remains editable`() {
        assertTrue(shouldRequestTypingInputFocus(enabled = true, successInProgress = false))
        assertFalse(shouldRequestTypingInputFocus(enabled = false, successInProgress = false))
        assertFalse(shouldRequestTypingInputFocus(enabled = true, successInProgress = true))
    }

    @Test
    fun `typing front hides only manual primary audio interaction`() {
        assertFalse(
            shouldRenderManualSceneAudio(
                PresentedAudioRole.PRIMARY_WORD,
                allowPrimaryAudioInteraction = false
            )
        )
        assertTrue(
            shouldRenderManualSceneAudio(
                PresentedAudioRole.MEANING_TRANSLATION,
                allowPrimaryAudioInteraction = false
            )
        )
        assertTrue(
            shouldRenderManualSceneAudio(
                PresentedAudioRole.PRIMARY_WORD,
                allowPrimaryAudioInteraction = true
            )
        )
    }

    @Test
    fun `production input retains editable behavior and keyed autofocus`() {
        val source = studySource("StudyScreen.kt")
        val start = source.indexOf("private fun TypingRecallInput(")
        val end = source.indexOf("private fun TypingSuccessFocusOverlay(", start)
        val input = source.substring(start, end)

        assertTrue(input.contains("LaunchedEffect(focusIdentity, enabled)"))
        assertTrue(input.contains("typingLiveDiffVisualTransformation("))
        assertTrue(input.contains("singleLine = false"))
        assertTrue(input.contains("minLines = 2"))
        assertTrue(input.contains("maxLines = 5"))
        assertTrue(input.contains(".heightIn("))
        assertTrue(input.contains("RoundedCornerShape(16.dp)"))
        assertTrue(input.contains("Shortcut: Enter"))
        assertFalse(input.contains("TextButton("))
    }

    @Test
    fun `success overlay is root level canonical presentation before existing orchestration`() {
        val source = studySource("StudyScreen.kt")
        val effect = source.substring(
            source.indexOf("LaunchedEffect(\n        uiState.currentLearningItemId,\n        typingState.successInProgress"),
            source.indexOf("LaunchedEffect(\n        focusTransitionKey")
        )
        val overlay = source.substring(
            source.indexOf("private fun TypingSuccessFocusOverlay("),
            source.indexOf("private fun TypingEvaluationFeedback(")
        )

        assertTrue(source.contains("(learningScene as? TypingScene)?.prompt?.expectedAnswer"))
        assertTrue(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("1.04f at 280"))
        assertTrue(effect.indexOf("withFrameNanos") < effect.indexOf("awaitTypingAnswerAudio"))
        assertTrue(overlay.contains("fillMaxSize()"))
        assertTrue(overlay.contains("LiveRegionMode.Assertive"))
        assertTrue(overlay.contains("\"Correct. \$canonicalAnswer.\""))
        assertTrue(overlay.contains("softWrap = true"))
        assertFalse(overlay.contains("vietnameseMeaning"))
        assertFalse(overlay.contains("onTypingCorrectCompleted"))
    }

    @Test
    fun `success blocks competing shortcuts while preserving cancel paths`() {
        val source = studySource("StudyScreen.kt")
        val start = source.indexOf("fun performKeyboardAction(")
        val end = source.indexOf("BoxWithConstraints(", start)
        val keyboard = source.substring(start, end)

        assertTrue(keyboard.contains("typingSuccessInProgress"))
        assertTrue(keyboard.contains("StudyKeyboardAction.UNDO_LATEST"))
        assertTrue(keyboard.contains("StudyKeyboardAction.PAUSE_WORKSPACE"))
        assertTrue(keyboard.contains("action !in"))
    }

    private fun studySource(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return if (fromRoot.isFile) fromRoot.readText()
        else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name").readText()
    }
}
