package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import java.nio.file.Path
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun LibraryOverviewSection(
    uiState: LibraryUiState.Content,
    onArchivePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onRestorePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onSetActivePackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveUpPackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveDownPackage: ((InstalledPackageId) -> Unit)? = null,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    onCheckPackageIntegrity: ((String) -> Unit)? = null,
    integrityScanningPackageId: String? = null,
    integrityScanBusy: Boolean = false,
    exportBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PackageListSection(
            title = "Active Installed Packages",
            packages = uiState.activePackages,
            packageProgress = uiState.packageProgress,
            activePackageId = uiState.activePackageId,
            onArchivePackage = onArchivePackage,
            onRestorePackage = onRestorePackage,
            onSetActivePackage = onSetActivePackage,
            onMoveUpPackage = onMoveUpPackage,
            onMoveDownPackage = onMoveDownPackage,
            onOpenLibrary = onOpenLibrary,
            onExportPackage = onExportPackage,
            onRemovePackage = onRemovePackage,
            onCheckPackageIntegrity = onCheckPackageIntegrity,
            integrityScanningPackageId = integrityScanningPackageId,
            integrityScanBusy = integrityScanBusy,
            exportBusy = exportBusy
        )

        ActiveCollectionListSection(
            collections = uiState.collections
        )

        if (uiState.archivedPackages.isNotEmpty()) {
            PackageListSection(
                title = "Archived Packages",
                packages = uiState.archivedPackages,
                packageProgress = uiState.packageProgress,
                activePackageId = uiState.activePackageId,
                onArchivePackage = onArchivePackage,
                onRestorePackage = onRestorePackage,
                onSetActivePackage = onSetActivePackage,
                onMoveUpPackage = onMoveUpPackage,
                onMoveDownPackage = onMoveDownPackage,
                onOpenLibrary = onOpenLibrary,
                onExportPackage = onExportPackage,
                onRemovePackage = onRemovePackage,
                onCheckPackageIntegrity = onCheckPackageIntegrity,
                integrityScanningPackageId = integrityScanningPackageId,
                integrityScanBusy = integrityScanBusy,
                exportBusy = exportBusy
            )
        }

        if (uiState.deletedCollections.isNotEmpty()) {
            DeletedCollectionListSection(
                collections = uiState.deletedCollections
            )
        }
    }
}
