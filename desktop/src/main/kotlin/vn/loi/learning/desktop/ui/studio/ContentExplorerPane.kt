package vn.loi.learning.desktop.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
    // PLE-020: search field focus requester for Ctrl+F
    searchFocusRequester: FocusRequester? = null,
    // PLE-020: context menu callbacks
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null,
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
                    text = "${items.size} items",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
            }

            HorizontalDivider(color = LEColors.borderSubtle)

            // Search & Filter Controls
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
                    // PLE-020: wire Ctrl+F search focus + Esc to clear
                    val searchModifier = if (searchFocusRequester != null) {
                        Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && uiState.appliedQuery.isNotBlank()) {
                                    onClearQuery()
                                    true
                                } else false
                            }
                    } else Modifier.weight(1f)

                    LESearchField(
                        query = uiState.appliedQuery,
                        onQueryChanged = onQueryChanged,
                        onClearQuery = onClearQuery,
                        placeholderText = "Search content... (Ctrl+F)",
                        modifier = searchModifier
                    )
                    LEIconButton(
                        icon = LEIcons.Filter,
                        onClick = {},
                        contentDescription = "Filters"
                    )
                }

                // Quick Media Filter Chips
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

            // Scrollable Items List
            Box(modifier = Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(LESpacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            Icon(
                                imageVector = LEIcons.Search,
                                contentDescription = null,
                                tint = LEColors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (uiState.appliedQuery.isNotBlank()) "No results for \"${uiState.appliedQuery}\""
                                       else "No content items",
                                style = LETypography.secondaryMetadata,
                                color = LEColors.textMuted
                            )
                            if (uiState.appliedQuery.isNotBlank()) {
                                Text(
                                    text = "Press Esc to clear search",
                                    style = LETypography.caption,
                                    color = LEColors.textMuted
                                )
                            }
                        }
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
                                playbackCoordinator = playbackCoordinator,
                                onDuplicateItem = onDuplicateItem,
                                onCopyQuestion = onCopyQuestion,
                                onCopyAnswer = onCopyAnswer
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
                    text = "Total: ${uiState.allItems.size} items  ·  ↑↓ navigate",
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
    playbackCoordinator: PlaybackCoordinator?,
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Animated background color — selected > hover > default
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> LEColors.primarySoft
            isHovered -> LEColors.surfaceElevated
            else -> LEColors.surface
        },
        animationSpec = tween(durationMillis = 120),
        label = "rowBg"
    )

    // PLE-020: Right-click context menu using ContextMenuArea (Compose Desktop)
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Edit") { onSelect() },
                ContextMenuItem("Duplicate") { onDuplicateItem?.invoke(item.contentId.value) },
                ContextMenuItem("Delete") { /* handled by toolbar */ },
                ContextMenuItem("Copy Question") { onCopyQuestion?.invoke(item.questionText) },
                ContextMenuItem("Copy Answer") { onCopyAnswer?.invoke(item.answerText) }
            )
        }
    ) {
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
                        Text("Right-click for options", style = LETypography.caption, color = LEColors.textMuted.copy(alpha = 0.7f))
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(LERadius.sm)
                    .background(bgColor)
                    // PLE-020: left accent border on selection
                    .drawBehind {
                        if (isSelected) {
                            drawLine(
                                color = LEColors.primary,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                    .hoverable(interactionSource)
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
                        .padding(start = LESpacing.sm + 2.dp, end = LESpacing.sm, top = LESpacing.sm, bottom = LESpacing.sm),
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

                    // Vector Status Icons
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
}
