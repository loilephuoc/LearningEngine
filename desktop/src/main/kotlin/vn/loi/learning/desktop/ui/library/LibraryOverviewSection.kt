package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryOverviewSection(
    uiState: LibraryUiState.Content,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PackageListSection(
            title = "Active Installed Packages",
            packages = uiState.activePackages
        )

        ActiveCollectionListSection(
            collections = uiState.collections
        )

        if (uiState.archivedPackages.isNotEmpty()) {
            PackageListSection(
                title = "Archived Packages",
                packages = uiState.archivedPackages
            )
        }

        if (uiState.deletedCollections.isNotEmpty()) {
            DeletedCollectionListSection(
                collections = uiState.deletedCollections
            )
        }
    }
}
