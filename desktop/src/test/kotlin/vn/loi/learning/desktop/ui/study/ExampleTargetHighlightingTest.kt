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
    fun `Vietnamese phrase matching supports exact multiple occurrences`() {
        val text = "thảm họa này là một thảm họa lớn."

        assertEquals(
            listOf(
                ExampleTargetMatch(0, 8),
                ExampleTargetMatch(20, 28)
            ),
            resolveExampleTargetMatches(text, "thảm họa", ExampleTargetLanguage.VIETNAMESE)
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
