package vn.loi.learning.desktop.ui.contentlibrary

/**
 * UI state của Lesson Browser.
 */
data class LessonBrowserUiState(
    val libraryId: String = "",
    val libraryName: String = "",
    val lessons: List<LessonBrowserItem> = emptyList(),
    val selectedLessonId: String? = null,
    val query: String = "",
    val appliedQuery: String = query,
    val filter: LessonBrowserFilter = LessonBrowserFilter.ALL,
    val sort: LessonBrowserSort = LessonBrowserSort.PACKAGE_ORDER
) {

    val lessonCount: Int
        get() = lessons.size

    val isEmpty: Boolean
        get() = lessons.isEmpty()

    val visibleLessons: List<LessonBrowserItem>
        get() = projectLessons(lessons, appliedQuery, filter, sort)

    val selectedLesson: LessonBrowserItem?
        get() =
            lessons.firstOrNull {
                it.id == selectedLessonId
            }

    fun select(
        lessonId: String
    ): LessonBrowserUiState =
        copy(
            selectedLessonId = lessonId
        )

    fun clearSelection(): LessonBrowserUiState =
        copy(
            selectedLessonId = null
        )
}
