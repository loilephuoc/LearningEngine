package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingExpectedPrefixErrorTest {
    private val evaluator = TypingAnswerEvaluator()

    @Test
    fun `progressive prefix and remaining suffix are not errors`() {
        val prompt = TypingRecallPrompt("flute player")

        listOf("f", "flu", "flute").forEach {
            assertFalse(evaluator.evaluateExpectedPrefix(prompt, it).hasError)
        }
    }

    @Test
    fun `replacement insertion deletion and extra suffix have deterministic distance`() {
        val prompt = TypingRecallPrompt("flute")

        assertEquals(1, evaluator.evaluateExpectedPrefix(prompt, "fluxe").editDistance)
        assertEquals(1, evaluator.evaluateExpectedPrefix(prompt, "fluute").editDistance)
        assertEquals(2, evaluator.evaluateExpectedPrefix(prompt, "flte").editDistance)
        assertEquals(1, evaluator.evaluateExpectedPrefix(prompt, "flutex").editDistance)
        assertTrue(evaluator.evaluateExpectedPrefix(prompt, "fulte").editDistance > 0)
    }

    @Test
    fun `normalization remains aligned with final evaluator`() {
        val prompt = TypingRecallPrompt("Flute   Player")

        assertEquals(0, evaluator.evaluateExpectedPrefix(prompt, " flute player ").editDistance)
    }
}
