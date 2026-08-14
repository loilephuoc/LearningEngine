package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import javax.swing.JFileChooser
import vn.loi.learning.application.integrity.IntegritySeverity
import vn.loi.learning.application.port.ContentMediaStorage

@Composable
fun ContentLibraryScreen(
    viewModel: ContentLibraryViewModel,
    contentMediaStorage: ContentMediaStorage,
    onStartLessonStudy: (PackageLessonSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentLibraryContent(
        uiState = viewModel.uiState,
        lessonBrowserUiState =
            viewModel.lessonBrowserUiState,
        createCollectionDialogState =
            viewModel.createCollectionDialogState,
        renameCollectionDialogState =
            viewModel.renameCollectionDialogState,
        deleteCollectionDialogState =
            viewModel.deleteCollectionDialogState,
        attachPackageDialogState =
            viewModel.attachPackageDialogState,
        detachPackageDialogState =
            viewModel.detachPackageDialogState,
        packageIntegrityDialogState = viewModel.packageIntegrityDialogState,
        onRefresh =
            viewModel::refresh,
        onImportDirectory =
            viewModel::importFromDirectory,
        onOpenLibrary =
            viewModel::openLibrary,
        onShowCreateCollectionDialog =
            viewModel::showCreateCollectionDialog,
        onCreateCollectionNameChanged =
            viewModel::updateCreateCollectionName,
        onDismissCreateCollectionDialog =
            viewModel::dismissCreateCollectionDialog,
        onConfirmCreateCollection =
            viewModel::confirmCreateCollection,
        onShowRenameCollectionDialog =
            viewModel::showRenameCollectionDialog,
        onRenameCollectionNameChanged =
            viewModel::updateRenameCollectionName,
        onDismissRenameCollectionDialog =
            viewModel::dismissRenameCollectionDialog,
        onConfirmRenameCollection =
            viewModel::confirmRenameCollection,
        onShowDeleteCollectionDialog =
            viewModel::showDeleteCollectionDialog,
        onDismissDeleteCollectionDialog =
            viewModel::dismissDeleteCollectionDialog,
        onConfirmDeleteCollection =
            viewModel::confirmDeleteCollection,
        onShowAttachPackageDialog =
            viewModel::showAttachPackageDialog,
        onPackageSelectedForAttachment =
            viewModel::selectPackageForAttachment,
        onDismissAttachPackageDialog =
            viewModel::dismissAttachPackageDialog,
        onConfirmAttachPackage =
            viewModel::confirmAttachPackage,
        onShowDetachPackageDialog =
            viewModel::showDetachPackageDialog,
        onDismissDetachPackageDialog =
            viewModel::dismissDetachPackageDialog,
        onConfirmDetachPackage =
            viewModel::confirmDetachPackage,
        onCloseLibrary =
            viewModel::closeLibrary,
        onSelectLesson =
            viewModel::selectLesson,
        onClearLessonSelection =
            viewModel::clearLessonSelection,
        onLessonQueryChanged = viewModel::updateLessonQuery,
        onClearLessonQuery = viewModel::clearLessonQuery,
        onLessonFilterChanged = viewModel::updateLessonFilter,
        onLessonSortChanged = viewModel::updateLessonSort,
        onStartLessonStudy =
            onStartLessonStudy,
        onExportPackage = viewModel::exportPackage,
        onCheckPackageIntegrity = viewModel::checkPackageIntegrity,
        onDismissPackageIntegrityReport = viewModel::dismissPackageIntegrityReport,
        thumbnailLoader = remember(contentMediaStorage) { LessonThumbnailLoader(contentMediaStorage) },
        modifier = modifier
    )
}

@Composable
private fun ContentLibraryContent(
    uiState: ContentLibraryUiState,
    lessonBrowserUiState: LessonBrowserUiState?,
    createCollectionDialogState:
    CreateCollectionDialogState,
    renameCollectionDialogState:
    RenameCollectionDialogState,
    deleteCollectionDialogState:
    DeleteCollectionDialogState,
    attachPackageDialogState:
    AttachPackageDialogState,
    detachPackageDialogState:
    DetachPackageDialogState,
    packageIntegrityDialogState: PackageIntegrityDialogState,
    onRefresh: () -> Unit,
    onImportDirectory: (Path) -> Unit,
    onOpenLibrary: (String) -> Unit,
    onShowCreateCollectionDialog: (String) -> Unit,
    onCreateCollectionNameChanged: (String) -> Unit,
    onDismissCreateCollectionDialog: () -> Unit,
    onConfirmCreateCollection: () -> Unit,
    onShowRenameCollectionDialog: (String) -> Unit,
    onRenameCollectionNameChanged: (String) -> Unit,
    onDismissRenameCollectionDialog: () -> Unit,
    onConfirmRenameCollection: () -> Unit,
    onShowDeleteCollectionDialog: (String) -> Unit,
    onDismissDeleteCollectionDialog: () -> Unit,
    onConfirmDeleteCollection: () -> Unit,
    onShowAttachPackageDialog: (String) -> Unit,
    onPackageSelectedForAttachment: (String) -> Unit,
    onDismissAttachPackageDialog: () -> Unit,
    onConfirmAttachPackage: () -> Unit,
    onShowDetachPackageDialog: (String, String) -> Unit,
    onDismissDetachPackageDialog: () -> Unit,
    onConfirmDetachPackage: () -> Unit,
    onCloseLibrary: () -> Unit,
    onSelectLesson: (String) -> Unit,
    onClearLessonSelection: () -> Unit,
    onLessonQueryChanged: (String) -> Unit,
    onClearLessonQuery: () -> Unit,
    onLessonFilterChanged: (LessonBrowserFilter) -> Unit,
    onLessonSortChanged: (LessonBrowserSort) -> Unit,
    onStartLessonStudy: (PackageLessonSelection) -> Unit,
    onExportPackage: (String, String, Path) -> Unit = { _, _, _ -> },
    onCheckPackageIntegrity: (String) -> Unit = {},
    onDismissPackageIntegrityReport: () -> Unit = {},
    thumbnailLoader: LessonThumbnailLoader,
    modifier: Modifier = Modifier
 ) {
    val focusRequester =
        remember {
            FocusRequester()
        }

    val dialogVisible =
        createCollectionDialogState.visible ||
            renameCollectionDialogState.visible ||
            deleteCollectionDialogState.visible ||
            attachPackageDialogState.visible ||
            detachPackageDialogState.visible ||
            packageIntegrityDialogState.visible

    val keyboardContext =
        ContentLibraryKeyboardContext(
            dialogVisible = dialogVisible,
            lessonBrowserOpen = lessonBrowserUiState != null,
            lessonSelected =
                lessonBrowserUiState
                    ?.selectedLessonId != null
        )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun performKeyboardAction(
        action: ContentLibraryKeyboardAction
    ) {
        when (action) {
            ContentLibraryKeyboardAction.Refresh ->
                onRefresh()

            ContentLibraryKeyboardAction.ImportPackage ->
                choosePackageDirectory()
                    ?.let(onImportDirectory)

            ContentLibraryKeyboardAction.ClearLessonSelection ->
                onClearLessonSelection()

            ContentLibraryKeyboardAction.CloseLessonBrowser ->
                onCloseLibrary()
        }
    }

    CreateCollectionDialog(
        state = createCollectionDialogState,
        onCollectionNameChanged =
            onCreateCollectionNameChanged,
        onDismiss =
            onDismissCreateCollectionDialog,
        onCreate =
            onConfirmCreateCollection
    )

    RenameCollectionDialog(
        state = renameCollectionDialogState,
        onCollectionNameChanged =
            onRenameCollectionNameChanged,
        onDismiss =
            onDismissRenameCollectionDialog,
        onRename =
            onConfirmRenameCollection
    )

    DeleteCollectionDialog(
        state = deleteCollectionDialogState,
        onDismiss =
            onDismissDeleteCollectionDialog,
        onDelete =
            onConfirmDeleteCollection
    )

    AttachPackageDialog(
        state = attachPackageDialogState,
        onPackageSelected =
            onPackageSelectedForAttachment,
        onDismiss =
            onDismissAttachPackageDialog,
        onAttach =
            onConfirmAttachPackage
    )

    DetachPackageDialog(
        state = detachPackageDialogState,
        onDismiss =
            onDismissDetachPackageDialog,
        onDetach =
            onConfirmDetachPackage
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    val key =
                        when (event.key) {
                            Key.R ->
                                ContentLibraryKeyboardKey.R

                            Key.I ->
                                ContentLibraryKeyboardKey.I

                            Key.Escape ->
                                ContentLibraryKeyboardKey.ESCAPE

                            else -> null
                        }

                    val action =
                        key?.let {
                            resolveContentLibraryKeyboardAction(
                                key = it,
                                controlPressed = event.isCtrlPressed,
                                context = keyboardContext
                            )
                        }

                    if (action == null) {
                        false
                    } else {
                        performKeyboardAction(action)
                        true
                    }
                }
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        ContentLibraryHeader(
            packageCount = uiState.packageCount,
            libraryCount = uiState.libraryCount,
            collectionCount =
                uiState.collectionCount,
            busy = uiState.operation !is ContentLibraryOperation.Idle,
            onRefresh = onRefresh,
            onImportDirectory =
                onImportDirectory
        )

        when (val operation = uiState.operation) {
            ContentLibraryOperation.Idle -> Unit
            is ContentLibraryOperation.Loading -> {
                Text(operation.title, fontWeight = FontWeight.SemiBold)
                Text(operation.phase)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is ContentLibraryOperation.Importing -> {
                Text("Importing package", fontWeight = FontWeight.SemiBold)
                Text(operation.phase)
                val fraction = operation.fraction
                if (fraction == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${operation.processed} of ${operation.total}")
                }
            }
            is ContentLibraryOperation.Exporting -> {
                Text("Exporting package ${operation.packageName}", fontWeight = FontWeight.SemiBold)
                Text(operation.phase)
                LinearProgressIndicator(progress = { operation.fraction }, modifier = Modifier.fillMaxWidth())
            }
        }

        uiState.importMessage?.let { message ->
            ImportMessageCard(
                message = message,
                isError = false
            )
        }

        uiState.importError?.let { message ->
            ImportMessageCard(
                message = message,
                isError = true
            )
        }

        uiState.loadError?.let { message ->
            ContentLibraryLoadErrorCard(
                presentation =
                    resolveContentLibraryLoadErrorPresentation(
                        message
                    ),
                onRetry = onRefresh
            )
        }

        if (uiState.isEmpty) {
            EmptyContentLibrary(
                onImportDirectory =
                    onImportDirectory
            )
        } else {
            if (
                lessonBrowserUiState == null &&
                uiState.libraries.isNotEmpty()
            ) {
                SectionTitle(
                    "Libraries"
                )

                uiState.libraries.forEach { libraryItem ->
                    ContentLibraryCard(
                        libraryItem = libraryItem,
                        onOpen = {
                            onOpenLibrary(
                                libraryItem.id
                            )
                        },
                        onCreateCollection = {
                            onShowCreateCollectionDialog(
                                libraryItem.id
                            )
                        },
                        onRenameCollection = { collectionId ->
                            onShowRenameCollectionDialog(
                                collectionId
                            )
                        },
                        onDeleteCollection = { collectionId ->
                            onShowDeleteCollectionDialog(
                                collectionId
                            )
                        },
                        onAttachPackage = { collectionId ->
                            onShowAttachPackageDialog(
                                collectionId
                            )
                        },
                        onDetachPackage = {
                                collectionId,
                                packageId ->
                            onShowDetachPackageDialog(
                                collectionId,
                                packageId
                            )
                        }
                    )
                }
            }

            lessonBrowserUiState?.let { browserUiState ->
                LessonBrowserCard(
                    uiState = browserUiState,
                    onClose = onCloseLibrary,
                    onSelectLesson =
                        onSelectLesson,
                    onClearLessonSelection =
                        onClearLessonSelection,
                    onQueryChanged = onLessonQueryChanged,
                    onClearQuery = onClearLessonQuery,
                    onFilterChanged = onLessonFilterChanged,
                    onSortChanged = onLessonSortChanged,
                    thumbnailLoader = thumbnailLoader,
                    onStartStudy =
                        onStartLessonStudy
                )
            }

            if (
                lessonBrowserUiState == null &&
                uiState.packages.isNotEmpty()
            ) {
                SectionTitle(
                    "Installed Packages"
                )

                uiState.packages.forEach { packageItem ->
                    ContentPackageCard(
                        packageItem = packageItem,
                        onExportPackage = onExportPackage,
                        onCheckIntegrity = onCheckPackageIntegrity,
                        integrityBusy = packageIntegrityDialogState.scanning && packageIntegrityDialogState.packageId == packageItem.id
                    )
                }
            }
        }
    }

    if (packageIntegrityDialogState.visible) {
        PackageIntegrityDialog(packageIntegrityDialogState, onDismissPackageIntegrityReport)
    }
}

@Composable
private fun ContentLibraryHeader(
    packageCount: Int,
    libraryCount: Int,
    collectionCount: Int,
    busy: Boolean,
    onRefresh: () -> Unit,
    onImportDirectory: (Path) -> Unit
) {
    val accessibility =
        resolveContentLibraryHeaderAccessibility(
            libraryCount = libraryCount,
            collectionCount = collectionCount,
            packageCount = packageCount
        )

    val refreshAccessibility =
        resolveContentLibraryActionAccessibility(
            ContentLibraryAction.Refresh
        )

    val importAccessibility =
        resolveContentLibraryActionAccessibility(
            ContentLibraryAction.ImportPackage
        )

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column(
            modifier =
                Modifier.semantics(
                    mergeDescendants = true
                ) {
                    heading()
                    contentDescription =
                        accessibility.contentDescription
                }
        ) {
            Text(
                text = "Content Library",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    accessibility.summary,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !busy,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            refreshAccessibility.contentDescription
                    }
            ) {
                Text(
                    "Refresh"
                )
            }

            Button(
                onClick = {
                    choosePackageDirectory()
                        ?.let(
                            onImportDirectory
                        )
                },
                enabled = !busy,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            importAccessibility.contentDescription
                    }
            ) {
                Text(
                    "Import Package"
                )
            }
        }
        }

        Text(
            text = contentLibraryKeyboardHint(),
            modifier =
                Modifier.semantics {
                    contentDescription =
                        contentLibraryKeyboardHint()
                },
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

private fun choosePackageDirectory(): Path? {
    val files = vn.loi.learning.desktop.ui.library.choosePackageFiles()
    return files.firstOrNull()
}

@Composable
private fun ContentLibraryLoadErrorCard(
    presentation: ContentLibraryLoadErrorPresentation,
    onRetry: () -> Unit
) {
    val retryAccessibility =
        resolveContentLibraryActionAccessibility(
            ContentLibraryAction.RetryLoad
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        presentation.contentDescription
                    liveRegion =
                        LiveRegionMode.Assertive
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .errorContainer
            )
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = presentation.title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer
            )

            Text(
                text = presentation.message,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer
            )

            Text(
                text = presentation.guidance,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer
            )

            OutlinedButton(
                onClick = onRetry,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            retryAccessibility.contentDescription
                    }
            ) {
                Text(
                    presentation.actionLabel
                )
            }
        }
    }
}

