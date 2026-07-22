package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewHistoryRefinementTest {
    @Test
    fun defaultsAreNotRefined() {
        assertTrue(ReviewHistoryUiState().refinementState().isDefault)
    }

    @Test
    fun queryFilterAndSortAreCounted() {
        val state = ReviewHistoryUiState(
            query = "good",
            filter = ReviewHistoryFilter.GOOD,
            sort = ReviewHistorySort.OLDEST
        )

        assertFalse(state.refinementState().isDefault)
        assertEquals(3, state.refinementState().activeCount)
        val presentation = reviewHistoryRefinementPresentation(state)
        assertEquals("3 refinements active", presentation.label)
        assertEquals(3, presentation.actions.size)
    }
}
