package vn.loi.learning.desktop.ui.study

data class StudySchedulerFeedbackAccessibility(
    val announcement: String,
    val conciseSummary: String
)

fun resolveStudySchedulerFeedbackAccessibility(
    feedback: StudySchedulerFeedback
): StudySchedulerFeedbackAccessibility {
    val conciseSummary =
        "${feedback.rating} rating; " +
            "${feedback.stageTransition}; " +
            "next interval ${feedback.scheduledInterval}; " +
            "next review ${feedback.nextReviewAt}"

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
        conciseSummary = conciseSummary
    )
}
