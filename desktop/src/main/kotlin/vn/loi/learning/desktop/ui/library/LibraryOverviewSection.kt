package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun LibraryOverviewSection(
    uiState: LibraryUiState.Content,
    onArchivePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onRestorePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onSetActivePackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveUpPackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveDownPackage: ((InstalledPackageId) -> Unit)? = null,
    onOpenLibrary: ((String) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PackageListSection(
            title = "Active Installed Packages",
            packages = uiState.activePackages,
            activePackageId = uiState.activePackageId,
            onArchivePackage = onArchivePackage,
            onRestorePackage = onRestorePackage,
            onSetActivePackage = onSetActivePackage,
            onMoveUpPackage = onMoveUpPackage,
            onMoveDownPackage = onMoveDownPackage,
            onOpenLibrary = onOpenLibrary,
            onRemovePackage = onRemovePackage
        )

        ActiveCollectionListSection(
            collections = uiState.collections
        )

        if (uiState.archivedPackages.isNotEmpty()) {
            PackageListSection(
                title = "Archived Packages",
                packages = uiState.archivedPackages,
                activePackageId = uiState.activePackageId,
                onArchivePackage = onArchivePackage,
                onRestorePackage = onRestorePackage,
                onSetActivePackage = onSetActivePackage,
                onMoveUpPackage = onMoveUpPackage,
                onMoveDownPackage = onMoveDownPackage,
                onOpenLibrary = onOpenLibrary,
                onRemovePackage = onRemovePackage
            )
        }

        if (uiState.deletedCollections.isNotEmpty()) {
            DeletedCollectionListSection(
                collections = uiState.deletedCollections
            )
        }
    }
}
