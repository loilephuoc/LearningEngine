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
        title = "Study data needs attention",
        message = error,
        guidance =
            "Correct or restore the persisted study data, then retry loading without restarting the application.",
        actionLabel = "Retry loading",
        shortcutHint = "Enter or Space"
    )
}
