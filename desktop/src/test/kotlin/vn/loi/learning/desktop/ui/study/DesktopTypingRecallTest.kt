package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

class DesktopTypingRecallTest {
    private val evaluator = TypingAnswerEvaluator()
    private val socks = TypingRecallPrompt("socks")

    @Test
    fun `interaction resets on real item identity and isolates switched-away input`() {
        assertEquals(
            TypingRecallUiState(itemId = "item-2"),
            TypingRecallInteraction.initial("item-2")
        )
    }

    @Test
    fun `every input change evaluates immediately without Check`() {
        val state =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "so",
                socks,
                evaluator
            )

        assertEquals("so", state.liveEvaluation?.originalAnswer)
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, state.liveEvaluation?.status)
        assertFalse(state.automaticSuccessRequested)
        assertFalse(state.explicitIncorrectFeedback)
    }

    @Test
    fun `exact realtime input requests success without submit`() {
        val state =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "socks",
                socks,
                evaluator
            )

        assertEquals(TypingAnswerEvaluationStatus.CORRECT, state.liveEvaluation?.status)
        assertTrue(state.automaticSuccessRequested)
        assertFalse(state.successInProgress)
    }

    @Test
    fun `input change cancels a pending success and clears explicit feedback`() {
        val correct =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "socks",
                socks,
                evaluator
            )
        val changed =
            TypingRecallInteraction.updateInput(correct, "sock", socks, evaluator)

        assertFalse(changed.automaticSuccessRequested)
        assertFalse(changed.successInProgress)
        assertFalse(changed.explicitIncorrectFeedback)
        assertTrue(changed.inputRevision > correct.inputRevision)
    }

    @Test
    fun `same text selection edit does not schedule a duplicate request`() {
        val correct =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "socks",
                socks,
                evaluator
            )
        val selectionOnly =
            TypingRecallInteraction.updateInput(
                correct,
                TextFieldValue("socks", selection = TextRange(2)),
                socks,
                evaluator
            )

        assertEquals(correct.inputRevision, selectionOnly.inputRevision)
        assertEquals(TextRange(2), selectionOnly.selection)
    }

    @Test
    fun `debounce confirmation succeeds only for the current revision`() {
        val pending =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "socks",
                socks,
                evaluator
            )

        assertTrue(
            TypingRecallInteraction.confirmRealtimeSuccess(
                pending,
                pending.inputRevision
            ).successInProgress
        )
        assertFalse(
            TypingRecallInteraction.confirmRealtimeSuccess(
                pending,
                pending.inputRevision - 1
            ).successInProgress
        )
    }

    @Test
    fun `active IME composition prevents automatic success until committed`() {
        val composing =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                TextFieldValue("socks", TextRange(5), TextRange(4, 5)),
                socks,
                evaluator
            )
        val committed =
            TypingRecallInteraction.updateInput(
                composing,
                TextFieldValue("socks", TextRange(5), composition = null),
                socks,
                evaluator
            )

        assertFalse(composing.automaticSuccessRequested)
        assertTrue(committed.automaticSuccessRequested)
    }

    @Test
    fun `incorrect explicit submit preserves input and enables nearby feedback`() {
        val live =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "soaks",
                socks,
                evaluator
            )
        val submitted =
            requireNotNull(TypingRecallInteraction.submit(live, socks, evaluator))

        assertEquals("soaks", submitted.state.input)
        assertTrue(submitted.state.explicitIncorrectFeedback)
        assertFalse(submitted.shouldRevealAnswer)
    }

    @Test
    fun `editing after incorrect submit clears feedback realtime`() {
        val submitted =
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = "soaks"),
                socks,
                evaluator
            )!!.state

        val edited = TypingRecallInteraction.updateInput(submitted, "sock", socks, evaluator)

        assertFalse(edited.explicitIncorrectFeedback)
    }

    @Test
    fun `reveal uses latest raw input and owns a distinct stable evaluation`() {
        val live =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "old",
                socks,
                evaluator
            )
        val edited = live.copy(input = "soaks")

        val revealed = TypingRecallInteraction.evaluateForReveal(edited, socks, evaluator)

        assertEquals("soaks", revealed.revealEvaluation?.originalAnswer)
        assertEquals("soaks", revealed.liveEvaluation?.originalAnswer)
        assertFalse(revealed.automaticSuccessRequested)
        assertFalse(revealed.explicitIncorrectFeedback)
    }

    @Test
    fun `empty reveal has no comparison evaluation content`() {
        val revealed =
            TypingRecallInteraction.evaluateForReveal(
                TypingRecallUiState(itemId = "item"),
                socks,
                evaluator
            )

        assertEquals(TypingAnswerEvaluationStatus.EMPTY, revealed.revealEvaluation?.status)
        assertNull(
            resolveTypingRevealComparison(
                revealed.revealEvaluation,
                "Your answer",
                "Correct answer"
            )
        )
    }

    @Test
    fun `manual correct reveal cancels pending success and remains manual`() {
        val pending =
            TypingRecallInteraction.updateInput(
                TypingRecallUiState(itemId = "item"),
                "socks",
                socks,
                evaluator
            )
        val revealed = TypingRecallInteraction.evaluateForReveal(pending, socks, evaluator)

        assertEquals(TypingAnswerEvaluationStatus.CORRECT, revealed.revealEvaluation?.status)
        assertFalse(revealed.automaticSuccessRequested)
        assertFalse(revealed.successInProgress)
    }

    @Test
    fun `runtime input wiring evaluates through the existing prompt and evaluator`() {
        val source =
            Files.readString(
                Path.of(
                    "src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"
                )
            )

        assertTrue(source.contains("TypingRecallInteraction.updateInput("))
        assertTrue(source.contains("typingScene.prompt,\n                                        TypingAnswerEvaluator()"))
        assertTrue(source.contains("awaitTypingRealtimeSuccessDebounce()"))
    }

    @Test
    fun `post reveal comparison reads item scoped reveal authority without scene type gate`() {
        val source =
            Files.readString(
                Path.of(
                    "src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"
                )
            )
        val comparisonStart = source.indexOf("val typingComparisonPresentation =")
        val comparisonEnd = source.indexOf("val typingComparison:", comparisonStart)
        val comparisonBlock = source.substring(comparisonStart, comparisonEnd)

        assertTrue(comparisonBlock.contains("typingState.revealEvaluation"))
        assertFalse(comparisonBlock.contains("learningScene is TypingScene"))
    }
}
