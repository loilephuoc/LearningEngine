package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

class DesktopTypingRecallTest {
    @Test
    fun `interaction resets on real item identity and isolates switched-away input`() {
        val first = TypingRecallUiState(itemId = "item-1", input = "draft")

        assertEquals(
            TypingRecallUiState(itemId = "item-2"),
            TypingRecallInteraction.initial("item-2")
        )
        assertEquals(
            TypingRecallUiState(itemId = "item-1"),
            TypingRecallInteraction.initial(first.itemId)
        )
    }

    @Test
    fun `empty and whitespace submissions do not complete or reveal`() {
        val evaluator = TypingAnswerEvaluator()
        val prompt = TypingRecallPrompt("Answer")
        val empty = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item"),
                prompt,
                evaluator
            )
        )
        val whitespace = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = " \n\t "),
                prompt,
                evaluator
            )
        )

        assertEquals(TypingAnswerEvaluationStatus.EMPTY, empty.state.evaluation?.status)
        assertFalse(empty.shouldRevealAnswer)
        assertEquals(empty, TypingRecallInteraction.submit(empty.state, prompt, evaluator))
        assertEquals(TypingAnswerEvaluationStatus.EMPTY, whitespace.state.evaluation?.status)
        assertFalse(whitespace.shouldRevealAnswer)
    }

    @Test
    fun `typing after empty clears feedback and can complete a correct attempt`() {
        val evaluator = TypingAnswerEvaluator()
        val prompt = TypingRecallPrompt("Answer")
        val empty = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item"),
                prompt,
                evaluator
            )
        )

        val updated = TypingRecallInteraction.updateInput(empty.state, " answer ")
        val correct = requireNotNull(
            TypingRecallInteraction.submit(updated, prompt, evaluator)
        )

        assertNull(updated.evaluation)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, correct.state.evaluation?.status)
        assertTrue(correct.shouldRevealAnswer)
    }

    @Test
    fun `incorrect non-empty submission completes and reveals`() {
        val outcome = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = "different"),
                TypingRecallPrompt("Answer"),
                TypingAnswerEvaluator()
            )
        )

        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, outcome.state.evaluation?.status)
        assertTrue(outcome.shouldRevealAnswer)
    }

    @Test
    fun `completed attempt reveals only once and busy action blocks submission`() {
        val prompt = TypingRecallPrompt("Answer")
        val evaluator = TypingAnswerEvaluator()
        val state = TypingRecallUiState(itemId = "item", input = "Answer")
        var revealCount = 0

        val first = requireNotNull(
            TypingRecallInteraction.submit(state, prompt, evaluator)
        )
        if (first.shouldRevealAnswer) revealCount++
        val repeated = TypingRecallInteraction.submit(first.state, prompt, evaluator)
        if (repeated?.shouldRevealAnswer == true) revealCount++

        assertEquals(1, revealCount)
        assertNull(repeated)
        assertNull(
            TypingRecallInteraction.submit(
                state,
                prompt,
                evaluator,
                actionInProgress = true
            )
        )
    }

}
