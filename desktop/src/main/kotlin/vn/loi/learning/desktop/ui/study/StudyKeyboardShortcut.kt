package vn.loi.learning.desktop.ui.study

enum class StudyKeyboardKey {
    ENTER,
    SPACE,
    ONE,
    TWO,
    THREE,
    FOUR
}

enum class StudyKeyboardAction {
    RETRY_LOAD,
    START_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY
}

fun resolveStudyKeyboardAction(
    uiState: StudyUiState,
    key: StudyKeyboardKey
): StudyKeyboardAction? {
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
