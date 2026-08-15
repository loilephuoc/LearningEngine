package vn.loi.learning.desktop.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.isCtrlPressed as isPointerCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ContentExplorerPane(
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit,
    onSelectRow: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onToggleHighlight: (String) -> Unit,
    onToggleMultiSelection: (String) -> Unit = {},
    onSelectMultiRange: (String) -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onClearMultiSelection: () -> Unit = {},
    onHighlightSelected: () -> Unit = {},
    onRemoveHighlightSelected: () -> Unit = {},
    onCheckSelectedMedia: () -> Unit = {},
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onSortChanged: (BrowserSortOption) -> Unit,
    onProblemFilterChanged: (ContentProblemFilter) -> Unit = {},
    onPreviousProblem: () -> Unit = {},
    onNextProblem: () -> Unit = {},
    onResetFilters: () -> Unit,
    onDoubleClickRow: ((String) -> Unit)?,
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
    val listFocusRequester = remember { FocusRequester() }
    var handledCenterRequest by remember(uiState.installedPackageId) {
        mutableLongStateOf(uiState.centerSelectedRowRequest)
    }

    // Preserve the viewport when the selected row is already visible.
    // Scroll only when keyboard/search navigation selects an off-screen row.
    LaunchedEffect(uiState.selectedContentId, items) {
        // Clearing search has its own centering authority below. Let that effect be the
        // only scroll writer for this projection change so the two animations cannot race.
        if (handledCenterRequest != uiState.centerSelectedRowRequest) return@LaunchedEffect
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

    LaunchedEffect(uiState.centerSelectedRowRequest, items) {
        if (handledCenterRequest == uiState.centerSelectedRowRequest) return@LaunchedEffect
        handledCenterRequest = uiState.centerSelectedRowRequest
        val selectedIndex = items.indexOfFirst { it.contentId.value == uiState.selectedContentId }
        if (selectedIndex < 0) return@LaunchedEffect
        val visibleCount = snapshotFlow {
            listState.layoutInfo.totalItemsCount to listState.layoutInfo.visibleItemsInfo.size
        }.first { (total, visible) -> total == items.size && visible > 0 }.second
        val targetFirstIndex = centeredExplorerFirstIndex(selectedIndex, items.size, visibleCount)
        listState.animateScrollToItem(targetFirstIndex)
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
            val searchKeyModifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Escape && uiState.appliedQuery.isNotBlank() -> { onClearQuery(); true }
                        else -> false
                    }
                }
            val searchModifier = if (searchFocusRequester != null) {
                searchKeyModifier.focusRequester(searchFocusRequester)
            } else searchKeyModifier

            CompactExplorerSearchField(
                query = uiState.query,
                onQueryChanged = onQueryChanged,
                onClearQuery = onClearQuery,
                onSubmit = onSubmitSearch,
                placeholderText = "Search content... (Ctrl+F)",
                modifier = searchModifier
                    .padding(horizontal = LESpacing.sm, vertical = 5.dp)
            )

            ProblemNavigationControls(
                uiState = uiState,
                onFilterChanged = onProblemFilterChanged,
                onPrevious = onPreviousProblem,
                onNext = onNextProblem
            )

            if (uiState.selectedContentIds.isNotEmpty()) {
                MultiSelectionActions(
                    selectedCount = uiState.selectedContentIds.size,
                    visibleSelectedCount = items.count { it.contentId.value in uiState.selectedContentIds },
                    onHighlightSelected = onHighlightSelected,
                    onRemoveHighlightSelected = onRemoveHighlightSelected,
                    onCheckSelectedMedia = onCheckSelectedMedia,
                    onClearSelection = onClearMultiSelection
                )
            }

            HorizontalDivider(color = LEColors.borderSubtle)

            // Scrollable Items List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onPointerEvent(PointerEventType.Press) { event ->
                        if (event.button == PointerButton.Primary) listFocusRequester.requestFocus()
                    }
            ) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(listFocusRequester)
                            .focusable()
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                when {
                                    event.isCtrlPressed && event.key == Key.A -> { onSelectAllVisible(); true }
                                    event.key == Key.Escape && uiState.appliedQuery.isBlank() && uiState.selectedContentIds.isNotEmpty() -> {
                                        onClearMultiSelection(); true
                                    }
                                    else -> false
                                }
                            },
                        // Compact padding so the explorer can show more rows at once.
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.contentId.value }) { _, item ->
                            ExplorerRowItem(
                                item = item,
                                isSelected = item.contentId.value == uiState.selectedContentId,
                                isMultiSelected = item.contentId.value in uiState.selectedContentIds,
                                isHighlighted = item.contentId.value in uiState.highlightedContentIds,
                                problemCount = uiState.problemProjection.problemsFor(item.contentId.value).size,
                                onSelect = {
                                    listFocusRequester.requestFocus()
                                    onSelectRow(item.contentId.value)
                                },
                                onModifiedSelect = { ctrl, shift ->
                                    listFocusRequester.requestFocus()
                                    when {
                                        shift -> onSelectMultiRange(item.contentId.value)
                                        ctrl -> onToggleMultiSelection(item.contentId.value)
                                    }
                                },
                                onDoubleClick = { onDoubleClickRow?.invoke(item.contentId.value) },
                                onToggleHighlight = { onToggleHighlight(item.contentId.value) },
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
private fun MultiSelectionActions(
    selectedCount: Int,
    visibleSelectedCount: Int,
    onHighlightSelected: () -> Unit,
    onRemoveHighlightSelected: () -> Unit,
    onCheckSelectedMedia: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = LESpacing.sm, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            if (visibleSelectedCount == selectedCount) "$selectedCount selected"
            else "$selectedCount selected · $visibleSelectedCount visible",
            style = LETypography.caption,
            color = LEColors.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "$selectedCount selected items" }
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(
                onClick = onHighlightSelected,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                modifier = Modifier.semantics { contentDescription = "Highlight $selectedCount selected items" }
            ) { Text("Highlight Selected", style = LETypography.caption) }
            TextButton(
                onClick = onRemoveHighlightSelected,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                modifier = Modifier.semantics { contentDescription = "Remove highlight from $selectedCount selected items" }
            ) { Text("Remove Highlight", style = LETypography.caption) }
            TextButton(
                onClick = onCheckSelectedMedia,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                modifier = Modifier.semantics { contentDescription = "Check media for $selectedCount selected items" }
            ) { Text("Check Selected Media", style = LETypography.caption) }
            TextButton(
                onClick = onClearSelection,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
            ) { Text("Clear Selection", style = LETypography.caption) }
        }
    }
}

