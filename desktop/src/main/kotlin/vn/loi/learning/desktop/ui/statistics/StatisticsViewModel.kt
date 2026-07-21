package vn.loi.learning.desktop.ui.statistics

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StatisticsViewModel(
    private val facade: StatisticsFacade
) {

    var uiState by mutableStateOf(
        StatisticsUiState()
    )
        private set

    init {
        refresh()
    }

    fun refresh() {
        uiState =
            uiState.copy(
                loadState =
                    DesktopLoadState.Loading
            )

        uiState =
            try {
                facade.loadUiState().copy(
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