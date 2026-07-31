package vn.loi.learning.desktop.ui.study

internal data class StudyAnswerScrollTransitionKey(
    val currentLearningItemId: String?
)

internal fun resolveTypingAnswerScrollTransitionKey(
    currentLearningItemId: String?,
    typingAnswerSideActive: Boolean
): StudyAnswerScrollTransitionKey? =
    if (typingAnswerSideActive) {
        currentLearningItemId?.let(::StudyAnswerScrollTransitionKey)
    } else {
        null
    }
