package vn.loi.learning.desktop.ui.statistics

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StatisticsViewModel(
    private val facade: StatisticsFacade,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
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

        taskRunner.run(
            work = facade::loadUiState,
            onSuccess = { loaded -> uiState = loaded.copy(loadState = DesktopLoadState.Ready) },
            onFailure = { failure ->
                uiState = uiState.copy(loadState = DesktopLoadState.Failed(failure.toDesktopFailureMessage()))
            }
        )
    }
}
