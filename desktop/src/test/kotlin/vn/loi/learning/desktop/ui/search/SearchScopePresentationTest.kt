package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SearchScopePresentationTest {
    @Test
    fun `presents normalized fields and active refinements`() {
        val result = presentSearchScope(
            noun = "lessons",
            fields = listOf(" Title ", "Translation", "Title", ""),
            query = "  alpha  ",
            filterLabel = "Vocabulary",
            sortLabel = "Title"
        )

        assertEquals(listOf("Title", "Translation"), result.fields)
        assertEquals("Query: alpha · Filter: Vocabulary · Sort: Title", result.activeSummary)
        assertEquals(
            "Search lessons across Title, Translation. Query: alpha. Filter Vocabulary. Sort Title.",
            result.contentDescription
        )
    }

    @Test
    fun `blank query is disclosed explicitly`() {
        val result = presentSearchScope(
            noun = "review history",
            fields = listOf("Rating"),
            query = " ",
            filterLabel = "All",
            sortLabel = "Newest"
        )

        assertEquals("No text query · Filter: All · Sort: Newest", result.activeSummary)
    }

    @Test
    fun `requires a usable noun and field`() {
        assertFailsWith<IllegalArgumentException> {
            presentSearchScope(" ", listOf("Title"), "", "All", "Newest")
        }
        assertFailsWith<IllegalArgumentException> {
            presentSearchScope("lessons", listOf(" "), "", "All", "Newest")
        }
    }
}
