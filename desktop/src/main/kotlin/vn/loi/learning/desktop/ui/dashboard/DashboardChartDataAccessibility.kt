package vn.loi.learning.desktop.ui.dashboard

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DashboardChartValueAccessibility(
    val label: String,
    val value: String,
    val contentDescription: String
)

data class DashboardHeatmapDayAccessibility(
    val contentDescription: String
)

fun resolveDashboardChartValueAccessibility(
    item: DashboardChartValue,
    unit: String
): DashboardChartValueAccessibility {
    val normalizedLabel =
        item.label.trim().ifBlank {
            "Unlabeled value"
        }

    val normalizedUnit =
        unit.trim().ifBlank {
            "items"
        }

    return DashboardChartValueAccessibility(
        label = normalizedLabel,
        value = item.value.toString(),
        contentDescription =
            "$normalizedLabel: ${item.value} $normalizedUnit."
    )
}

fun resolveDashboardMemoryStageAccessibility(
    item: DashboardChartValue,
    total: Int
): DashboardChartValueAccessibility {
    val normalizedLabel =
        item.label.trim().ifBlank {
            "Unlabeled stage"
        }

    val percentage =
        if (total <= 0) {
            0
        } else {
            (
                item.value.toDouble() /
                    total.toDouble() *
                    100.0
            ).toInt()
        }

    return DashboardChartValueAccessibility(
        label = normalizedLabel,
        value = "${item.value} · $percentage%",
        contentDescription =
            "$normalizedLabel: ${item.value} cards, $percentage percent."
    )
}

fun resolveDashboardHeatmapDayAccessibility(
    day: DashboardHeatmapDay
): DashboardHeatmapDayAccessibility {
    val date =
        LocalDate
            .ofEpochDay(day.epochDay)
            .format(
                DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d, uuuu",
                    Locale.US
                )
            )

    val activityDescription =
        when {
            day.isFuture ->
                "Future date."

            day.reviewCount == 0 ->
                "No reviews."

            day.reviewCount == 1 ->
                "1 review."

            else ->
                "${day.reviewCount} reviews."
        }

    return DashboardHeatmapDayAccessibility(
        contentDescription =
            "$date. $activityDescription"
    )
}
