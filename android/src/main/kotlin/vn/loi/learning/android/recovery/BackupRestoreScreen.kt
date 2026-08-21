package vn.loi.learning.android.recovery

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    onReload: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.createBackup(context.cacheDir) { context.contentResolver.openOutputStream(uri) }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.stageAndPreviewRestore(context.cacheDir) { context.contentResolver.openInputStream(uri) }
        }
    }

    val isBusy = uiState is BackupRestoreUiState.BackingUp ||
            uiState is BackupRestoreUiState.Previewing ||
            uiState is BackupRestoreUiState.Restoring

    BackHandler(enabled = true) {
        if (uiState is BackupRestoreUiState.Restoring) {
            // Prevent leaving during destructive mutation
            return@BackHandler
        }
        if (uiState is BackupRestoreUiState.PreviewReady) {
            viewModel.cancelPreview((uiState as BackupRestoreUiState.PreviewReady).stagedFile)
            return@BackHandler
        }
        if (uiState is BackupRestoreUiState.PreviewFailure ||
            uiState is BackupRestoreUiState.BackupSuccess ||
            uiState is BackupRestoreUiState.BackupFailure ||
            uiState is BackupRestoreUiState.RestoreFailure) {
            viewModel.dismissResult()
            return@BackHandler
        }
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isBusy) onBack()
                        },
                        enabled = !isBusy,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(LearningSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
        ) {
            // Full Backup Card
            FullBackupCard(
                enabled = !isBusy,
                onCreateBackup = {
                    val defaultName = viewModel.generateDefaultBackupFilename()
                    createBackupLauncher.launch(defaultName)
                }
            )

            // Restore Backup Card
            RestoreBackupCard(
                enabled = !isBusy,
                onSelectBackup = {
                    openBackupLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                }
            )

            // Safety Notes Card
            SafetyNotesCard()
        }
    }

    // Dialogs
    when (val state = uiState) {
        is BackupRestoreUiState.BackingUp -> {
            ProgressDialog(title = "Creating Full Backup", message = state.message)
        }
        is BackupRestoreUiState.Previewing -> {
            ProgressDialog(title = "Reading Backup", message = state.message)
        }
        is BackupRestoreUiState.Restoring -> {
            ProgressDialog(
                title = "Restoring Learning Data",
                message = state.message,
                dismissible = false
            )
        }
        is BackupRestoreUiState.BackupSuccess -> {
            BackupSuccessDialog(
                summary = state.summary,
                onDismiss = { viewModel.dismissResult() }
            )
        }
        is BackupRestoreUiState.BackupFailure -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Backup Failed") },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("Dismiss")
                    }
                }
            )
        }
        is BackupRestoreUiState.PreviewReady -> {
            RestorePreviewDialog(
                preview = state.preview,
                onConfirm = { viewModel.confirmRestore(state.stagedFile) },
                onCancel = { viewModel.cancelPreview(state.stagedFile) }
            )
        }
        is BackupRestoreUiState.PreviewFailure -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Invalid Backup File") },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("Close")
                    }
                }
            )
        }
        is BackupRestoreUiState.RestoreSuccess -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Restore Completed Successfully") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your learning data has been restored (${state.restoredEntriesCount} items restored).")
                        Text(
                            "A safety backup of your previous data was saved to: ${state.safetyBackupPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Learning Engine will now reload to apply the restored state.",
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissResult()
                            onReload()
                        },
                        modifier = Modifier.semantics { contentDescription = "Reload Learning Engine after restore" }
                    ) {
                        Text("Reload Learning Engine")
                    }
                },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            )
        }
        is BackupRestoreUiState.RestoreFailure -> {
            RestoreFailureDialog(
                result = state.result,
                onDismiss = { viewModel.dismissResult() }
            )
        }
        is BackupRestoreUiState.Idle -> {}
    }
}

@Composable
private fun FullBackupCard(
    enabled: Boolean,
    onCreateBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Create Full Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Exports a complete, portable .lebak archive containing:",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(
                modifier = Modifier.padding(start = LearningSpacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BulletItem("Packages and vocabulary content")
                BulletItem("FSRS memory states and review history")
                BulletItem("Active study sessions and queues")
                BulletItem("Audio, images, and Quick Voice recordings")
                BulletItem("Difficult markers and durable app settings")
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onCreateBackup,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Create full backup file" }
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(Modifier.width(LearningSpacing.small))
                Text("Create Full Backup (.lebak)")
            }
        }
    }
}

