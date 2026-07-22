package vn.loi.learning.desktop.ui.study

data class StudyUiState(
    val hasActiveSession: Boolean = false,
    val sessionStarted: Boolean = false,
    val studyTitle: String = "All learning items",
    val isLessonStudy: Boolean = false,
    val contentText: String = "--",
    val translationText: String = "--",
    val canRevealAnswer: Boolean = false,
    val canReview: Boolean = false,
    val reviewedCount: Int = 0,
    val newItemsReviewed: Int = 0,
    val reviewItemsReviewed: Int = 0,
    val totalItems: Int = 0,
    val currentItemPosition: Int = 0,
    val sessionCompleted: Boolean = false,
    val loadError: String? = null,
    val schedulerFeedback:
    StudySchedulerFeedback? = null,
    val message: String = "Press Start Study",
    val workspaceState: ReviewWorkspaceState =
        ReviewWorkspaceState.projectLegacy(
            hasActiveSession = hasActiveSession,
            canRevealAnswer = canRevealAnswer,
            canReview = canReview,
            sessionCompleted = sessionCompleted,
            hasLoadError = loadError != null
        )
) {

    val hasKnownTotal: Boolean
        get() =
            totalItems > 0

    val progress: Float
        get() {
            if (!hasKnownTotal) {
                return 0f
            }

            if (sessionCompleted) {
                return 1f
            }

            return (
                    reviewedCount.toFloat() /
                            totalItems.toFloat()
                    ).coerceIn(
                    minimumValue = 0f,
                    maximumValue = 1f
                )
        }

    val progressLabel: String
        get() {
            if (!hasKnownTotal) {
                return reviewedCount.toString()
            }

            if (sessionCompleted) {
                return "$totalItems of $totalItems"
            }

            return "$currentItemPosition of $totalItems"
        }
}
