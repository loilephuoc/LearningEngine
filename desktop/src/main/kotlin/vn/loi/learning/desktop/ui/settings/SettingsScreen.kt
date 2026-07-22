package vn.loi.learning.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.ui.localization.DesktopStrings

@Composable
fun SettingsScreen(
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    strings: DesktopStrings,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    var aboutVisible by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = strings.settingsSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(
            title = "Learning Engine",
            properties =
                listOf(
                    "Scheduler" to "FSRS",
                    "Architecture" to "Clean Architecture + DDD",
                    "Persistence" to "JSON",
                    "Runtime" to "Kotlin/JVM 21"
                )
        )

        SettingsSection(
            title = strings.applicationSection,
            properties =
                listOf(
                    "Design system" to "Material 3",
                    strings.theme to strings.theme(runtimeConfiguration.theme),
                    strings.language to strings.language(runtimeConfiguration.locale),
                    "Application" to "Learning Engine 2.0"
                )
        )

        SettingsChoiceSection(
            title = strings.theme,
            options = DesktopThemePreference.entries,
            selected = runtimeConfiguration.theme,
            label = strings::theme,
            onSelected = { preference ->
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(theme = preference)
                )
            }
        )

        SettingsChoiceSection(
            title = strings.language,
            options = DesktopLocale.entries,
            selected = runtimeConfiguration.locale,
            label = strings::language,
            onSelected = { locale ->
                onRuntimeConfigurationChanged(runtimeConfiguration.copy(locale = locale))
            }
        )

        SettingsSection(
            title = strings.aboutAndSupport,
            properties =
                resolveRuntimeDiagnosticProperties(
                    runtimeDiagnostics
                )
        )

        Button(onClick = { aboutVisible = true }) {
            Text(strings.aboutButton)
        }
    }

    if (aboutVisible) {
        val presentation = resolveAboutDialogPresentation(runtimeDiagnostics)
        AlertDialog(
            onDismissRequest = { aboutVisible = false },
            title = { Text(presentation.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(presentation.version, fontWeight = FontWeight.SemiBold)
                    Text(strings.runtimeInformation, style = MaterialTheme.typography.titleSmall)
                    Text(presentation.supportSummary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutVisible = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun <T> SettingsChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    properties: List<Pair<String, String>>
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        resolveSettingsSectionContentDescription(
                            title = title,
                            properties = properties
                        )
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            properties.forEach { (label, value) ->
                val accessibility =
                    resolveSettingsPropertyAccessibility(
                        label = label,
                        value = value
                    )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics(
                                mergeDescendants = true
                            ) {
                                contentDescription =
                                    accessibility.contentDescription
                            },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = accessibility.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = accessibility.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