@Composable
private fun RestoreBackupCard(
    enabled: Boolean,
    onSelectBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Restore from Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Restores your learning data from a selected .lebak file.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Replace Everything Mode: Restoring replaces all current learning data. A safety backup of current data will be created automatically before restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSelectBackup,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Select backup file to restore" }
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(LearningSpacing.small))
                Text("Select Backup File (.lebak)")
            }
        }
    }
}

@Composable
private fun SafetyNotesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Backup & Privacy Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "• Backups are stored locally on your device in the folder you choose.\n" +
                "• No data is uploaded to any cloud service.\n" +
                "• Android system permissions (such as microphone or notifications) are not backed up and remain controlled by your device settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Text(
        "• $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ProgressDialog(
    title: String,
    message: String,
    dismissible: Boolean = true
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
            ) {
                CircularProgressIndicator()
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {},
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = false
        )
    )
}

@Composable
private fun BackupSuccessDialog(
    summary: BackupSuccessSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Created Successfully") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("File: ${summary.fileName}", fontWeight = FontWeight.Bold)
                Text("Size: ${summary.fileSizeFormatted}")
                Text("Created at: ${summary.createdAtUtc}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Archive Contents:", fontWeight = FontWeight.Medium)
                Text("• Packages: ${summary.counts.packages}")
                Text("• Vocabulary Items: ${summary.counts.contents}")
                Text("• Learning Items: ${summary.counts.learningItems}")
                Text("• FSRS Memory States: ${summary.counts.memoryStates}")
                Text("• Review Events: ${summary.counts.reviewEvents}")
                Text("• Quick Voice Recordings: ${summary.counts.recordings}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun RestorePreviewDialog(
    preview: PortableBackupV2Preview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Replace current learning data?",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Caution: This operation will REPLACE all current local learning data with the contents of this backup file. This is NOT a merge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "A safety backup of your current data will be created automatically before any changes are made.",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Backup Information:", fontWeight = FontWeight.Bold)
                Text("• Created: ${preview.createdAtUtc}")
                Text("• App Version: ${preview.appVersion} (${preview.sourcePlatform})")
                Text("• Packages: ${preview.counts.packages}")
                Text("• Vocabulary Items: ${preview.counts.contents}")
                Text("• Learning Items: ${preview.counts.learningItems}")
                Text("• FSRS Cards: ${preview.counts.memoryStates}")
                Text("• Review History: ${preview.counts.reviewEvents}")
                Text("• Voice Recordings: ${preview.counts.recordings}")
                Text("• Total Expanded Size: ${formatBytesHelper(preview.bytes.totalExpandedBytes)}")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.semantics { contentDescription = "Confirm destructive restore" }
            ) {
                Text("Replace & Restore")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RestoreFailureDialog(
    result: PortableBackupV2RestoreResult,
    onDismiss: () -> Unit
) {
    val (title, message) = when (result) {
        is PortableBackupV2RestoreResult.ValidationFailed -> {
            "Invalid Backup" to (result.detail ?: result.message)
        }
        is PortableBackupV2RestoreResult.InsufficientSpace -> {
            "Insufficient Storage" to "Restore requires at least ${formatBytesHelper(result.requiredBytes)} free space, but only ${formatBytesHelper(result.availableBytes)} is available."
        }
        is PortableBackupV2RestoreResult.Busy -> {
            "Operation Busy" to "Restore cannot proceed. If a Quick Voice recording is active, please stop it before restoring."
        }
        is PortableBackupV2RestoreResult.SafetyBackupFailed -> {
            "Safety Backup Failed" to "Restore was not started because current data could not be safely backed up (${result.causeMessage ?: result.message}). Your current data remains unchanged."
        }
        is PortableBackupV2RestoreResult.RestoreFailedRolledBack -> {
            "Restore Failed (Data Preserved)" to "Restore failed (${result.failureReason}), but your previous learning data was rolled back and preserved successfully without loss. Safety backup retained at: ${result.safetyBackupPath}."
        }
        is PortableBackupV2RestoreResult.RollbackFailed -> {
            "Critical Recovery Error" to "Restore and automatic rollback both failed. Do not continue studying. Your pre-restore safety backup is saved at: ${result.safetyBackupPath}. Error: ${result.restoreFailure} / ${result.rollbackFailure}"
        }
        is PortableBackupV2RestoreResult.UnsupportedSchema -> {
            "Unsupported Backup Version" to "Backup schema version ${result.foundVersion} is not supported (expected ${result.supportedVersion})."
        }
        is PortableBackupV2RestoreResult.Success -> {
            "Restore Completed" to "Restore finished successfully."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.error) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private fun formatBytesHelper(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.2f MB", mb)
}
