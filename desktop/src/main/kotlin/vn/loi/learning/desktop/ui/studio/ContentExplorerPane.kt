package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

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
    onSelectImage: ((String) -> Unit)? = null,
    onPlayQuestionAudio: ((String, String) -> Unit)? = null,
    playbackCoordinator: PlaybackCoordinator? = null,
    modifier: Modifier = Modifier
) {
    val items = uiState.filteredItems
    val listState = rememberLazyListState()

    // Auto-scroll to selected row
    LaunchedEffect(uiState.selectedContentId, uiState.filteredItems) {
        val selectedIndex = uiState.filteredItems.indexOfFirst { it.contentId.value == uiState.selectedContentId }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = LEColors.surface,
        tonalElevation = LEElevation.flat
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Explorer Title & Count Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LESpacing.md, vertical = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CONTENT EXPLORER",
                    style = LETypography.paneTitle,
                    color = LEColors.textSecondary
                )
                Text(
                    text = "${items.size} content items",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
            }

            HorizontalDivider(color = LEColors.borderSubtle)

            // Search & Filter Controls (Matching Approved Mockup)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LESpacing.md),
                verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    LESearchField(
                        query = uiState.appliedQuery,
                        onQueryChanged = onQueryChanged,
                        onClearQuery = onClearQuery,
                        placeholderText = "Search content...",
                        modifier = Modifier.weight(1f)
                    )
                    LEIconButton(
                        icon = LEIcons.Filter,
                        onClick = {},
                        contentDescription = "Filters"
                    )
                }

                // Quick Media Filter Chips (Only image, Only audio, Missing media)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                ) {
                    LEFilterChip(
                        text = "Only image",
                        selected = uiState.mediaFilter == BrowserMediaFilter.HAS_IMAGE,
                        onClick = {
                            onMediaFilterChanged(
                                if (uiState.mediaFilter == BrowserMediaFilter.HAS_IMAGE) BrowserMediaFilter.ALL else BrowserMediaFilter.HAS_IMAGE
                            )
                        }
                    )
                    LEFilterChip(
                        text = "Only audio",
                        selected = uiState.mediaFilter == BrowserMediaFilter.HAS_AUDIO,
                        onClick = {
                            onMediaFilterChanged(
                                if (uiState.mediaFilter == BrowserMediaFilter.HAS_AUDIO) BrowserMediaFilter.ALL else BrowserMediaFilter.HAS_AUDIO
                            )
                        }
                    )
                    LEFilterChip(
                        text = "Missing media",
                        selected = uiState.mediaFilter == BrowserMediaFilter.MISSING_IMAGE,
                        onClick = {
                            onMediaFilterChanged(
                                if (uiState.mediaFilter == BrowserMediaFilter.MISSING_IMAGE) BrowserMediaFilter.ALL else BrowserMediaFilter.MISSING_IMAGE
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = LEColors.borderSubtle)

            // Scrollable Items List (No Pagination)
            Box(modifier = Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(LESpacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching items",
                            style = LETypography.secondaryMetadata,
                            color = LEColors.textMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(LESpacing.xs)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.contentId.value }) { _, item ->
                            ExplorerRowItem(
                                item = item,
                                isSelected = item.contentId.value == uiState.selectedContentId,
                                onSelect = { onSelectRow(item.contentId.value) },
                                onDoubleClick = { onDoubleClickRow?.invoke(item.contentId.value) },
                                onSelectImage = onSelectImage,
                                onPlayQuestionAudio = onPlayQuestionAudio,
                                playbackCoordinator = playbackCoordinator
                            )
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                }
            }

            // Bottom Explorer Total Counter
            Surface(
                color = LEColors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total: ${uiState.allItems.size} items",
                    style = LETypography.caption,
                    color = LEColors.textSecondary,
                    modifier = Modifier.padding(horizontal = LESpacing.md, vertical = LESpacing.xs)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerRowItem(
    item: PackageContentBrowserItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDoubleClick: (() -> Unit)?,
    onSelectImage: ((String) -> Unit)?,
    onPlayQuestionAudio: ((String, String) -> Unit)?,
    playbackCoordinator: PlaybackCoordinator?
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    TooltipArea(
        tooltip = {
            Surface(
                color = LEColors.textPrimary,
                shape = LERadius.xs,
                tonalElevation = LEElevation.popup
            ) {
                Column(modifier = Modifier.padding(LESpacing.sm)) {
                    Text("Q: ${item.questionText}", style = LETypography.caption, color = LEColors.surface)
                    if (item.answerText.isNotBlank()) {
                        Text("A: ${item.answerText}", style = LETypography.caption, color = LEColors.surface)
                    }
                    Text("Lesson: ${item.lesson}", style = LETypography.caption, color = LEColors.textMuted)
                }
            }
        }
    ) {
        Surface(
            color = if (isSelected) LEColors.primarySoft else LEColors.surface,
            shape = LERadius.sm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
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
                    .padding(horizontal = LESpacing.sm, vertical = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row Index #
                Text(
                    text = item.index.toString(),
                    style = LETypography.caption,
                    color = if (isSelected) LEColors.primaryText else LEColors.textMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(28.dp)
                )

                // Question & Answer Summary
                Column(modifier = Modifier.weight(1f).padding(end = LESpacing.xs)) {
                    Text(
                        text = item.questionText,
                        style = LETypography.fieldValue,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) LEColors.primaryText else LEColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.answerText.isNotBlank()) {
                        Text(
                            text = item.answerText,
                            style = LETypography.caption,
                            color = if (isSelected) LEColors.primaryText.copy(alpha = 0.8f) else LEColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Vector Status Icons (Image & Audio - matching Approved Mockup)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.hasImage) {
                        TooltipArea(
                            tooltip = {
                                Surface(color = LEColors.textPrimary, shape = LERadius.xs) {
                                    Text(
                                        text = "View image",
                                        style = LETypography.caption,
                                        color = LEColors.surface,
                                        modifier = Modifier.padding(LESpacing.xs)
                                    )
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { onSelectImage?.invoke(item.contentId.value) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = LEIcons.Image,
                                    contentDescription = "View image",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    val audioRef = item.questionAudioRef
                    if (audioRef != null) {
                        val isPlaying = playbackCoordinator?.getButtonState(audioRef) is AudioButtonState.Playing
                        val tooltipText = if (isPlaying) "Stop question audio" else "Play question audio"
                        TooltipArea(
                            tooltip = {
                                Surface(color = LEColors.textPrimary, shape = LERadius.xs) {
                                    Text(
                                        text = tooltipText,
                                        style = LETypography.caption,
                                        color = LEColors.surface,
                                        modifier = Modifier.padding(LESpacing.xs)
                                    )
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { onPlayQuestionAudio?.invoke(item.contentId.value, audioRef) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) LEIcons.Stop else LEIcons.Audio,
                                    contentDescription = tooltipText,
                                    tint = if (isPlaying) LEColors.primary else Color(0xFF8B5CF6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
