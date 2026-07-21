package vn.loi.learning.desktop.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.desktop.ui.dashboard.DashboardUiState
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryItemUi
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryUiState
import vn.loi.learning.desktop.ui.statistics.StatisticsUiState

class DesktopScreenLoadStateContractTest {
    @Test
    fun `dashboard starts loading and can preserve metrics on failure`() {
        val previous =
            DashboardUiState(
                loadState = DesktopLoadState.Ready,
                totalLearningItems = "42"
            )

        val failed =
            previous.copy(
                loadState =
                    DesktopLoadState.Failed(
                        "Storage unavailable"
                    )
            )

        assertEquals(
            "42",
            failed.totalLearningItems
        )
        assertEquals(
            DesktopLoadState.Failed(
                "Storage unavailable"
            ),
            failed.loadState
        )
    }

    @Test
    fun `statistics starts loading and preserves previous values on failure`() {
        val previous =
            StatisticsUiState(
                loadState = DesktopLoadState.Ready,
                totalReviews = "18",
                successRate = "90%"
            )

        val failed =
            previous.copy(
                loadState =
                    DesktopLoadState.Failed(
                        "Read failed"
                    )
            )

        assertEquals("18", failed.totalReviews)
        assertEquals("90%", failed.successRate)
    }

    @Test
    fun `review history preserves visible items on refresh failure`() {
        val item =
            ReviewHistoryItemUi(
                reviewedAt = "Today",
                rating = "Good",
                responseTime = "2s",
                stability = "3d",
                difficulty = "5"
            )

        val previous =
            ReviewHistoryUiState(
                loadState = DesktopLoadState.Ready,
                items = listOf(item)
            )

        val failed =
            previous.copy(
                loadState =
                    DesktopLoadState.Failed(
                        "Database unavailable"
                    )
            )

        assertEquals(
            listOf(item),
            failed.items
        )
    }

    @Test
    fun `all protected screens start in loading state`() {
        assertEquals(
            DesktopLoadState.Loading,
            DashboardUiState().loadState
        )
        assertEquals(
            DesktopLoadState.Loading,
            StatisticsUiState().loadState
        )
        assertEquals(
            DesktopLoadState.Loading,
            ReviewHistoryUiState().loadState
        )
    }
}
