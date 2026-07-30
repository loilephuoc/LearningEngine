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
            assertNull(resolvePositionalTypingLiveDiff(input, evaluator.evaluate(prompt, input)), input)
        }
    }

    @Test
    fun `exact socks has no danger span`() {
        val evaluation = evaluator.evaluate(prompt, "socks")
        val transformed =
            typingLiveDiffVisualTransformation(evaluation, Color.Red)
                .filter(AnnotatedString("socks"))

        assertNull(resolvePositionalTypingLiveDiff("socks", evaluation))
        assertTrue(transformed.text.spanStyles.none { it.item.color == Color.Red })
    }

    @Test
    fun `soaks against socks highlights only replacement a`() {
        val evaluation = evaluator.evaluate(prompt, "soaks")
        val live = assertNotNull(resolvePositionalTypingLiveDiff("soaks", evaluation))
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
    fun `middle insertion marks the positionally shifted typed suffix`() {
        val evaluation = evaluator.evaluate(prompt, "socXks")
        val live =
            assertNotNull(resolvePositionalTypingLiveDiff("socXks", evaluation))

        assertEquals(
            listOf(
                TypingLiveMismatchSpan(3, 4, TypingDifferenceKind.REPLACEMENT),
                TypingLiveMismatchSpan(4, 5, TypingDifferenceKind.REPLACEMENT),
                TypingLiveMismatchSpan(5, 6, TypingDifferenceKind.INSERTION)
            ),
            live.mismatchSpans
        )
    }

    @Test
    fun `socs highlights the typed character occupying the missing k position`() {
        val evaluation = evaluator.evaluate(prompt, "socs")
        val live = assertNotNull(resolvePositionalTypingLiveDiff("socs", evaluation))

        assertEquals(
            listOf(TypingLiveMismatchSpan(3, 4, TypingDifferenceKind.REPLACEMENT)),
            live.mismatchSpans
        )
    }

    @Test
    fun `backspace to correct prefix and correction to exact clear danger`() {
        assertNotNull(resolvePositionalTypingLiveDiff("soaks", evaluator.evaluate(prompt, "soaks")))
        assertNull(resolvePositionalTypingLiveDiff("so", evaluator.evaluate(prompt, "so")))
        assertNull(resolvePositionalTypingLiveDiff("socks", evaluator.evaluate(prompt, "socks")))
    }

    @Test
    fun `extra final code point is the only insertion danger span`() {
        val input = "sockss"
        val live =
            assertNotNull(resolvePositionalTypingLiveDiff(input, evaluator.evaluate(prompt, input)))

        assertEquals(
            listOf(TypingLiveMismatchSpan(5, 6, TypingDifferenceKind.INSERTION)),
            live.mismatchSpans
        )
    }

    @Test
    fun `positional live comparison is Unicode code point safe`() {
        val unicodePrompt = TypingRecallPrompt("a🙂b")
        val input = "a🙃b"
        val live =
            assertNotNull(
                resolvePositionalTypingLiveDiff(
                    input,
                    evaluator.evaluate(unicodePrompt, input)
                )
            )

        assertEquals(
            listOf(TypingLiveMismatchSpan(1, 2, TypingDifferenceKind.REPLACEMENT)),
            live.mismatchSpans
        )
    }

    @Test
    fun `Vietnamese apostrophe hyphen and multi-word prefixes remain neutral`() {
        listOf(
            TypingRecallPrompt("tiếng Việt") to "tiếng ",
            TypingRecallPrompt("don't stop") to "don't ",
            TypingRecallPrompt("mother-in-law") to "mother-in",
            TypingRecallPrompt("take off now") to "take off"
        ).forEach { (expected, input) ->
            assertNull(
                resolvePositionalTypingLiveDiff(input, evaluator.evaluate(expected, input)),
                input
            )
        }
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
    fun `Reveal keeps Levenshtein deletion while live uses positional replacement`() {
        val evaluation = evaluator.evaluate(prompt, "socs")
        val live = assertNotNull(resolvePositionalTypingLiveDiff("socs", evaluation))
        val reveal =
            assertNotNull(
                resolveTypingRevealComparison(
                    evaluation,
                    "You typed",
                    "Correct answer"
                )
            )

        assertEquals(TypingDifferenceKind.REPLACEMENT, live.mismatchSpans.single().kind)
        assertTrue(reveal.differences.any { it.kind == TypingDifferenceKind.DELETION })
    }

    @Test
    fun `correct manual reveal omits redundant comparison`() {
        assertNull(
            resolveTypingRevealComparison(
                evaluator.evaluate(prompt, "socks"),
                "You typed",
                "Correct answer"
            )
        )
    }

    @Test
    fun `long sentence diff remains natural text with one localized replacement`() {
        val expected = "Where is the nearest hospital?"
        val typed = "Where is the nearest hospitl?"
        val evaluation = evaluator.evaluate(TypingRecallPrompt(expected), typed)
        val revealed =
            assertNotNull(
                resolveTypingRevealComparison(
                    evaluation,
                    "You typed",
                    "Correct answer"
                )
            )

        assertEquals(typed, revealed.userAnswer)
        assertEquals(expected, revealed.correctAnswer)
        assertTrue(revealed.expectedMismatchSpans.isNotEmpty())
        assertTrue(revealed.differences.any { it.kind != TypingDifferenceKind.MATCH })
    }
}
