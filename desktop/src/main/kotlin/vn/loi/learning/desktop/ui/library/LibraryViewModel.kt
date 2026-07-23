package vn.loi.learning.desktop.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Controller / ViewModel quản lý trạng thái hiển thị của Desktop Library Screen.
 */
class LibraryViewModel(
    private val facade: LibraryFacade,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner,
    private val targetLibraryId: LibraryId = LibraryId("default-library")
) {

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Loading)
        private set

    init {
        refresh()
    }

    fun refresh() {
        uiState = LibraryUiState.Loading
        taskRunner.run(
            work = { facade.loadNavigationTree(targetLibraryId) },
            onSuccess = { tree ->
                uiState = if (tree == null || (tree.installedPackages.isEmpty() && tree.collections.isEmpty() && tree.deletedCollections.isEmpty())) {
                    LibraryUiState.Empty("Library is empty or unavailable.")
                } else {
                    LibraryUiState.Content(tree = tree, selectedSection = LibrarySection.OVERVIEW)
                }
            },
            onFailure = { exception ->
                uiState = LibraryUiState.Error(
                    message = exception.message ?: "Failed to load library content."
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
