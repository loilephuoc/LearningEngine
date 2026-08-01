package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    tone: DashboardMetricTone = DashboardMetricTone.NEUTRAL
) {
    val accessibility =
        resolveDashboardMetricAccessibility(
            title = title,
            value = value,
            supportingText = supportingText
        )

    val accentColor =
        when (tone) {
            DashboardMetricTone.NEUTRAL ->
                LETheme.colors.metricNeutral

            DashboardMetricTone.PRIMARY ->
                LETheme.colors.accentPrimary

            DashboardMetricTone.WARNING ->
                LETheme.colors.warning

            DashboardMetricTone.DANGER ->
                LETheme.colors.danger

            DashboardMetricTone.SUCCESS ->
                LETheme.colors.success

            DashboardMetricTone.INFO ->
                LETheme.colors.info
        }

    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        accessibility.contentDescription
                },
        contentPadding = LETheme.spacing.space4,
        border = null,
        shadowElevation = LETheme.elevation.elevation0
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)
        ) {
            Text(
                text = accessibility.title,
                style = LETheme.typography.metricLabel,
                color = LETheme.colors.textSecondary
            )

            Text(
                text = accessibility.value,
                style = LETheme.typography.metricValue,
                color = accentColor
            )

            Text(
                text = accessibility.supportingText,
                style = LETheme.typography.metricSubtitle,
                color = LETheme.colors.textMuted
            )
        }
    }
}
