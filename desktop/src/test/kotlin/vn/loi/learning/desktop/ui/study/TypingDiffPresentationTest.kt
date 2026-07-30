package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

class TypingDiffPresentationTest {
    private val evaluator = TypingAnswerEvaluator()

    @Test
    fun `live presentation marks typed suffix without exposing expected answer`() {
        val evaluation = evaluator.evaluate(TypingRecallPrompt("hill"), "hilsge")
        val live = assertNotNull(resolveTypingLiveDiff("hilsge", evaluation))

        assertEquals("hil", live.correctPrefix)
        assertEquals("sge", live.incorrectRemainder)
        assertEquals(3, live.firstMismatchIndex)
        assertFalse(live.toString().contains("hill"))
    }

    @Test
    fun `missing character exposes only a boundary marker`() {
        val evaluation = evaluator.evaluate(TypingRecallPrompt("hill"), "hil")
        val live = assertNotNull(resolveTypingLiveDiff("hil", evaluation))

        assertTrue(live.missingCharacterAtBoundary)
        assertEquals("", live.incorrectRemainder)
    }

    @Test
    fun `revealed comparison preserves exact user and expected display strings`() {
        val evaluation = evaluator.evaluate(TypingRecallPrompt("Híll"), " hilsge ")
        val revealed =
            assertNotNull(resolveTypingRevealComparison(evaluation, "Your answer", "Correct answer"))

        assertEquals(" hilsge ", revealed.userAnswer)
        assertEquals("Híll", revealed.correctAnswer)
        assertTrue(revealed.differences.isNotEmpty())
    }

    @Test
    fun `correct or absent evaluation does not create error comparison`() {
        assertNull(resolveTypingRevealComparison(null, "Your answer", "Correct answer"))
        assertNull(
            resolveTypingRevealComparison(
                evaluator.evaluate(TypingRecallPrompt("hill"), "HILL"),
                "Your answer",
                "Correct answer"
            )
        )
    }
}
