package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LearningInsightCard(
    presentation: LearningInsightPresentation,
    strings: LearningInsightStrings,
    visibleMetricLimit: Int,
    modifier: Modifier = Modifier
) {
    var expanded by remember(presentation) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = presentation.accessibilityText
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(presentation.heading, style = MaterialTheme.typography.labelLarge)
            Text(presentation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(presentation.summary, style = MaterialTheme.typography.bodyMedium)
            presentation.recommendation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            presentation.metrics.take(if (expanded) Int.MAX_VALUE else visibleMetricLimit).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (presentation.metrics.size > visibleMetricLimit) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) strings.collapse else strings.expand)
                }
            }
        }
    }
}
