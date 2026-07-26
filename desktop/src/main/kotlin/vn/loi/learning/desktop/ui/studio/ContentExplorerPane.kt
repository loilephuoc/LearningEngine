package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.search.SearchResultSummary
import vn.loi.learning.desktop.ui.search.SearchField

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ContentExplorerPane(
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit,
    onSelectRow: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onSortChanged: (BrowserSortOption) -> Unit,
    onResetFilters: () -> Unit,
    onDoubleClickRow: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val items = uiState.filteredItems
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }

    // Scroll to selected content
    LaunchedEffect(uiState.selectedContentId, uiState.filteredItems) {
        val selectedIndex = uiState.filteredItems.indexOfFirst { it.contentId.value == uiState.selectedContentId }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Content Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("Back to Library")
                }
            }

            Divider()

            // Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(
                    query = uiState.appliedQuery,
                    label = "Search...",
                    summary = SearchResultSummary(visibleCount = items.size, totalCount = uiState.allItems.size, query = uiState.appliedQuery),
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExplorerLessonFilter(
                        selectedLesson = uiState.selectedLessonFilter,
                        availableLessons = uiState.availableLessons,
                        onLessonSelected = onLessonFilterChanged
                    )
                    ExplorerMediaFilter(
                        selectedMediaFilter = uiState.mediaFilter,
                        onMediaFilterSelected = onMediaFilterChanged
                    )
                    ExplorerSortFilter(
                        selectedSort = uiState.sortOption,
                        onSortSelected = onSortChanged
                    )
                    if (uiState.appliedQuery.isNotBlank() || uiState.selectedLessonFilter != "ALL" || uiState.mediaFilter != BrowserMediaFilter.ALL) {
                        TextButton(onClick = onResetFilters) {
                            Text("Clear")
                        }
                    }
                }
            }

            Divider()

            // List Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall)
                Text("Content", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Media", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall)
            }
            Divider()

            // List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.contentId.value }) { index, item ->
                        ExplorerRow(
                            item = item,
                            isSelected = item.contentId.value == uiState.selectedContentId,
                            onSelect = { onSelectRow(item.contentId.value) },
                            onDoubleClick = { onDoubleClickRow?.invoke(item.contentId.value) }
                        )
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState)
                )
            }
            
            // Total/Result count
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${items.size} / ${uiState.allItems.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExplorerRow(
    item: PackageContentBrowserItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDoubleClick: (() -> Unit)?
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val currentTime = System.currentTimeMillis()
                if (onDoubleClick != null && currentTime - lastClickTime < 400L) {
                    onDoubleClick()
                } else {
                    onSelect()
                }
                lastClickTime = currentTime
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.index.toString(),
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.questionText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.answerText.isNotBlank()) {
                    Text(
                        text = item.answerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(modifier = Modifier.width(60.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.hasImage) {
                    Text("Img", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (item.hasAudio) {
                    Text("Aud", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun ExplorerLessonFilter(
    selectedLesson: String,
    availableLessons: List<String>,
    onLessonSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(if (selectedLesson == "ALL") "All Lessons" else selectedLesson, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableLessons.forEach { lesson ->
                DropdownMenuItem(
                    text = { Text(lesson) },
                    onClick = {
                        onLessonSelected(lesson)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExplorerMediaFilter(
    selectedMediaFilter: BrowserMediaFilter,
    onMediaFilterSelected: (BrowserMediaFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedMediaFilter.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BrowserMediaFilter.entries.forEach { media ->
                DropdownMenuItem(
                    text = { Text(media.label) },
                    onClick = {
                        onMediaFilterSelected(media)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExplorerSortFilter(
    selectedSort: BrowserSortOption,
    onSortSelected: (BrowserSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedSort.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BrowserSortOption.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}
