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
    onEditContent: (() -> Unit)? = null,
    onSaveEdit: (() -> Unit)? = null,
    onDiscardEdit: (() -> Unit)? = null,
    onUpdateDraftQuestion: ((String) -> Unit)? = null,
    onUpdateDraftAnswer: ((String) -> Unit)? = null,
    onUpdateDraftPronunciation: ((String) -> Unit)? = null,
    onUpdateDraftPartOfSpeech: ((String) -> Unit)? = null,
    onUpdateDraftExampleText: ((String) -> Unit)? = null,
    onUpdateDraftExampleTranslation: ((String) -> Unit)? = null,
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
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.isCtrlPressed && event.key == Key.S -> {
                            if (uiState.isDirty) {
                                onSaveEdit?.invoke()
                                true
                            } else false
                        }
                        event.key == Key.Escape -> {
                            if (uiState.isDirty) {
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
        // TOP TOOLBAR BAR
        StudioTopBar(
            packageName = uiState.packageName,
            isDirty = uiState.isDirty,
            onClose = onClose
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // MAIN 3-PANE LAYOUT
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Content Explorer (~22%)
                ContentExplorerPane(
                    modifier = Modifier.weight(0.22f),
                    uiState = uiState,
                    onClose = onClose,
                    onSelectRow = onSelectRow,
                    onQueryChanged = onQueryChanged,
                    onClearQuery = onClearQuery,
                    onLessonFilterChanged = onLessonFilterChanged,
                    onMediaFilterChanged = onMediaFilterChanged,
                    onSortChanged = onSortChanged,
                    onResetFilters = onResetFilters,
                    onDoubleClickRow = onDoubleClickRow
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // CENTER: Editor (~56%, priority)
                ContentEditorPane(
                    modifier = Modifier.weight(0.56f),
                    uiState = uiState,
                    playbackCoordinator = playbackCoordinator,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    thumbnailLoader = thumbnailLoader,
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

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // RIGHT: Media & Details Inspector (~22%)
                MediaInspectorPane(
                    modifier = Modifier.weight(0.22f),
                    uiState = uiState,
                    playbackCoordinator = playbackCoordinator,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    thumbnailLoader = thumbnailLoader
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // BOTTOM BREADCRUMB BAR
        StudioBreadcrumbBar(uiState = uiState)
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        val itemToDelete = uiState.selectedItemAnywhere
        AlertDialog(
            onDismissRequest = { onDismissDelete?.invoke() },
            title = { Text("Delete Content?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Delete \"${itemToDelete?.questionText ?: "this content"}\"?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This will permanently remove the content and all its associated learning items. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmDelete?.invoke() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onDismissDelete?.invoke() }) {
                    Text("Cancel")
                }
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
            title = { Text("Unsaved Changes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have unsaved changes to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "“$targetName”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmSaveAndProceed?.invoke() }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onConfirmDiscardAndProceed?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Discard")
                    }
                    TextButton(onClick = { onCancelUnsavedDialog?.invoke() }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun StudioTopBar(
    packageName: String,
    isDirty: Boolean,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Content Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (isDirty) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Unsaved Changes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            OutlinedButton(onClick = onClose) {
                Text("Back to Library")
            }
        }
    }
}

@Composable
private fun StudioBreadcrumbBar(uiState: PackageContentBrowserUiState) {
    val selectedItem = uiState.selectedItemInView ?: uiState.selectedItemAnywhere
    val index = selectedItem?.index ?: 0
    val total = uiState.allItems.size
    val lesson = selectedItem?.lesson ?: uiState.selectedLessonFilter.takeIf { it != "ALL" } ?: "All Lessons"
    val contentId = selectedItem?.contentId?.value ?: "-"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Package: ${uiState.packageName}  ›  Lesson: $lesson  ›  Item $index of $total ($contentId)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (uiState.isDirty) "Dirty Draft" else "Ready",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.isDirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        }
    }
}
