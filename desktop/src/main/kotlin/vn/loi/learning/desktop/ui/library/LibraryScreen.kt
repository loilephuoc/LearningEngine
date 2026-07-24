package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LibraryScreenContent(
            uiState = viewModel.uiState,
            feedbackMessage = viewModel.feedbackMessage,
            onClearFeedback = viewModel::clearFeedback,
            onSelectSection = viewModel::selectSection,
            onRefresh = viewModel::refresh,
            onCreateCollection = viewModel::openCreateCollectionDialog,
            onRenameCollection = viewModel::openRenameCollectionDialog,
            onDeleteCollection = viewModel::openDeleteCollectionDialog,
            onAssignPackage = viewModel::openAssignPackageDialog,
            onRemoveAssignment = viewModel::openRemoveAssignmentDialog,
            onArchivePackage = viewModel::openArchivePackageDialog,
            onRestorePackage = viewModel::openRestorePackageDialog,
            modifier = Modifier.fillMaxSize()
        )

        LibraryDialogHost(
            dialogState = viewModel.activeDialog,
            isBusy = viewModel.isBusy,
            onClose = viewModel::closeDialog,
            onSubmitCreateCollection = viewModel::submitCreateCollection,
            onSubmitRenameCollection = { newName ->
                (viewModel.activeDialog as? LibraryDialogState.RenameCollection)?.let {
                    viewModel.submitRenameCollection(it.collectionId, newName)
                }
            },
            onSubmitDeleteCollection = {
                (viewModel.activeDialog as? LibraryDialogState.DeleteCollectionConfirm)?.let {
                    viewModel.submitDeleteCollection(it.collectionId)
                }
            },
            onSubmitAssignPackage = { installedPackageId ->
                (viewModel.activeDialog as? LibraryDialogState.AssignPackage)?.let {
                    viewModel.submitAssignPackage(it.collectionId, installedPackageId)
                }
            },
            onSubmitRemoveAssignment = {
                (viewModel.activeDialog as? LibraryDialogState.RemoveAssignmentConfirm)?.let {
                    viewModel.submitRemoveAssignment(it.collectionId, it.installedPackageId)
                }
            },
            onSubmitArchivePackage = {
                (viewModel.activeDialog as? LibraryDialogState.ArchivePackageConfirm)?.let {
                    viewModel.submitArchivePackage(it.installedPackageId)
                }
            },
            onSubmitRestorePackage = {
                (viewModel.activeDialog as? LibraryDialogState.RestorePackageConfirm)?.let {
                    viewModel.submitRestorePackage(it.installedPackageId)
                }
            }
        )
    }
}

@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    feedbackMessage: String? = null,
    onClearFeedback: () -> Unit = {},
    onSelectSection: (LibrarySection) -> Unit = {},
    onRefresh: () -> Unit = {},
    onCreateCollection: () -> Unit = {},
    onRenameCollection: (CollectionId, String) -> Unit = { _, _ -> },
    onDeleteCollection: (CollectionId, String) -> Unit = { _, _ -> },
    onAssignPackage: (CollectionId, String) -> Unit = { _, _ -> },
    onRemoveAssignment: (CollectionId, String, InstalledPackageId, String) -> Unit = { _, _, _, _ -> },
    onArchivePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onRestorePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LibraryUiState.Loading -> {
            LibraryLoadingView(modifier = modifier)
        }
        is LibraryUiState.Empty -> {
            LibraryEmptyView(
                message = uiState.message,
                onRefresh = onRefresh,
                modifier = modifier
            )
        }
        is LibraryUiState.Error -> {
            LibraryErrorView(
                message = uiState.message,
                onRetry = onRefresh,
                modifier = modifier
            )
        }
        is LibraryUiState.Content -> {
            val scrollState = rememberScrollState()
            val sectionCounts = mapOf(
                LibrarySection.OVERVIEW to uiState.installedPackages.size + uiState.collections.size,
                LibrarySection.INSTALLED to uiState.installedPackages.size,
                LibrarySection.ACTIVE to uiState.activePackages.size,
                LibrarySection.ARCHIVED to uiState.archivedPackages.size,
                LibrarySection.COLLECTIONS to uiState.collections.size,
                LibrarySection.DELETED to uiState.deletedCollections.size
            )

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (feedbackMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feedbackMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearFeedback) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                LibraryHeader(
                    libraryName = uiState.tree.libraryName,
                    statistics = uiState.statistics,
                    onCreateCollection = onCreateCollection
                )

                LibrarySectionTabs(
                    selectedSection = uiState.selectedSection,
                    onSelectSection = onSelectSection,
                    counts = sectionCounts
                )

                when (uiState.selectedSection) {
                    LibrarySection.OVERVIEW ->
                        LibraryOverviewSection(uiState = uiState)

                    LibrarySection.INSTALLED ->
                        PackageListSection(
                            title = "Installed Packages (${uiState.installedPackages.size})",
                            packages = uiState.installedPackages,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage
                        )

                    LibrarySection.ACTIVE ->
                        PackageListSection(
                            title = "Active Packages (${uiState.activePackages.size})",
                            packages = uiState.activePackages,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage
                        )

                    LibrarySection.ARCHIVED ->
                        PackageListSection(
                            title = "Archived Packages (${uiState.archivedPackages.size})",
                            packages = uiState.archivedPackages,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage
                        )

                    LibrarySection.COLLECTIONS ->
                        ActiveCollectionListSection(
                            collections = uiState.collections,
                            onCreateCollection = onCreateCollection,
                            onRenameCollection = onRenameCollection,
                            onDeleteCollection = onDeleteCollection,
                            onAssignPackage = onAssignPackage,
                            onRemoveAssignment = onRemoveAssignment
                        )

                    LibrarySection.DELETED ->
                        DeletedCollectionListSection(
                            collections = uiState.deletedCollections
                        )
                }
            }
        }
    }
}
