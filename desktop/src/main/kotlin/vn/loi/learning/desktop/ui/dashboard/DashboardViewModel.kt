package vn.loi.learning.desktop.ui.dashboard

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.toDesktopFailureMessage

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
    private val facade: DashboardFacade
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
