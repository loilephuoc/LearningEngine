package vn.loi.learning.desktop.ui.settings

data class SettingsPropertyAccessibility(
    val label: String,
    val value: String,
    val contentDescription: String
)

fun resolveSettingsPropertyAccessibility(
    label: String,
    value: String
): SettingsPropertyAccessibility {
    val normalizedValue =
        value.trim().ifBlank {
            "Unavailable"
        }

    return SettingsPropertyAccessibility(
        label = label,
        value = normalizedValue,
        contentDescription =
            "$label: $normalizedValue."
    )
}

fun resolveSettingsSectionContentDescription(
    title: String,
    properties: List<Pair<String, String>>
): String =
    properties.joinToString(
        separator = " ",
        prefix = "$title settings. "
    ) { (label, value) ->
        resolveSettingsPropertyAccessibility(
            label = label,
            value = value
        ).contentDescription
    }
