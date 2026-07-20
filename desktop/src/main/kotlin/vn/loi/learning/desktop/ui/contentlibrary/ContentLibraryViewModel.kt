package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Path

/**
 * Quản lý Presentation State của Content Library.
 */
class ContentLibraryViewModel(
    private val facade: ContentLibraryFacade,
    private val lessonBrowserFacade: LessonBrowserFacade,
    private val onContentDataChanged:
    (() -> Unit)? = null
) {

    var uiState by mutableStateOf(
        facade.load()
    )
        private set

    var lessonBrowserUiState by mutableStateOf<LessonBrowserUiState?>(null)
        private set

    fun refresh() {
        val refreshedState =
            facade.load()

        uiState =
            refreshedState.copy(
                importMessage = uiState.importMessage,
                importError = uiState.importError
            )

        val currentLessonBrowserState =
            lessonBrowserUiState

        if (currentLessonBrowserState != null) {
            val selectedLibrary =
                uiState.libraries.firstOrNull { library ->
                    library.id ==
                            currentLessonBrowserState.libraryId
                }

            lessonBrowserUiState =
                if (selectedLibrary == null) {
                    null
                } else {
                    lessonBrowserFacade.load(
                        libraryId = selectedLibrary.id,
                        libraryName = selectedLibrary.name
                    )
                }
        }
    }

    fun openLibrary(
        libraryId: String
    ) {
        val library =
            uiState.libraries.firstOrNull { item ->
                item.id == libraryId
            } ?: return

        lessonBrowserUiState =
            lessonBrowserFacade.load(
                libraryId = library.id,
                libraryName = library.name
            )
    }

    fun closeLibrary() {
        lessonBrowserUiState = null
    }

    fun selectLesson(
        lessonId: String
    ) {
        val currentState =
            lessonBrowserUiState
                ?: return

        lessonBrowserUiState =
            currentState.select(
                lessonId
            )
    }

    fun clearLessonSelection() {
        val currentState =
            lessonBrowserUiState
                ?: return

        lessonBrowserUiState =
            currentState.clearSelection()
    }

    fun importFromDirectory(
        directory: Path
    ) {
        uiState =
            uiState.copy(
                importMessage = null,
                importError = null
            )

        try {
            val result =
                facade.importFromDirectory(
                    directory
                )

            val refreshedState =
                facade.load()

            uiState =
                refreshedState.copy(
                    importMessage =
                        if (
                            result.importedPackageCount == 0
                        ) {
                            "No .opd3 or .pkg files were found in the selected directory."
                        } else {
                            buildString {
                                append("Imported ")
                                append(
                                    result.importedPackageCount
                                )
                                append(" package")

                                if (
                                    result.importedPackageCount != 1
                                ) {
                                    append("s")
                                }

                                append(", ")
                                append(
                                    result.importedLibraryCount
                                )
                                append(" libraries, ")
                                append(
                                    result.importedContentCount
                                )
                                append(" contents and ")
                                append(
                                    result.importedLearningItemCount
                                )
                                append(" learning items.")
                            }
                        }
                )

            lessonBrowserUiState = null

            onContentDataChanged?.invoke()
        } catch (exception: Exception) {
            uiState =
                uiState.copy(
                    importMessage = null,
                    importError =
                        exception.message
                            ?: exception::class.simpleName
                            ?: "Package import failed."
                )
        }
    }
}