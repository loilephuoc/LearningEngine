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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.platform.DesktopFileActions
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun ContentStudioScreen(
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
    modifier: Modifier = Modifier
) {
    val playbackCoordinator = remember(contentMediaStorage) {
        contentMediaStorage?.let { PlaybackCoordinator(it) }
    }

    val screenFocusRequester = remember { FocusRequester() }
    // PLE-020: search field focus requester (passed down to ContentExplorerPane)
    val searchFocusRequester = remember { FocusRequester() }

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
                            if (uiState.isDirty || uiState.isCreatingNewItem) {
                                onClose()
                                true
                            } else {
                                onClose()
                                true
                            }
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
            canUndoDelete = uiState.canUndoDelete && !uiState.isDirty && !uiState.isCreatingNewItem,
            isCreateSubmitting = uiState.isCreateSubmitting
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
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    onLessonFilterChanged = onLessonFilterChanged,
                    onMediaFilterChanged = onMediaFilterChanged,
                    onSortChanged = onSortChanged,
                    onResetFilters = onResetFilters,
                    onDoubleClickRow = onDoubleClickRow,
                    onSelectImage = { id -> onSelectRow(id) },
                    onPlayQuestionAudio = { id, ref ->
                        onSelectRow(id)
                        playbackCoordinator?.play(ref) ?: onPlayAudio?.invoke(ref)
                    },
                    playbackCoordinator = playbackCoordinator,
                    // PLE-020
                    searchFocusRequester = searchFocusRequester,
                    onDuplicateItem = onDuplicateItem,
                    onCopyQuestion = onCopyQuestion,
                    onCopyAnswer = onCopyAnswer
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
        val itemToDelete = uiState.selectedItemAnywhere
        AlertDialog(
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
                LEDangerButton(text = "Delete", onClick = { onConfirmDelete?.invoke() })
            },
            dismissButton = {
                LESecondaryButton(text = "Cancel", onClick = { onDismissDelete?.invoke() })
            }
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
    canUndoDelete: Boolean,
    isCreateSubmitting: Boolean
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
                            text = "Delete",
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
                            text = "Delete",
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
