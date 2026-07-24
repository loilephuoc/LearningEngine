package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.domain.content.model.ContentId

/**
 * Loại lý do đề xuất bài học tiếp theo.
 */
enum class RecommendationReasonType {
    DUE_NOW,
    CONTINUE_IN_PROGRESS,
    START_NEW,
    REVIEW_COMPLETED,
    NONE
}

/**
 * Presentation model cho bài học được hệ thống đề xuất trong Lesson Browser.
 */
data class PackageLearningRecommendation(
    val contentId: ContentId,
    val lessonTitle: String,
    val reasonType: RecommendationReasonType,
    val actionLabel: String,
    val reasonText: String,
    val totalItemCount: Int = 0,
    val masteredItemCount: Int = 0,
    val dueItemCount: Int = 0
)

/**
 * Pure policy để xác định bài học tiếp theo phù hợp nhất trong một package.
 *
 * Priority: DUE_NOW -> CONTINUE_IN_PROGRESS -> START_NEW -> REVIEW_COMPLETED -> NONE
 * Tie-breaker: Thứ tự bài học canonical ban đầu trong package.
 */
object PackageLearningRecommendationPolicy {

    fun evaluate(lessons: List<LessonBrowserItem>): PackageLearningRecommendation? {
        val validLessons = lessons.filter { getItemTotalCount(it) > 0 }
        if (validLessons.isEmpty()) return null

        // Priority 1: DUE_NOW (dueItemCount > 0)
        val dueLesson = validLessons.firstOrNull { it.progress.dueItemCount > 0 }
        if (dueLesson != null) {
            val dueCount = dueLesson.progress.dueItemCount
            val reasonText = if (dueCount == 1) "1 item is due now" else "$dueCount item(s) are due now"
            return PackageLearningRecommendation(
                contentId = ContentId(dueLesson.id),
                lessonTitle = dueLesson.title,
                reasonType = RecommendationReasonType.DUE_NOW,
                actionLabel = "Review due items",
                reasonText = reasonText,
                totalItemCount = getItemTotalCount(dueLesson),
                masteredItemCount = dueLesson.progress.masteredItemCount,
                dueItemCount = dueCount
            )
        }

        // Priority 2: CONTINUE_IN_PROGRESS (startedItemCount > 0 && masteredItemCount < total)
        val continueLesson = validLessons.firstOrNull {
            val total = getItemTotalCount(it)
            it.progress.startedItemCount > 0 && it.progress.masteredItemCount < total
        }
        if (continueLesson != null) {
            val total = getItemTotalCount(continueLesson)
            val mastered = continueLesson.progress.masteredItemCount
            return PackageLearningRecommendation(
                contentId = ContentId(continueLesson.id),
                lessonTitle = continueLesson.title,
                reasonType = RecommendationReasonType.CONTINUE_IN_PROGRESS,
                actionLabel = "Continue this lesson",
                reasonText = "$mastered of $total items mastered",
                totalItemCount = total,
                masteredItemCount = mastered,
                dueItemCount = continueLesson.progress.dueItemCount
            )
        }

        // Priority 3: START_NEW (startedItemCount == 0 && masteredItemCount == 0)
        val startLesson = validLessons.firstOrNull {
            it.progress.startedItemCount == 0 && it.progress.masteredItemCount == 0
        }
        if (startLesson != null) {
            val total = getItemTotalCount(startLesson)
            return PackageLearningRecommendation(
                contentId = ContentId(startLesson.id),
                lessonTitle = startLesson.title,
                reasonType = RecommendationReasonType.START_NEW,
                actionLabel = "Start this lesson",
                reasonText = "This is the next unstarted lesson",
                totalItemCount = total,
                masteredItemCount = 0,
                dueItemCount = 0
            )
        }

        // Priority 4: REVIEW_COMPLETED (masteredItemCount == total)
        val reviewLesson = validLessons.firstOrNull {
            val total = getItemTotalCount(it)
            it.progress.masteredItemCount == total
        }
        if (reviewLesson != null) {
            val total = getItemTotalCount(reviewLesson)
            return PackageLearningRecommendation(
                contentId = ContentId(reviewLesson.id),
                lessonTitle = reviewLesson.title,
                reasonType = RecommendationReasonType.REVIEW_COMPLETED,
                actionLabel = "Review this lesson",
                reasonText = "All items are mastered",
                totalItemCount = total,
                masteredItemCount = total,
                dueItemCount = reviewLesson.progress.dueItemCount
            )
        }

        return null
    }

    private fun getItemTotalCount(item: LessonBrowserItem): Int {
        return maxOf(item.progress.totalLearningItemCount, item.learningItemCount)
    }
}
