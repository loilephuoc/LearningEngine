package vn.loi.learning.desktop.ui.study

data class StudyLoadErrorPresentation(
    val title: String,
    val message: String,
    val guidance: String,
    val actionLabel: String,
    val shortcutHint: String
)

fun resolveStudyLoadErrorPresentation(
    uiState: StudyUiState
): StudyLoadErrorPresentation? {
    val error =
        uiState.loadError
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null

    return StudyLoadErrorPresentation(
        title = when (uiState.failureKind) {
            StudyFailureKind.REVIEW_TRANSACTION -> "Rating was not saved"
            StudyFailureKind.UNDO -> "Rating could not be undone"
            StudyFailureKind.PREPARATION -> "Session could not be prepared"
            StudyFailureKind.CONTENT -> "Learning content is unavailable"
            StudyFailureKind.SESSION_RECOVERY, null -> "Study data needs attention"
        },
        message = error,
        guidance =
            "Your last confirmed session state is preserved. Retry when ready; repeated retries do not repeat a confirmed rating.",
        actionLabel = "Retry loading",
        shortcutHint = "Enter or Space"
    )
}
