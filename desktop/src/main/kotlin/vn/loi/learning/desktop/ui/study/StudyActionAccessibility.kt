package vn.loi.learning.desktop.ui.study

enum class StudyActionControl {
    RETRY_LOAD,
    START_STUDY,
    START_GENERAL_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY
}

data class StudyActionAccessibility(
    val visibleLabel: String,
    val shortcutHint: String,
    val contentDescription: String
)

fun resolveStudyActionAccessibility(
    control: StudyActionControl
): StudyActionAccessibility =
    when (control) {
        StudyActionControl.RETRY_LOAD ->
            StudyActionAccessibility(
                visibleLabel = "Retry Load",
                shortcutHint = "Enter or Space",
                contentDescription =
                    "Retry loading the study session. Keyboard shortcut: Enter or Space."
            )

        StudyActionControl.START_STUDY ->
            StudyActionAccessibility(
                visibleLabel = "Start Study",
                shortcutHint = "Enter",
                contentDescription =
                    "Start the study session. Keyboard shortcut: Enter or Space."
            )

        StudyActionControl.START_GENERAL_STUDY ->
            StudyActionAccessibility(
                visibleLabel = "Start General Study",
                shortcutHint = "Enter",
                contentDescription =
                    "Start a general study session. Keyboard shortcut: Enter or Space."
            )

        StudyActionControl.REVEAL_ANSWER ->
            StudyActionAccessibility(
                visibleLabel = "Reveal Answer",
                shortcutHint = "Space",
                contentDescription =
                    "Reveal the answer for the current learning item. Keyboard shortcut: Enter or Space."
            )

        StudyActionControl.REVIEW_AGAIN ->
            reviewAction("Again", "1")

        StudyActionControl.REVIEW_HARD ->
            reviewAction("Hard", "2")

        StudyActionControl.REVIEW_GOOD ->
            reviewAction("Good", "3")

        StudyActionControl.REVIEW_EASY ->
            reviewAction("Easy", "4")
    }

private fun reviewAction(
    rating: String,
    shortcut: String
): StudyActionAccessibility =
    StudyActionAccessibility(
        visibleLabel = rating,
        shortcutHint = shortcut,
        contentDescription =
            "Grade the current learning item $rating. Keyboard shortcut: $shortcut."
    )
