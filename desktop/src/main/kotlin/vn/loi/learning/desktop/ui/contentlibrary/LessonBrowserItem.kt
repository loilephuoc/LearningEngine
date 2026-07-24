package vn.loi.learning.desktop.ui.contentlibrary

data class LessonProgressUiModel(
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
        fun empty(): LessonProgressUiModel = LessonProgressUiModel()

        fun from(progress: vn.loi.learning.application.packageprogress.LessonLearningProgress): LessonProgressUiModel =
            LessonProgressUiModel(
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
 * Presentation model cho một Content có thể học trong Lesson Browser.
 *
 * group, section và lesson được giữ nguyên từ ContentMetadata
 * để Presentation Layer có thể hiển thị đúng cấu trúc của package
 * mà không tạo thêm business model trong Desktop.
 */
data class LessonBrowserItem(
    val id: String,
    val title: String,
    val type: String,
    val group: String?,
    val section: String?,
    val lesson: String?,
    val primaryText: String,
    val translatedText: String?,
    val learningItemCount: Int,
    val imagePath: String? = null,
    val progress: LessonProgressUiModel = LessonProgressUiModel.empty()
) {

    val hierarchyPath: String
        get() =
            listOfNotNull(
                group?.takeIf {
                    it.isNotBlank()
                },
                section?.takeIf {
                    it.isNotBlank()
                },
                lesson?.takeIf {
                    it.isNotBlank()
                }
            )
                .distinct()
                .joinToString(
                    separator = " → "
                )

    val hasHierarchy: Boolean
        get() =
            hierarchyPath.isNotBlank()
}
