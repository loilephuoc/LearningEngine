package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Facade cho Lesson Browser.
 *
 * Chỉ chuyển Application DTO -> Presentation model.
 * Không chứa business logic.
 */
class LessonBrowserFacade(
    private val applicationContext: LearningApplicationContext
) {

    fun load(
        libraryId: String,
        libraryName: String
    ): LessonBrowserUiState {

        val lessons =
            applicationContext
                .libraryContents
                .query(
                    ContentLibraryId(libraryId)
                )
                .map { content ->
                    LessonBrowserItem(
                        id = content.id,
                        title = content.title,
                        type = content.type,
                        primaryText = content.primaryText,
                        translatedText = content.translatedText,
                        learningItemCount =
                            content.learningItemCount
                    )
                }

        return LessonBrowserUiState(
            libraryId = libraryId,
            libraryName = libraryName,
            lessons = lessons
        )
    }
}