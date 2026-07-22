package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchQueryTermsTest {
    @Test
    fun `normalizes whitespace and preserves term order`() {
        val result = parseSearchQuery("  alpha   beta\tgamma  ")

        assertEquals("alpha beta gamma", result.normalizedQuery)
        assertEquals(listOf("alpha", "beta", "gamma"), result.terms)
        assertTrue(result.isMultiTerm)
    }

    @Test
    fun `removes duplicate terms without case sensitivity`() {
        val result = parseSearchQuery("Alpha beta ALPHA Beta")

        assertEquals(listOf("Alpha", "beta"), result.terms)
    }

    @Test
    fun `blank query has no terms`() {
        val result = parseSearchQuery(" \t ")

        assertTrue(result.isBlank)
        assertFalse(result.isMultiTerm)
    }
}
