package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ReviewHistoryViewModel(
    private val facade: ReviewHistoryFacade,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {

    var uiState by mutableStateOf(
        ReviewHistoryUiState()
    )
        private set

    // Loaded on first navigation to this destination; avoid startup I/O contention.

    fun updateQuery(query: String) { uiState = uiState.copy(query = query) }
    fun clearQuery() { updateQuery("") }
    fun updateFilter(filter: ReviewHistoryFilter) { uiState = uiState.copy(filter = filter) }
    fun updateSort(sort: ReviewHistorySort) { uiState = uiState.copy(sort = sort) }

    fun selectTab(tab: ReviewCenterTab) {
        uiState = uiState.copy(selectedTab = tab)
        if (tab == ReviewCenterTab.HISTORY && !uiState.historyLoaded) refresh()
    }

    fun openReviewCenter() {
        uiState = uiState.copy(selectedTab = ReviewCenterTab.QUICK_REVIEW)
    }

    fun invalidateHistory() {
        uiState = uiState.copy(historyLoaded = false)
    }

    fun refresh() {
        uiState =
            uiState.copy(
                loadState =
                    DesktopLoadState.Loading
            )

        taskRunner.run(
            work = facade::load,
            onSuccess = { loaded ->
                uiState = loaded.copy(
                    loadState = DesktopLoadState.Ready,
                    selectedTab = ReviewCenterTab.HISTORY,
                    historyLoaded = true,
                    query = uiState.query,
                    filter = uiState.filter,
                    sort = uiState.sort
                )
            },
            onFailure = { failure ->
                uiState = uiState.copy(loadState = DesktopLoadState.Failed(failure.toDesktopFailureMessage()))
            }
        )
    }
}
