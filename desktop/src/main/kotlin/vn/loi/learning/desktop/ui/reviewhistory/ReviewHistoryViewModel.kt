package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ReviewHistoryViewModel(
    private val facade: ReviewHistoryFacade
) {

    var uiState by mutableStateOf(
        ReviewHistoryUiState()
    )
        private set

    init {
        refresh()
    }

    fun updateQuery(query: String) { uiState = uiState.copy(query = query) }
    fun clearQuery() { updateQuery("") }
    fun updateFilter(filter: ReviewHistoryFilter) { uiState = uiState.copy(filter = filter) }
    fun updateSort(sort: ReviewHistorySort) { uiState = uiState.copy(sort = sort) }

    fun refresh() {
        uiState =
            uiState.copy(
                loadState =
                    DesktopLoadState.Loading
            )

        uiState =
            try {
                facade.load().copy(
                    loadState =
                        DesktopLoadState.Ready
                )
            } catch (failure: Throwable) {
                uiState.copy(
                    loadState =
                        DesktopLoadState.Failed(
                            failure
                                .toDesktopFailureMessage()
                        )
                )
            }
    }
}