@Composable
private fun ImportMessageCard(
    message: String,
    isError: Boolean
) {
    val accessibility =
        resolveContentLibraryMessageAccessibility(
            message = message,
            isError = isError
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        accessibility.contentDescription
                    liveRegion =
                        if (isError) {
                            LiveRegionMode.Assertive
                        } else {
                            LiveRegionMode.Polite
                        }
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isError) {
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                    }
            )
    ) {
        Text(
            text = accessibility.message,
            modifier =
                Modifier.padding(16.dp),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                if (isError) {
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
                }
        )
    }
}

@Composable
private fun EmptyContentLibrary(
    onImportDirectory: (Path) -> Unit
) {
    val presentation =
        resolveContentLibraryEmptyPresentation()

    val importAccessibility =
        resolveContentLibraryActionAccessibility(
            ContentLibraryAction.ImportPackage
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        presentation.contentDescription
                },
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(24.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = presentation.title,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = presentation.description,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Button(
                onClick = {
                    choosePackageDirectory()
                        ?.let(onImportDirectory)
                },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            importAccessibility.contentDescription
                    }
            ) {
                Text(
                    presentation.actionLabel
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        modifier =
            Modifier.semantics {
                heading()
                contentDescription =
                    resolveContentLibrarySectionContentDescription(
                        title
                    )
            },
        style =
            MaterialTheme
                .typography
                .titleLarge,
        fontWeight =
            FontWeight.SemiBold
    )
}

@Composable
private fun ContentLibraryCard(
    libraryItem: ContentLibraryItem,
    onOpen: () -> Unit,
    onCreateCollection: () -> Unit,
    onRenameCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onAttachPackage: (String) -> Unit,
    onDetachPackage: (String, String) -> Unit
) {
    val accessibility =
        resolveLibraryCardAccessibility(
            libraryItem
        )

    val openAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.OpenLibrary,
            targetName = accessibility.title
        )

    val createCollectionAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.CreateCollection,
            targetName = accessibility.title
        )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                        contentDescription =
                            accessibility.contentDescription
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            PackageProperty(
                label = "Contents",
                value =
                    libraryItem
                        .contentCount
                        .toString()
            )

            PackageProperty(
                label = "Learning Items",
                value =
                    libraryItem
                        .learningItemCount
                        .toString()
            )

            PackageProperty(
                label = "Collections",
                value =
                    libraryItem
                        .collectionCount
                        .toString()
            )

            PackageProperty(
                label = "Library ID",
                value = libraryItem.id
            )

            if (
                libraryItem.collections.isNotEmpty()
            ) {
                Text(
                    text = "Collections",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                libraryItem.collections.forEach { collection ->
                    LibraryCollectionCard(
                        collection = collection,
                        onRename = {
                            onRenameCollection(
                                collection.id
                            )
                        },
                        onDelete = {
                            onDeleteCollection(
                                collection.id
                            )
                        },
                        onAttachPackage = {
                            onAttachPackage(
                                collection.id
                            )
                        },
                        onDetachPackage = { packageId ->
                            onDetachPackage(
                                collection.id,
                                packageId
                            )
                        }
                    )
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                openAccessibility.contentDescription
                        }
                ) {
                    Text(
                        "Open Library"
                    )
                }

                OutlinedButton(
                    onClick = onCreateCollection,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                createCollectionAccessibility.contentDescription
                        }
                ) {
                    Text(
                        "Create Collection"
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCollectionCard(
    collection: ContentLibraryCollectionItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAttachPackage: () -> Unit,
    onDetachPackage: (String) -> Unit
) {
    val accessibility =
        resolveCollectionCardAccessibility(
            collection
        )

    val attachAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.AttachPackage,
            targetName = accessibility.title
        )

    val renameAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.RenameCollection,
            targetName = accessibility.title
        )

    val deleteAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.DeleteCollection,
            targetName = accessibility.title
        )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                        contentDescription =
                            accessibility.contentDescription
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            PackageProperty(
                label = "Packages",
                value =
                    collection
                        .packageCount
                        .toString()
            )

            PackageProperty(
                label = "Collection ID",
                value = collection.id
            )

            if (collection.attachedPackages.isEmpty()) {
                Text(
                    text =
                        "No packages are attached to this collection.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            } else {
                Text(
                    text = "Attached Packages",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )

                collection.attachedPackages.forEach { attachedPackage ->
                    AttachedPackageCard(
                        packageItem = attachedPackage,
                        onDetach = {
                            onDetachPackage(
                                attachedPackage.id
                            )
                        }
                    )
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAttachPackage,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                attachAccessibility.contentDescription
                        }
                ) {
                    Text(
                        "Attach Package"
                    )
                }

                OutlinedButton(
                    onClick = onRename,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                renameAccessibility.contentDescription
                        }
                ) {
                    Text(
                        "Rename"
                    )
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                deleteAccessibility.contentDescription
                        }
                ) {
                    Text(
                        "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedPackageCard(
    packageItem: ContentLibraryAttachedPackageItem,
    onDetach: () -> Unit
) {
    val accessibility =
        resolveAttachedPackageCardAccessibility(
            packageItem
        )

    val detachAccessibility =
        resolveContentLibraryActionAccessibility(
            action = ContentLibraryAction.DetachPackage,
            targetName = accessibility.title
        )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                        contentDescription =
                            accessibility.contentDescription
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            if (packageItem.version.isNotBlank()) {
                PackageProperty(
                    label = "Version",
                    value = packageItem.version
                )
            }

            if (packageItem.format.isNotBlank()) {
                PackageProperty(
                    label = "Format",
                    value = packageItem.format
                )
            }

            PackageProperty(
                label = "Package ID",
                value = packageItem.id
            )

            OutlinedButton(
                onClick = onDetach,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            detachAccessibility.contentDescription
                    }
            ) {
                Text(
                    "Detach"
                )
            }
        }
    }
}

