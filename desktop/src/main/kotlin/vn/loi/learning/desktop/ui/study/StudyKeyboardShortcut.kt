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
    if (uiState.loadError != null) {
        return null
    }

    if (
        !uiState.hasActiveSession ||
        uiState.sessionCompleted
    ) {
        return when (key) {
            StudyKeyboardKey.ENTER,
            StudyKeyboardKey.SPACE ->
                StudyKeyboardAction.START_STUDY

            else -> null
        }
    }

    if (uiState.canRevealAnswer) {
        return when (key) {
            StudyKeyboardKey.ENTER,
            StudyKeyboardKey.SPACE ->
                StudyKeyboardAction.REVEAL_ANSWER

            else -> null
        }
    }

    if (!uiState.canReview) {
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
