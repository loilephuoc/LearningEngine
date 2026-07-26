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
    onConfirmSaveAndProceed: (() -> Unit)? = null,
    onConfirmDiscardAndProceed: (() -> Unit)? = null,
    onCancelUnsavedDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val playbackCoordinator = remember(contentMediaStorage) {
        contentMediaStorage?.let { PlaybackCoordinator(it) }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LEColors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
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
                            if (uiState.isCreatingNewItem) {
                                onCancelNewItem?.invoke()
                                true
                            } else if (uiState.isDirty) {
                                onDiscardEdit?.invoke()
                                true
                            } else {
                                onClose()
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // TOP TOOLBAR BAR (Modernized as per Approved Mockup)
        StudioTopBar(
            packageName = uiState.packageName,
            isDirty = uiState.isDirty,
            isEditing = uiState.editingContentId != null || uiState.isCreatingNewItem,
            isCreatingNewItem = uiState.isCreatingNewItem,
            onNewItemClick = { onStartNewItem?.invoke() },
            onEditClick = { onEditContent?.invoke() },
            onSaveClick = { onSaveEdit?.invoke() },
            onDiscardClick = { onDiscardEdit?.invoke() },
            onSaveNewItemClick = { onSaveNewItem?.invoke() },
            onCancelNewItemClick = { onCancelNewItem?.invoke() },
            onDeleteClick = { onRequestDelete?.invoke() }
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
                    playbackCoordinator = playbackCoordinator
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
                    onImportMediaFile = onImportMediaFile
                )
            }
        }

        HorizontalDivider(color = LEColors.borderSubtle)

        // BOTTOM BREADCRUMB BAR
        StudioBreadcrumbBar(uiState = uiState, onClose = onClose)
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
                        text = "This will permanently remove the content and all its associated learning items. This action cannot be undone.",
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
                        text = "“$targetName”",
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

@Composable
private fun StudioTopBar(
    packageName: String,
    isDirty: Boolean,
    isEditing: Boolean,
    isCreatingNewItem: Boolean,
    onNewItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onSaveNewItemClick: () -> Unit,
    onCancelNewItemClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                        Text(
                            text = "Learning Engine 2.0",
                            style = LETypography.caption,
                            color = LEColors.textSecondary
                        )
                    }
                    Text(
                        text = "Content Studio",
                        style = LETypography.appTitle
                    )
                }

                // Action Toolbar Buttons (Matching Approved Mockup)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = LESpacing.lg)
                ) {
                    if (isCreatingNewItem) {
                        LEPrimaryButton(
                            text = "Save New Item",
                            onClick = onSaveNewItemClick,
                            icon = LEIcons.Save
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

                        LEPrimaryButton(
                            text = "Edit Item",
                            onClick = onEditClick,
                            icon = LEIcons.Save
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
    uiState: PackageContentBrowserUiState,
    onClose: () -> Unit
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
            }

            LESecondaryButton(
                text = "Back to Library",
                onClick = onClose
            )
        }
    }
}