@Composable
private fun ProblemNavigationControls(
    uiState: PackageContentBrowserUiState,
    onFilterChanged: (ContentProblemFilter) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = uiState.filteredItems
    val selectedIndex = items.indexOfFirst { it.contentId.value == uiState.selectedContentId }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = LESpacing.sm, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Problems: ${uiState.problemProjection.problematicContentCount}",
                style = LETypography.caption,
                color = LEColors.textSecondary
            )
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.semantics {
                        contentDescription = "Problem filter. Selected ${uiState.problemFilter.label}"
                    }
                ) { Text(uiState.problemFilter.label, style = LETypography.caption) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ContentProblemFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                val count = filter.problem?.let(uiState.problemProjection::count)
                                Text(if (count == null) filter.label else "${filter.label} ($count)")
                            },
                            onClick = { expanded = false; onFilterChanged(filter) }
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = uiState.problemFilter != ContentProblemFilter.NONE && selectedIndex > 0,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).heightIn(min = 30.dp)
            ) { Text("← Previous Problem", style = LETypography.caption, maxLines = 1) }
            OutlinedButton(
                onClick = onNext,
                enabled = uiState.problemFilter != ContentProblemFilter.NONE && selectedIndex >= 0 && selectedIndex < items.lastIndex,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).heightIn(min = 30.dp)
            ) { Text("Next Problem →", style = LETypography.caption, maxLines = 1) }
        }
        if (uiState.problemFilter != ContentProblemFilter.NONE && items.isEmpty()) {
            Text(
                if (uiState.problemProjection.problematicContentCount == 0) "No problems found"
                else "No items with ${uiState.problemFilter.label}",
                style = LETypography.caption,
                color = LEColors.textMuted
            )
        }
    }
}

internal fun centeredExplorerFirstIndex(
    selectedIndex: Int,
    itemCount: Int,
    visibleItemCount: Int
): Int {
    val capacity = visibleItemCount.coerceAtLeast(1)
    val maxFirstIndex = (itemCount - capacity).coerceAtLeast(0)
    return (selectedIndex - capacity / 2).coerceIn(0, maxFirstIndex)
}

