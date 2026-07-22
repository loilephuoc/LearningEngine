package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewHistorySearchGuidanceTest {
    @Test
    fun `maps review history examples and query`() {
        val result = reviewHistorySearchGuidance(ReviewHistoryUiState(query = "Good"))

        assertEquals("Try rating", result.placeholder)
        assertEquals("Searching review history for “Good”.", result.supportingText)
    }

    @Test
    fun `empty review query exposes searchable examples`() {
        val result = reviewHistorySearchGuidance(ReviewHistoryUiState())

        assertEquals("Try rating, review date, response time", result.supportingText)
    }
}
