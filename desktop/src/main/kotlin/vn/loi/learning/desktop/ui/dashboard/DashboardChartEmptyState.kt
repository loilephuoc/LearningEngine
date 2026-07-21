package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DashboardChartEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val accessibility =
        resolveDashboardChartEmptyAccessibility(
            title = title,
            description = description
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        accessibility.contentDescription
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                space = 2.dp,
                alignment = Alignment.CenterVertically
            )
    ) {
        Text(
            text = accessibility.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = accessibility.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}