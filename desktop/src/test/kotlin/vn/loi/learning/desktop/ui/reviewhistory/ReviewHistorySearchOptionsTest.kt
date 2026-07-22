package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewHistorySearchOptionsTest {
    @Test
    fun activeReviewFilterAndSortAreNamed() {
        val state = ReviewHistoryUiState(
            items = emptyList(),
            filter = ReviewHistoryFilter.HARD,
            sort = ReviewHistorySort.RESPONSE_TIME
        )

        val filter = reviewHistoryFilterPresentation(state)
        val sort = reviewHistorySortPresentation(state)

        assertEquals("Hard", filter.options.single { it.selected }.label)
        assertEquals("Response time", sort.options.single { it.selected }.label)
        assertTrue(filter.contentDescription.contains("review history"))
        assertTrue(sort.contentDescription.contains("Selected: Response time"))
    }
}
