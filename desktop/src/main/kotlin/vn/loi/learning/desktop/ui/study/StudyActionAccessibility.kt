package vn.loi.learning.desktop.ui.study

enum class StudyActionControl {
    RETRY_LOAD,
    START_STUDY,
    START_GENERAL_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY,
    UNDO_LATEST,
    PAUSE_WORKSPACE
}

data class StudyActionAccessibility(
    val visibleLabel: String,
    val shortcutHint: String,
    val contentDescription: String
)

fun resolveStudyActionAccessibility(
    control: StudyActionControl,
    strings: StudyWorkspaceStrings = StudyWorkspaceStrings.ENGLISH
): StudyActionAccessibility {
    val shortcut = when (control) {
        StudyActionControl.RETRY_LOAD -> "Enter or Space"
        StudyActionControl.START_STUDY, StudyActionControl.START_GENERAL_STUDY -> "Enter or Space"
        StudyActionControl.REVEAL_ANSWER -> "Enter or Space"
        StudyActionControl.REVIEW_AGAIN -> "1"
        StudyActionControl.REVIEW_HARD -> "2"
        StudyActionControl.REVIEW_GOOD -> "3"
        StudyActionControl.REVIEW_EASY -> "4"
        StudyActionControl.UNDO_LATEST -> "Ctrl+Z"
        StudyActionControl.PAUSE_WORKSPACE -> "Escape"
    }
    val label = strings.label(control)
    return StudyActionAccessibility(label, shortcut, strings.shortcutTemplate(label, shortcut))
}
