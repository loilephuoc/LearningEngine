package vn.loi.learning.desktop.ui.settings

import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics

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

fun resolveRuntimeDiagnosticProperties(
    diagnostics: DesktopRuntimeDiagnostics
): List<Pair<String, String>> =
    listOf(
        "Version" to diagnostics.version,
        "Build channel" to diagnostics.buildChannel,
        "Build revision" to diagnostics.buildRevision,
        "Build number" to diagnostics.buildNumber,
        "Operating system" to diagnostics.operatingSystem,
        "Architecture" to diagnostics.architecture,
        "Java runtime" to diagnostics.javaRuntime,
        "Data directory" to diagnostics.dataDirectory,
        "Logs directory" to diagnostics.logsDirectory,
        "Current log" to diagnostics.logFile
    )
