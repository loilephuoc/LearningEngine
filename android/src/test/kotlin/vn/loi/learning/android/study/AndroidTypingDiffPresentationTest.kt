package vn.loi.learning.android.study

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.android.study.modes.androidTypingComparisonAnnotatedText
import vn.loi.learning.android.study.modes.resolveAndroidTypingRevealComparison
import vn.loi.learning.application.learningexperience.TypingDifferenceKind

class AndroidTypingDiffPresentationTest {
    @Test
    fun `replacement styles both answer lines with code point safe offsets`() {
        val comparison = resolveAndroidTypingRevealComparison("c😀hg", "c😀tg")
        val actual = comparison.actualSpans.single()
        val expected = comparison.expectedSpans.single()

        assertEquals(TypingDifferenceKind.REPLACEMENT, actual.kind)
        assertEquals(2, actual.startCodePoint)
        assertEquals(2, expected.startCodePoint)
        val annotated = androidTypingComparisonAnnotatedText(comparison.actual, comparison.actualSpans, Color.Red)
        val style = annotated.spanStyles.single()
        assertEquals("h", comparison.actual.substring(style.start, style.end))
        assertEquals(TextDecoration.Underline, style.item.textDecoration)
    }

    @Test
    fun `insertion is a removal cue and deletion is an expected success cue`() {
        val insertion = resolveAndroidTypingRevealComparison("coxt", "cot")
        assertEquals(TypingDifferenceKind.INSERTION, insertion.actualSpans.single().kind)
        val insertedText = androidTypingComparisonAnnotatedText(insertion.actual, insertion.actualSpans, Color.Red)
        assertEquals(TextDecoration.LineThrough, insertedText.spanStyles.single().item.textDecoration)

        val deletion = resolveAndroidTypingRevealComparison("cot", "coat")
        assertEquals(TypingDifferenceKind.DELETION, deletion.expectedSpans.single().kind)
        val expectedText = androidTypingComparisonAnnotatedText(deletion.expected, deletion.expectedSpans, Color.Green)
        assertEquals(TextDecoration.Underline, expectedText.spanStyles.single().item.textDecoration)
        assertTrue(deletion.accessibilityDescription.contains("Missing a."))
    }
}
