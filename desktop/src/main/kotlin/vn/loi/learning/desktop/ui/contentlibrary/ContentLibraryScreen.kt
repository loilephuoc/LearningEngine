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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import javax.swing.JFileChooser

@Composable
fun ContentLibraryScreen(
    viewModel: ContentLibraryViewModel,
    onStartLessonStudy: (String) -> Unit,
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
        onStartLessonStudy =
            onStartLessonStudy,
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
    onStartLessonStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            onRefresh = onRefresh,
            onImportDirectory =
                onImportDirectory
        )

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

        if (uiState.isEmpty) {
            EmptyContentLibrary()
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
                        packageItem
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentLibraryHeader(
    packageCount: Int,
    libraryCount: Int,
    collectionCount: Int,
    onRefresh: () -> Unit,
    onImportDirectory: (Path) -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Column {
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
                    buildString {
                        append(libraryCount)

                        append(
                            if (libraryCount == 1) {
                                " library"
                            } else {
                                " libraries"
                            }
                        )

                        append(" · ")
                        append(collectionCount)
                        append(" collection")

                        if (collectionCount != 1) {
                            append("s")
                        }

                        append(" · ")
                        append(packageCount)
                        append(" installed package")

                        if (packageCount != 1) {
                            append("s")
                        }
                    },
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
                onClick = onRefresh
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
                }
            ) {
                Text(
                    "Import Package"
                )
            }
        }
    }
}

private fun choosePackageDirectory(): Path? {
    val chooser =
        JFileChooser().apply {
            dialogTitle =
                "Select directory containing .opd3 or .pkg files"

            fileSelectionMode =
                JFileChooser.DIRECTORIES_ONLY

            isAcceptAllFileFilterUsed =
                false
        }

    return if (
        chooser.showOpenDialog(null) ==
        JFileChooser.APPROVE_OPTION
    ) {
        chooser.selectedFile.toPath()
    } else {
        null
    }
}

@Composable
private fun ImportMessageCard(
    message: String,
    isError: Boolean
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
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
            text = message,
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
private fun EmptyContentLibrary() {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(24.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No content libraries",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Choose Import Package and select a directory containing .opd3 or .pkg files.",
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
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
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
                text = libraryItem.name,
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
                    onClick = onOpen
                ) {
                    Text(
                        "Open Library"
                    )
                }

                OutlinedButton(
                    onClick = onCreateCollection
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
                text = collection.name,
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
                    onClick = onAttachPackage
                ) {
                    Text(
                        "Attach Package"
                    )
                }

                OutlinedButton(
                    onClick = onRename
                ) {
                    Text(
                        "Rename"
                    )
                }

                OutlinedButton(
                    onClick = onDelete
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
                text = packageItem.name,
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
                onClick = onDetach
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
    packageItem: ContentLibraryPackageItem
) {
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
                text = packageItem.name,
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
        }
    }
}

@Composable
private fun PackageProperty(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
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
            text = value,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight =
                FontWeight.Medium
        )
    }
}