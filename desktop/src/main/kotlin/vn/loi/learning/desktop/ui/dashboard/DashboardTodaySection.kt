package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DashboardTodaySection(
    presentation: DashboardTodayPresentation,
    onStudyNow: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(LETheme.spacing.space5),
            horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space6),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)) {
                Text(presentation.title, style = LETheme.typography.headlinePane, modifier = Modifier.semantics { heading() })
                Row(horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space6), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(presentation.value, style = LETheme.typography.displayWord, color = LETheme.colors.accentPrimary)
                    presentation.supportingFacts.forEach { metric ->
                        Column {
                            Text(metric.value, style = LETheme.typography.metricValue)
                            Text(metric.title, style = LETheme.typography.metricLabel, color = LETheme.colors.textSecondary)
                        }
                    }
                }
            }
            Button(onClick = onStudyNow) {
                Text("Học ngay")
            }
        }
    }
}
