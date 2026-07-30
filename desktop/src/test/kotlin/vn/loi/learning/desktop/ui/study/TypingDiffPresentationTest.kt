package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.Color
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
    fun `correct incomplete prefix remains neutral`() {
        val evaluation = evaluator.evaluate(TypingRecallPrompt("hill"), "hil")

        assertNull(resolveTypingLiveDiff("hil", evaluation))
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
    fun `correct manual reveal creates coherent comparison while absent evaluation does not`() {
        assertNull(resolveTypingRevealComparison(null, "Your answer", "Correct answer"))
        val correct = assertNotNull(
            resolveTypingRevealComparison(
                evaluator.evaluate(TypingRecallPrompt("hill"), "HILL"),
                "Your answer",
                "Correct answer"
            )
        )
        assertEquals("HILL", correct.userAnswer)
        assertTrue(correct.differences.all { it.kind.name == "MATCH" })
    }

    @Test
    fun `editable visual transformation colors only the real mismatching suffix`() {
        val evaluation = evaluator.evaluate(TypingRecallPrompt("hill"), "hils")
        val transformed =
            typingLiveDiffVisualTransformation(
                evaluation,
                normalColor = Color.Black,
                dangerColor = Color.Red
            ).filter(androidx.compose.ui.text.AnnotatedString("hils"))

        assertEquals("hils", transformed.text.text)
        assertEquals(2, transformed.text.spanStyles.size)
        assertEquals(0, transformed.text.spanStyles[0].start)
        assertEquals(3, transformed.text.spanStyles[0].end)
        assertEquals(Color.Red, transformed.text.spanStyles[1].item.color)
        assertEquals(3, transformed.text.spanStyles[1].start)
        assertEquals(4, transformed.text.spanStyles[1].end)
        assertEquals(2, transformed.offsetMapping.originalToTransformed(2))
    }

    @Test
    fun `editing back to a correct Unicode prefix clears danger presentation`() {
        val prompt = TypingRecallPrompt("tiếng Việt")
        val wrong = evaluator.evaluate(prompt, "tiếng Vị")
        val correctedPrefix = evaluator.evaluate(prompt, "tiếng Vi")

        assertNotNull(resolveTypingLiveDiff("tiếng Vị", wrong))
        assertNull(resolveTypingLiveDiff("tiếng Vi", correctedPrefix))
    }
}