@Composable
private fun ContentPackageCard(
    packageItem: ContentLibraryPackageItem,
    onExportPackage: (String, String, Path) -> Unit = { _, _, _ -> },
    onCheckIntegrity: (String) -> Unit = {},
    integrityBusy: Boolean = false
) {
    val accessibility =
        resolveInstalledPackageCardAccessibility(
            packageItem
        )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = accessibility.title,
                modifier =
                    Modifier.semantics {
                        heading()
                        contentDescription =
                            accessibility.contentDescription
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            PackageProperty(
                label = "Version",
                value = packageItem.version
            )

            PackageProperty(
                label = "Format",
                value = packageItem.format
            )

            PackageProperty(
                label = "Libraries",
                value =
                    packageItem
                        .libraryCount
                        .toString()
            )

            PackageProperty(
                label = "Package ID",
                value = packageItem.id
            )

            OutlinedButton(
                onClick = { onCheckIntegrity(packageItem.id) },
                enabled = !integrityBusy,
                modifier = Modifier.semantics {
                    contentDescription = if (integrityBusy) {
                        "Checking integrity for ${packageItem.name}"
                    } else {
                        "Check integrity for ${packageItem.name}"
                    }
                }
            ) {
                Text(if (integrityBusy) "Checking…" else "Check Integrity")
            }
        }
    }
}

