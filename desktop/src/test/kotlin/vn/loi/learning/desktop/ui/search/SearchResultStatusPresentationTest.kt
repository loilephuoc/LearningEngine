package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultStatusPresentationTest {
    @Test
    fun unfilteredResultsDescribeCompleteCollection() {
        val result = presentSearchResultStatus(SearchResultSummary(4, 4, ""), "lessons", false)
        assertEquals("4 lessons", result.label)
        assertEquals("All 4 lessons are shown.", result.contentDescription)
        assertFalse(result.isFiltered)
        assertTrue(result.hasResults)
    }

    @Test
    fun refinementsDescribeVisibleSubset() {
        val result = presentSearchResultStatus(SearchResultSummary(2, 5, "word"), "lessons", true)
        assertEquals("2 of 5 lessons", result.label)
        assertTrue(result.isFiltered)
    }

    @Test
    fun emptyMatchIsExplicit() {
        val result = presentSearchResultStatus(SearchResultSummary(0, 5, "missing"), "lessons", false)
        assertEquals("No matching lessons", result.label)
        assertFalse(result.hasResults)
    }
}
