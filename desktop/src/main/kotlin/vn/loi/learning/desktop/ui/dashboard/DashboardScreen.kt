package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your learning progress at a glance",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardMetricCard(
                title = "Learning items",
                value = uiState.totalLearningItems,
                supportingText = "Total available",
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "Due today",
                value = uiState.dueToday,
                supportingText = "Ready for review",
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "New items",
                value = uiState.newItems,
                supportingText = "Not studied yet",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardMetricCard(
                title = "Retention",
                value = uiState.retention,
                supportingText = "Estimated memory strength",
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "Study streak",
                value = uiState.studyStreak,
                supportingText = "Consecutive study days",
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "Recent activity",
                value = uiState.lastStudy,
                supportingText = "Last 30 days",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}