@Composable
internal fun PackageIntegrityDialog(
    state: PackageIntegrityDialogState,
    onDismiss: () -> Unit
) {
    val report = state.report
    val presentation = report?.toPresentation()
    val summaryDescription = presentation?.accessibilityDescription ?: "Checking integrity for ${state.packageName}"
    AlertDialog(
        onDismissRequest = { if (!state.scanning) onDismiss() },
        title = { Text("Package Integrity", modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = summaryDescription }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(state.packageName, fontWeight = FontWeight.SemiBold)
                when {
                    state.scanning -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Checking canonical package data and media references…")
                    }
                    state.error != null -> Text("Integrity check failed: ${state.error}")
                    report != null -> {
                        Text("Status: ${presentation?.statusText}", fontWeight = FontWeight.SemiBold)
                        Text(requireNotNull(presentation).summaryText)
                        if (report.findings.isEmpty()) Text("No integrity findings.")
                        IntegritySeverity.entries.forEach { severity ->
                            val sectionFindings = report.findings.filter { it.severity == severity }
                            if (sectionFindings.isNotEmpty()) {
                                Text("PACKAGE ${severity.name}S", fontWeight = FontWeight.Bold)
                                sectionFindings.forEach { finding ->
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(finding.code, fontWeight = FontWeight.SemiBold)
                                            Text(finding.message)
                                            Text("${finding.entityType}: ${finding.entityId}", style = MaterialTheme.typography.bodySmall)
                                            finding.details.forEach { (key, value) ->
                                                Text("$key: $value", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !state.scanning) { Text("Close") }
        }
    )
}

private fun choosePackageExportDestination(defaultPackageName: String): Path? {
    val sanitized = defaultPackageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val defaultFileName = "$sanitized.opd3"
    val chooser = JFileChooser().apply {
        dialogTitle = "Export OPD3 Package Archive"
        selectedFile = java.io.File(defaultFileName)
        fileFilter = javax.swing.filechooser.FileNameExtensionFilter("OPD3 Package (*.opd3)", "opd3")
    }
    val result = chooser.showSaveDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        var selected = chooser.selectedFile.toPath()
        if (!selected.toString().lowercase().endsWith(".opd3")) {
            selected = selected.parent?.resolve("${selected.fileName}.opd3") ?: selected
        }
        return selected
    }
    return null
}

@Composable
private fun PackageProperty(
    label: String,
    value: String
) {
    val accessibility =
        resolveContentLibraryPropertyAccessibility(
            label = label,
            value = value
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        accessibility.contentDescription
                },
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = accessibility.label,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = accessibility.value,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight =
                FontWeight.Medium
        )
    }
}
