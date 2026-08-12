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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
    val listScrollScope = rememberCoroutineScope()

    // Preserve the viewport when the selected row is already visible.
    // Scroll only when keyboard/search navigation selects an off-screen row.
    LaunchedEffect(uiState.selectedContentId, items) {
        val selectedIndex = items.indexOfFirst { it.contentId.value == uiState.selectedContentId }
        if (selectedIndex < 0) return@LaunchedEffect

        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val firstVisibleIndex = visibleItems.firstOrNull()?.index
        val lastVisibleIndex = visibleItems.lastOrNull()?.index

        when {
            firstVisibleIndex == null || lastVisibleIndex == null ->
                listState.scrollToItem(selectedIndex)

            selectedIndex < firstVisibleIndex ->
                listState.animateScrollToItem(selectedIndex)

            selectedIndex > lastVisibleIndex -> {
                val visibleItemCount =
                    (lastVisibleIndex - firstVisibleIndex + 1).coerceAtLeast(1)
                val targetFirstIndex =
                    (selectedIndex - visibleItemCount + 1).coerceAtLeast(0)
                listState.animateScrollToItem(targetFirstIndex)
            }
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
                    .padding(horizontal = LESpacing.md, vertical = 3.dp),
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

            // Compact search only. The rarely-used quick media chips were removed so the
            // list gets substantially more vertical space.
            val searchModifier = if (searchFocusRequester != null) {
                Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && uiState.appliedQuery.isNotBlank()) {
                            onClearQuery()
                            true
                        } else false
                    }
            } else Modifier.fillMaxWidth()

            CompactExplorerSearchField(
                query = uiState.appliedQuery,
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                placeholderText = "Search content... (Ctrl+F)",
                modifier = searchModifier
                    .padding(horizontal = LESpacing.sm, vertical = 5.dp)
            )

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
                        // Compact padding so the explorer can show more rows at once.
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
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

                    // Focus-preserving navigation: each click moves exactly 2 items.
                    // This keeps the current neighborhood visible instead of jumping a full page.
                    val stepItems = 2
                    if (listState.canScrollBackward) {
                        ExplorerPageButton(
                            text = "▲",
                            contentDescription = "Previous page",
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                            onClick = {
                                val target = (listState.firstVisibleItemIndex - stepItems).coerceAtLeast(0)
                                listScrollScope.launch { listState.animateScrollToItem(target) }
                            }
                        )
                    }
                    if (listState.canScrollForward) {
                        ExplorerPageButton(
                            text = "▼",
                            contentDescription = "Next page",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                            onClick = {
                                val maxIndex = (items.size - 1).coerceAtLeast(0)
                                val target = (listState.firstVisibleItemIndex + stepItems).coerceAtMost(maxIndex)
                                listScrollScope.launch { listState.animateScrollToItem(target) }
                            }
                        )
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

@Composable
private fun CompactExplorerSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier
) {
    var rawText by remember(query) { mutableStateOf(query) }

    Surface(
        shape = LERadius.sm,
        color = LEColors.primarySoft.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.45f)),
        tonalElevation = LEElevation.flat,
        shadowElevation = 1.dp,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = LEIcons.Search,
                contentDescription = "Search",
                tint = LEColors.primary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = rawText,
                onValueChange = { newText ->
                    rawText = newText
                    onQueryChanged(newText)
                },
                singleLine = true,
                textStyle = LETypography.fieldValue.copy(color = LEColors.textPrimary, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (rawText.isEmpty()) {
                            Text(
                                text = placeholderText,
                                style = LETypography.fieldValue,
                                color = LEColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (rawText.isNotBlank()) {
                Text(
                    text = "×",
                    style = LETypography.fieldValue,
                    color = LEColors.textMuted,
                    modifier = Modifier
                        .clip(LERadius.xs)
                        .clickable {
                            rawText = ""
                            onClearQuery()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExplorerPageButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(LERadius.sm)
            .clickable(onClick = onClick),
        color = LEColors.surfaceElevated.copy(alpha = 0.96f),
        contentColor = LEColors.primary,
        tonalElevation = LEElevation.popup,
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = LETypography.caption,
                fontWeight = FontWeight.Bold,
                color = LEColors.primary
            )
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
                    .padding(vertical = 0.dp)
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
                        .padding(start = LESpacing.sm + 2.dp, end = LESpacing.sm, top = 2.dp, bottom = 2.dp),
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

                    // Ultra-compact single-line summary: question + answer stay visible on one row.
                    Row(
                        modifier = Modifier.weight(1f).padding(end = LESpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.questionText,
                            style = LETypography.fieldValue,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) LEColors.primaryText else LEColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.56f)
                        )
                        if (item.answerText.isNotBlank()) {
                            Text(
                                text = "  ·  ${item.answerText}",
                                style = LETypography.caption,
                                color = if (isSelected) LEColors.primaryText.copy(alpha = 0.8f) else LEColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.44f)
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