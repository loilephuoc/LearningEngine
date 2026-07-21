package vn.loi.learning.desktop.ui.study

data class StudyRatingGuidance(
    val control: StudyActionControl,
    val label: String,
    val description: String
)

fun resolveStudyRatingGuidance(): List<StudyRatingGuidance> =
    listOf(
        StudyRatingGuidance(
            control = StudyActionControl.REVIEW_AGAIN,
            label = "Again",
            description = "I did not remember this yet; show it again soon."
        ),
        StudyRatingGuidance(
            control = StudyActionControl.REVIEW_HARD,
            label = "Hard",
            description = "I remembered with difficulty; schedule a shorter interval."
        ),
        StudyRatingGuidance(
            control = StudyActionControl.REVIEW_GOOD,
            label = "Good",
            description = "I remembered correctly; use the normal review interval."
        ),
        StudyRatingGuidance(
            control = StudyActionControl.REVIEW_EASY,
            label = "Easy",
            description = "I remembered immediately; schedule a longer interval."
        )
    )

fun resolveStudyRatingGuidanceDescription(): String =
    resolveStudyRatingGuidance()
        .joinToString(separator = " ") { guidance ->
            val shortcut =
                resolveStudyActionAccessibility(guidance.control)
                    .shortcutHint
            "$shortcut ${guidance.label}: ${guidance.description}"
        }
