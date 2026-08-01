package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DashboardTodaySection(
    presentation: DashboardTodayPresentation,
    modifier: Modifier = Modifier
) {
    LESurface(
        variant = LESurfaceVariant.PRIMARY,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = buildString {
                append("${presentation.title}: ${presentation.value}.")
                presentation.supportingFacts.forEach { append(" ${it.title}: ${it.value}.") }
                append(" ${presentation.actionGuidance}.")
            }
        },
        contentPadding = LETheme.spacing.space0,
        border = LETheme.borders.default,
        shadowElevation = LETheme.elevation.elevation2
    ) {
        Column(
            modifier = Modifier.padding(LETheme.spacing.space6),
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)
        ) {
            Text(
                presentation.title,
                style = LETheme.typography.headlinePane,
                color = LETheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                presentation.value,
                style = LETheme.typography.displayWord,
                color = LETheme.colors.accentPrimary
            )
            Text(
                presentation.actionGuidance,
                style = LETheme.typography.bodyDefinition,
                color = LETheme.colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)
            ) {
                presentation.supportingFacts.forEach { metric ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(metric.value, style = LETheme.typography.metricValue)
                        Text(
                            metric.title,
                            style = LETheme.typography.metricLabel,
                            color = LETheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
