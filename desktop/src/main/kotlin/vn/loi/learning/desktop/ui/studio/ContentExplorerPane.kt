package vn.loi.learning.desktop.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowDropDown
import vn.loi.learning.application.contentmedia.MediaReferencePolicy
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.DuplicateImageGroup
import vn.loi.learning.desktop.ui.browser.ImageStatusFilter
import vn.loi.learning.desktop.ui.browser.ImageStatusProjectionPolicy
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter
import vn.loi.learning.desktop.ui.browser.ContentProblemProjection
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
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
    onPosReviewSelected: (() -> Unit)? = null,
    onExportJsonSelected: (() -> Unit)? = null,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onImageStatusFilterChanged: (ImageStatusFilter) -> Unit = {},
    onSortChanged: (BrowserSortOption) -> Unit,
    onProblemFilterChanged: (ContentProblemFilter) -> Unit = {},
    onPreviousProblem: () -> Unit = {},
    onNextProblem: () -> Unit = {},
    onResetFilters: () -> Unit,
    onDoubleClickRow: ((String) -> Unit)?,
    onPlayQuestionAudio: ((String, String) -> Unit)? = null,
    playbackCoordinator: PlaybackCoordinator? = null,
    thumbnailLoader: LessonThumbnailLoader? = null,
    // PLE-020: search field focus requester for Ctrl+F
    searchFocusRequester: FocusRequester? = null,
    // PLE-020: context menu callbacks
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null,
    onRequestGenerateTts: ((contentId: String, field: vn.loi.learning.desktop.tts.TtsField?) -> Unit)? = null,
    onRequestGenerateTtsBatch: ((contentIds: Set<String>) -> Unit)? = null,
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
        val selectedIndex = if (uiState.imageStatusFilter == ImageStatusFilter.DUPLICATE_IMAGE) {
            uiState.duplicateImageGroups.indexOfFirst { group ->
                group.items.any { it.contentId.value == uiState.selectedContentId }
            }
        } else {
            items.indexOfFirst { it.contentId.value == uiState.selectedContentId }
        }
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
        if (uiState.imageStatusFilter == ImageStatusFilter.DUPLICATE_IMAGE) {
            val groupIndex = uiState.duplicateImageGroups.indexOfFirst { group ->
                group.items.any { it.contentId.value == uiState.selectedContentId }
            }
            if (groupIndex >= 0) {
                listState.animateScrollToItem(groupIndex)
            }
            return@LaunchedEffect
        }
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

            // Compact search and Image Status Filter row
            val searchKeyModifier = Modifier
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LESpacing.sm, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactExplorerSearchField(
                    query = uiState.query,
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    onSubmit = onSubmitSearch,
                    placeholderText = "Search text or #item... (Ctrl+F)",
                    modifier = searchModifier.weight(1f)
                )

                ImageStatusDropdownFilter(
                    selectedFilter = uiState.imageStatusFilter,
                    selectedProblemFilter = uiState.problemFilter,
                    onFilterSelected = { filter ->
                        onImageStatusFilterChanged(filter)
                        if (uiState.problemFilter == ContentProblemFilter.MISSING_ANY_AUDIO) {
                            onProblemFilterChanged(ContentProblemFilter.NONE)
                        }
                    },
                    onMissingAnyAudioSelected = {
                        onImageStatusFilterChanged(ImageStatusFilter.ALL)
                        onProblemFilterChanged(ContentProblemFilter.MISSING_ANY_AUDIO)
                    }
                )
            }

            ProblemNavigationControls(
                uiState = uiState,
                onFilterChanged = onProblemFilterChanged,
                onPrevious = onPreviousProblem,
                onNext = onNextProblem,
                onRequestGenerateTts = onRequestGenerateTts
            )

            if (uiState.selectedContentIds.isNotEmpty()) {
                MultiSelectionActions(
                    selectedCount = uiState.selectedContentIds.size,
                    visibleSelectedCount = items.count { it.contentId.value in uiState.selectedContentIds },
                    onHighlightSelected = onHighlightSelected,
                    onRemoveHighlightSelected = onRemoveHighlightSelected,
                    onCheckSelectedMedia = onCheckSelectedMedia,
                    onPosReviewSelected = onPosReviewSelected,
                    onExportJsonSelected = onExportJsonSelected,
                    onGenerateTts = when {
                        onRequestGenerateTtsBatch != null -> {
                            { onRequestGenerateTtsBatch(uiState.selectedContentIds) }
                        }
                        uiState.selectedContentIds.size == 1 && onRequestGenerateTts != null -> {
                            { onRequestGenerateTts(uiState.selectedContentIds.first(), null) }
                        }
                        else -> null
                    },
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
                        if (uiState.imageStatusFilter == ImageStatusFilter.DUPLICATE_IMAGE) {
                            items(uiState.duplicateImageGroups, key = { "group_${it.imageKey}" }) { group ->
                                DuplicateGroupCard(
                                    group = group,
                                    packageName = uiState.packageName,
                                    selectedContentId = uiState.selectedContentId,
                                    selectedContentIds = uiState.selectedContentIds,
                                    highlightedContentIds = uiState.highlightedContentIds,
                                    thumbnailLoader = thumbnailLoader,
                                    problemProjection = uiState.problemProjection,
                                    onSelectRow = { id ->
                                        listFocusRequester.requestFocus()
                                        onSelectRow(id)
                                    },
                                    onModifiedSelect = { id, ctrl, shift ->
                                        listFocusRequester.requestFocus()
                                        when {
                                            shift -> onSelectMultiRange(id)
                                            ctrl -> onToggleMultiSelection(id)
                                        }
                                    },
                                    onDoubleClickRow = onDoubleClickRow,
                                    onToggleHighlight = onToggleHighlight,
                                    onPlayQuestionAudio = onPlayQuestionAudio,
                                    playbackCoordinator = playbackCoordinator,
                                    onDuplicateItem = onDuplicateItem,
                                    onCopyQuestion = onCopyQuestion,
                                    onCopyAnswer = onCopyAnswer,
                                    onRequestGenerateTts = onRequestGenerateTts
                                )
                            }
                        } else {
                            itemsIndexed(items, key = { _, item -> item.contentId.value }) { _, item ->
                                ExplorerRowItem(
                                    item = item,
                                    isSelected = item.contentId.value == uiState.selectedContentId,
                                    isMultiSelected = item.contentId.value in uiState.selectedContentIds,
                                    isHighlighted = item.contentId.value in uiState.highlightedContentIds,
                                    problemCount = uiState.problemProjection.problemsFor(item.contentId.value).size,
                                    duplicateInfo = null,
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
                                    onCopyAnswer = onCopyAnswer,
                                    onRequestGenerateTts = onRequestGenerateTts
                                )
                            }
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
    onPosReviewSelected: (() -> Unit)? = null,
    onExportJsonSelected: (() -> Unit)? = null,
    onGenerateTts: (() -> Unit)? = null,
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
            if (onPosReviewSelected != null) {
                TextButton(
                    onClick = onPosReviewSelected,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                    modifier = Modifier.semantics { contentDescription = "POS Review for $selectedCount selected items" }
                ) { Text("POS Review", style = LETypography.caption) }
            }
            if (onExportJsonSelected != null) {
                TextButton(
                    onClick = onExportJsonSelected,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                    modifier = Modifier.semantics { contentDescription = "Export JSON for $selectedCount selected items" }
                ) { Text("Export JSON", style = LETypography.caption) }
            }
            if (onGenerateTts != null) {
                TextButton(
                    onClick = onGenerateTts,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                    modifier = Modifier.semantics { contentDescription = "Generate Audio" }
                ) { Text("Generate Audio", style = LETypography.caption, color = LEColors.primary, fontWeight = FontWeight.Bold) }
            }
            TextButton(
                onClick = onClearSelection,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
            ) { Text("Clear Selection", style = LETypography.caption) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProblemNavigationControls(
    uiState: PackageContentBrowserUiState,
    onFilterChanged: (ContentProblemFilter) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRequestGenerateTts: ((contentId: String, field: vn.loi.learning.desktop.tts.TtsField?) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val items = uiState.filteredItems
    val selectedIndex = items.indexOfFirst { it.contentId.value == uiState.selectedContentId }
    val problemCount = uiState.problemProjection.problematicContentCount
    val isProblemActive = uiState.problemFilter != ContentProblemFilter.NONE
    val canPrev = isProblemActive && selectedIndex > 0
    val canNext = isProblemActive && selectedIndex >= 0 && selectedIndex < items.lastIndex

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = LESpacing.sm, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Problem badge & Dropdown Filter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = if (problemCount > 0) LEColors.warningContainer else LEColors.surfaceElevated,
                shape = LERadius.xs,
                modifier = Modifier.semantics { contentDescription = "Problems: $problemCount" }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "⚠",
                        style = LETypography.caption,
                        color = if (problemCount > 0) LEColors.warning else LEColors.textMuted
                    )
                    Text(
                        text = "$problemCount",
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = if (problemCount > 0) LEColors.warning else LEColors.textMuted
                    )
                }
            }

            Box {
                Surface(
                    shape = LERadius.xs,
                    color = if (isProblemActive) LEColors.primarySoft else androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isProblemActive) LEColors.primary else LEColors.borderSubtle),
                    modifier = Modifier
                        .clip(LERadius.xs)
                        .clickable { expanded = true }
                        .semantics {
                            contentDescription = "Problem filter. Selected ${uiState.problemFilter.label}"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = uiState.problemFilter.label,
                            style = LETypography.caption,
                            fontWeight = if (isProblemActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isProblemActive) LEColors.primary else LEColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "▾",
                            style = LETypography.caption,
                            color = if (isProblemActive) LEColors.primary else LEColors.textMuted
                        )
                    }
                }

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

        // Right: Compact Previous / Next icon buttons + Generate Audio action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val selectedItem = items.firstOrNull { it.contentId.value == uiState.selectedContentId }
            val hasMissingAudio = selectedItem != null && (
                selectedItem.questionAudioRef.isNullOrBlank() ||
                selectedItem.answerAudioRef.isNullOrBlank() ||
                selectedItem.exampleAudioRef.isNullOrBlank() ||
                selectedItem.translationAudioRef.isNullOrBlank()
            )
            if (hasMissingAudio && onRequestGenerateTts != null && uiState.selectedContentId != null) {
                Surface(
                    shape = LERadius.xs,
                    color = LEColors.primary,
                    modifier = Modifier
                        .height(26.dp)
                        .clip(LERadius.xs)
                        .clickable { onRequestGenerateTts(uiState.selectedContentId, null) }
                        .semantics { contentDescription = "Generate Audio" }
                        .testTag("explorer-generate-audio-button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = LEIcons.Audio,
                            contentDescription = null,
                            tint = LEColors.surface,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Generate Audio",
                            style = LETypography.caption,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.surface
                        )
                    }
                }
            }

            TooltipArea(
                tooltip = {
                    Surface(color = LEColors.textPrimary, shape = LERadius.xs) {
                        Text("Previous Problem", style = LETypography.caption, color = LEColors.surface, modifier = Modifier.padding(LESpacing.xs))
                    }
                }
            ) {
                Surface(
                    shape = LERadius.xs,
                    color = if (canPrev) LEColors.surfaceElevated else LEColors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (canPrev) LEColors.borderSubtle else LEColors.borderSubtle.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(width = 28.dp, height = 26.dp)
                        .clip(LERadius.xs)
                        .clickable(enabled = canPrev, onClick = onPrevious)
                        .semantics { contentDescription = "Previous Problem" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "‹",
                            style = LETypography.paneTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (canPrev) LEColors.primary else LEColors.textMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            TooltipArea(
                tooltip = {
                    Surface(color = LEColors.textPrimary, shape = LERadius.xs) {
                        Text("Next Problem", style = LETypography.caption, color = LEColors.surface, modifier = Modifier.padding(LESpacing.xs))
                    }
                }
            ) {
                Surface(
                    shape = LERadius.xs,
                    color = if (canNext) LEColors.surfaceElevated else LEColors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (canNext) LEColors.borderSubtle else LEColors.borderSubtle.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(width = 28.dp, height = 26.dp)
                        .clip(LERadius.xs)
                        .clickable(enabled = canNext, onClick = onNext)
                        .semantics { contentDescription = "Next Problem" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "›",
                            style = LETypography.paneTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (canNext) LEColors.primary else LEColors.textMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }
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
    duplicateInfo: Pair<String, Int>? = null,
    onSelect: () -> Unit,
    onModifiedSelect: (ctrl: Boolean, shift: Boolean) -> Unit,
    onDoubleClick: (() -> Unit)?,
    onToggleHighlight: () -> Unit,
    onPlayQuestionAudio: ((String, String) -> Unit)?,
    playbackCoordinator: PlaybackCoordinator?,
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null,
    onRequestGenerateTts: ((contentId: String, field: vn.loi.learning.desktop.tts.TtsField?) -> Unit)? = null
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
            listOfNotNull(
                ContextMenuItem(if (isHighlighted) "Remove Highlight" else "Highlight Item") { onToggleHighlight() },
                ContextMenuItem("Edit") { onSelect() },
                if (onRequestGenerateTts != null) ContextMenuItem("Generate Audio (TTS)") { onRequestGenerateTts(item.contentId.value, null) } else null,
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
                    // Row Index # (Stable canonical package number)
                    Text(
                        text = item.index.toString(),
                        style = LETypography.caption,
                        color = if (isSelected) LEColors.primaryText else LEColors.textMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.widthIn(min = 32.dp).padding(end = 4.dp)
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

                    // Vector Status Icons & Duplicate Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (duplicateInfo != null) {
                            val (dupKey, count) = duplicateInfo
                            TooltipArea(
                                tooltip = {
                                    Surface(color = LEColors.textPrimary, shape = LERadius.xs) {
                                        Text(
                                            text = "Duplicate image: $dupKey ($count items)",
                                            style = LETypography.caption,
                                            color = LEColors.surface,
                                            modifier = Modifier.padding(LESpacing.xs)
                                        )
                                    }
                                }
                            ) {
                                Surface(
                                    color = LEColors.warningContainer.copy(alpha = 0.7f),
                                    shape = LERadius.xs,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, LEColors.warning.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "$dupKey ×$count",
                                        style = LETypography.caption,
                                        color = LEColors.warning,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

@Composable
private fun ImageStatusDropdownFilter(
    selectedFilter: ImageStatusFilter,
    selectedProblemFilter: ContentProblemFilter,
    onFilterSelected: (ImageStatusFilter) -> Unit,
    onMissingAnyAudioSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isMissingAnyAudio = selectedProblemFilter == ContentProblemFilter.MISSING_ANY_AUDIO
    val isActive = selectedFilter != ImageStatusFilter.ALL || isMissingAnyAudio

    Box(modifier = modifier) {
        Surface(
            color = if (isActive) LEColors.primary.copy(alpha = 0.12f) else LEColors.surfaceElevated,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isActive) LEColors.primary else LEColors.borderSubtle
            ),
            modifier = Modifier
                .height(34.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isMissingAnyAudio) LEIcons.Audio else LEIcons.Image,
                    contentDescription = if (isMissingAnyAudio) "Missing audio filter" else "Content filter",
                    tint = if (isActive) LEColors.primary else LEColors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isMissingAnyAudio) {
                        "Missing Audio"
                    } else {
                        when (selectedFilter) {
                            ImageStatusFilter.ALL -> "All"
                            ImageStatusFilter.MISSING_IMAGE -> "Missing"
                            ImageStatusFilter.DUPLICATE_IMAGE -> "Dup Image"
                            ImageStatusFilter.DUPLICATE_QUESTION -> "Dup Question"
                            ImageStatusFilter.HAS_IMAGE -> "Has Image"
                        }
                    },
                    style = LETypography.caption,
                    color = if (isActive) LEColors.primary else LEColors.textSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isActive) LEColors.primary else LEColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ImageStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filter.label,
                            fontWeight = if (!isMissingAnyAudio && filter == selectedFilter) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                            color = if (!isMissingAnyAudio && filter == selectedFilter) LEColors.primary else LEColors.textPrimary
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(color = LEColors.borderSubtle)
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Missing Any Audio",
                        fontWeight = if (isMissingAnyAudio) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMissingAnyAudio) LEColors.primary else LEColors.textPrimary
                    )
                },
                onClick = {
                    onMissingAnyAudioSelected()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun DuplicateGroupThumbnail(
    reference: String,
    packageName: String,
    loader: LessonThumbnailLoader?,
    modifier: Modifier = Modifier
) {
    if (loader != null) {
        val result by produceState<ThumbnailResult>(ThumbnailResult.Loading, reference, packageName) {
            value = if (reference.isBlank() || MediaReferencePolicy.isNoImageSentinel(reference)) {
                ThumbnailResult.Unavailable
            } else {
                withContext(Dispatchers.IO) { loader.load(reference, packageName) }
            }
        }
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                when (val current = result) {
                    ThumbnailResult.Loading -> Text("…", style = LETypography.caption, color = LEColors.textMuted)
                    ThumbnailResult.Unavailable -> Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No image",
                        tint = LEColors.textMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    is ThumbnailResult.Ready -> Image(
                        bitmap = current.bitmap,
                        contentDescription = "Shared image thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    } else {
        Surface(
            shape = LERadius.xs,
            color = LEColors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
            modifier = modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Image preview",
                    tint = LEColors.textMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateImageGroup,
    packageName: String,
    selectedContentId: String?,
    selectedContentIds: Set<String>,
    highlightedContentIds: Set<String>,
    thumbnailLoader: LessonThumbnailLoader?,
    problemProjection: ContentProblemProjection,
    onSelectRow: (String) -> Unit,
    onModifiedSelect: (id: String, ctrl: Boolean, shift: Boolean) -> Unit,
    onDoubleClickRow: ((String) -> Unit)?,
    onToggleHighlight: (String) -> Unit,
    onPlayQuestionAudio: ((String, String) -> Unit)?,
    playbackCoordinator: PlaybackCoordinator?,
    onDuplicateItem: ((String) -> Unit)?,
    onCopyQuestion: ((String) -> Unit)?,
    onCopyAnswer: ((String) -> Unit)?,
    onRequestGenerateTts: ((contentId: String, field: vn.loi.learning.desktop.tts.TtsField?) -> Unit)? = null
) {
    Surface(
        color = LEColors.surfaceElevated,
        shape = LERadius.sm,
        border = androidx.compose.foundation.BorderStroke(1.dp, LEColors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(LESpacing.xs)) {
            // Group Header: Shared Thumbnail + Count Badge + Filename
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                DuplicateGroupThumbnail(
                    reference = group.imageRef,
                    packageName = packageName,
                    loader = thumbnailLoader
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LEColors.warningContainer.copy(alpha = 0.8f),
                            shape = LERadius.xs,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, LEColors.warning.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${group.items.size} items",
                                style = LETypography.caption,
                                color = LEColors.warning,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = group.imageKey,
                        style = LETypography.caption,
                        color = LEColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = LEColors.borderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(2.dp))

            // Group Items
            group.items.forEach { item ->
                ExplorerRowItem(
                    item = item,
                    isSelected = item.contentId.value == selectedContentId,
                    isMultiSelected = item.contentId.value in selectedContentIds,
                    isHighlighted = item.contentId.value in highlightedContentIds,
                    problemCount = problemProjection.problemsFor(item.contentId.value).size,
                    duplicateInfo = null,
                    onSelect = { onSelectRow(item.contentId.value) },
                    onModifiedSelect = { ctrl, shift -> onModifiedSelect(item.contentId.value, ctrl, shift) },
                    onDoubleClick = { onDoubleClickRow?.invoke(item.contentId.value) },
                    onToggleHighlight = { onToggleHighlight(item.contentId.value) },
                    onPlayQuestionAudio = onPlayQuestionAudio,
                    playbackCoordinator = playbackCoordinator,
                    onDuplicateItem = onDuplicateItem,
                    onCopyQuestion = onCopyQuestion,
                    onCopyAnswer = onCopyAnswer,
                    onRequestGenerateTts = onRequestGenerateTts
                )
            }
        }
    }
}