@Composable
private fun CompactExplorerSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmit: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = LERadius.sm,
        color = LEColors.primarySoft.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.45f)),
        tonalElevation = LEElevation.flat,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(38.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter -> {
                        onSubmit(query)
                        true
                    }
                    Key.Escape -> {
                        if (query.isBlank()) return@onPreviewKeyEvent false
                        onClearQuery()
                        true
                    }
                    else -> false
                }
            }
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
                value = query,
                onValueChange = { newText ->
                    onQueryChanged(newText)
                },
                singleLine = true,
                textStyle = LETypography.fieldValue.copy(color = LEColors.textPrimary, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .weight(1f)
                    .testTag("explorer-search-input"),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
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

            if (query.isNotBlank()) {
                Text(
                    text = "×",
                    style = LETypography.fieldValue,
                    color = LEColors.textMuted,
                    modifier = Modifier
                        .testTag("explorer-clear-search")
                        .clip(LERadius.xs)
                        .clickable {
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ExplorerRowItem(
    item: PackageContentBrowserItem,
    isSelected: Boolean,
    isMultiSelected: Boolean,
    isHighlighted: Boolean,
    problemCount: Int,
    onSelect: () -> Unit,
    onModifiedSelect: (ctrl: Boolean, shift: Boolean) -> Unit,
    onDoubleClick: (() -> Unit)?,
    onToggleHighlight: () -> Unit,
    onPlayQuestionAudio: ((String, String) -> Unit)?,
    playbackCoordinator: PlaybackCoordinator?,
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    var lastModifiedClickTime by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Animated background color — selected > hover > default
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> LEColors.primarySoft
            isMultiSelected -> LEColors.primarySoft.copy(alpha = 0.55f)
            isHighlighted -> LEColors.warningContainer
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
                ContextMenuItem(if (isHighlighted) "Remove Highlight" else "Highlight Item") { onToggleHighlight() },
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
                    .testTag("explorer-row-${item.contentId.value}")
                    .semantics {
                        contentDescription = "Row ${item.index}: ${item.questionText}"
                        selected = isMultiSelected
                        stateDescription = buildList {
                            if (isMultiSelected) add("Selected")
                            if (isHighlighted) add("Highlighted")
                            if (problemCount > 0) add("$problemCount problems")
                        }.joinToString(", ")
                    }
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
                        if (isHighlighted) {
                            drawCircle(
                                color = LEColors.warning,
                                radius = 3.dp.toPx(),
                                center = Offset(6.dp.toPx(), size.height / 2f)
                            )
                        }
                        if (isMultiSelected) {
                            drawLine(
                                color = LEColors.primary,
                                start = Offset(size.width - 2.dp.toPx(), 2.dp.toPx()),
                                end = Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    .hoverable(interactionSource)
                    .pointerInput(item.contentId.value, onSelect, onModifiedSelect, onDoubleClick) {
                        awaitEachGesture {
                            val downEvent = awaitPointerEvent(PointerEventPass.Main)
                            if (downEvent.button != PointerButton.Primary) return@awaitEachGesture
                            val down = downEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                                ?: return@awaitEachGesture
                            val ctrl = downEvent.keyboardModifiers.isPointerCtrlPressed
                            val shift = downEvent.keyboardModifiers.isPointerShiftPressed
                            down.consume()
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            up.consume()
                            val now = System.currentTimeMillis()
                            if (ctrl || shift) {
                                if (now - lastModifiedClickTime >= 400L) {
                                    onModifiedSelect(ctrl, shift)
                                }
                                lastModifiedClickTime = now
                            } else {
                                if (onDoubleClick != null && now - lastClickTime < 400L) {
                                    onDoubleClick()
                                } else {
                                    onSelect()
                                }
                                lastClickTime = now
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = LESpacing.sm + 2.dp, end = LESpacing.sm, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(LERadius.xs)
                            .border(1.dp, if (isMultiSelected) LEColors.primary else LEColors.borderSubtle, LERadius.xs)
                            .background(if (isMultiSelected) LEColors.primary else LEColors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMultiSelected) {
                            Text("✓", style = LETypography.caption, color = LEColors.surface)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
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
                                        tint = if (isPlaying) LEColors.primary else LEColors.info,
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
