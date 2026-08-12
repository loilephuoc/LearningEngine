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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.DesktopLoadStateCard
import vn.loi.learning.desktop.ui.theme.LETheme

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRetry: () -> Unit,
    onStudyNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val presentation = DashboardPresentationResolver.resolve(uiState)

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
                    .padding(end = LETheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(LETheme.spacing.space5)
        ) {
            DesktopLoadStateCard(
                state = uiState.loadState,
                screenName = "Dashboard",
                onRetry = onRetry
            )

            if (
                uiState.loadState !=
                DesktopLoadState.Loading
            ) {
                Header()

                DashboardTodaySection(presentation = presentation.today, onStudyNow = onStudyNow)

                DashboardOverviewSection(
                    metrics = presentation.keyMetrics
                )

            DashboardActivitySection(
                uiState = uiState
            )

            }
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
        modifier =
            Modifier.semantics(
                mergeDescendants = true
            ) {
                heading()
                contentDescription =
                    resolveDashboardHeaderContentDescription()
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Trang chủ",
            style = LETheme.typography.headlinePane,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tổng quan nhanh việc học của bạn",
            style = LETheme.typography.bodyDefinition,
            color = LETheme.colors.textSecondary
        )
    }
}
