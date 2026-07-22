package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewHistoryProjectionTest {
    private val items =
        listOf(
            ReviewHistoryItemUi("new", "Good", "2s", "3d", "5"),
            ReviewHistoryItemUi("old", "Again", "8s", "1d", "8")
        )

    @Test
    fun `searches by one term`() {
        assertEquals(
            1,
            projectReviewHistory(items, "good", ReviewHistoryFilter.ALL, ReviewHistorySort.NEWEST).size
        )
    }

    @Test
    fun `multi-term search requires all words across the searchable row`() {
        assertEquals(
            "Good",
            projectReviewHistory(items, "2s good", ReviewHistoryFilter.ALL, ReviewHistorySort.NEWEST)
                .single()
                .rating
        )
        assertEquals(
            emptyList(),
            projectReviewHistory(items, "good 8s", ReviewHistoryFilter.ALL, ReviewHistorySort.NEWEST)
        )
    }

    @Test
    fun `filters by rating`() {
        assertEquals(
            "Again",
            projectReviewHistory(items, "", ReviewHistoryFilter.AGAIN, ReviewHistorySort.NEWEST)
                .single()
                .rating
        )
    }

    @Test
    fun `sorts oldest first`() {
        assertEquals(
            "old",
            projectReviewHistory(items, "", ReviewHistoryFilter.ALL, ReviewHistorySort.OLDEST)
                .first()
                .reviewedAt
        )
    }
}
