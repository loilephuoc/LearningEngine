package vn.loi.learning.desktop.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.EdgeTtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.ui.DesktopTtsDialog
import vn.loi.learning.desktop.tts.ui.TtsDialogTarget
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.platform.DesktopFileActions
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun ContentStudioScreen(
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit,
    onSelectRow: (String) -> Unit,
    onSubmitSearch: (String) -> Unit = {},
    onToggleHighlight: (String) -> Unit = {},
    onToggleMultiSelection: (String) -> Unit = {},
    onSelectMultiRange: (String) -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onClearMultiSelection: () -> Unit = {},
    onHighlightSelected: () -> Unit = {},
    onRemoveHighlightSelected: () -> Unit = {},
    onCheckSelectedMedia: () -> Unit = {},
    onConfirmBatchDelete: () -> Unit = {},
    onDismissBatchDelete: () -> Unit = {},
    onDismissBatchDeleteBlocker: () -> Unit = {},
    onDismissSelectedMediaCheck: () -> Unit = {},
    onConfirmBatchPartOfSpeech: () -> Unit = {},
    onCancelBatchPartOfSpeech: () -> Unit = {},
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLessonFilterChanged: (String) -> Unit,
    onMediaFilterChanged: (BrowserMediaFilter) -> Unit,
    onImageStatusFilterChanged: (vn.loi.learning.desktop.ui.browser.ImageStatusFilter) -> Unit = {},
    onSortChanged: (BrowserSortOption) -> Unit,
    onProblemFilterChanged: (ContentProblemFilter) -> Unit = {},
    onPreviousProblem: () -> Unit = {},
    onNextProblem: () -> Unit = {},
    onResetFilters: () -> Unit,
    onPlayAudio: ((String) -> Unit)? = null,
    onStopAudio: (() -> Unit)? = null,
    thumbnailLoader: LessonThumbnailLoader,
    contentMediaStorage: ContentMediaStorage? = null,
    onStartNewItem: (() -> Unit)? = null,
    onEditContent: (() -> Unit)? = null,
    onSaveEdit: (() -> Unit)? = null,
    onDiscardEdit: (() -> Unit)? = null,
    onSaveNewItem: (() -> Unit)? = null,
    onCancelNewItem: (() -> Unit)? = null,
    onUpdateDraftQuestion: ((String) -> Unit)? = null,
    onUpdateDraftAnswer: ((String) -> Unit)? = null,
    onUpdateDraftPronunciation: ((String) -> Unit)? = null,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)? = null,
    onUpdateDraftExampleText: ((String) -> Unit)? = null,
    onUpdateDraftExampleTranslation: ((String) -> Unit)? = null,
    onUpdateDraftImageRef: ((String?) -> Unit)? = null,
    onUpdateDraftQuestionAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftAnswerAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftExampleAudioRef: ((String?) -> Unit)? = null,
    onUpdateDraftTranslationAudioRef: ((String?) -> Unit)? = null,
    onImportMediaFile: ((java.io.File, String) -> Unit)? = null,
    onDoubleClickRow: ((String) -> Unit)? = null,
    onRequestDelete: (() -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    onDismissDelete: (() -> Unit)? = null,
    onUndoDelete: (() -> Unit)? = null,
    onConfirmSaveAndProceed: (() -> Unit)? = null,
    onConfirmDiscardAndProceed: (() -> Unit)? = null,
    onCancelUnsavedDialog: (() -> Unit)? = null,
    // PLE-020: keyboard navigation callbacks
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    onNavigateEnd: (() -> Unit)? = null,
    onNavigatePageUp: (() -> Unit)? = null,
    onNavigatePageDown: (() -> Unit)? = null,
    // PLE-020: context menu callbacks
    onDuplicateItem: ((String) -> Unit)? = null,
    onCopyQuestion: ((String) -> Unit)? = null,
    onCopyAnswer: ((String) -> Unit)? = null,
    onOpenImageReuseReview: (() -> Unit)? = null,
    onOpenPosReview: (() -> Unit)? = null,
    onSelectPosReviewScope: ((vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope) -> Unit)? = null,
    onTogglePosReviewRowSelection: ((String) -> Unit)? = null,
    onTogglePosReviewAllFiltered: (() -> Unit)? = null,
    onClearPosReviewSelection: (() -> Unit)? = null,
    onUpdatePosReviewRowNewPos: ((String, String) -> Unit)? = null,
    onAnalyzePosReview: (() -> Unit)? = null,
    onResetPosReviewDrafts: (() -> Unit)? = null,
    onBatchSetPosReviewSelectedPos: ((String) -> Unit)? = null,
    onBatchSetPosReviewFilteredPos: ((String) -> Unit)? = null,
    onPosReviewSearchQueryChanged: ((String) -> Unit)? = null,
    onPosReviewStatusFilterChanged: ((vn.loi.learning.desktop.ui.browser.posreview.PosReviewRowStatus) -> Unit)? = null,
    onRequestUnlockPosReviewSelected: (() -> Unit)? = null,
    onCancelUnlockPosReviewConfirmation: (() -> Unit)? = null,
    onConfirmUnlockPosReviewSelected: (() -> Unit)? = null,
    onRequestApplyPosReview: (() -> Unit)? = null,
    onCancelApplyPosReview: (() -> Unit)? = null,
    onConfirmApplyPosReview: (() -> Unit)? = null,
    onClosePosReview: (() -> Unit)? = null,
    onOpenContentMaintenanceExport: (() -> Unit)? = null,
    onSelectContentMaintenanceExportScope: ((vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope) -> Unit)? = null,
    onTargetExportDirectoryChanged: ((String) -> Unit)? = null,
    onTargetExportFileNameChanged: ((String) -> Unit)? = null,
    onExecuteContentMaintenanceExport: (() -> Unit)? = null,
    onCloseContentMaintenanceExport: (() -> Unit)? = null,
    ttsAudioService: DesktopTtsAudioService? = null,
    onApplyTtsAudio: ((contentId: String, field: TtsField, audioRef: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playbackCoordinator = remember(contentMediaStorage) {
        contentMediaStorage?.let { PlaybackCoordinator(it) }
    }

    val screenFocusRequester = remember { FocusRequester() }
    // PLE-020: search field focus requester (passed down to ContentExplorerPane)
    val searchFocusRequester = remember { FocusRequester() }

    var ttsDialogTarget by remember { mutableStateOf<TtsDialogTarget?>(null) }
    val handleRequestTts: (String, TtsField?) -> Unit = { contentId, field ->
        val targetItem = uiState.allItems.firstOrNull { it.contentId.value == contentId }
            ?: uiState.selectedItemAnywhere
        if (targetItem != null) {
            ttsDialogTarget = TtsDialogTarget.fromBrowserItem(targetItem, field)
        }
    }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LEColors.background)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    event.isCtrlPressed && event.key == Key.Z && uiState.canUndoDelete && !uiState.isDirty && !uiState.isCreatingNewItem -> { onUndoDelete?.invoke(); true }
                    event.key == Key.Delete && !uiState.isCreatingNewItem && uiState.selectedContentId != null -> { onRequestDelete?.invoke(); true }
                    event.key == Key.DirectionUp && !event.isCtrlPressed -> { onNavigateUp?.invoke(); true }
                    event.key == Key.DirectionDown && !event.isCtrlPressed -> { onNavigateDown?.invoke(); true }
                    event.key == Key.MoveHome && !event.isCtrlPressed -> { onNavigateHome?.invoke(); true }
                    event.key == Key.MoveEnd && !event.isCtrlPressed -> { onNavigateEnd?.invoke(); true }
                    event.key == Key.PageUp -> { onNavigatePageUp?.invoke(); true }
                    event.key == Key.PageDown -> { onNavigatePageDown?.invoke(); true }
                    else -> false
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        // --- Existing shortcuts ---
                        event.isCtrlPressed && event.key == Key.S -> {
                            if (uiState.isCreatingNewItem) {
                                onSaveNewItem?.invoke()
                                true
                            } else if (uiState.isDirty) {
                                onSaveEdit?.invoke()
                                true
                            } else false
                        }
                        event.key == Key.Escape -> {
                            when {
                                uiState.appliedQuery.isNotBlank() -> onClearQuery()
                                uiState.selectedContentIds.isNotEmpty() -> onClearMultiSelection()
                                else -> onClose()
                            }
                            true
                        }

                        // --- PLE-020 new shortcuts ---
                        event.isCtrlPressed && event.key == Key.N -> {
                            onStartNewItem?.invoke()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.F -> {
                            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // TOP TOOLBAR BAR
        StudioTopBar(
            packageName = uiState.packageName,
            onBack = onClose,
            isDirty = uiState.isDirty,
            isEditing = uiState.editingContentId != null || uiState.isCreatingNewItem,
            isCreatingNewItem = uiState.isCreatingNewItem,
            onNewItemClick = { onStartNewItem?.invoke() },
            onEditClick = { onEditContent?.invoke() },
            onSaveClick = { onSaveEdit?.invoke() },
            onDiscardClick = { onDiscardEdit?.invoke() },
            onSaveNewItemClick = { onSaveNewItem?.invoke() },
            onCancelNewItemClick = { onCancelNewItem?.invoke() },
            onDeleteClick = { onRequestDelete?.invoke() },
            onUndoDeleteClick = { onUndoDelete?.invoke() },
            onImageReuseReviewClick = { onOpenImageReuseReview?.invoke() },
            onPosReviewClick = { onOpenPosReview?.invoke() },
            onExportJsonClick = { onOpenContentMaintenanceExport?.invoke() },
            canUndoDelete = uiState.canUndoDelete && !uiState.isDirty && !uiState.isCreatingNewItem,
            isCreateSubmitting = uiState.isCreateSubmitting,
            deleteTargetCount = uiState.selectedContentIds.size
        )

        HorizontalDivider(color = LEColors.borderSubtle)

        // MAIN 3-PANE LAYOUT
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Content Explorer (~22%)
                ContentExplorerPane(
                    modifier = Modifier.weight(0.22f),
                    uiState = uiState,
                    onClose = onClose,
                    onSelectRow = { id -> onSelectRow(id) },
                    onSubmitSearch = onSubmitSearch,
                    onToggleHighlight = onToggleHighlight,
                    onToggleMultiSelection = onToggleMultiSelection,
                    onSelectMultiRange = onSelectMultiRange,
                    onSelectAllVisible = onSelectAllVisible,
                    onClearMultiSelection = onClearMultiSelection,
                    onHighlightSelected = onHighlightSelected,
                    onRemoveHighlightSelected = onRemoveHighlightSelected,
                    onCheckSelectedMedia = onCheckSelectedMedia,
                    onPosReviewSelected = { onOpenPosReview?.invoke() },
                    onExportJsonSelected = { onOpenContentMaintenanceExport?.invoke() },
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    onLessonFilterChanged = onLessonFilterChanged,
                    onMediaFilterChanged = onMediaFilterChanged,
                    onImageStatusFilterChanged = onImageStatusFilterChanged,
                    onSortChanged = onSortChanged,
                    onProblemFilterChanged = onProblemFilterChanged,
                    onPreviousProblem = onPreviousProblem,
                    onNextProblem = onNextProblem,
                    onResetFilters = onResetFilters,
                    onDoubleClickRow = onDoubleClickRow,
                    onPlayQuestionAudio = { id, ref ->
                        onSelectRow(id)
                        playbackCoordinator?.play(ref) ?: onPlayAudio?.invoke(ref)
                    },
                    playbackCoordinator = playbackCoordinator,
                    thumbnailLoader = thumbnailLoader,
                    // PLE-020
                    searchFocusRequester = searchFocusRequester,
                    onDuplicateItem = onDuplicateItem,
                    onCopyQuestion = onCopyQuestion,
                    onCopyAnswer = onCopyAnswer,
                    onRequestGenerateTts = handleRequestTts
                )

                VerticalDivider(color = LEColors.borderSubtle)

                // CENTER: Editor (~56%, Priority)
                ContentEditorPane(
                    modifier = Modifier.weight(0.56f),
                    uiState = uiState,
                    isCreatingNewItem = uiState.isCreatingNewItem,
                    playbackCoordinator = playbackCoordinator,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    thumbnailLoader = thumbnailLoader,
                    contentMediaStorage = contentMediaStorage,
                    onEditContent = onEditContent,
                    onSaveEdit = onSaveEdit,
                    onDiscardEdit = onDiscardEdit,
                    onUpdateDraftQuestion = onUpdateDraftQuestion,
                    onUpdateDraftAnswer = onUpdateDraftAnswer,
                    onUpdateDraftPronunciation = onUpdateDraftPronunciation,
                    onUpdateDraftPartOfSpeech = onUpdateDraftPartOfSpeech,
                    onUpdateDraftExampleText = onUpdateDraftExampleText,
                    onUpdateDraftExampleTranslation = onUpdateDraftExampleTranslation,
                    onUpdateDraftImageRef = onUpdateDraftImageRef,
                    onImportMediaFile = onImportMediaFile,
                    onRequestDelete = onRequestDelete,
                    onConfirmDelete = onConfirmDelete,
                    onDismissDelete = onDismissDelete,
                    onConfirmSaveAndProceed = onConfirmSaveAndProceed,
                    onConfirmDiscardAndProceed = onConfirmDiscardAndProceed,
                    onCancelUnsavedDialog = onCancelUnsavedDialog
                )

                VerticalDivider(color = LEColors.borderSubtle)

                // RIGHT: Media Manager & Quality/AI Review (~22%)
                MediaInspectorPane(
                    modifier = Modifier.weight(0.22f),
                    uiState = uiState,
                    playbackCoordinator = playbackCoordinator,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    thumbnailLoader = thumbnailLoader,
                    onUpdateDraftImageRef = onUpdateDraftImageRef,
                    onUpdateDraftQuestionAudioRef = onUpdateDraftQuestionAudioRef,
                    onUpdateDraftAnswerAudioRef = onUpdateDraftAnswerAudioRef,
                    onUpdateDraftExampleAudioRef = onUpdateDraftExampleAudioRef,
                    onUpdateDraftTranslationAudioRef = onUpdateDraftTranslationAudioRef,
                    onImportMediaFile = onImportMediaFile,
                    onShowImageInFolder =
                        contentMediaStorage?.let { storage ->
                            { reference: String ->
                                storage.resolve(reference)?.let { path ->
                                    DesktopFileActions.showInFolder(path)
                                }
                            }
                        },
                    resolveImageFileName =
                        contentMediaStorage?.let { storage ->
                            { reference: String ->
                                storage.resolve(reference)?.fileName?.toString()
                            }
                        },
                    onRequestGenerateTts = { field ->
                        uiState.selectedContentId?.let { handleRequestTts(it, field) }
                    }
                )
            }
        }

        HorizontalDivider(color = LEColors.borderSubtle)

        // BOTTOM BREADCRUMB BAR
        StudioBreadcrumbBar(uiState = uiState)
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        val itemToDelete = uiState.allItems.firstOrNull { it.contentId.value == uiState.deleteTargetContentId }
            ?: uiState.selectedItemAnywhere
        val deleteFocusRequester = remember { FocusRequester() }
        var deleteDispatched by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { deleteFocusRequester.requestFocus() }
        AlertDialog(
            modifier = Modifier.testTag("delete-confirm-dialog").onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter -> {
                        if (!deleteDispatched) {
                            deleteDispatched = true
                            onConfirmDelete?.invoke()
                        }
                        true
                    }
                    Key.Escape -> { onDismissDelete?.invoke(); true }
                    else -> false
                }
            },
            onDismissRequest = { onDismissDelete?.invoke() },
            title = { Text("Delete Content?", style = LETypography.paneTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    Text(
                        text = "Delete \"${itemToDelete?.questionText ?: "this content"}\"?",
                        style = LETypography.fieldValueEmphasized
                    )
                    Text(
                        text = "This removes the content and its learning items. You can undo the most recent deletion while this Content Studio session remains open.",
                        style = LETypography.secondaryMetadata
                    )
                }
            },
            confirmButton = {
                LEDangerButton(
                    text = "Delete",
                    onClick = {
                        if (!deleteDispatched) {
                            deleteDispatched = true
                            onConfirmDelete?.invoke()
                        }
                    },
                    modifier = Modifier.focusRequester(deleteFocusRequester)
                )
            },
            dismissButton = {
                LESecondaryButton(text = "Cancel", onClick = { onDismissDelete?.invoke() })
            }
        )
    }

    if (uiState.pendingBatchDeleteContentIds.size >= 2) {
        val count = uiState.pendingBatchDeleteContentIds.size
        val focusRequester = remember { FocusRequester() }
        var dispatched by remember(uiState.pendingBatchDeleteContentIds) { mutableStateOf(false) }
        LaunchedEffect(uiState.pendingBatchDeleteContentIds) { focusRequester.requestFocus() }
        AlertDialog(
            modifier = Modifier.testTag("batch-delete-confirm-dialog").onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter -> { if (!dispatched) { dispatched = true; onConfirmBatchDelete() }; true }
                    Key.Escape -> { onDismissBatchDelete(); true }
                    else -> false
                }
            },
            onDismissRequest = onDismissBatchDelete,
            title = { Text("Delete $count selected items?", style = LETypography.paneTitle) },
            text = { Text("This action removes the selected Content and their LearningItems. You can undo this delete during the current Content Studio session.") },
            confirmButton = {
                LEDangerButton(
                    text = "Delete $count Items",
                    enabled = !uiState.isBatchDeleteSubmitting,
                    modifier = Modifier.focusRequester(focusRequester),
                    onClick = { if (!dispatched) { dispatched = true; onConfirmBatchDelete() } }
                )
            },
            dismissButton = { LESecondaryButton(text = "Cancel", onClick = onDismissBatchDelete) }
        )
    }

    uiState.batchDeleteBlockerMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissBatchDeleteBlocker,
            title = { Text("Cannot delete selected items") },
            text = { Text(message) },
            confirmButton = { LEPrimaryButton(text = "OK", onClick = onDismissBatchDeleteBlocker) }
        )
    }

    // Unsaved changes dialog
    if (uiState.showUnsavedChangesDialog) {
        val editedItem = uiState.selectedItemAnywhere
        val targetName = editedItem?.questionText?.takeIf { it.isNotBlank() }
            ?: uiState.editingContentId
            ?: "this content"
        AlertDialog(
            onDismissRequest = { onCancelUnsavedDialog?.invoke() },
            title = { Text("Unsaved Changes", style = LETypography.paneTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    Text(
                        text = "You have unsaved changes to:",
                        style = LETypography.fieldValue
                    )
                    Text(
                        text = "\u201c$targetName\u201d",
                        style = LETypography.fieldValueEmphasized
                    )
                }
            },
            confirmButton = {
                LEPrimaryButton(text = "Save Changes", onClick = { onConfirmSaveAndProceed?.invoke() })
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)) {
                    LEDangerButton(text = "Discard", onClick = { onConfirmDiscardAndProceed?.invoke() })
                    LESecondaryButton(text = "Cancel", onClick = { onCancelUnsavedDialog?.invoke() })
                }
            }
        )
    }

    uiState.selectedMediaCheck?.let { summary ->
        AlertDialog(
            onDismissRequest = onDismissSelectedMediaCheck,
            title = { Text("Selected Media Check", style = LETypography.paneTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                    Text("${summary.selectedItemCount} items", style = LETypography.fieldValueEmphasized)
                    vn.loi.learning.desktop.ui.browser.ContentProblem.entries.forEach { problem ->
                        Text("${problem.label}: ${summary.count(problem)}", style = LETypography.secondaryMetadata)
                    }
                }
            },
            confirmButton = { LEPrimaryButton(text = "Close", onClick = onDismissSelectedMediaCheck) }
        )
    }

    uiState.pendingBatchPartOfSpeech?.let { target ->
        AlertDialog(
            onDismissRequest = onCancelBatchPartOfSpeech,
            title = { Text("Change POS", style = LETypography.paneTitle) },
            text = {
                Text(
                    "Set POS to $target for ${uiState.selectedContentIds.size} selected items?",
                    style = LETypography.fieldValue
                )
            },
            confirmButton = {
                LEPrimaryButton(
                    text = "Apply",
                    onClick = onConfirmBatchPartOfSpeech,
                    enabled = !uiState.isBatchPartOfSpeechSubmitting
                )
            },
            dismissButton = {
                LESecondaryButton(text = "Cancel", onClick = onCancelBatchPartOfSpeech)
            }
        )
    }

    uiState.posReviewState?.let { posState ->
        val scopes = mapOf(
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.ALL_ITEMS to uiState.allItems.size,
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.SELECTED_ITEMS to uiState.selectedContentIds.size,
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_SEARCH_RESULTS to (if (uiState.appliedQuery.isBlank()) uiState.allItems.size else uiState.allItems.count { it.searchableText.contains(uiState.appliedQuery.lowercase()) }),
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_FILTER_RESULTS to uiState.filteredItems.size
        )
        vn.loi.learning.desktop.ui.browser.posreview.PosBatchReviewDialog(
            state = posState,
            availableScopes = scopes,
            onSelectScope = { scope -> onSelectPosReviewScope?.invoke(scope) },
            onToggleRowSelection = { id -> onTogglePosReviewRowSelection?.invoke(id) },
            onToggleAllFiltered = { onTogglePosReviewAllFiltered?.invoke() },
            onClearSelection = { onClearPosReviewSelection?.invoke() },
            onUpdateRowNewPos = { id, pos -> onUpdatePosReviewRowNewPos?.invoke(id, pos) },
            onAnalyzePos = { onAnalyzePosReview?.invoke() },
            onResetDrafts = { onResetPosReviewDrafts?.invoke() },
            onBatchSetSelectedPos = { pos -> onBatchSetPosReviewSelectedPos?.invoke(pos) },
            onBatchSetFilteredPos = { pos -> onBatchSetPosReviewFilteredPos?.invoke(pos) },
            onSearchQueryChanged = { q -> onPosReviewSearchQueryChanged?.invoke(q) },
            onStatusFilterChanged = { f -> onPosReviewStatusFilterChanged?.invoke(f) },
            onRequestUnlockSelected = { onRequestUnlockPosReviewSelected?.invoke() },
            onCancelUnlockConfirmation = { onCancelUnlockPosReviewConfirmation?.invoke() },
            onConfirmUnlockSelected = { onConfirmUnlockPosReviewSelected?.invoke() },
            onRequestApply = { onRequestApplyPosReview?.invoke() },
            onCancelApplyConfirmation = { onCancelApplyPosReview?.invoke() },
            onConfirmApply = { onConfirmApplyPosReview?.invoke() },
            onDismiss = { onClosePosReview?.invoke() }
        )
    }

    uiState.contentMaintenanceExportState?.let { exportState ->
        val scopes = mapOf(
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_FILTER_RESULTS to uiState.filteredItems.size,
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.SELECTED_ITEMS to uiState.selectedContentIds.size,
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_SEARCH_RESULTS to (if (uiState.appliedQuery.isBlank()) uiState.allItems.size else uiState.allItems.count { it.searchableText.contains(uiState.appliedQuery.lowercase()) }),
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.ALL_ITEMS to uiState.allItems.size
        )
        vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportDialog(
            state = exportState,
            availableScopes = scopes,
            onSelectScope = { scope -> onSelectContentMaintenanceExportScope?.invoke(scope) },
            onTargetDirectoryChanged = { dir -> onTargetExportDirectoryChanged?.invoke(dir) },
            onTargetFileNameChanged = { name -> onTargetExportFileNameChanged?.invoke(name) },
            onExecuteExport = { onExecuteContentMaintenanceExport?.invoke() },
            onDismiss = { onCloseContentMaintenanceExport?.invoke() }
        )
    }

    ttsDialogTarget?.let { target ->
        val resolvedTtsService = remember(contentMediaStorage, ttsAudioService) {
            ttsAudioService ?: contentMediaStorage?.let { storage ->
                DesktopTtsAudioService(
                    ttsEngine = EdgeTtsEngine(),
                    mediaStorage = storage
                )
            }
        }
        if (resolvedTtsService != null) {
            DesktopTtsDialog(
                target = target,
                packageName = uiState.packageName,
                ttsService = resolvedTtsService,
                contentMediaStorage = contentMediaStorage,
                onApply = { contentId, field, audioRef ->
                    if (onApplyTtsAudio != null) {
                        onApplyTtsAudio(contentId, field, audioRef)
                    } else {
                        when (field) {
                            TtsField.QUESTION -> onUpdateDraftQuestionAudioRef?.invoke(audioRef)
                            TtsField.ANSWER -> onUpdateDraftAnswerAudioRef?.invoke(audioRef)
                            TtsField.EXAMPLE -> onUpdateDraftExampleAudioRef?.invoke(audioRef)
                            TtsField.TRANSLATION -> onUpdateDraftTranslationAudioRef?.invoke(audioRef)
                        }
                        onSaveEdit?.invoke()
                    }
                    ttsDialogTarget = null
                },
                onDismiss = { ttsDialogTarget = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioTopBar(
    packageName: String,
    onBack: () -> Unit,
    isDirty: Boolean,
    isEditing: Boolean,
    isCreatingNewItem: Boolean,
    onNewItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onSaveNewItemClick: () -> Unit,
    onCancelNewItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUndoDeleteClick: () -> Unit,
    onImageReuseReviewClick: (() -> Unit)? = null,
    onPosReviewClick: (() -> Unit)? = null,
    onExportJsonClick: (() -> Unit)? = null,
    canUndoDelete: Boolean,
    isCreateSubmitting: Boolean,
    deleteTargetCount: Int
) {
    Surface(
        color = LEColors.surface,
        tonalElevation = LEElevation.flat,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.lg, vertical = LESpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left App Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Back to Library") } },
                    state = rememberTooltipState()
                ) {
                    LEIconButton(
                        icon = LEIcons.Back,
                        onClick = onBack,
                        contentDescription = "Back to Library"
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                        Text(
                            text = "Learning Engine 2.0",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                    Text(
                        text = "Content Studio — $packageName",
                        style = LETypography.appTitle,
                        maxLines = 1
                    )
                }

                // Action Toolbar Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = LESpacing.lg)
                ) {
                    LESecondaryButton(
                        text = "Undo Delete",
                        onClick = onUndoDeleteClick,
                        icon = LEIcons.Undo,
                        enabled = canUndoDelete
                    )
                    LESecondaryButton(
                        text = "Image Reuse Review",
                        onClick = { onImageReuseReviewClick?.invoke() },
                        icon = LEIcons.Image,
                        enabled = !isCreatingNewItem
                    )
                    LESecondaryButton(
                        text = "POS Review",
                        onClick = { onPosReviewClick?.invoke() },
                        enabled = !isCreatingNewItem
                    )
                    LESecondaryButton(
                        text = "Export JSON",
                        onClick = { onExportJsonClick?.invoke() },
                        enabled = !isCreatingNewItem
                    )
                    if (isCreatingNewItem) {
                        LEPrimaryButton(
                            text = "Save New Item",
                            onClick = onSaveNewItemClick,
                            icon = LEIcons.Save,
                            enabled = !isCreateSubmitting
                        )

                        LESecondaryButton(
                            text = "Cancel",
                            onClick = onCancelNewItemClick,
                            icon = LEIcons.Discard
                        )
                    } else if (isEditing) {
                        LEPrimaryButton(
                            text = "Save",
                            onClick = onSaveClick,
                            icon = LEIcons.Save,
                            enabled = isDirty
                        )

                        LESecondaryButton(
                            text = "Discard",
                            onClick = onDiscardClick,
                            icon = LEIcons.Discard
                        )

                        LEDangerButton(
                            text = if (deleteTargetCount >= 2) "Delete Selected ($deleteTargetCount)" else "Delete",
                            onClick = onDeleteClick,
                            icon = LEIcons.Delete
                        )
                    } else {
                        LESecondaryButton(
                            text = "New Item",
                            onClick = onNewItemClick,
                            icon = LEIcons.New,
                            enabled = true
                        )

                        LEDangerButton(
                            text = if (deleteTargetCount >= 2) "Delete Selected ($deleteTargetCount)" else "Delete",
                            onClick = onDeleteClick,
                            icon = LEIcons.Delete
                        )
                    }
                }
            }

            // Right Header Utilities
            Row(
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LESecondaryButton(
                    text = "Keyboard Shortcuts",
                    onClick = {},
                    icon = LEIcons.Keyboard
                )
                LEIconButton(icon = LEIcons.Help, onClick = {}, contentDescription = "Help")
                LEIconButton(icon = LEIcons.Settings, onClick = {}, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun StudioBreadcrumbBar(
    uiState: PackageContentBrowserUiState
) {
    val selectedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val index = selectedItem?.index ?: 0
    val total = uiState.allItems.size
    val lesson = selectedItem?.lesson ?: uiState.selectedLessonFilter.takeIf { it != "ALL" } ?: "General"

    Surface(
        color = LEColors.surfaceSubtle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.lg, vertical = LESpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                Text("Library", style = LETypography.secondaryMetadata, color = LEColors.primaryText)
                Text(" › ", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
                Text(uiState.packageName, style = LETypography.secondaryMetadata, color = LEColors.primaryText, fontWeight = FontWeight.Bold)
                Text(" › ", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
                Text("Lesson: $lesson", style = LETypography.secondaryMetadata, color = LEColors.textSecondary)
                Text(" › ", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
                Text("Item $index of $total", style = LETypography.secondaryMetadata, color = LEColors.textSecondary)
                if (total > 0) {
                    Text(" · ", style = LETypography.secondaryMetadata, color = LEColors.textMuted)
                    Text("↑↓ Navigate · Ctrl+N New · Ctrl+S Save · Ctrl+F Search · Del Delete", style = LETypography.caption, color = LEColors.textMuted)
                }
            }

        }
    }
}
