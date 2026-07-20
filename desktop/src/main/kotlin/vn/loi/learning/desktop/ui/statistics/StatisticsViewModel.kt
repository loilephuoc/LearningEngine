package vn.loi.learning.desktop.ui.statistics

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
            facade.loadUiState()
    }
}