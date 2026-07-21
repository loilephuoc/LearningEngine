package vn.loi.learning.desktop.ui.dashboard

data class DashboardMetricAccessibility(
    val title: String,
    val value: String,
    val supportingText: String,
    val contentDescription: String
)

data class DashboardSectionAccessibility(
    val title: String,
    val description: String,
    val contentDescription: String
)

fun resolveDashboardMetricAccessibility(
    title: String,
    value: String,
    supportingText: String
): DashboardMetricAccessibility {
    val normalizedValue =
        value.trim().ifBlank {
            "Unavailable"
        }

    val normalizedSupportingText =
        supportingText.trim().ifBlank {
            "No additional details"
        }

    return DashboardMetricAccessibility(
        title = title,
        value = normalizedValue,
        supportingText = normalizedSupportingText,
        contentDescription =
            "$title: $normalizedValue. $normalizedSupportingText."
    )
}

fun resolveDashboardSectionAccessibility(
    title: String,
    description: String
): DashboardSectionAccessibility {
    val normalizedDescription =
        description.trim().ifBlank {
            "No description available"
        }

    return DashboardSectionAccessibility(
        title = title,
        description = normalizedDescription,
        contentDescription =
            "$title. $normalizedDescription."
    )
}

fun resolveDashboardHeaderContentDescription(): String =
    "Dashboard. Your learning progress at a glance."
