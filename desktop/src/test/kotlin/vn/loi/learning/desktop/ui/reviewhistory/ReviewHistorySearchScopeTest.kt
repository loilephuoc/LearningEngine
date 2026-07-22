package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewHistorySearchScopeTest {
    @Test
    fun `review history exposes every searchable metric`() {
        val result = reviewHistorySearchScope(ReviewHistoryUiState(query = "again"))

        assertEquals(
            listOf("Rating", "Reviewed time", "Response time", "Stability", "Difficulty"),
            result.fields
        )
        assertTrue(result.activeSummary.startsWith("Query: again"))
        assertTrue(result.contentDescription.startsWith("Search review history across"))
    }
}
