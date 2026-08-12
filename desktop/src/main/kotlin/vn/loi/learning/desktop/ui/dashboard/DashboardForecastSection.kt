package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardForecastSection(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardSectionHeader(
            title = "Lịch ôn sắp tới",
            description = "Khối lượng dự kiến trong bảy ngày tới"
        )

        DashboardAnalyticsLayout(
            visualization = { visualizationModifier ->
                DashboardForecastChart(
                    values = uiState.forecastBuckets,
                    modifier = visualizationModifier
                )
            },
            metrics = { metricsModifier ->
                DashboardMetricGrid(
                    metrics =
                        listOf(
                            DashboardMetric(
                                title = "Ngày tiếp theo",
                                value = uiState.forecastNextDay,
                                supportingText = "Trong 24 giờ",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Ngày 2–3",
                                value =
                                    uiState.forecastDaysTwoToThree,
                                supportingText = "Sắp đến hạn",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Ngày 4–7",
                                value =
                                    uiState.forecastDaysFourToSeven,
                                supportingText = "Cuối tuần này",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Tổng cộng",
                                value = uiState.forecastTotal,
                                supportingText = "Khối lượng bảy ngày",
                                tone = DashboardMetricTone.DANGER
                            )
                        ),
                    modifier = metricsModifier,
                    preferredColumnCount = 2
                )
            }
        )
    }
}
