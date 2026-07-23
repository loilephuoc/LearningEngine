package vn.loi.learning.desktop.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner

/**
 * Controller / ViewModel quản lý trạng thái hiển thị của Desktop Library Screen.
 */
class LibraryViewModel(
    private val facade: LibraryFacade?,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
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
                uiState = if (tree.installedPackages.isEmpty() && tree.collections.isEmpty() && tree.deletedCollections.isEmpty()) {
                    LibraryUiState.Empty("Library '${tree.libraryName}' is empty. No installed packages or active collections found.")
                } else {
                    LibraryUiState.Content(tree = tree, selectedSection = LibrarySection.OVERVIEW)
                }
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
}
