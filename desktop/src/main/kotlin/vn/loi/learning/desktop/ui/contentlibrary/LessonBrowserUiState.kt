package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.library.model.InstalledPackageId

data class PackageProgressUiModel(
    val totalLessonCount: Int = 0,
    val totalLearningItemCount: Int = 0,
    val unseenItemCount: Int = 0,
    val newStateItemCount: Int = 0,
    val startedItemCount: Int = 0,
    val masteredItemCount: Int = 0,
    val dueItemCount: Int = 0,
    val suspendedItemCount: Int = 0,
    val completionPercent: Int = 0,
    val startedPercent: Int = 0
) {
    companion object {
        fun from(progress: vn.loi.learning.application.packageprogress.PackageLearningProgress): PackageProgressUiModel =
            PackageProgressUiModel(
                totalLessonCount = progress.totalLessonCount,
                totalLearningItemCount = progress.totalLearningItemCount,
                unseenItemCount = progress.unseenItemCount,
                newStateItemCount = progress.newStateItemCount,
                startedItemCount = progress.startedItemCount,
                masteredItemCount = progress.masteredItemCount,
                dueItemCount = progress.dueItemCount,
                suspendedItemCount = progress.suspendedItemCount,
                completionPercent = progress.completionPercent,
                startedPercent = progress.startedPercent
            )
    }
}

/**
 * UI state của Lesson Browser.
 */
data class LessonBrowserUiState(
    val libraryId: String = "",
    val libraryName: String = "",
    val installedPackageId: InstalledPackageId? = null,
    val packageProgress: PackageProgressUiModel? = null,
    val lessons: List<LessonBrowserItem> = emptyList(),
    val selectedLessonId: String? = null,
    val query: String = "",
    val appliedQuery: String = query,
    val filter: LessonBrowserFilter = LessonBrowserFilter.ALL,
    val sort: LessonBrowserSort = LessonBrowserSort.PACKAGE_ORDER
) {

    val lessonCount: Int
        get() = lessons.size

    val totalLearningItemCount: Int
        get() = lessons.sumOf { it.learningItemCount }

    val isEmpty: Boolean
        get() = lessons.isEmpty()

    val visibleLessons: List<LessonBrowserItem>
        get() = projectLessons(lessons, appliedQuery, filter, sort)

    val selectedLesson: LessonBrowserItem?
        get() =
            lessons.firstOrNull {
                it.id == selectedLessonId
            }

    val selectedLessonInView: LessonBrowserItem?
        get() = visibleLessons.firstOrNull {
            it.id == selectedLessonId
        }

    val selectedAction: LessonStudyAction?
        get() = selectedLessonInView?.let { LessonStudyActionPolicy.evaluate(it.progress, it.learningItemCount) }

    val isStartEnabled: Boolean
        get() = installedPackageId != null && selectedLessonInView != null && (selectedAction?.isEnabled == true)

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
