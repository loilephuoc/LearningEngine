package vn.loi.learning.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.ui.localization.DesktopStrings
import vn.loi.learning.desktop.ui.study.resolveStudyTypographyPreview
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.ShortcutChangeResult
import vn.loi.learning.desktop.shortcut.ShortcutChordFormatter
import vn.loi.learning.desktop.shortcut.ShortcutConflict
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand
import vn.loi.learning.desktop.shortcut.toDesktopKeyChord
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderSettingsController

@Composable
fun SettingsScreen(
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    vocabularyReminderSettingsController: DesktopVocabularyReminderSettingsController?,
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
    var systemInformationVisible by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 1080.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        SettingsCategoryHeading("Giao diện")

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

        SettingsCategoryHeading("Trải nghiệm học")

        vocabularyReminderSettingsController?.let {
            DesktopVocabularyReminderSetting(controller = it)
        }

        StudyTypographySetting(
            preferences = runtimeConfiguration.studyTypography,
            onApply = { preferences ->
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(studyTypography = preferences)
                )
            }
        )

        StudyPresentationSetting(
            preferences = runtimeConfiguration.studyPresentation,
            onApply = { preferences ->
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(studyPresentation = preferences)
                )
            }
        )

        SettingsCategoryHeading("Phiên học")

        SessionLimitSetting(
            title = "Thẻ mới mỗi phiên",
            value = runtimeConfiguration.newItemsPerSession,
            presets = listOf(5, 10, 20, 30, 50),
            validRange = DesktopRuntimeConfiguration.MIN_NEW_ITEMS_PER_SESSION..
                DesktopRuntimeConfiguration.MAX_NEW_ITEMS_PER_SESSION,
            otherValue = runtimeConfiguration.reviewItemsPerSession,
            customValue = runtimeConfiguration.newItemsPerSession,
            onValidValue = {
                onRuntimeConfigurationChanged(runtimeConfiguration.copy(newItemsPerSession = it))
            }
        )
        Text(
            text = "Thay đổi áp dụng cho phiên mới và không ảnh hưởng phiên đang học.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SessionLimitSetting(
            title = "Thẻ ôn tập mỗi phiên",
            value = runtimeConfiguration.reviewItemsPerSession,
            presets = listOf(20, 50, 100, 200),
            validRange = DesktopRuntimeConfiguration.MIN_REVIEW_ITEMS_PER_SESSION..
                DesktopRuntimeConfiguration.MAX_REVIEW_ITEMS_PER_SESSION,
            otherValue = runtimeConfiguration.newItemsPerSession,
            customValue = runtimeConfiguration.reviewItemsPerSession,
            onValidValue = {
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(
                        reviewItemsPerSession = it,
                        customReviewItemsPerSession = it
                    )
                )
            },
            onValidCustomValue = {
                onRuntimeConfigurationChanged(
                    runtimeConfiguration.copy(
                        reviewItemsPerSession = it,
                        customReviewItemsPerSession = it
                    )
                )
            }
        )
        Text(
            "Đây là giới hạn tối đa; phiên có thể ít thẻ hơn khi không đủ nội dung phù hợp. Thay đổi áp dụng cho phiên mới tiếp theo.",
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    modifier = Modifier.fillMaxWidth().widthIn(max = 300.dp)
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

        SettingsCategoryHeading("Phím tắt")

        StudyShortcutSetting(
            registry = runtimeConfiguration.studyShortcuts,
            onRegistryChanged = { registry ->
                onRuntimeConfigurationChanged(runtimeConfiguration.copy(studyShortcuts = registry))
            }
        )

        SettingsCategoryHeading("Dữ liệu")

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

        SettingsCategoryHeading("Giới thiệu & hệ thống")

        TextButton(onClick = { systemInformationVisible = !systemInformationVisible }) {
            Text(if (systemInformationVisible) "Ẩn thông tin hệ thống" else "Xem thông tin hệ thống")
        }

        if (systemInformationVisible) {
            SettingsSection(
                title = "Learning Engine 2.0",
                properties =
                    listOf(
                        "Trình lập lịch" to "FSRS",
                        "Kiến trúc" to "Clean Architecture + DDD",
                        "Lưu trữ" to "JSON",
                        "Môi trường chạy" to "Kotlin/JVM 21",
                        "Hệ thống thiết kế" to "Material 3"
                    ) + resolveRuntimeDiagnosticProperties(runtimeDiagnostics)
            )

            Button(onClick = { aboutVisible = true }) {
                Text(strings.aboutButton)
            }
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
private fun StudyPresentationSetting(
    preferences: StudyPresentationPreferences,
    onApply: (StudyPresentationPreferences) -> Unit
) {
    var state by remember(preferences) {
        mutableStateOf(StudyPresentationSettingsState(active = preferences))
    }
    val draft = state.draft
    val presentation = resolveStudyPresentationSettings(draft)
    Card(
        modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Trình bày thích ứng khi học",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyPresentationControlMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.controlMode == mode,
                        onClick = { state = state.edit(draft.copy(controlMode = mode)) },
                        label = { Text(mode.settingsLabel()) }
                    )
                }
            }
            Text(
                presentation.guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PresentationSwitch(
                label = "Hiển thị tiếng Anh",
                checked = draft.showEnglish,
                enabled = presentation.controlsEnabled,
                onCheckedChange = { state = state.edit(draft.copy(showEnglish = it)) }
            )
            PresentationSwitch(
                label = "Hiển thị tiếng Việt",
                checked = draft.showVietnamese,
                enabled = presentation.controlsEnabled,
                onCheckedChange = { state = state.edit(draft.copy(showVietnamese = it)) }
            )
            PresentationSwitch(
                label = "Tự phát âm thanh tiếng Anh",
                checked = draft.autoplayEnglish,
                enabled = presentation.controlsEnabled,
                onCheckedChange = { state = state.edit(draft.copy(autoplayEnglish = it)) }
            )
            PresentationSwitch(
                label = "Tự phát âm thanh tiếng Việt",
                checked = draft.autoplayVietnamese,
                enabled = presentation.controlsEnabled,
                onCheckedChange = { state = state.edit(draft.copy(autoplayVietnamese = it)) }
            )
            Text(
                "Preview (không tự phát âm thanh)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (presentation.showEnglishPreview) {
                Text("There are many homeless people.", fontWeight = FontWeight.SemiBold)
            }
            if (presentation.showVietnamesePreview) {
                Text("Có rất nhiều người vô gia cư.")
            }
            if (!presentation.showEnglishPreview && !presentation.showVietnamesePreview) {
                Text(
                    "Không có nội dung bổ trợ được chọn.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { onApply(draft) },
                enabled = draft != preferences
            ) {
                Text("Áp dụng")
            }
        }
    }
}

