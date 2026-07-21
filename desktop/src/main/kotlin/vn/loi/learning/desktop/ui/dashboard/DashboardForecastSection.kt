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
            title = "Forecast",
            description =
                "Expected review workload for the next seven days"
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
                                title = "Next day",
                                value = uiState.forecastNextDay,
                                supportingText = "Due within 24 hours",
                                tone = DashboardMetricTone.WARNING
                            ),
                            DashboardMetric(
                                title = "Days 2–3",
                                value =
                                    uiState.forecastDaysTwoToThree,
                                supportingText = "Upcoming reviews",
                                tone = DashboardMetricTone.INFO
                            ),
                            DashboardMetric(
                                title = "Days 4–7",
                                value =
                                    uiState.forecastDaysFourToSeven,
                                supportingText = "Later this week",
                                tone = DashboardMetricTone.PRIMARY
                            ),
                            DashboardMetric(
                                title = "Total",
                                value = uiState.forecastTotal,
                                supportingText = "Seven-day workload",
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