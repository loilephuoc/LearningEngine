package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.state.DesktopLoadState

class ReviewHistoryEmptySearchPresentationTest {
    private val item =
        ReviewHistoryItemUi(
            reviewedAt = "Today",
            rating = "Good",
            responseTime = "1s",
            stability = "2d",
            difficulty = "3"
        )

    @Test
    fun queryAndRatingFilterExposeBothRecoveryActions() {
        val state =
            ReviewHistoryUiState(
                loadState = DesktopLoadState.Ready,
                items = listOf(item),
                query = "missing",
                filter = ReviewHistoryFilter.AGAIN
            )

        val presentation = reviewHistoryEmptySearchPresentation(state)

        assertTrue(presentation.showClearQuery)
        assertTrue(presentation.showResetView)
    }

    @Test
    fun genuinelyEmptyHistoryDoesNotOfferSearchRecovery() {
        val presentation =
            reviewHistoryEmptySearchPresentation(
                ReviewHistoryUiState(loadState = DesktopLoadState.Ready)
            )

        assertFalse(presentation.showClearQuery)
        assertFalse(presentation.showResetView)
    }
}
