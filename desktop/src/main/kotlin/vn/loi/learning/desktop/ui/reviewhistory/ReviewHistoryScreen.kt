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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.search.HighlightedSearchText
import vn.loi.learning.desktop.ui.search.SearchEmptyStateCard
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchKeyboardAction
import vn.loi.learning.desktop.ui.search.SearchOptionGroup
import vn.loi.learning.desktop.ui.search.SearchKeyboardKey
import vn.loi.learning.desktop.ui.search.SearchRefinementBar
import vn.loi.learning.desktop.ui.search.SearchRefinementAction
import vn.loi.learning.desktop.ui.search.SearchResultStatus
import vn.loi.learning.desktop.ui.search.presentSearchKeyboardShortcuts
import vn.loi.learning.desktop.ui.search.resolveSearchKeyboardAction
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
    val searchFocusRequester = remember { FocusRequester() }
    val resetView = {
        onQueryChanged("")
        onFilterChanged(ReviewHistoryFilter.ALL)
        onSortChanged(ReviewHistorySort.NEWEST)
    }

    Column(
        modifier =
            modifier.onPreviewKeyEvent { event ->
                val key =
                    when (event.key) {
                        Key.F -> SearchKeyboardKey.F
                        Key.Escape -> SearchKeyboardKey.ESCAPE
                        else -> SearchKeyboardKey.OTHER
                    }
                when (
                    resolveSearchKeyboardAction(
                        key = key,
                        isKeyDown = event.type == KeyEventType.KeyDown,
                        controlPressed = event.isCtrlPressed,
                        hasQuery = uiState.query.isNotBlank(),
                        hasNonQueryRefinement =
                            uiState.filter != ReviewHistoryFilter.ALL ||
                                uiState.sort != ReviewHistorySort.NEWEST
                    )
                ) {
                    SearchKeyboardAction.FOCUS_SEARCH -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    SearchKeyboardAction.CLEAR_QUERY -> {
                        onClearQuery()
                        true
                    }
                    SearchKeyboardAction.RESET_VIEW -> {
                        resetView()
                        true
                    }
                    SearchKeyboardAction.NONE -> false
                }
            },
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
                onClearQuery = onClearQuery,
                focusRequester = searchFocusRequester,
                keyboardPresentation = presentSearchKeyboardShortcuts("review history")
            )

            SearchResultStatus(reviewHistoryResultStatus(uiState))

            val refinements = uiState.refinementState()
            SearchRefinementBar(
                presentation = reviewHistoryRefinementPresentation(uiState),
                resetEnabled = !refinements.isDefault,
                onAction = { action ->
                    when (action) {
                        SearchRefinementAction.CLEAR_QUERY -> onClearQuery()
                        SearchRefinementAction.RESET_FILTER -> onFilterChanged(ReviewHistoryFilter.ALL)
                        SearchRefinementAction.RESET_SORT -> onSortChanged(ReviewHistorySort.NEWEST)
                    }
                },
                onReset = resetView
            )

            SearchOptionGroup(
                presentation = reviewHistoryFilterPresentation(uiState),
                onOptionSelected = { label ->
                    ReviewHistoryFilter.entries.firstOrNull { it.label == label }?.let(onFilterChanged)
                }
            )

            SearchOptionGroup(
                presentation = reviewHistorySortPresentation(uiState),
                onOptionSelected = { label ->
                    ReviewHistorySort.entries.firstOrNull { it.label == label }?.let(onSortChanged)
                }
            )

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
                        ReviewHistoryCard(item, uiState.query)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryCard(
    item: ReviewHistoryItemUi,
    query: String
) {
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
                HighlightedSearchText(item.rating, query, fontWeight = FontWeight.SemiBold)
                HighlightedSearchText(item.reviewedAt, query)
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Metric("Response time", item.responseTime, query, Modifier.weight(1f))
                Metric("Stability", item.stability, query, Modifier.weight(1f))
                Metric("Difficulty", item.difficulty, query, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    query: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        HighlightedSearchText(value, query, fontWeight = FontWeight.Medium)
    }
}
