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
    @Test
    fun `matches composed query against decomposed source text`() {
        assertTrue("Cafe\u0301 lesson".containsSearchQuery("Café"))
    }

    @Test
    fun `matches compatibility width variants`() {
        assertTrue("ABC lesson".containsSearchQuery("ＡＢＣ"))
    }
}

