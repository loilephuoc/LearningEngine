package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Phân loại hành động học cho một Lesson dựa trên trạng thái tiến độ.
 */
enum class LessonStudyActionType {
    UNAVAILABLE,
    START,
    CONTINUE,
    REVIEW
}

/**
 * Presentation model cho nút hành động học của Lesson.
 */
data class LessonStudyAction(
    val type: LessonStudyActionType,
    val label: String,
    val isEnabled: Boolean,
    val dueCount: Int = 0
) {
    val isAvailable: Boolean
        get() = type != LessonStudyActionType.UNAVAILABLE

    val dueText: String?
        get() = if (dueCount > 0) {
            "$dueCount item(s) due now"
        } else null
}

/**
 * Pure presentation policy để xác định hành động học và nhãn cho Lesson.
 *
 * Không phụ thuộc Compose, Infrastructure, Repository hay Clock.
 * Priority: UNAVAILABLE -> REVIEW -> CONTINUE -> START
 */
object LessonStudyActionPolicy {

    fun evaluate(
        progress: LessonProgressUiModel,
        itemLearningItemCount: Int = 0
    ): LessonStudyAction {
        val total = maxOf(progress.totalLearningItemCount, itemLearningItemCount)
        val started = progress.startedItemCount
        val mastered = progress.masteredItemCount
        val due = progress.dueItemCount

        val (type, label, isEnabled) = when {
            total == 0 -> Triple(
                LessonStudyActionType.UNAVAILABLE,
                "No Learning Items",
                false
            )
            mastered == total -> Triple(
                LessonStudyActionType.REVIEW,
                "Review Lesson",
                true
            )
            started > 0 -> Triple(
                LessonStudyActionType.CONTINUE,
                "Continue Lesson",
                true
            )
            else -> Triple(
                LessonStudyActionType.START,
                "Start Lesson",
                true
            )
        }

        return LessonStudyAction(
            type = type,
            label = label,
            isEnabled = isEnabled,
            dueCount = due
        )
    }
}
