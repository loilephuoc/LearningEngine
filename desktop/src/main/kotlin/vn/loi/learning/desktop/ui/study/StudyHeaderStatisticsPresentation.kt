package vn.loi.learning.desktop.ui.study

enum class StudyHeaderMetricType {
    TOTAL,
    NEW,
    REVIEW,
    DUE,
    AGAIN,
    HARD,
    GOOD,
    EASY
}

enum class StudyMetricEmphasis {
    NEUTRAL,
    ACTIVE,
    MUTED
}

data class StudyHeaderMetricPresentation(
    val type: StudyHeaderMetricType,
    val label: String,
    val primaryValue: String,
    val secondaryValue: String? = null,
    val subtitle: String? = null,
    val emphasis: StudyMetricEmphasis,
    val accessibilityText: String
)

data class StudyHeaderStatisticsPresentation(
    val metrics: List<StudyHeaderMetricPresentation>,
    val accessibilityDescription: String
)

data class StudyStatisticsLayoutPresentation(
    val metricsPerRow: Int,
    val showSubtitles: Boolean = false
)

internal fun resolveStudyStatisticsLayout(
    viewportClass: StudyViewportClass
): StudyStatisticsLayoutPresentation = when (viewportClass) {
    StudyViewportClass.WIDE,
    StudyViewportClass.STANDARD -> StudyStatisticsLayoutPresentation(
        metricsPerRow = 8,
        showSubtitles = false
    )
    StudyViewportClass.COMPACT -> StudyStatisticsLayoutPresentation(
        metricsPerRow = 4,
        showSubtitles = false
    )
}

internal fun resolveStudyHeaderStatisticsPresentation(
    state: StudyHeaderStatisticsState,
    strings: StudyStatisticsStrings
): StudyHeaderStatisticsPresentation? {
    val statistics = when (state) {
        is StudyHeaderStatisticsState.Available -> state.value
        is StudyHeaderStatisticsState.Unavailable -> state.lastKnownGood
        StudyHeaderStatisticsState.Loading -> null
    } ?: return null

    val metrics = listOf(
        StudyHeaderMetricPresentation(
            type = StudyHeaderMetricType.TOTAL,
            label = strings.total,
            primaryValue = statistics.total.toString(),
            subtitle = strings.learnedDistribution,
            emphasis = StudyMetricEmphasis.NEUTRAL,
            accessibilityText = strings.totalAccessibility(statistics.total)
        ),
        fractionMetric(
            type = StudyHeaderMetricType.NEW,
            label = strings.new,
            numerator = statistics.newCompleted,
            denominator = statistics.newConfiguredTarget,
            subtitle = strings.sessionProgress,
            accessibilityText = strings.newAccessibility(
                statistics.newCompleted,
                statistics.newConfiguredTarget
            )
        ),
        fractionMetric(
            type = StudyHeaderMetricType.REVIEW,
            label = strings.review,
            numerator = statistics.reviewRemaining,
            denominator = statistics.reviewConfiguredTarget,
            subtitle = strings.remainingQueue,
            accessibilityText = strings.reviewAccessibility(
                statistics.reviewRemaining,
                statistics.reviewConfiguredTarget
            )
        ),
        countMetric(
            StudyHeaderMetricType.DUE,
            strings.due,
            statistics.dueCount,
            strings.dueNow,
            strings.dueAccessibility(statistics.dueCount)
        ),
        countMetric(
            StudyHeaderMetricType.AGAIN,
            strings.again,
            statistics.againCount,
            accessibilityText = strings.ratingAccessibility(strings.again, statistics.againCount)
        ),
        countMetric(
            StudyHeaderMetricType.HARD,
            strings.hard,
            statistics.hardCount,
            accessibilityText = strings.ratingAccessibility(strings.hard, statistics.hardCount)
        ),
        countMetric(
            StudyHeaderMetricType.GOOD,
            strings.good,
            statistics.goodCount,
            accessibilityText = strings.ratingAccessibility(strings.good, statistics.goodCount)
        ),
        countMetric(
            StudyHeaderMetricType.EASY,
            strings.easy,
            statistics.easyCount,
            accessibilityText = strings.ratingAccessibility(strings.easy, statistics.easyCount)
        )
    )
    return StudyHeaderStatisticsPresentation(
        metrics = metrics,
        accessibilityDescription = metrics.joinToString(" ") { it.accessibilityText }
    )
}

private fun fractionMetric(
    type: StudyHeaderMetricType,
    label: String,
    numerator: Int,
    denominator: Int,
    subtitle: String,
    accessibilityText: String
) = StudyHeaderMetricPresentation(
    type = type,
    label = label,
    primaryValue = numerator.toString(),
    secondaryValue = "/$denominator",
    subtitle = subtitle,
    emphasis = if (numerator == 0) StudyMetricEmphasis.MUTED else StudyMetricEmphasis.ACTIVE,
    accessibilityText = accessibilityText
)

private fun countMetric(
    type: StudyHeaderMetricType,
    label: String,
    value: Int,
    subtitle: String? = null,
    accessibilityText: String
) = StudyHeaderMetricPresentation(
    type = type,
    label = label,
    primaryValue = value.toString(),
    subtitle = subtitle,
    emphasis = if (value == 0) StudyMetricEmphasis.MUTED else StudyMetricEmphasis.ACTIVE,
    accessibilityText = accessibilityText
)
