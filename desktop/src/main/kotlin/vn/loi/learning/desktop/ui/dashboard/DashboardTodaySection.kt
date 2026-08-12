package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
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
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopContentWidthClass
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopResponsivePolicyResolver

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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val policy = DesktopResponsivePolicyResolver.resolve(maxWidth.value.toInt())
            val content: @Composable (Modifier) -> Unit = { contentModifier ->
            Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space2)) {
                Text(presentation.title, style = LETheme.typography.headlinePane, modifier = Modifier.semantics { heading() })
                Text(presentation.value, maxLines = 1, style = LETheme.typography.displayWord, color = LETheme.colors.accentPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space3)) {
                    presentation.supportingFacts.forEach { metric ->
                        Column(Modifier.weight(1f)) {
                            Text(metric.value, maxLines = 1, style = LETheme.typography.metricValue)
                            Text(metric.title, maxLines = 1, style = LETheme.typography.metricLabel, color = LETheme.colors.textSecondary)
                        }
                    }
                }
            }
            }
            if (policy.widthClass == DesktopContentWidthClass.WIDE) {
                Row(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(LETheme.spacing.space5), horizontalArrangement = Arrangement.spacedBy(LETheme.spacing.space6), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    content(Modifier.weight(1f))
                    Button(onClick = onStudyNow) { Text("Học ngay", maxLines = 1) }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(LETheme.spacing.space5), verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space4)) {
                    content(Modifier.fillMaxWidth())
                    Button(onClick = onStudyNow, modifier = Modifier.fillMaxWidth()) {
                Text("Học ngay")
                    }
                }
            }
        }
    }
}
