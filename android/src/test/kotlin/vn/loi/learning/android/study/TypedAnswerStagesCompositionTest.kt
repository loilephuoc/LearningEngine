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
    private val viewModel = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyViewModel.kt")
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
        assertTrue(modes.contains("typedModeMediaRole(false, feedbackVisible, imeVisible)"))
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

    @Test
    fun `Typing success is audio gated and wrong state owns one nearby Reveal action`() {
        val typing = modes.substringAfter("internal fun TypingStudyStage(")
            .substringBefore("private fun formatTypingSeconds")
        val activeTyping = typing.substringBefore("internal fun TypingDifferenceComparison(")
        val genericFeedback = screen.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")

        assertTrue(screen.contains("TypingSuccessAudioCompleted"))
        assertTrue(screen.contains("isLooping = false"))
        assertTrue(modes.contains("SelectTypingRatingOverride"))
        assertTrue(typing.contains("onReveal = { onEvent(AndroidStudyEvent.Reveal(currentInput)) }"))
        assertTrue(modes.contains("OutlinedButton("))
        assertTrue(modes.contains("Text(\"Reveal answer\")"))
        assertTrue(genericFeedback.contains("is AndroidStudyState.ExampleCompletion -> true"))
        assertFalse(genericFeedback.contains("is AndroidStudyState.Typing, is AndroidStudyState.ExampleCompletion -> true"))
        assertFalse(activeTyping.contains("TypingDifferenceComparison("))
        val frontTyping = activeTyping.substringAfter("} else {\n        if (!feedbackVisible)")
        assertFalse(frontTyping.substringBefore("feedbackContent()").contains("answerContract.canonicalAnswer"))
        assertTrue(typing.contains("StudyRatingBar("))
        assertTrue(typing.contains("selectedRating = state.manualRating"))
        assertTrue(typing.contains("TypingInputActions("))
        assertTrue(typing.contains("modifier = Modifier.bringIntoViewRequester(stableActionsRequester)"))
        assertFalse(typing.contains("stableActionsRequester.bringIntoView()"))
        assertTrue(typing.contains("fillViewport = true"))
        assertTrue(modes.contains("Modifier.fillMaxSize().verticalScroll(rememberScrollState())"))
        assertFalse(typing.contains("Spacer("))
        assertFalse(typing.contains("state.pronunciation"))
        assertFalse(genericFeedback.substringAfter("if (typingSuccessPending)").substringBefore("} else {").contains("StudyRatingBar("))
        assertTrue(typing.contains("if (state.completionPending)"))
        assertTrue(typing.contains("englishExample = null"))
        assertTrue(typing.contains("pronunciation = null"))
        assertFalse(typing.substringAfter("if (state.completionPending)").substringBefore("} else {").contains("feedbackContent()"))
        assertTrue(viewModel.contains("delay(AndroidTypingSuccessPresentationPolicy.minimumDwellMillis)"))
        assertTrue(viewModel.contains("typingSuccessReady("))
        assertTrue(screen.contains("typingLeadContent = if (state is AndroidStudyState.Typing && state.revealed)"))
        assertTrue(screen.indexOf("TypingDifferenceComparison(state.answer") < screen.indexOf("StudyMedia(\n                            state.resolvedImage"))
        assertTrue(genericFeedback.contains("typingLeadContent?.invoke()"))
        assertTrue(genericFeedback.indexOf("typingLeadContent?.invoke()") < genericFeedback.indexOf("StudyAnswerSection("))
        assertTrue(typing.contains("if (!feedbackVisible)"))
        assertTrue(typing.contains("if (!state.revealed)"))
        assertEquals(1, Regex("partOfSpeech = \\(state as\\? AndroidStudyState\\.Typing\\)").findAll(genericFeedback).count())
    }

    @Test
    fun `revealed Typing comparison is a two-line annotated hero without visual labels`() {
        val comparison = modes.substringAfter("internal fun TypingDifferenceComparison(")
            .substringBefore("private fun formatTypingSeconds")

        assertEquals(2, Regex("Text\\(\\s+androidTypingComparisonAnnotatedText\\(").findAll(comparison).count())
        assertFalse(comparison.contains("Text(\"Your answer\""))
        assertFalse(comparison.contains("Text(\"Expected\""))
        assertTrue(comparison.contains("HorizontalDivider("))
        assertTrue(comparison.contains("TextDecoration.LineThrough"))
        assertTrue(comparison.contains("TextDecoration.Underline"))
        assertTrue(comparison.contains("offsetByCodePoints"))
        assertTrue(comparison.contains("clearAndSetSemantics"))
    }

    @Test
    fun `READY and wrong Typing share one stable action skeleton`() {
        val actions = modes.substringAfter("private fun TypingInputActions(")
            .substringBefore("@Composable\ninternal fun TypingDifferenceComparison(")

        assertTrue(actions.contains("heightIn(min = 24.dp)"))
        assertTrue(actions.contains("if (showRetry)"))
        assertTrue(actions.contains("Text(\"Retry\")"))
        assertTrue(actions.contains("Text(\"Check\")"))
        assertTrue(actions.contains("Text(\"Reveal answer\")"))
        assertEquals(2, Regex("Row\\(").findAll(actions).count())
    }
}
