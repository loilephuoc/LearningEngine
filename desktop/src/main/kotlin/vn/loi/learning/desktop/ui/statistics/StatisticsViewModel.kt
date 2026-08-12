package vn.loi.learning.desktop.ui.statistics

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.library.model.InstalledPackageId

class StatisticsViewModel(
    private val facade: StatisticsFacade,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {
    private var loadGeneration = 0L

    var uiState by mutableStateOf(
        StatisticsUiState()
    )
        private set

    // Loaded on first navigation to this destination; avoid startup I/O contention.

    fun refresh() = load(uiState.selectedPackageId)

    fun selectScope(packageId: InstalledPackageId?) {
        if (packageId == uiState.selectedPackageId && uiState.loadState == DesktopLoadState.Ready) return
        load(packageId)
    }

    private fun load(packageId: InstalledPackageId?) {
        val generation = ++loadGeneration
        uiState =
            uiState.copy(
                selectedPackageId = packageId,
                loadState =
                    DesktopLoadState.Loading
            )

        taskRunner.run(
            work = { facade.loadUiState(packageId) },
            onSuccess = { loaded ->
                if (generation == loadGeneration) uiState = loaded.copy(loadState = DesktopLoadState.Ready)
            },
            onFailure = { failure ->
                if (generation == loadGeneration) {
                    uiState = uiState.copy(loadState = DesktopLoadState.Failed(failure.toDesktopFailureMessage()))
                }
            }
        )
    }
}
