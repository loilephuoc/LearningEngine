package vn.loi.learning.desktop.ui.reviewhistory

data class ReviewHistoryEmptyAccessibility(
    val title: String,
    val description: String,
    val contentDescription: String
)

fun formatReviewHistoryCount(
    count: Int
): String =
    when (count) {
        1 -> "1 review"
        else -> "$count reviews"
    }

fun resolveReviewHistoryEmptyAccessibility():
    ReviewHistoryEmptyAccessibility =
    ReviewHistoryEmptyAccessibility(
        title = "No review history",
        description =
            "Complete a study review to create the first event.",
        contentDescription =
            "Review History is empty. " +
                "Complete a study review to create the first event."
    )

fun resolveReviewHistoryItemContentDescription(
    item: ReviewHistoryItemUi
): String =
    buildString {
        append("Review rated ")
        append(item.rating)
        append(". Reviewed ")
        append(item.reviewedAt)
        append(". Response time ")
        append(item.responseTime)
        append(". Stability ")
        append(item.stability)
        append(". Difficulty ")
        append(item.difficulty)
        append(".")
    }
