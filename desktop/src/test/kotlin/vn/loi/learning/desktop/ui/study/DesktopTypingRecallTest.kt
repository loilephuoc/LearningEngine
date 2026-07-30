package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `typing after empty clears feedback and enters automatic success without reveal`() {
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
        kotlin.test.assertTrue(correct.state.automaticSuccessRequested)
        assertFalse(correct.shouldRevealAnswer)
    }

    @Test
    fun `incorrect Check preserves input for correction and does not reveal`() {
        val outcome = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = "different"),
                TypingRecallPrompt("Answer"),
                TypingAnswerEvaluator()
            )
        )

        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, outcome.state.evaluation?.status)
        assertFalse(outcome.shouldRevealAnswer)
        assertEquals("different", outcome.state.input)
        kotlin.test.assertTrue(
            TypingRecallInteraction.submit(outcome.state, TypingRecallPrompt("Answer"), TypingAnswerEvaluator()) != null
        )
    }

    @Test
    fun `completed attempt freezes duplicate submission and busy action blocks submission`() {
        val prompt = TypingRecallPrompt("Answer")
        val evaluator = TypingAnswerEvaluator()
        val state = TypingRecallUiState(itemId = "item", input = "Answer")
        val first = requireNotNull(
            TypingRecallInteraction.submit(state, prompt, evaluator)
        )
        val repeated = TypingRecallInteraction.submit(first.state, prompt, evaluator)

        assertFalse(first.shouldRevealAnswer)
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

    @Test
    fun `reveal evaluates the latest current snapshot without requiring Check`() {
        val prompt = TypingRecallPrompt("Answer")
        val evaluator = TypingAnswerEvaluator()
        val stale =
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = "old"),
                prompt,
                evaluator
            )!!.state
        val edited = stale.copy(input = "newest")

        val revealed = TypingRecallInteraction.evaluateForReveal(edited, prompt, evaluator)

        assertEquals("newest", revealed.evaluation?.originalAnswer)
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, revealed.evaluation?.status)
        assertFalse(revealed.automaticSuccessRequested)
    }

    @Test
    fun `manual reveal of a correct snapshot does not request automatic success`() {
        val revealed =
            TypingRecallInteraction.evaluateForReveal(
                TypingRecallUiState(itemId = "item", input = "Answer"),
                TypingRecallPrompt("Answer"),
                TypingAnswerEvaluator()
            )

        assertEquals(TypingAnswerEvaluationStatus.CORRECT, revealed.evaluation?.status)
        assertFalse(revealed.automaticSuccessRequested)
    }
}
