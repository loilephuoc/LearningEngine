package vn.loi.learning.desktop.ui.reviewhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.SearchEmptyStateCard
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchRefinementBar
import vn.loi.learning.desktop.ui.state.DesktopLoadState
import vn.loi.learning.desktop.ui.state.DesktopLoadStateCard

@Composable
fun ReviewHistoryScreen(
    uiState: ReviewHistoryUiState,
    onRetry: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterChanged: (ReviewHistoryFilter) -> Unit,
    onSortChanged: (ReviewHistorySort) -> Unit,
    modifier: Modifier = Modifier
) {
    val resetView = {
        onQueryChanged("")
        onFilterChanged(ReviewHistoryFilter.ALL)
        onSortChanged(ReviewHistorySort.NEWEST)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DesktopLoadStateCard(
            state = uiState.loadState,
            screenName = "Review History",
            onRetry = onRetry
        )

        if (uiState.loadState != DesktopLoadState.Loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(reviewHistorySearchSummary(uiState).label)
            }

            SearchField(
                query = uiState.query,
                label = "Search review history",
                summary = reviewHistorySearchSummary(uiState),
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery
            )

            val refinements = uiState.refinementState()
            SearchRefinementBar(
                presentation = reviewHistoryRefinementPresentation(uiState),
                resetEnabled = !refinements.isDefault,
                onReset = resetView
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewHistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewHistorySort.entries.forEach { sort ->
                    FilterChip(
                        selected = uiState.sort == sort,
                        onClick = { onSortChanged(sort) },
                        label = { Text(sort.label) }
                    )
                }
            }

            HorizontalDivider()

            if (uiState.visibleItems.isEmpty()) {
                SearchEmptyStateCard(
                    presentation = reviewHistoryEmptySearchPresentation(uiState),
                    onClearQuery = onClearQuery,
                    onResetView = resetView
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.visibleItems) { item ->
                        ReviewHistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryCard(item: ReviewHistoryItemUi) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = resolveReviewHistoryItemContentDescription(item)
                }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.rating, fontWeight = FontWeight.SemiBold)
                Text(item.reviewedAt)
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Metric("Response time", item.responseTime, Modifier.weight(1f))
                Metric("Stability", item.stability, Modifier.weight(1f))
                Metric("Difficulty", item.difficulty, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