@Composable
private fun PresentationSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsCategoryHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun StudyShortcutSetting(
    registry: ShortcutRegistry,
    onRegistryChanged: (ShortcutRegistry) -> Unit
) {
    var editingCommand by remember { mutableStateOf<StudyShortcutCommand?>(null) }
    var capturedChord by remember { mutableStateOf<DesktopKeyChord?>(null) }
    var conflict by remember { mutableStateOf<ShortcutConflict?>(null) }
    Card(
        modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Phím tắt khi học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val narrow = maxWidth < 620.dp
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!narrow) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Thao tác", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Phím tắt", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Đổi / Đặt lại", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                registry.bindings.forEach { binding ->
                    val details: @Composable (Modifier) -> Unit = { detailsModifier -> Column(detailsModifier) {
                        Text(binding.command.displayName)
                        Text(ShortcutChordFormatter.format(binding.chord, compact = true), fontWeight = FontWeight.SemiBold)
                    } }
                    val actions: @Composable (Modifier) -> Unit = { actionsModifier -> Row(modifier = actionsModifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            editingCommand = binding.command
                            capturedChord = null
                            conflict = null
                        }) { Text("Đổi") }
                        TextButton(onClick = {
                            val defaultChord = ShortcutRegistry.defaults().chordFor(binding.command)
                            when (val result = registry.requestChange(binding.command, defaultChord)) {
                                is ShortcutChangeResult.Changed -> onRegistryChanged(result.registry)
                                is ShortcutChangeResult.Conflict -> {
                                    editingCommand = binding.command
                                    capturedChord = defaultChord
                                    conflict = result.conflict
                                }
                            }
                        }) { Text("Đặt lại") }
                    } }
                    if (narrow) Column(Modifier.fillMaxWidth()) { details(Modifier.fillMaxWidth()); actions(Modifier.fillMaxWidth()) }
                    else Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { details(Modifier.weight(2f)); actions(Modifier.weight(1f)) }
                }
                }
            }
            Button(
                onClick = { onRegistryChanged(ShortcutRegistry.defaults()) },
                enabled = registry != ShortcutRegistry.defaults()
            ) {
                Text("Khôi phục mặc định")
            }
        }
    }

    editingCommand?.let { command ->
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(command) { focusRequester.requestFocus() }
        AlertDialog(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val chord = event.toDesktopKeyChord() ?: return@onPreviewKeyEvent false
                    capturedChord = chord
                    conflict = null
                    true
                },
            onDismissRequest = {
                editingCommand = null
                capturedChord = null
                conflict = null
            },
            title = { Text("Đổi phím tắt: ${command.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhấn phím hoặc tổ hợp phím mới.")
                    Text(
                        capturedChord?.let {
                            ShortcutChordFormatter.format(it, compact = true)
                        } ?: "Chưa có phím",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    conflict?.let {
                        Text(
                            "Shortcut đã được gán cho: ${it.occupiedBy.displayName}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = capturedChord != null && conflict == null,
                    onClick = {
                        when (val result = registry.requestChange(command, requireNotNull(capturedChord))) {
                            is ShortcutChangeResult.Changed -> {
                                onRegistryChanged(result.registry)
                                editingCommand = null
                                capturedChord = null
                            }
                            is ShortcutChangeResult.Conflict -> conflict = result.conflict
                        }
                    }
                ) { Text("Lưu") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        editingCommand = null
                        capturedChord = null
                        conflict = null
                    }) { Text("Hủy") }
                }
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
        modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
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
            Text("Xem trước", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                Text("Áp dụng")
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
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
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
    customValue: Int,
    onValidValue: (Int) -> Unit,
    onValidCustomValue: (Int) -> Unit = onValidValue
) {
    var text by remember(customValue) { mutableStateOf(customValue.toString()) }
    var invalid by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { invalid = false; onValidValue(preset) },
                    label = { Text(preset.toString()) }
                )
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                val parsed = parseSessionLimitInput(it, validRange, otherValue)
                if (parsed != null) {
                    invalid = false
                    onValidCustomValue(parsed)
                } else {
                    invalid = it.isNotEmpty()
                }
            },
            singleLine = true,
            isError = invalid,
            label = { Text("Tùy chỉnh (${validRange.first}–${validRange.last})") },
            supportingText = {
                if (invalid) Text("Nhập giới hạn hợp lệ; hai giới hạn không thể đồng thời bằng 0.")
            },
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    val parsed = parseSessionLimitInput(text, validRange, otherValue)
                    if (parsed != null) {
                        invalid = false
                        onValidCustomValue(parsed)
                    } else invalid = true
                }
            ),
            modifier = Modifier.fillMaxWidth().widthIn(max = 300.dp)
        )
    }
}

internal fun parseSessionLimitInput(
    text: String,
    validRange: IntRange,
    otherValue: Int
): Int? =
    text.toIntOrNull()?.takeIf { it in validRange && (it > 0 || otherValue > 0) }

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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                .widthIn(max = 900.dp)
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
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = accessibility.value,
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
