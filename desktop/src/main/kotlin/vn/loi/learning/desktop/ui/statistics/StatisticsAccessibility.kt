package vn.loi.learning.desktop.ui.statistics

data class StatisticAccessibility(
    val title: String,
    val value: String,
    val contentDescription: String
)

fun resolveStatisticAccessibility(
    title: String,
    value: String
): StatisticAccessibility {
    val normalizedValue =
        value.trim().ifBlank {
            "Unavailable"
        }

    val spokenValue =
        when (normalizedValue) {
            "--" -> "Unavailable"
            else -> normalizedValue
        }

    return StatisticAccessibility(
        title = title,
        value = normalizedValue,
        contentDescription =
            "$title: $spokenValue."
    )
}

fun resolveStatisticsScreenContentDescription(
    uiState: StatisticsUiState
): String =
    listOf(
        resolveStatisticAccessibility(
            "Total reviews",
            uiState.totalReviews
        ),
        resolveStatisticAccessibility(
            "Successful reviews",
            uiState.successfulReviews
        ),
        resolveStatisticAccessibility(
            "Success rate",
            uiState.successRate
        ),
        resolveStatisticAccessibility(
            "Again",
            uiState.againCount
        ),
        resolveStatisticAccessibility(
            "Good",
            uiState.goodCount
        ),
        resolveStatisticAccessibility(
            "Average response time",
            uiState.averageResponseTime
        )
    ).joinToString(
        separator = " ",
        prefix = "Statistics summary. "
    ) {
        it.contentDescription
    }
