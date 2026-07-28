package vn.loi.learning.desktop.ui.study

data class StudyHeaderMetricPresentation(
    val label: String,
    val value: String
)

data class StudyHeaderStatisticsPresentation(
    val primary: List<StudyHeaderMetricPresentation>,
    val ratings: List<StudyHeaderMetricPresentation>,
    val accessibilityDescription: String
)

internal fun resolveStudyHeaderStatisticsPresentation(
    state: StudyHeaderStatisticsState,
    strings: StudyStatisticsStrings
): StudyHeaderStatisticsPresentation? {
    val statistics = when (state) {
        is StudyHeaderStatisticsState.Available -> state.value
        is StudyHeaderStatisticsState.Unavailable -> state.lastKnownGood
        StudyHeaderStatisticsState.Loading -> null
    } ?: return null
    val primary = listOf(
        StudyHeaderMetricPresentation(strings.total, statistics.total.toString()),
        StudyHeaderMetricPresentation(
            strings.new,
            "${statistics.newCompleted}/${statistics.newConfiguredTarget}"
        ),
        StudyHeaderMetricPresentation(
            strings.review,
            "${statistics.reviewRemaining}/${statistics.reviewConfiguredTarget}"
        ),
        StudyHeaderMetricPresentation(strings.due, statistics.dueCount.toString())
    )
    val ratings = listOf(
        StudyHeaderMetricPresentation(strings.again, statistics.againCount.toString()),
        StudyHeaderMetricPresentation(strings.hard, statistics.hardCount.toString()),
        StudyHeaderMetricPresentation(strings.good, statistics.goodCount.toString()),
        StudyHeaderMetricPresentation(strings.easy, statistics.easyCount.toString())
    )
    return StudyHeaderStatisticsPresentation(
        primary = primary,
        ratings = ratings,
        accessibilityDescription = (primary + ratings).joinToString(", ") {
            "${it.label} ${it.value}"
        } + " ${strings.itemNoun}"
    )
}
