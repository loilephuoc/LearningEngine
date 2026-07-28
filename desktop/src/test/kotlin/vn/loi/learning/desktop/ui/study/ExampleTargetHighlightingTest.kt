package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.SpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleTargetHighlightingTest {
    @Test
    fun `English target matching is case insensitive and supports multiple occurrences`() {
        val text = "A DISASTER can follow another disaster."

        assertEquals(
            listOf(
                ExampleTargetMatch(2, 10),
                ExampleTargetMatch(30, 38)
            ),
            resolveExampleTargetMatches(text, "disaster", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `English target requires word boundaries`() {
        val text = "A disaster is not highlighted inside disastrous wording."

        assertEquals(
            listOf(ExampleTargetMatch(2, 10)),
            resolveExampleTargetMatches(text, "disaster", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `multi-word English target with punctuation matches accurately`() {
        val text = "Please sign here at the bottom."

        assertEquals(
            listOf(ExampleTargetMatch(17, 31)),
            resolveExampleTargetMatches(text, "at the bottom.", ExampleTargetLanguage.ENGLISH)
        )
        assertEquals(
            listOf(ExampleTargetMatch(17, 30)),
            resolveExampleTargetMatches(text, "at the bottom", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `multi-word English target uses exact phrase boundaries`() {
        val text = "Many homeless people help other homeless people."

        assertEquals(
            listOf(
                ExampleTargetMatch(5, 20),
                ExampleTargetMatch(32, 47)
            ),
            resolveExampleTargetMatches(text, "homeless people", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `Vietnamese phrase matching is case-insensitive and supports multiple occurrences`() {
        val text = "THẢM HỌA này là một thảm họa lớn."

        assertEquals(
            listOf(
                ExampleTargetMatch(0, 8),
                ExampleTargetMatch(20, 28)
            ),
            resolveExampleTargetMatches(text, "thảm họa", ExampleTargetLanguage.VIETNAMESE)
        )
    }

    @Test
    fun `inflected English phrase matching highlights target with common suffix`() {
        val text = "George is Karen and Jack's uncle."

        assertEquals(
            listOf(ExampleTargetMatch(27, 32)),
            resolveExampleTargetMatches(text, "uncle", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `English infinitive verb target is normalized to canonical verb`() {
        val signText = "Please sign here at the bottom."
        assertEquals(
            listOf(ExampleTargetMatch(7, 11)),
            resolveExampleTargetMatches(signText, "to sign", ExampleTargetLanguage.ENGLISH)
        )

        val studyText = "Students need to study every day."
        assertEquals(
            listOf(ExampleTargetMatch(14, 22)),
            resolveExampleTargetMatches(studyText, "to study", ExampleTargetLanguage.ENGLISH)
        )

        val workText = "She works hard for her family."
        assertEquals(
            listOf(ExampleTargetMatch(4, 9)),
            resolveExampleTargetMatches(workText, "to work", ExampleTargetLanguage.ENGLISH)
        )
    }

    @Test
    fun `English infinitive normalization does not match partial substring in non-target word`() {
        val signatureText = "Please check signature"
        assertTrue(
            resolveExampleTargetMatches(signatureText, "to sign", ExampleTargetLanguage.ENGLISH).isEmpty()
        )
    }

    @Test
    fun `uncertain substring does not highlight and raw text is unchanged`() {
        val original = "This was disastrous."

        val annotated = highlightedExampleText(
            text = original,
            target = "disaster",
            language = ExampleTargetLanguage.ENGLISH,
            highlightStyle = SpanStyle()
        )

        assertEquals(original, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
        assertEquals("This was disastrous.", original)
    }
}
