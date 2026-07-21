package vn.loi.learning.desktop.ui.study

data class StudySchedulerFeedbackAccessibility(
    val announcement: String,
    val conciseSummary: String,
    val detailsDescription: String
)

fun resolveStudySchedulerFeedbackAccessibility(
    feedback: StudySchedulerFeedback
): StudySchedulerFeedbackAccessibility {
    val conciseSummary =
        "${feedback.rating} rating; " +
            "${feedback.stageTransition}; " +
            "next interval ${feedback.scheduledInterval}; " +
            "next review ${feedback.nextReviewAt}"

    val detailsDescription =
        buildString {
            append("Scheduler feedback. ")
            append(conciseSummary)
            append(". Difficulty ")
            append(feedback.difficultyBefore)
            append(" to ")
            append(feedback.difficultyAfter)
            append(". Stability ")
            append(feedback.stabilityBefore)
            append(" to ")
            append(feedback.stabilityAfter)
            append(". Review count ")
            append(feedback.reviewCount)
            append(". Lapse count ")
            append(feedback.lapseCount)
            append(".")
        }

    val announcement =
        buildString {
            append("Latest review saved. ")
            append(conciseSummary)
            append(". Review count ")
            append(feedback.reviewCount)
            append("; lapse count ")
            append(feedback.lapseCount)
            append(".")
        }

    return StudySchedulerFeedbackAccessibility(
        announcement = announcement,
        conciseSummary = conciseSummary,
        detailsDescription = detailsDescription
    )
}
