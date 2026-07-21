package vn.loi.learning.desktop.ui.dashboard

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val scrollbarStyle =
        ScrollbarStyle(
            minimalHeight = 48.dp,
            thickness = 10.dp,
            shape = RoundedCornerShape(5.dp),
            hoverDurationMillis = 250,
            unhoverColor =
                MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = 0.55f),
            hoverColor =
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.95f)
        )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 40.dp,
                        bottom = 24.dp
                    ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Header()

            DashboardOverviewSection(
                uiState = uiState
            )

            DashboardActivitySection(
                uiState = uiState
            )

            DashboardSchedulingSection(
                uiState = uiState
            )

            DashboardMemorySection(
                uiState = uiState
            )

            DashboardRetentionSection(
                uiState = uiState
            )

            DashboardForecastSection(
                uiState = uiState
            )
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(14.dp)
                    .padding(
                        top = 8.dp,
                        end = 4.dp,
                        bottom = 8.dp
                    ),
            style = scrollbarStyle
        )
    }
}

@Composable
private fun Header() {
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
}