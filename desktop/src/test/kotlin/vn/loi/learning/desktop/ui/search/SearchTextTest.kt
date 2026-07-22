package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTextTest {
    @Test
    fun `matches every term in any order`() {
        assertTrue("Advanced vocabulary lesson".containsSearchQuery("lesson advanced"))
    }

    @Test
    fun `requires every term`() {
        assertFalse("Advanced vocabulary lesson".containsSearchQuery("advanced translation"))
    }

    @Test
    fun `blank query matches all text`() {
        assertTrue("Anything".containsSearchQuery("   "))
    }
}
