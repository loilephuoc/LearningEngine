package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchMatchPresentationTest {
    @Test
    fun `finds every non-overlapping match without case sensitivity`() {
        val presentation = presentSearchMatches("Alpha beta ALPHA", "alpha")

        assertEquals(
            listOf(SearchMatchRange(0, 5), SearchMatchRange(11, 16)),
            presentation.ranges
        )
        assertTrue(presentation.hasMatches)
        assertEquals("Alpha beta ALPHA. 2 search matches.", presentation.contentDescription)
    }

    @Test
    fun `highlights every query term independently`() {
        val presentation = presentSearchMatches("Beta then alpha", "alpha beta")

        assertEquals(
            listOf(SearchMatchRange(0, 4), SearchMatchRange(10, 15)),
            presentation.ranges
        )
    }

    @Test
    fun `merges overlapping term highlights`() {
        val presentation = presentSearchMatches("alphabet", "alpha alphabet")

        assertEquals(listOf(SearchMatchRange(0, 8)), presentation.ranges)
        assertEquals("alphabet. 1 search match.", presentation.contentDescription)
    }

    @Test
    fun `trims query before matching`() {
        val presentation = presentSearchMatches("Response time", "  time  ")

        assertEquals(listOf(SearchMatchRange(9, 13)), presentation.ranges)
        assertEquals("Response time. 1 search match.", presentation.contentDescription)
    }

    @Test
    fun `blank query preserves plain text presentation`() {
        val presentation = presentSearchMatches("Lesson title", "   ")

        assertFalse(presentation.hasMatches)
        assertEquals(emptyList(), presentation.ranges)
        assertEquals("Lesson title", presentation.contentDescription)
    }

    @Test
    fun `query absent from text has no highlight`() {
        val presentation = presentSearchMatches("Again", "Hard")

        assertFalse(presentation.hasMatches)
        assertEquals("Again", presentation.contentDescription)
    }
    @Test
    fun `highlights the complete original decomposed grapheme`() {
        val text = "Cafe\u0301 lesson"
        val presentation = presentSearchMatches(text, "Café")

        assertEquals(
            listOf(SearchMatchRange(0, 5)),
            presentation.ranges
        )
        assertEquals(
            "$text. 1 search match.",
            presentation.contentDescription
        )
    }

    @Test
    fun `highlights original text when query uses compatibility width`() {
        val presentation = presentSearchMatches("ABC lesson", "ＡＢＣ")

        assertEquals(
            listOf(SearchMatchRange(0, 3)),
            presentation.ranges
        )
    }
}

