package vn.loi.learning.desktop.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.application.library.query.LibraryNavigationTree
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackageId

/**
 * Controller / ViewModel quản lý trạng thái hiển thị và lệnh (Command Experience) của Desktop Library Screen.
 *
 * Đảm bảo:
 * - Tiếp nhận mọi lệnh ghi thông qua [LibraryFacade].
 * - Tái làm mới trạng thái (refresh) duy nhất qua [LibraryQueryService] sau khi lệnh thành công.
 * - Ngăn ngừa gửi trùng lệnh (double-click prevention) bằng cờ [isBusy].
 * - Chuyển đổi typed failures thành thông điệp tường minh cho người dùng.
 * - Giữ nguyên projection cũ khi thất bại, không fabricated optimistic mutation.
 */
class LibraryViewModel(
    private val facade: LibraryFacade?,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner,
    private val onLibraryDataChanged: (() -> Unit)? = null
) {

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
        private set

    var activeDialog by mutableStateOf<LibraryDialogState>(LibraryDialogState.None)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    var selectedCollectionId by mutableStateOf<CollectionId?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        uiState = LibraryUiState.Loading

        val activeFacade = facade
        if (activeFacade == null) {
            uiState = LibraryUiState.Error(
                message = LibraryFailureMessage.forCategory(LibraryFailureCategory.MISCONFIGURED_SERVICE)
            )
            return
        }

        taskRunner.run(
            work = { activeFacade.loadNavigationTree() },
            onSuccess = { tree ->
                updateProjection(tree)
            },
            onFailure = { exception ->
                uiState = LibraryUiState.Error(
                    message = LibraryFailureMessage.forFailure(exception)
                )
            }
        )
    }

    fun selectSection(section: LibrarySection) {
        val current = uiState
        if (current is LibraryUiState.Content) {
            uiState = current.copy(selectedSection = section)
        }
    }

    fun selectCollection(collectionId: CollectionId?) {
        selectedCollectionId = collectionId
    }

    fun closeDialog() {
        if (!isBusy) {
            activeDialog = LibraryDialogState.None
        }
    }

    fun clearFeedback() {
        feedbackMessage = null
    }

    // --- DIALOG TRIGGER ACTIONS ---

    fun openCreateCollectionDialog() {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.CreateCollection()
    }

    fun openRenameCollectionDialog(collectionId: CollectionId, currentName: String) {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.RenameCollection(
            collectionId = collectionId,
            currentName = currentName,
            newNameInput = currentName
        )
    }

    fun openDeleteCollectionDialog(collectionId: CollectionId, collectionName: String) {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.DeleteCollectionConfirm(
            collectionId = collectionId,
            collectionName = collectionName
        )
    }

    fun openAssignPackageDialog(collectionId: CollectionId, collectionName: String) {
        if (isBusy) return
        clearFeedback()
        val currentContent = uiState as? LibraryUiState.Content ?: return
        val collectionNode = currentContent.collections.firstOrNull { it.collection.id == collectionId } ?: return
        val assignedIds = collectionNode.assignedPackages.map { it.id }.toSet()
        val candidatePackages = currentContent.activePackages.filterNot { it.id in assignedIds }

        activeDialog = LibraryDialogState.AssignPackage(
            collectionId = collectionId,
            collectionName = collectionName,
            candidatePackages = candidatePackages,
            selectedPackageId = candidatePackages.firstOrNull()?.id
        )
    }

    fun openRemoveAssignmentDialog(
        collectionId: CollectionId,
        collectionName: String,
        installedPackageId: InstalledPackageId,
        packageName: String
    ) {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.RemoveAssignmentConfirm(
            collectionId = collectionId,
            collectionName = collectionName,
            installedPackageId = installedPackageId,
            packageName = packageName
        )
    }

    fun openArchivePackageDialog(installedPackageId: InstalledPackageId, packageName: String) {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.ArchivePackageConfirm(
            installedPackageId = installedPackageId,
            packageName = packageName
        )
    }

    fun openRestorePackageDialog(installedPackageId: InstalledPackageId, packageName: String) {
        if (isBusy) return
        clearFeedback()
        activeDialog = LibraryDialogState.RestorePackageConfirm(
            installedPackageId = installedPackageId,
            packageName = packageName
        )
    }

    // --- COMMAND SUBMIT HANDLERS ---

    fun submitCreateCollection(nameInput: String, descriptionInput: String = "") {
        if (isBusy) return
        val trimmedName = nameInput.trim()
        if (trimmedName.isBlank()) {
            activeDialog = LibraryDialogState.CreateCollection(
                nameInput = nameInput,
                descriptionInput = descriptionInput,
                errorMessage = "Collection name cannot be empty."
            )
            return
        }

        executeCommand(
            work = { activeFacade ->
                activeFacade.createCollection(
                    name = CollectionName(trimmedName),
                    description = descriptionInput.trim()
                )
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                activeDialog = LibraryDialogState.CreateCollection(
                    nameInput = nameInput,
                    descriptionInput = descriptionInput,
                    errorMessage = errorMsg
                )
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                feedbackMessage = "Collection '$trimmedName' created successfully."
            }
        )
    }

    fun submitRenameCollection(collectionId: CollectionId, newNameInput: String) {
        if (isBusy) return
        val trimmedName = newNameInput.trim()
        if (trimmedName.isBlank()) {
            val currentDialog = activeDialog as? LibraryDialogState.RenameCollection
            if (currentDialog != null) {
                activeDialog = currentDialog.copy(errorMessage = "Collection name cannot be empty.")
            }
            return
        }

        executeCommand(
            work = { activeFacade ->
                activeFacade.renameCollection(
                    collectionId = collectionId,
                    newName = CollectionName(trimmedName)
                )
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.RenameCollection
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                selectedCollectionId = collectionId
                feedbackMessage = "Collection renamed to '$trimmedName'."
            }
        )
    }

    fun submitDeleteCollection(collectionId: CollectionId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.deleteCollection(collectionId = collectionId)
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.DeleteCollectionConfirm
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                if (selectedCollectionId == collectionId) {
                    selectedCollectionId = null
                }
                feedbackMessage = "Collection deleted."
            }
        )
    }

    fun submitAssignPackage(collectionId: CollectionId, installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.assignPackageToCollection(
                    collectionId = collectionId,
                    installedPackageId = installedPackageId
                )
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.AssignPackage
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                selectedCollectionId = collectionId
                feedbackMessage = "Package assigned to collection successfully."
            }
        )
    }

    fun submitRemoveAssignment(collectionId: CollectionId, installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.removePackageFromCollection(
                    collectionId = collectionId,
                    installedPackageId = installedPackageId
                )
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.RemoveAssignmentConfirm
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                selectedCollectionId = collectionId
                feedbackMessage = "Package assignment removed."
            }
        )
    }

    fun submitArchivePackage(installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.archivePackage(installedPackageId = installedPackageId)
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.ArchivePackageConfirm
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                feedbackMessage = "Package archived successfully."
            }
        )
    }

    fun submitRestorePackage(installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.restorePackage(installedPackageId = installedPackageId)
            },
            onTypedFailure = { result ->
                val errorMsg = LibraryFailureMessage.forCommandResult(result)
                val currentDialog = activeDialog as? LibraryDialogState.RestorePackageConfirm
                if (currentDialog != null) {
                    activeDialog = currentDialog.copy(errorMessage = errorMsg)
                }
                feedbackMessage = errorMsg
            },
            onSuccessRefreshed = {
                feedbackMessage = "Package restored successfully."
            }
        )
    }

    fun setActivePackage(installedPackageId: InstalledPackageId?) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.setActivePackage(installedPackageId = installedPackageId)
            },
            onTypedFailure = { result ->
                feedbackMessage = LibraryFailureMessage.forCommandResult(result)
            },
            onSuccessRefreshed = {
                feedbackMessage = if (installedPackageId != null) "Active package updated." else "Active package cleared."
            }
        )
    }

    fun movePackageUp(installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.movePackageUp(installedPackageId = installedPackageId)
            },
            onTypedFailure = { result ->
                feedbackMessage = LibraryFailureMessage.forCommandResult(result)
            },
            onSuccessRefreshed = {
                feedbackMessage = "Package order updated."
            }
        )
    }

    fun movePackageDown(installedPackageId: InstalledPackageId) {
        if (isBusy) return

        executeCommand(
            work = { activeFacade ->
                activeFacade.movePackageDown(installedPackageId = installedPackageId)
            },
            onTypedFailure = { result ->
                feedbackMessage = LibraryFailureMessage.forCommandResult(result)
            },
            onSuccessRefreshed = {
                feedbackMessage = "Package order updated."
            }
        )
    }

    fun openResetPackageProgressDialog(installedPackageId: InstalledPackageId, packageName: String) {
        if (isBusy) return
        activeDialog = LibraryDialogState.ResetPackageProgressConfirm(
            installedPackageId = installedPackageId,
            packageName = packageName
        )
    }

    fun submitResetPackageProgress(onResetProgress: (InstalledPackageId) -> Boolean) {
        val current = activeDialog as? LibraryDialogState.ResetPackageProgressConfirm ?: return
        if (isBusy) return

        isBusy = true
        taskRunner.run(
            work = { onResetProgress(current.installedPackageId) },
            onSuccess = { success ->
                isBusy = false
                if (success) {
                    activeDialog = LibraryDialogState.None
                    feedbackMessage = "Đã đặt lại tiến độ học cho chủ đề '${current.packageName}'."
                    refresh()
                    onLibraryDataChanged?.invoke()
                } else {
                    activeDialog = current.copy(errorMessage = "Không thể đặt lại tiến độ học cho chủ đề này.")
                }
            },
            onFailure = { exception ->
                isBusy = false
                activeDialog = current.copy(errorMessage = exception.message ?: "Đặt lại tiến độ học thất bại.")
            }
        )
    }

    // --- HELPER EXECUTION PIPELINE ---

    private fun <T> executeCommand(
        work: (LibraryFacade) -> LibraryCommandResult<T>,
        onTypedFailure: (LibraryCommandResult<T>) -> Unit,
        onSuccessRefreshed: () -> Unit
    ) {
        val activeFacade = facade
        if (activeFacade == null) {
            feedbackMessage = LibraryFailureMessage.SERVICE_UNAVAILABLE_MESSAGE
            return
        }

        isBusy = true
        taskRunner.run(
            work = {
                val cmdResult = work(activeFacade)
                if (cmdResult is LibraryCommandResult.Success) {
                    cmdResult to activeFacade.loadNavigationTree()
                } else {
                    cmdResult to null
                }
            },
            onSuccess = { (cmdResult, refreshedTree) ->
                isBusy = false
                if (cmdResult is LibraryCommandResult.Success && refreshedTree != null) {
                    activeDialog = LibraryDialogState.None
                    updateProjection(refreshedTree)
                    onSuccessRefreshed()
                    onLibraryDataChanged?.invoke()
                } else {
                    onTypedFailure(cmdResult)
                }
            },
            onFailure = { exception ->
                isBusy = false
                val errorMsg = LibraryFailureMessage.forFailure(exception)
                feedbackMessage = errorMsg
            }
        )
    }

    private fun updateProjection(tree: LibraryNavigationTree) {
        val currentSection = (uiState as? LibraryUiState.Content)?.selectedSection ?: LibrarySection.OVERVIEW
        uiState = if (tree.installedPackages.isEmpty() && tree.collections.isEmpty() && tree.deletedCollections.isEmpty()) {
            LibraryUiState.Empty("Library '${tree.libraryName}' is empty. No installed packages or active collections found.")
        } else {
            LibraryUiState.Content(tree = tree, selectedSection = currentSection)
        }

        // Reconcile selected collection ID if it no longer exists in active collections
        val activeCollectionIds = tree.collections.map { it.collection.id }.toSet()
        if (selectedCollectionId != null && selectedCollectionId !in activeCollectionIds) {
            selectedCollectionId = null
        }
    }
}
