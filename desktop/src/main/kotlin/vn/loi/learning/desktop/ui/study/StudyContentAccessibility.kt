package vn.loi.learning.desktop.ui.study

data class StudyContentAccessibility(
    val promptDescription: String,
    val answerDescription: String?
)

fun resolveStudyContentAccessibility(
    uiState: StudyUiState
): StudyContentAccessibility {
    val promptText = if (uiState.domainContent != null) {
        if (uiState.canReview) {
            uiState.domainContent.text.primaryText.ifBlank { uiState.contentText }
        } else {
            uiState.domainContent.text.translatedText?.takeIf { it.isNotBlank() } ?: "Study prompt cue"
        }
    } else {
        uiState.contentText
    }.trim().ifEmpty { "No prompt text available" }

    val answerText = (uiState.translationText.takeIf { it.isNotBlank() }
        ?: uiState.domainContent?.text?.translatedText.orEmpty())
        .trim().ifEmpty { "No answer text available" }

    return StudyContentAccessibility(
        promptDescription = "Study prompt. $promptText",
        answerDescription = if (uiState.canReview) "Study answer. $answerText" else null
    )
}
