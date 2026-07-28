package vn.loi.learning.desktop.ui.study

data class StudyHeaderMetricPresentation(
    val label: String,
    val value: Int
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
        StudyHeaderMetricPresentation(strings.total, statistics.total),
        StudyHeaderMetricPresentation(strings.new, statistics.newCount),
        StudyHeaderMetricPresentation(strings.review, statistics.reviewCount),
        StudyHeaderMetricPresentation(strings.due, statistics.dueCount)
    )
    val ratings = listOf(
        StudyHeaderMetricPresentation(strings.again, statistics.againCount),
        StudyHeaderMetricPresentation(strings.hard, statistics.hardCount),
        StudyHeaderMetricPresentation(strings.good, statistics.goodCount),
        StudyHeaderMetricPresentation(strings.easy, statistics.easyCount)
    )
    return StudyHeaderStatisticsPresentation(
        primary = primary,
        ratings = ratings,
        accessibilityDescription = (primary + ratings).joinToString(", ") {
            "${it.label} ${it.value}"
        } + " ${strings.itemNoun}"
    )
}
