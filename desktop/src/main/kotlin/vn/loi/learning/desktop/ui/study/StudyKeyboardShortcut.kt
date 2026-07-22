package vn.loi.learning.desktop.ui.study

enum class StudyKeyboardKey {
    ENTER,
    SPACE,
    ONE,
    TWO,
    THREE,
    FOUR,
    Z,
    ESCAPE
}

enum class StudyKeyboardAction {
    RETRY_LOAD,
    START_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY,
    UNDO_LATEST,
    PAUSE_WORKSPACE
}

data class StudyKeyboardInput(
    val key: StudyKeyboardKey,
    val controlPressed: Boolean = false,
    val textInputFocused: Boolean = false,
    val repeated: Boolean = false
)

fun resolveStudyKeyboardAction(uiState: StudyUiState, input: StudyKeyboardInput): StudyKeyboardAction? {
    if (uiState.actionInProgress || input.textInputFocused || input.repeated) return null
    if (input.key == StudyKeyboardKey.ESCAPE && uiState.hasActiveSession) {
        return StudyKeyboardAction.PAUSE_WORKSPACE
    }
    if (input.key == StudyKeyboardKey.Z && input.controlPressed && uiState.canUndo) {
        return StudyKeyboardAction.UNDO_LATEST
    }
    return resolveStudyKeyboardAction(uiState, input.key)
}

fun resolveStudyKeyboardAction(
    uiState: StudyUiState,
    key: StudyKeyboardKey
): StudyKeyboardAction? {
    if (uiState.actionInProgress) return null
    val workspaceState = uiState.workspaceState

    if (workspaceState.allows(ReviewWorkspaceAction.Retry)) {
        return when (key) {
            StudyKeyboardKey.ENTER,
            StudyKeyboardKey.SPACE ->
                StudyKeyboardAction.RETRY_LOAD

            else -> null
        }
    }

    if (
        workspaceState.allows(ReviewWorkspaceAction.Start)
    ) {
        return when (key) {
            StudyKeyboardKey.ENTER,
            StudyKeyboardKey.SPACE ->
                StudyKeyboardAction.START_STUDY

            else -> null
        }
    }

    if (workspaceState.allows(ReviewWorkspaceAction.ShowAnswer)) {
        return when (key) {
            StudyKeyboardKey.ENTER,
            StudyKeyboardKey.SPACE ->
                StudyKeyboardAction.REVEAL_ANSWER

            else -> null
        }
    }

    if (workspaceState !is ReviewWorkspaceState.AnswerRevealed) {
        return null
    }

    return when (key) {
        StudyKeyboardKey.ONE ->
            StudyKeyboardAction.REVIEW_AGAIN

        StudyKeyboardKey.TWO ->
            StudyKeyboardAction.REVIEW_HARD

        StudyKeyboardKey.THREE ->
            StudyKeyboardAction.REVIEW_GOOD

        StudyKeyboardKey.FOUR ->
            StudyKeyboardAction.REVIEW_EASY

        else -> null
    }
}
