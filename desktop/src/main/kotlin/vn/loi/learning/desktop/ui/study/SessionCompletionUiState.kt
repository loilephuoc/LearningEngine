package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.ui.contentlibrary.LessonProgressUiModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonStudyAction
import vn.loi.learning.desktop.ui.contentlibrary.LessonStudyActionPolicy
import vn.loi.learning.desktop.ui.contentlibrary.PackageLearningRecommendation
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot

enum class SessionCompletionStatus {
    COMPLETED,
    PAUSED,
    STOPPED,
    ABANDONED,
    INTERRUPTED
}

/**
 * Transient presentation state cho màn hình hoàn thành/tóm tắt phiên học (Session Completion & Reflection).
 */
data class SessionCompletionUiState(
    val sessionId: String? = null,
    val installedPackageId: InstalledPackageId? = null,
    val contentId: ContentId? = null,
    val isLessonStudy: Boolean = false,
    val packageName: String? = null,
    val lessonTitle: String = "Selected lesson",
    val status: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
    val statusLabel: String = "Session Completed",
    val reviewedCount: Int = 0,
    val newItemsReviewed: Int = 0,
    val reviewItemsReviewed: Int = 0,
    val totalItems: Int = 0,
    val lessonProgress: LessonProgressUiModel? = null,
    val nextAction: LessonStudyAction? = null,
    val isRecommended: Boolean = false,
    val recommendationReason: String? = null,
    val reflectionMessage: String = "",
    val snapshot: SessionCompletionSnapshot? = null
) {
    val canContinueGeneralStudy: Boolean
        get() =
            status == SessionCompletionStatus.COMPLETED &&
                !isLessonStudy &&
                installedPackageId != null
}

/**
 * Pure policy chiếu dữ liệu từ StudyUiState và progress DTO thành SessionCompletionUiState.
 */
object SessionCompletionProjectionPolicy {

    fun create(
        studyUiState: StudyUiState,
        lessonProgress: LessonProgressUiModel? = null,
        recommendation: PackageLearningRecommendation? = null
    ): SessionCompletionUiState {
        val status = when {
            studyUiState.sessionCompleted || (studyUiState.sessionProgress?.isCompleted == true) -> SessionCompletionStatus.COMPLETED
            studyUiState.hasActiveSession -> SessionCompletionStatus.PAUSED
            else -> SessionCompletionStatus.STOPPED
        }

        val statusLabel = when (status) {
            SessionCompletionStatus.COMPLETED -> "Session Completed"
            SessionCompletionStatus.PAUSED -> "Session Paused"
            SessionCompletionStatus.STOPPED -> "Session Stopped"
            SessionCompletionStatus.ABANDONED -> "Session Abandoned"
            SessionCompletionStatus.INTERRUPTED -> "Session Interrupted"
        }

        val nextAction = lessonProgress?.let {
            LessonStudyActionPolicy.evaluate(it, it.totalLearningItemCount)
        }

        val contentId = studyUiState.activeContentId

        val isRec = recommendation != null && contentId != null && recommendation.contentId == contentId
        val recReason = if (isRec) recommendation.reasonText else null

        val reflectionMsg = buildString {
            append("You reviewed ${studyUiState.reviewedCount} learning Content")
            append(".")
            if (lessonProgress != null) {
                if (lessonProgress.dueItemCount > 0) {
                    append(" ${lessonProgress.dueItemCount} item")
                    if (lessonProgress.dueItemCount != 1) append("s")
                    append(" remain due.")
                }
                if (lessonProgress.completionPercent > 0) {
                    append(" This lesson is ${lessonProgress.completionPercent}% complete.")
                }
            }
        }

        return SessionCompletionUiState(
            sessionId = null,
            installedPackageId = studyUiState.activeInstalledPackageId,
            contentId = contentId,
            isLessonStudy = studyUiState.isLessonStudy,
            packageName = null,
            lessonTitle = studyUiState.studyTitle,
            status = status,
            statusLabel = statusLabel,
            reviewedCount = studyUiState.reviewedCount,
            newItemsReviewed = studyUiState.newItemsReviewed,
            reviewItemsReviewed = studyUiState.reviewItemsReviewed,
            totalItems = studyUiState.totalItems,
            lessonProgress = lessonProgress,
            nextAction = nextAction,
            isRecommended = isRec,
            recommendationReason = recReason,
            reflectionMessage = reflectionMsg,
            snapshot = studyUiState.sessionCompletion
        )
    }
}
