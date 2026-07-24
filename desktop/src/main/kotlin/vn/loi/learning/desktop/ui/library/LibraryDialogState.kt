package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Representational State Model cho các Dialog/Form tương tác Command trên Desktop Library UI.
 */
sealed interface LibraryDialogState {
    data object None : LibraryDialogState

    data class CreateCollection(
        val nameInput: String = "",
        val descriptionInput: String = "",
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class RenameCollection(
        val collectionId: CollectionId,
        val currentName: String,
        val newNameInput: String = "",
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class DeleteCollectionConfirm(
        val collectionId: CollectionId,
        val collectionName: String,
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class AssignPackage(
        val collectionId: CollectionId,
        val collectionName: String,
        val candidatePackages: List<InstalledPackageSummary>,
        val selectedPackageId: InstalledPackageId? = null,
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class RemoveAssignmentConfirm(
        val collectionId: CollectionId,
        val collectionName: String,
        val installedPackageId: InstalledPackageId,
        val packageName: String,
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class ArchivePackageConfirm(
        val installedPackageId: InstalledPackageId,
        val packageName: String,
        val errorMessage: String? = null
    ) : LibraryDialogState

    data class RestorePackageConfirm(
        val installedPackageId: InstalledPackageId,
        val packageName: String,
        val errorMessage: String? = null
    ) : LibraryDialogState
}
