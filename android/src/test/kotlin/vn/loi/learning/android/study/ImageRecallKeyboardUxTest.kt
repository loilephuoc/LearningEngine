package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.android.study.modes.shouldDismissImageRecallImeOnSubmit

class ImageRecallKeyboardUxTest {
    private val imageStage = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/ImageRecallStage.kt")
    )
    private val typedStages = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
    )

    @Test
    fun `incorrect submit dismisses focus and IME while live typo alone does not`() {
        assertTrue(shouldDismissImageRecallImeOnSubmit(TypingAnswerEvaluationStatus.INCORRECT))
        assertFalse(shouldDismissImageRecallImeOnSubmit(TypingAnswerEvaluationStatus.EMPTY))
        assertFalse(shouldDismissImageRecallImeOnSubmit(TypingAnswerEvaluationStatus.CORRECT))

        val submit = imageStage.substringAfter("val submitImageRecall:")
            .substringBefore("if (state.completionPending")
        assertTrue(submit.contains("onEvent(AndroidStudyEvent.Submit(answer))"))
    }

    @Test
    fun `Retry recreates focused input and requests IME while Reveal only dismisses`() {
        val actions = imageStage.substringAfter("TypingInputActions(")
            .substringBefore("\n            )")
        assertTrue(actions.contains("inputFocusGeneration++"))
        assertTrue(actions.contains("onEvent(AndroidStudyEvent.Retry)"))
        assertTrue(imageStage.contains("withFrameNanos { }"))
        assertTrue(imageStage.contains("keyboardController?.show()"))

        val reveal = actions.substringAfter("onReveal = {")
        assertTrue(reveal.contains("focusManager.clearFocus()"))
        assertTrue(reveal.contains("keyboardController?.hide()"))
        assertFalse(reveal.contains("keyboardController?.show()"))
    }

    @Test
    fun `correct compact success remains render gated and unchanged`() {
        val success = imageStage.substringAfter(
            "if (state.completionPending && state.outcome == RecallOutcome.CORRECT && !state.revealed)"
        ).substringBefore("if (state.completed || state.revealed)")
        assertTrue(success.contains("TypedCompactSuccess("))
        assertTrue(success.contains("typedResultPresentation(state)"))
        val shared = typedStages.substringAfter("internal fun TypedCompactSuccess(")
            .substringBefore("internal fun TypingStudyStage(")
        assertTrue(shared.contains("SideEffect(onRendered)"))
        assertTrue(shared.contains("StudyMedia("))
        assertTrue(shared.contains("StudyAnswerSection("))
        assertTrue(shared.contains("resolveTypingRatingTransition("))
    }
}
