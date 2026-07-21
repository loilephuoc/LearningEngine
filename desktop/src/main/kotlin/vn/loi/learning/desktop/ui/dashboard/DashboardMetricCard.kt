package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                MaterialTheme.colorScheme.onSurfaceVariant

            DashboardMetricTone.PRIMARY ->
                MaterialTheme.colorScheme.primary

            DashboardMetricTone.WARNING ->
                MaterialTheme.colorScheme.tertiary

            DashboardMetricTone.DANGER ->
                MaterialTheme.colorScheme.error

            DashboardMetricTone.SUCCESS ->
                MaterialTheme.colorScheme.secondary

            DashboardMetricTone.INFO ->
                MaterialTheme.colorScheme.primary
        }

    val containerColor =
        when (tone) {
            DashboardMetricTone.NEUTRAL ->
                MaterialTheme.colorScheme.surfaceContainer

            else ->
                accentColor.copy(alpha = 0.08f)
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 136.dp)
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        accessibility.contentDescription
                },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = borderColor(
                    tone = tone,
                    accentColor = accentColor
                )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = accessibility.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = accessibility.value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = accessibility.supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun borderColor(
    tone: DashboardMetricTone,
    accentColor: Color
): Color =
    when (tone) {
        DashboardMetricTone.NEUTRAL ->
            MaterialTheme.colorScheme.outlineVariant

        else ->
            accentColor.copy(alpha = 0.24f)
    }