package vn.loi.learning.desktop.ui.dashboard

data class DashboardVisualizationAccessibility(
    val title: String,
    val contentDescription: String
)

data class DashboardChartEmptyAccessibility(
    val title: String,
    val description: String,
    val contentDescription: String
)

data class DashboardRetentionAccessibility(
    val label: String,
    val contentDescription: String
)

fun resolveDashboardVisualizationAccessibility(
    title: String,
    hasData: Boolean
): DashboardVisualizationAccessibility {
    val normalizedTitle =
        title.trim().ifBlank {
            "Untitled visualization"
        }

    return DashboardVisualizationAccessibility(
        title = normalizedTitle,
        contentDescription =
            if (hasData) {
                "$normalizedTitle visualization."
            } else {
                "$normalizedTitle visualization. No data available."
            }
    )
}

fun resolveDashboardChartEmptyAccessibility(
    title: String,
    description: String
): DashboardChartEmptyAccessibility {
    val normalizedTitle =
        title.trim().ifBlank {
            "No data available"
        }

    val normalizedDescription =
        description.trim().ifBlank {
            "Complete more learning activity to generate this visualization."
        }

    return DashboardChartEmptyAccessibility(
        title = normalizedTitle,
        description = normalizedDescription,
        contentDescription =
            "$normalizedTitle. $normalizedDescription"
    )
}

fun resolveDashboardRetentionAccessibility(
    retentionLabel: String,
    retentionValue: Float
): DashboardRetentionAccessibility {
    val normalizedValue =
        retentionValue.coerceIn(
            minimumValue = 0f,
            maximumValue = 1f
        )

    val percentage =
        (normalizedValue * 100f)
            .toInt()
            .coerceIn(
                minimumValue = 0,
                maximumValue = 100
            )

    val normalizedLabel =
        retentionLabel.trim().ifBlank {
            "$percentage%"
        }

    return DashboardRetentionAccessibility(
        label = normalizedLabel,
        contentDescription =
            "Average retention: $percentage percent."
    )
}
