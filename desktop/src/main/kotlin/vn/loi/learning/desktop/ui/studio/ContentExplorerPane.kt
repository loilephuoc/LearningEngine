package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchResultSummary

@OptIn(ExperimentalLayoutApi::class)
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

    // Scroll to selected content automatically
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
            // Explorer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Content Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${items.size} / ${uiState.allItems.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Explorer Search & Filter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(
                    query = uiState.appliedQuery,
                    label = "Search Explorer...",
                    summary = SearchResultSummary(
                        visibleCount = items.size,
                        totalCount = uiState.allItems.size,
                        query = uiState.appliedQuery
                    ),
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // List Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Content / Answer", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Media", modifier = Modifier.width(54.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Native Scrollable List (No pagination)
            Box(modifier = Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.contentId.value }) { _, item ->
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerRow(
    item: PackageContentBrowserItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDoubleClick: (() -> Unit)?
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    TooltipArea(
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = MaterialTheme.shapes.extraSmall,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Q: ${item.questionText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
                    if (item.answerText.isNotBlank()) {
                        Text("A: ${item.answerText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    Text("Lesson: ${item.lesson}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f))
                }
            }
        }
    ) {
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
                    .let {
                        if (isSelected) {
                            it.background(MaterialTheme.colorScheme.primaryContainer)
                        } else it
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index #
                Text(
                    text = item.index.toString(),
                    modifier = Modifier.width(32.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Question & Answer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.questionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.answerText.isNotBlank()) {
                        Text(
                            text = item.answerText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Media Status Badges
                Row(
                    modifier = Modifier.width(54.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (item.hasImage) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "Img",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (item.hasAudio) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "Aud",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
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
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(if (selectedLesson == "ALL") "All Lessons" else selectedLesson, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All Lessons") },
                onClick = {
                    onLessonSelected("ALL")
                    expanded = false
                }
            )
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
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(selectedMediaFilter.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
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
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(selectedSort.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
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
