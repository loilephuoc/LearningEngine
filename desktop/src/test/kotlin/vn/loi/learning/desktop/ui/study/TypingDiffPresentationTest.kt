package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingDifferenceKind
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

class TypingDiffPresentationTest {
    private val evaluator = TypingAnswerEvaluator()
    private val prompt = TypingRecallPrompt("socks")

    @Test
    fun `s so soc and sock against socks are all neutral`() {
        listOf("s", "so", "soc", "sock").forEach { input ->
            assertNull(resolveTypingLiveDiff(input, evaluator.evaluate(prompt, input)), input)
        }
    }

    @Test
    fun `exact socks has no danger span`() {
        val evaluation = evaluator.evaluate(prompt, "socks")
        val transformed =
            typingLiveDiffVisualTransformation(evaluation, Color.Red)
                .filter(AnnotatedString("socks"))

        assertNull(resolveTypingLiveDiff("socks", evaluation))
        assertTrue(transformed.text.spanStyles.none { it.item.color == Color.Red })
    }

    @Test
    fun `soaks against socks highlights only replacement a`() {
        val evaluation = evaluator.evaluate(prompt, "soaks")
        val live = assertNotNull(resolveTypingLiveDiff("soaks", evaluation))
        val transformed =
            typingLiveDiffVisualTransformation(evaluation, Color.Red)
                .filter(AnnotatedString("soaks"))

        assertEquals(
            listOf(TypingLiveMismatchSpan(2, 3, TypingDifferenceKind.REPLACEMENT)),
            live.mismatchSpans
        )
        val danger = transformed.text.spanStyles.single { it.item.color == Color.Red }
        assertEquals(2, danger.start)
        assertEquals(3, danger.end)
    }

    @Test
    fun `insertion highlights only inserted code point`() {
        val evaluation = evaluator.evaluate(prompt, "socXks")
        val live = assertNotNull(resolveTypingLiveDiff("socXks", evaluation))

        assertEquals(TypingDifferenceKind.INSERTION, live.mismatchSpans.single().kind)
        assertEquals(3, live.mismatchSpans.single().startCodePoint)
    }

    @Test
    fun `deletion alone never paints a character that does not exist`() {
        val evaluation = evaluator.evaluate(prompt, "socs")

        assertNull(resolveTypingLiveDiff("socs", evaluation))
    }

    @Test
    fun `backspace to correct prefix and correction to exact clear danger`() {
        assertNotNull(resolveTypingLiveDiff("soaks", evaluator.evaluate(prompt, "soaks")))
        assertNull(resolveTypingLiveDiff("so", evaluator.evaluate(prompt, "so")))
        assertNull(resolveTypingLiveDiff("socks", evaluator.evaluate(prompt, "socks")))
    }

    @Test
    fun `normalization length changes fall back to untouched editable text`() {
        val raw = "  socks"
        val evaluation = evaluator.evaluate(prompt, raw)
        val transformed =
            typingLiveDiffVisualTransformation(evaluation, Color.Red)
                .filter(AnnotatedString(raw))

        assertEquals(raw, transformed.text.text)
        assertTrue(transformed.text.spanStyles.none { it.item.color == Color.Red })
        assertEquals(3, transformed.offsetMapping.originalToTransformed(3))
    }

    @Test
    fun `revealed replacement preserves originals and localized operation`() {
        val evaluation = evaluator.evaluate(prompt, "soaks")
        val revealed =
            assertNotNull(
                resolveTypingRevealComparison(
                    evaluation,
                    "Your answer",
                    "Correct answer",
                    "Differences"
                )
            )

        assertEquals("soaks", revealed.userAnswer)
        assertEquals("socks", revealed.correctAnswer)
        assertEquals(TypingDifferenceKind.REPLACEMENT, revealed.differences[2].kind)
        assertEquals("a", revealed.differences[2].typedText)
        assertEquals("c", revealed.differences[2].expectedText)
    }

    @Test
    fun `correct manual reveal creates coherent comparison`() {
        val revealed =
            assertNotNull(
                resolveTypingRevealComparison(
                    evaluator.evaluate(prompt, "socks"),
                    "Your answer",
                    "Correct answer"
                )
            )

        assertTrue(revealed.differences.all { it.kind == TypingDifferenceKind.MATCH })
    }
}
