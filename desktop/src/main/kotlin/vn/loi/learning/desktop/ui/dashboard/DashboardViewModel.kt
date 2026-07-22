package vn.loi.learning.desktop.ui.dashboard

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Presentation state holder cho Dashboard.
 *
 * ViewModel không truy cập repository và không biết cách tạo
 * LearningDashboardQuery. Mọi dữ liệu đều đi qua DashboardFacade.
 */
class DashboardViewModel(
    private val facade: DashboardFacade,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {

    var uiState by mutableStateOf(DashboardUiState())
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
            work = facade::load,
            onSuccess = { loaded -> uiState = loaded.copy(loadState = DesktopLoadState.Ready) },
            onFailure = { failure ->
                uiState = uiState.copy(loadState = DesktopLoadState.Failed(failure.toDesktopFailureMessage()))
            }
        )
    }
}
