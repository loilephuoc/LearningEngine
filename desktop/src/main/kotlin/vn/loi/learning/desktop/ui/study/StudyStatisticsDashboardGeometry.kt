package vn.loi.learning.desktop.ui.study

internal data class StudyStatisticsDashboardGeometry(
    val rowCount: Int,
    val metricOrder: List<StudyHeaderMetricType>,
    val measuredHeightDp: Int
)

internal fun resolveStudyStatisticsDashboardGeometry(
    layout: StudyStatisticsLayoutPresentation,
    metrics: List<StudyHeaderMetricPresentation>,
    measuredMetricHeightsDp: List<Int>
): StudyStatisticsDashboardGeometry {
    require(metrics.isNotEmpty())
    require(metrics.size == measuredMetricHeightsDp.size)
    require(measuredMetricHeightsDp.all { it > 0 })

    val rowHeights =
        measuredMetricHeightsDp
            .chunked(layout.metricsPerRow)
            .map { row -> row.max() }
    return StudyStatisticsDashboardGeometry(
        rowCount = rowHeights.size,
        metricOrder = metrics.map { it.type },
        measuredHeightDp =
            layout.surfaceContentPaddingDp * 2 +
                rowHeights.sum() +
                layout.rowGapDp * (rowHeights.size - 1).coerceAtLeast(0)
    )
}
