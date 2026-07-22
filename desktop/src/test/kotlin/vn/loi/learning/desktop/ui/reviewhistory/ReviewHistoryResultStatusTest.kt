package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewHistoryResultStatusTest {
    @Test
    fun sortMarksStatusAsRefined() {
        val state = ReviewHistoryUiState(sort = ReviewHistorySort.entries.first { it != ReviewHistorySort.NEWEST })
        val result = reviewHistoryResultStatus(state)
        assertTrue(result.isFiltered)
        assertEquals("No review events yet", result.label)
    }
}
