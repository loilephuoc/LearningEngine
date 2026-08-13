package vn.loi.learning.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderActionResult
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderDraft
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderSelectionMode
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderSettingsController
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun DesktopVocabularyReminderSetting(controller: DesktopVocabularyReminderSettingsController) {
    var configuration by remember(controller) { mutableStateOf(controller.load()) }
    var dialogVisible by remember { mutableStateOf(false) }
    var draft by remember(dialogVisible) {
        mutableStateOf(DesktopVocabularyReminderDraft.from(configuration.settings))
    }
    var feedback by remember { mutableStateOf<String?>(null) }
    val settings = configuration.settings
    val selectedPackage = configuration.packages.firstOrNull { it.id == settings.selectedPackageId?.value }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Desktop Vocabulary Reminder", fontWeight = FontWeight.Bold)
                    Text(
                        buildSummary(settings.enabled, selectedPackage?.name, settings.selectedPackageId, settings.selectionMode.label(), settings.intervalMinutes, settings.pausedUntil),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.enabled, onCheckedChange = { enabled ->
                    feedback = controller.setEnabled(enabled).message("Reminder setting updated.")
                    configuration = controller.load()
                })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { draft = DesktopVocabularyReminderDraft.from(settings); feedback = null; dialogVisible = true }) {
                    Text("Configure ›")
                }
                if (settings.pausedUntil?.isAfter(Instant.now()) == true) {
                    TextButton(onClick = { feedback = controller.resumeNow().message("Reminders resumed."); configuration = controller.load() }) {
                        Text("Resume now")
                    }
                }
            }
            feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (dialogVisible) {
        ReminderConfigurationDialog(
            draft = draft,
            packages = configuration.packages,
            feedback = feedback,
            onDraftChanged = { draft = it; feedback = null },
            onPreview = { feedback = controller.preview(draft).message("Preview displayed.") },
            onPause30 = { feedback = controller.pause30Minutes().message("Paused for 30 minutes."); configuration = controller.load() },
            onPauseHour = { feedback = controller.pauseOneHour().message("Paused for 1 hour."); configuration = controller.load() },
            onPauseToday = { feedback = controller.pauseToday().message("Paused until tomorrow."); configuration = controller.load() },
            onApply = {
                when (val result = controller.apply(draft)) {
                    DesktopVocabularyReminderActionResult.Success -> {
                        configuration = controller.load(); feedback = "Reminder settings saved."; dialogVisible = false
                    }
                    is DesktopVocabularyReminderActionResult.Failure -> feedback = result.message
                }
            },
            onCancel = { feedback = null; dialogVisible = false }
        )
    }
}

@Composable
private fun ReminderConfigurationDialog(
    draft: DesktopVocabularyReminderDraft,
    packages: List<vn.loi.learning.application.contentpackaging.InstalledPackageItem>,
    feedback: String?,
    onDraftChanged: (DesktopVocabularyReminderDraft) -> Unit,
    onPreview: () -> Unit,
    onPause30: () -> Unit,
    onPauseHour: () -> Unit,
    onPauseToday: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    var packageMenu by remember { mutableStateOf(false) }
    val selected = packages.firstOrNull { it.id == draft.selectedPackageId?.value }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Desktop Vocabulary Reminder") },
        text = {
            Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enabled")
                    Switch(draft.enabled, { onDraftChanged(draft.copy(enabled = it)) })
                }
                Column {
                    Text("Package")
                    OutlinedButton(onClick = { packageMenu = true }) {
                        Text(selected?.name ?: draft.selectedPackageId?.let { "Unavailable (${it.value})" } ?: "Select package")
                    }
                    DropdownMenu(packageMenu, { packageMenu = false }) {
                        packages.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { packageMenu = false; onDraftChanged(draft.copy(selectedPackageId = InstalledPackageId(item.id))) }
                            )
                        }
                    }
                }
                Text("Content mode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesktopVocabularyReminderSelectionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == draft.selectionMode,
                            onClick = { onDraftChanged(draft.copy(selectionMode = mode)) },
                            label = { Text(mode.label()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                OutlinedTextField(draft.intervalText, { onDraftChanged(draft.copy(intervalText = it)) }, label = { Text("Show next vocabulary every (minutes)") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { value ->
                        FilterChip(draft.intervalText == value.toString(), { onDraftChanged(draft.copy(intervalText = value.toString())) }, { Text("$value min") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft.activeStartText, { onDraftChanged(draft.copy(activeStartText = it)) }, Modifier.weight(1f), label = { Text("Active from (HH:mm)") }, singleLine = true)
                    OutlinedTextField(draft.activeEndText, { onDraftChanged(draft.copy(activeEndText = it)) }, Modifier.weight(1f), label = { Text("Active until (HH:mm)") }, singleLine = true)
                }
                OutlinedTextField(draft.displayDurationText, { onDraftChanged(draft.copy(displayDurationText = it)) }, label = { Text("Popup duration (1.5–60 seconds)") }, singleLine = true)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Play pronunciation when popup appears", modifier = Modifier.weight(1f))
                    Switch(
                        checked = draft.autoPlayPronunciation,
                        onCheckedChange = { onDraftChanged(draft.copy(autoPlayPronunciation = it)) }
                    )
                }
                Button(onClick = onPreview) { Text("Preview notification") }
                Text("Pause reminders", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPause30) { Text("30 minutes") }
                    OutlinedButton(onClick = onPauseHour) { Text("1 hour") }
                    OutlinedButton(onClick = onPauseToday) { Text("Today") }
                }
                feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = { Button(onClick = onApply) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

private fun DesktopVocabularyReminderActionResult.message(success: String) = when (this) {
    DesktopVocabularyReminderActionResult.Success -> success
    is DesktopVocabularyReminderActionResult.Failure -> message
}

internal fun DesktopVocabularyReminderSelectionMode.label() = when (this) {
    DesktopVocabularyReminderSelectionMode.AGAIN_HARD -> "Again / Hard"
    DesktopVocabularyReminderSelectionMode.DUE -> "Due"
    DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED -> "Random learned"
    DesktopVocabularyReminderSelectionMode.RANDOM_ALL -> "Random all"
    DesktopVocabularyReminderSelectionMode.MARKED_DIFFICULT -> "Marked difficult"
}

private fun buildSummary(enabled: Boolean, packageName: String?, packageId: InstalledPackageId?, mode: String, interval: Int, pausedUntil: Instant?): String = buildList {
    add(if (enabled) "Enabled" else "Disabled")
    add(packageName ?: packageId?.let { "Unavailable (${it.value})" } ?: "No package")
    add(mode)
    add("Every $interval minutes")
    pausedUntil?.takeIf { it.isAfter(Instant.now()) }?.let {
        add("Paused until ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(it)}")
    }
}.joinToString(" · ")
