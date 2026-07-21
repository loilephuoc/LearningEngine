package vn.loi.learning.desktop.ui.study

data class StudySessionSummaryAccessibility(
    val contentDescription: String
)

fun resolveStudySessionSummaryAccessibility(
    uiState: StudyUiState
): StudySessionSummaryAccessibility {
    val itemLabel =
        if (uiState.reviewedCount == 1) {
            "learning item"
        } else {
            "learning items"
        }

    val lessonProgress =
        if (
            uiState.isLessonStudy &&
            uiState.hasKnownTotal
        ) {
            " Lesson progress ${uiState.reviewedCount} of " +
                "${uiState.totalItems} items completed."
        } else {
            ""
        }

    return StudySessionSummaryAccessibility(
        contentDescription =
            buildString {
                append("Study session completed. ")
                append(uiState.studyTitle.trim())
                append(". ")
                append(uiState.reviewedCount)
                append(" ")
                append(itemLabel)
                append(" reviewed. ")
                append("New items ")
                append(uiState.newItemsReviewed)
                append(". Scheduled review items ")
                append(uiState.reviewItemsReviewed)
                append(".")
                append(lessonProgress)
                append(" Start another study session with Enter.")
            }
    )
}
