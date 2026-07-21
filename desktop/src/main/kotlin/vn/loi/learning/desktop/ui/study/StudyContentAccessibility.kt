package vn.loi.learning.desktop.ui.study

data class StudyContentAccessibility(
    val promptDescription: String,
    val answerDescription: String?
)

fun resolveStudyContentAccessibility(
    uiState: StudyUiState
): StudyContentAccessibility {
    val prompt =
        uiState.contentText
            .trim()
            .ifEmpty { "No prompt text available" }

    val answer =
        uiState.translationText
            .trim()
            .ifEmpty { "No answer text available" }

    return StudyContentAccessibility(
        promptDescription = "Study prompt. $prompt",
        answerDescription =
            if (uiState.canReview) {
                "Study answer. $answer"
            } else {
                null
            }
    )
}
