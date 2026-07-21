package vn.loi.learning.desktop.ui.study

data class StudyFocusTransitionKey(
    val phase: StudyFocusPhase,
    val reviewedCount: Int,
    val currentItemPosition: Int,
    val loadError: String?
)

enum class StudyFocusPhase {
    IDLE,
    QUESTION,
    ANSWER_REVEALED,
    COMPLETED,
    ERROR,
    ACTIVE
}

fun resolveStudyFocusTransitionKey(
    uiState: StudyUiState
): StudyFocusTransitionKey {
    val normalizedError =
        uiState.loadError
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    val phase =
        when {
            normalizedError != null ->
                StudyFocusPhase.ERROR

            uiState.sessionCompleted ->
                StudyFocusPhase.COMPLETED

            !uiState.hasActiveSession ->
                StudyFocusPhase.IDLE

            uiState.canRevealAnswer ->
                StudyFocusPhase.QUESTION

            uiState.canReview ->
                StudyFocusPhase.ANSWER_REVEALED

            else ->
                StudyFocusPhase.ACTIVE
        }

    return StudyFocusTransitionKey(
        phase = phase,
        reviewedCount = uiState.reviewedCount,
        currentItemPosition = uiState.currentItemPosition,
        loadError = normalizedError
    )
}
