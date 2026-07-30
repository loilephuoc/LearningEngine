package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypingAnswerEvaluatorDiffTest {
    private val evaluator = TypingAnswerEvaluator()

    @Test
    fun `exact answer preserves originals and contains only matches`() {
        val result = evaluator.evaluate(TypingRecallPrompt("Hill"), "Hill")

        assertTrue(result.isCorrect)
        assertEquals("Hill", result.originalAnswer)
        assertEquals("Hill", result.originalExpectedAnswer)
        assertNull(result.firstMismatchIndex)
        assertTrue(result.differences.all { it.kind == TypingDifferenceKind.MATCH })
    }

    @Test
    fun `replacement insertion deletion and suffix are classified`() {
        assertEquals(
            listOf(
                TypingDifferenceKind.MATCH,
                TypingDifferenceKind.REPLACEMENT,
                TypingDifferenceKind.MATCH,
                TypingDifferenceKind.MATCH
            ),
            evaluator.evaluate(TypingRecallPrompt("hill"), "hall").differences.map { it.kind }
        )
        assertTrue(
            TypingDifferenceKind.INSERTION in
                evaluator.evaluate(TypingRecallPrompt("hill"), "hiXll").differences.map { it.kind }
        )
        assertTrue(
            TypingDifferenceKind.DELETION in
                evaluator.evaluate(TypingRecallPrompt("hill"), "hil").differences.map { it.kind }
        )
        assertEquals(
            TypingDifferenceKind.INSERTION,
            evaluator.evaluate(TypingRecallPrompt("hill"), "hills").differences.last().kind
        )
    }

    @Test
    fun `first mismatch and correct prefix are code point based`() {
        val result = evaluator.evaluate(TypingRecallPrompt("hill"), "hilsge")

        assertEquals(3, result.correctPrefixLength)
        assertEquals(3, result.firstMismatchIndex)
    }

    @Test
    fun `normalization handles case whitespace Vietnamese and punctuation safely`() {
        assertTrue(
            evaluator.evaluate(TypingRecallPrompt("  Xin   Chào  "), "xin chào").isCorrect
        )
        assertTrue(
            evaluator.evaluate(TypingRecallPrompt("café"), "cafe\u0301").isCorrect
        )
        assertTrue(
            evaluator.evaluate(TypingRecallPrompt("mother-in-law"), "MOTHER-IN-LAW").isCorrect
        )
        assertTrue(
            evaluator.evaluate(TypingRecallPrompt("don't stop"), "DON'T   STOP").isCorrect
        )
    }

    @Test
    fun `emoji is one comparison unit rather than split surrogate halves`() {
        val result = evaluator.evaluate(TypingRecallPrompt("a🙂b"), "a🙃b")

        assertEquals(3, result.differences.size)
        assertEquals(TypingDifferenceKind.REPLACEMENT, result.differences[1].kind)
    }
}
