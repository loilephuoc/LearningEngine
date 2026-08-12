package vn.loi.learning.desktop.ui.statistics

import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.dashboard.DashboardUiState
import vn.loi.learning.domain.library.model.InstalledPackageId

data class StatisticsScopeOption(val packageId: InstalledPackageId?, val label: String)

data class StatisticsUiState(
    val loadState: DesktopLoadState = DesktopLoadState.Loading,
    val totalReviews: String = "0",
    val successfulReviews: String = "0",
    val againCount: String = "0",
    val hardCount: String = "0",
    val goodCount: String = "0",
    val easyCount: String = "0",
    val successRate: String = "--",
    val averageResponseTime: String = "--",
    val analytics: DashboardUiState = DashboardUiState(),
    val scopeOptions: List<StatisticsScopeOption> = listOf(StatisticsScopeOption(null, "Táº¥t cáº£ ná»™i dung Ä‘Ã£ há»c")),
    val selectedPackageId: InstalledPackageId? = null
)
