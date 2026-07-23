package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    LibraryScreenContent(
        uiState = viewModel.uiState,
        onSelectSection = viewModel::selectSection,
        onRefresh = viewModel::refresh,
        modifier = modifier
    )
}

@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    onSelectSection: (LibrarySection) -> Unit,
    onRefresh: () -> Unit,
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
                LibraryHeader(
                    libraryName = uiState.tree.libraryName,
                    statistics = uiState.statistics
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
                            packages = uiState.installedPackages
                        )

                    LibrarySection.ACTIVE ->
                        PackageListSection(
                            title = "Active Packages (${uiState.activePackages.size})",
                            packages = uiState.activePackages
                        )

                    LibrarySection.ARCHIVED ->
                        PackageListSection(
                            title = "Archived Packages (${uiState.archivedPackages.size})",
                            packages = uiState.archivedPackages
                        )

                    LibrarySection.COLLECTIONS ->
                        ActiveCollectionListSection(
                            collections = uiState.collections
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
