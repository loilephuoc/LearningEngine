package vn.loi.learning.desktop.ui.reviewhistory

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

    fun refresh() {
        uiState = facade.load()
    }
}