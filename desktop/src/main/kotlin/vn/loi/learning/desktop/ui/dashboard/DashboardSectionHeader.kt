package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardSectionHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val accessibility =
        resolveDashboardSectionAccessibility(
            title = title,
            description = description
        )

    Column(
        modifier =
            modifier.semantics(
                mergeDescendants = true
            ) {
                heading()
                contentDescription =
                    accessibility.contentDescription
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = accessibility.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = accessibility.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}