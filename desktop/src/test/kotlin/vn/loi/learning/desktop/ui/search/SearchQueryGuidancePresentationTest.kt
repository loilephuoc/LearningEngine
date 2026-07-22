package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SearchQueryGuidancePresentationTest {
    @Test
    fun `presents examples for an empty query`() {
        val result = presentSearchQueryGuidance(
            noun = "Lessons",
            examples = listOf(" title ", "translation", "title", ""),
            query = " "
        )

        assertEquals("Try title", result.placeholder)
        assertEquals("Try title, translation", result.supportingText)
        assertEquals("Search guidance for lessons. Try title, translation.", result.contentDescription)
    }

    @Test
    fun `encourages a more specific one-character query`() {
        val result = presentSearchQueryGuidance("lessons", listOf("title"), " a ")

        assertEquals("Add another character for a more specific match.", result.supportingText)
        assertEquals(
            "Search query has one character. Add another character for a more specific match.",
            result.contentDescription
        )
    }

    @Test
    fun `announces an active normalized query`() {
        val result = presentSearchQueryGuidance("review history", listOf("rating"), "  Good  ")

        assertEquals("Searching review history for “Good”.", result.supportingText)
        assertEquals("Searching review history for Good.", result.contentDescription)
    }

    @Test
    fun `explains multi-term matching behavior`() {
        val result = presentSearchQueryGuidance(
            noun = "lessons",
            examples = listOf("title"),
            query = "  alpha   translation  "
        )

        assertEquals("All 2 words must match, in any order.", result.supportingText)
        assertEquals(
            "Search query has 2 words. All words must match, in any order.",
            result.contentDescription
        )
    }

    @Test
    fun `requires a usable noun and example`() {
        assertFailsWith<IllegalArgumentException> {
            presentSearchQueryGuidance(" ", listOf("title"), "")
        }
        assertFailsWith<IllegalArgumentException> {
            presentSearchQueryGuidance("lessons", listOf(" "), "")
        }
    }
}
