package vn.loi.learning.application.learningexperience

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingAnswerEvaluatorTest {
    private val evaluator = TypingAnswerEvaluator()
    private val prompt = TypingRecallPrompt("Xin chào, thế giới!")

    @Test
    fun `exact case and surrounding whitespace matches are correct`() {
        assertCorrect("Xin chào, thế giới!")
        assertCorrect("XIN CHÀO, THẾ GIỚI!")
        assertCorrect("  Xin chào, thế giới!  ")
    }

    @Test
    fun `internal whitespace and line breaks collapse deterministically`() {
        assertCorrect("Xin   chào,\n\tthế giới!")
    }

    @Test
    fun `punctuation diacritics and word order remain meaningful`() {
        assertIncorrect("Xin chào thế giới")
        assertIncorrect("Xin chao, the gioi!")
        assertIncorrect("thế giới! Xin chào,")
    }

    @Test
    fun `blank input is empty`() {
        val result = evaluator.evaluate(prompt, " \n\t ")

        assertEquals(TypingAnswerEvaluationStatus.EMPTY, result.status)
        assertFalse(result.isCorrect)
        assertFalse(result.isCompletedAttempt)
        assertEquals("", result.normalizedAnswer)
    }

    @Test
    fun `evaluation is repeatable and locale stable`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val first = evaluator.evaluate(TypingRecallPrompt("TITLE"), "title")
            val second = evaluator.evaluate(TypingRecallPrompt("TITLE"), "title")

            assertEquals(first, second)
            assertTrue(first.isCorrect)
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun assertCorrect(answer: String) {
        val result = evaluator.evaluate(prompt, answer)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, result.status)
        assertTrue(result.isCorrect)
        assertTrue(result.isCompletedAttempt)
    }

    private fun assertIncorrect(answer: String) {
        val result = evaluator.evaluate(prompt, answer)
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, result.status)
        assertFalse(result.isCorrect)
        assertTrue(result.isCompletedAttempt)
    }
}
