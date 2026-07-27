package vn.loi.learning.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.ui.localization.DesktopStrings
import vn.loi.learning.desktop.ui.study.resolveStudyTypographyPreview

@Composable
fun SettingsScreen(
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    strings: DesktopStrings,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit,
    onExportDiagnostics: () -> String?,
    onCreateBackup: () -> String?,
    onRestoreBackup: () -> String?,
    modifier: Modifier = Modifier
) {
    var aboutVisible by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var recoveryStatus by remember { mutableStateOf<String?>(null) }
    var restoreConfirmationVisible by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

        StudyTypographySetting(
            preferences = runtimeConfiguration.studyTypography,
            onApply = { preferences ->
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(studyTypography = preferences)
                )
            }
        )

        SessionLimitSetting(
            title = "New items per session",
            value = runtimeConfiguration.newItemsPerSession,
            presets = listOf(5, 10, 20, 30, 50),
            validRange = DesktopRuntimeConfiguration.MIN_NEW_ITEMS_PER_SESSION..
                DesktopRuntimeConfiguration.MAX_NEW_ITEMS_PER_SESSION,
            otherValue = runtimeConfiguration.reviewItemsPerSession,
            onValidValue = {
                onRuntimeConfigurationChanged(runtimeConfiguration.copy(newItemsPerSession = it))
            }
        )
        SessionLimitSetting(
            title = "Review items per session",
            value = runtimeConfiguration.reviewItemsPerSession,
            presets = listOf(20, 50, 100, 200),
            validRange = DesktopRuntimeConfiguration.MIN_REVIEW_ITEMS_PER_SESSION..
                DesktopRuntimeConfiguration.MAX_REVIEW_ITEMS_PER_SESSION,
            otherValue = runtimeConfiguration.newItemsPerSession,
            onValidValue = {
                onRuntimeConfigurationChanged(runtimeConfiguration.copy(reviewItemsPerSession = it))
            }
        )
        Text(
            "These are maximums: a session can contain fewer items when fewer candidates are available. Changes apply to the next new session.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        var audioDelayText by remember(runtimeConfiguration.audioLoopDelaySeconds) {
            mutableStateOf(runtimeConfiguration.audioLoopDelaySeconds.toString())
        }
        var audioDelayError by remember { mutableStateOf(false) }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Cấu hình thời gian nghỉ giữa các lần phát lặp âm thanh"
                }
        ) {
            Text(
                text = "Thời gian nghỉ giữa các lần phát lặp (giây)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.2, 0.35, 0.5, 1.0, 1.5, 2.0).forEach { preset ->
                    FilterChip(
                        selected = runtimeConfiguration.audioLoopDelaySeconds == preset,
                        onClick = {
                            audioDelayText = preset.toString()
                            audioDelayError = false
                            onRuntimeConfigurationChanged(
                                runtimeConfiguration.copy(audioLoopDelaySeconds = preset)
                            )
                        },
                        label = { Text("${preset}s") }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = audioDelayText,
                    onValueChange = { input ->
                        audioDelayText = input
                        audioDelayError = false
                    },
                    isError = audioDelayError,
                    singleLine = true,
                    label = { Text("Số giây (ví dụ: 0.2, 0.35, 0.5, 1, 1.5, 2)") },
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            val parsed = audioDelayText.toDoubleOrNull()
                            if (parsed != null && parsed in
                                DesktopRuntimeConfiguration.MIN_AUDIO_LOOP_DELAY_SECONDS..
                                DesktopRuntimeConfiguration.MAX_AUDIO_LOOP_DELAY_SECONDS
                            ) {
                                audioDelayError = false
                                onRuntimeConfigurationChanged(
                                    runtimeConfiguration.copy(audioLoopDelaySeconds = parsed)
                                )
                            } else audioDelayError = true
                        }
                    ),
                    modifier = Modifier.width(300.dp)
                )
                if (audioDelayError) {
                    Text(
                        text = "Vui lòng nhập số từ 0.0 đến 10.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

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

        SettingsChoiceSection(
            title = strings.recovery,
            options = listOf(strings.createBackup, strings.restoreBackup),
            selected = "",
            label = { it },
            onSelected = { action ->
                if (action == strings.createBackup) {
                    recoveryStatus = runCatching(onCreateBackup).fold(
                        { it?.let(strings::backupCreatedAt) },
                        { strings.recoveryFailed(it.message ?: "unknown error") }
                    )
                } else restoreConfirmationVisible = true
            }
        )
        recoveryStatus?.let { Text(it) }
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
                    Button(
                        onClick = {
                            exportStatus =
                                runCatching(onExportDiagnostics)
                                    .fold(
                                        onSuccess = { path ->
                                            path?.let { strings.diagnosticsExportedTo(it) }
                                        },
                                        onFailure = { failure ->
                                            strings.diagnosticsExportFailed(
                                                failure.message ?: "unknown error"
                                            )
                                        }
                                    )
                        }
                    ) {
                        Text(strings.exportDiagnostics)
                    }
                    exportStatus?.let { status -> Text(status) }
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutVisible = false }) { Text(strings.close) }
            }
        )
    }

    if (restoreConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { restoreConfirmationVisible = false },
            title = { Text(strings.restoreBackup) },
            text = { Text(strings.restoreWarning) },
            confirmButton = {
                Button(onClick = {
                    restoreConfirmationVisible = false
                    recoveryStatus = runCatching(onRestoreBackup).fold(
                        { it?.let(strings::restoreCompletedFrom) },
                        { strings.recoveryFailed(it.message ?: "unknown error") }
                    )
                }) { Text(strings.confirmRestore) }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmationVisible = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun StudyTypographySetting(
    preferences: StudyTypographyPreferences,
    onApply: (StudyTypographyPreferences) -> Unit
) {
    var draft by remember(preferences) { mutableStateOf(preferences) }
    val preview = resolveStudyTypographyPreview(draft, viewportWidthDp = 600)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cỡ chữ khi học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TypographyStepper(
                label = "Ví dụ tiếng Anh",
                value = draft.exampleEnglishFontSize,
                validRange = StudyTypographyPreferences.MIN_EXAMPLE_ENGLISH_FONT_SIZE..
                    StudyTypographyPreferences.MAX_EXAMPLE_ENGLISH_FONT_SIZE,
                onValueChanged = { draft = draft.copy(exampleEnglishFontSize = it) }
            )
            TypographyStepper(
                label = "Ví dụ tiếng Việt",
                value = draft.exampleVietnameseFontSize,
                validRange = StudyTypographyPreferences.MIN_EXAMPLE_VIETNAMESE_FONT_SIZE..
                    StudyTypographyPreferences.MAX_EXAMPLE_VIETNAMESE_FONT_SIZE,
                onValueChanged = { draft = draft.copy(exampleVietnameseFontSize = it) }
            )
            Text("Preview", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                preview.englishText,
                fontSize = preview.typography.exampleEnglishFontSize.sp,
                lineHeight = preview.typography.exampleEnglishLineHeight.sp,
                fontWeight = FontWeight.SemiBold,
                softWrap = preview.typography.softWrap
            )
            Text(
                preview.vietnameseText,
                fontSize = preview.typography.exampleVietnameseFontSize.sp,
                lineHeight = preview.typography.exampleVietnameseLineHeight.sp,
                fontWeight = FontWeight.Normal,
                softWrap = preview.typography.softWrap
            )
            Button(
                onClick = { onApply(draft) },
                enabled = draft != preferences
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun TypographyStepper(
    label: String,
    value: Int,
    validRange: IntRange,
    onValueChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onValueChanged(value - 1) },
                enabled = value > validRange.first
            ) { Text("−") }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = { onValueChanged(value + 1) },
                enabled = value < validRange.last
            ) { Text("+") }
        }
    }
}

@Composable
private fun SessionLimitSetting(
    title: String,
    value: Int,
    presets: List<Int>,
    validRange: IntRange,
    otherValue: Int,
    onValidValue: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var invalid by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { text = preset.toString(); invalid = false; onValidValue(preset) },
                    label = { Text(preset.toString()) }
                )
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = { text = it; invalid = false },
            singleLine = true,
            isError = invalid,
            label = { Text("Custom (${validRange.first}–${validRange.last})") },
            supportingText = {
                if (invalid) Text("Enter a valid limit; both limits cannot be zero.")
            },
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    val parsed = text.toIntOrNull()
                    if (parsed != null && parsed in validRange && (parsed > 0 || otherValue > 0)) {
                        invalid = false
                        onValidValue(parsed)
                    } else invalid = true
                }
            ),
            modifier = Modifier.width(300.dp)
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
