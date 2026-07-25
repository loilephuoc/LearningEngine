package vn.loi.learning.desktop.ui.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnail
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.search.HighlightedSearchText
import vn.loi.learning.desktop.ui.search.SearchField
import vn.loi.learning.desktop.ui.search.SearchResultSummary

@Composable
fun PackageContentBrowserCard(
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit,
    onSelectRow: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onSortChanged: (BrowserSortOption) -> Unit,
    onResetFilters: () -> Unit,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
) {
    val cardFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    val filteredItems = uiState.filteredItems
    val selectedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        cardFocusRequester.requestFocus()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(cardFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.DirectionDown -> {
                            val currentIndex = filteredItems.indexOfFirst { it.contentId.value == uiState.selectedContentId }
                            if (currentIndex >= 0 && currentIndex < filteredItems.size - 1) {
                                onSelectRow(filteredItems[currentIndex + 1].contentId.value)
                                true
                            } else false
                        }
                        Key.DirectionUp -> {
                            val currentIndex = filteredItems.indexOfFirst { it.contentId.value == uiState.selectedContentId }
                            if (currentIndex > 0) {
                                onSelectRow(filteredItems[currentIndex - 1].contentId.value)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. TOOLBAR
            BrowserToolbar(
                packageName = uiState.packageName,
                totalCount = uiState.totalCount,
                filteredCount = filteredItems.size,
                query = uiState.query,
                selectedLessonFilter = uiState.selectedLessonFilter,
                availableLessons = uiState.availableLessons,
                mediaFilter = uiState.mediaFilter,
                sortOption = uiState.sortOption,
                isFilterDefault = uiState.isFilterDefault,
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                onLessonFilterChanged = onLessonFilterChanged,
                onMediaFilterChanged = onMediaFilterChanged,
                onSortChanged = onSortChanged,
                onResetFilters = onResetFilters,
                onClose = onClose,
                searchFocusRequester = searchFocusRequester
            )

            HorizontalDivider()

            // MAIN CONTENT AREA: Data Table (B) + Preview Panel (C)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // B. DATA TABLE / LIST
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                ) {
                    // Header Row
                    DataTableHeader()

                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No content rows match the current search or filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onResetFilters) {
                                    Text("Clear Search & Filters")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(filteredItems, key = { _, item -> item.contentId.value }) { _, item ->
                                val isSelected = item.contentId.value == uiState.selectedContentId
                                DataTableRow(
                                    item = item,
                                    query = uiState.appliedQuery,
                                    isSelected = isSelected,
                                    onSelect = {
                                        onSelectRow(item.contentId.value)
                                    }
                                )
                            }
                        }
                    }
                }

                // C. PREVIEW PANEL
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    PreviewPanel(
                        item = selectedItem,
                        activePlayingAudioRef = uiState.activePlayingAudioRef,
                        onPlayAudio = onPlayAudio,
                        onStopAudio = onStopAudio,
                        thumbnailLoader = thumbnailLoader
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    packageName: String,
    totalCount: Int,
    filteredCount: Int,
    query: String,
    selectedLessonFilter: String,
    availableLessons: List<String>,
    mediaFilter: BrowserMediaFilter,
    sortOption: BrowserSortOption,
    isFilterDefault: Boolean,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onSortChanged: (BrowserSortOption) -> Unit,
    onResetFilters: () -> Unit,
    onClose: () -> Unit,
    searchFocusRequester: FocusRequester
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$packageName — Learning Browser 1.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Showing $filteredCount of $totalCount rows (1 row per Content)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onClose) {
                Text("Back to Library")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(
                query = query,
                label = "Search question, answer, IPA, POS, lesson...",
                summary = SearchResultSummary(visibleCount = filteredCount, totalCount = totalCount, query = query),
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                focusRequester = searchFocusRequester,
                modifier = Modifier.weight(1.2f)
            )

            // Lesson Dropdown
            LessonDropdownFilter(
                selectedLesson = selectedLessonFilter,
                availableLessons = availableLessons,
                onLessonSelected = onLessonFilterChanged,
                modifier = Modifier.weight(0.9f)
            )

            // Media Filter Dropdown
            MediaDropdownFilter(
                selectedMediaFilter = mediaFilter,
                onMediaFilterSelected = onMediaFilterChanged,
                modifier = Modifier.weight(0.9f)
            )

            // Sort Dropdown
            SortDropdownFilter(
                selectedSort = sortOption,
                onSortSelected = onSortChanged,
                modifier = Modifier.weight(0.9f)
            )

            if (!isFilterDefault) {
                TextButton(onClick = onResetFilters) {
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
private fun LessonDropdownFilter(
    selectedLesson: String,
    availableLessons: List<String>,
    onLessonSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedLesson == "ALL") "All Lessons" else selectedLesson,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Lessons") },
                onClick = {
                    onLessonSelected("ALL")
                    expanded = false
                }
            )
            availableLessons.forEach { lessonName ->
                DropdownMenuItem(
                    text = { Text(lessonName) },
                    onClick = {
                        onLessonSelected(lessonName)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MediaDropdownFilter(
    selectedMediaFilter: BrowserMediaFilter,
    onMediaFilterSelected: (BrowserMediaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedMediaFilter.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowserMediaFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onMediaFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SortDropdownFilter(
    selectedSort: BrowserSortOption,
    onSortSelected: (BrowserSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sort: ${selectedSort.label}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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

@Composable
private fun DataTableHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Question", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Answer", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("IPA", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("POS", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Lesson", modifier = Modifier.weight(1.0f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Img", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Aud", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DataTableRow(
    item: PackageContentBrowserItem,
    query: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .semantics {
                contentDescription = "Row ${item.index}: ${item.questionText}"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.index}",
                modifier = Modifier.width(36.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(modifier = Modifier.weight(1.5f)) {
                HighlightedSearchText(
                    text = item.questionText,
                    query = query,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(modifier = Modifier.weight(1.5f)) {
                HighlightedSearchText(
                    text = item.answerText.ifBlank { "—" },
                    query = query
                )
            }
            Text(
                text = item.pronunciation.ifBlank { "—" },
                modifier = Modifier.width(70.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.partOfSpeech,
                modifier = Modifier.width(60.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(modifier = Modifier.weight(1.0f)) {
                HighlightedSearchText(
                    text = item.lesson,
                    query = query
                )
            }
            Text(
                text = if (item.hasImage) "✓" else "—",
                modifier = Modifier.width(36.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.hasImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Text(
                text = if (item.hasAudio) "✓" else "—",
                modifier = Modifier.width(36.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.hasAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    item: PackageContentBrowserItem?,
    activePlayingAudioRef: String?,
    onPlayAudio: ((String) -> Unit)?,
    onStopAudio: (() -> Unit)?,
    thumbnailLoader: LessonThumbnailLoader
) {
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a row to preview content details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Item #${item.index} Preview",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Question / Primary Text
        Text(
            text = item.questionText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Answer / Translated Text
        if (item.answerText.isNotBlank()) {
            Text(
                text = item.answerText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // IPA & POS Badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.pronunciation.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "[${item.pronunciation}]",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.partOfSpeech,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider()

        // Image Section
        Text(text = "Image Asset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        val imageRef = item.imageRef
        if (item.hasImage && imageRef != null) {
            LessonThumbnail(imageRef, thumbnailLoader)
            Text(text = "File: $imageRef", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        } else {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No image file available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Audio Section
        Text(text = "Audio Asset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        val audioRef = item.audioRef
        if (item.hasAudio && audioRef != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isPlaying = activePlayingAudioRef == audioRef
                Button(
                    onClick = {
                        if (isPlaying) {
                            onStopAudio?.invoke()
                        } else {
                            onPlayAudio?.invoke(audioRef)
                        }
                    }
                ) {
                    Text(if (isPlaying) "Stop Audio" else "Play Audio")
                }
                Text(text = audioRef, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No audio file available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Example Text & Translation
        val exampleText = item.exampleText
        val exampleTranslation = item.exampleTranslation
        if (!exampleText.isNullOrBlank()) {
            HorizontalDivider()
            Text(text = "Example Sentence", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(text = exampleText, style = MaterialTheme.typography.bodyMedium)
            if (!exampleTranslation.isNullOrBlank()) {
                Text(text = exampleTranslation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        // Metadata & Identifiers
        Text(text = "Metadata & Identifiers", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(text = "Content ID: ${item.contentId.value}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Lesson: ${item.lesson}", style = MaterialTheme.typography.bodySmall)
        if (!item.group.isNullOrBlank()) Text(text = "Group: ${item.group}", style = MaterialTheme.typography.bodySmall)
        if (!item.section.isNullOrBlank()) Text(text = "Section: ${item.section}", style = MaterialTheme.typography.bodySmall)

        Text(
            text = "Learning Items (${item.learningItemCount}): ${item.learningModes.joinToString { it.name }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (item.tags.isNotEmpty()) {
            Text(text = "Tags: ${item.tags.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
