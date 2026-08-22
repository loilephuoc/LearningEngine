package vn.loi.learning.android.recovery

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.domain.sync.model.SyncPreviewReport
import vn.loi.learning.domain.sync.model.SyncResultSummary
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult
import vn.loi.learning.infrastructure.recovery.SafetyBackupCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    onReload: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val safetyBackupState by viewModel.safetyBackups.collectAsStateWithLifecycle()

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

    val createSyncLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportSync(context.cacheDir) { context.contentResolver.openOutputStream(uri) }
        }
    }

    val openSyncLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.stageAndPreviewSync(context.cacheDir) { context.contentResolver.openInputStream(uri) }
        }
    }

    val isBusy = uiState is BackupRestoreUiState.BackingUp ||
            uiState is BackupRestoreUiState.Previewing ||
            uiState is BackupRestoreUiState.Restoring ||
            uiState is BackupRestoreUiState.SyncExporting ||
            uiState is BackupRestoreUiState.SyncPreviewing ||
            uiState is BackupRestoreUiState.SyncImporting

    BackHandler(enabled = true) {
        if (uiState is BackupRestoreUiState.Restoring || uiState is BackupRestoreUiState.SyncImporting) {
            return@BackHandler
        }
        if (uiState is BackupRestoreUiState.PreviewReady) {
            val preview = uiState as BackupRestoreUiState.PreviewReady
            viewModel.cancelPreview(preview.stagedFile, preview.deleteSourceAfterUse)
            return@BackHandler
        }
        if (uiState is BackupRestoreUiState.SyncPreviewReady) {
            viewModel.cancelPreview((uiState as BackupRestoreUiState.SyncPreviewReady).stagedFile)
            return@BackHandler
        }
        if (uiState is BackupRestoreUiState.PreviewFailure ||
            uiState is BackupRestoreUiState.BackupSuccess ||
            uiState is BackupRestoreUiState.BackupFailure ||
            uiState is BackupRestoreUiState.RestoreFailure ||
            uiState is BackupRestoreUiState.SyncExportSuccess ||
            uiState is BackupRestoreUiState.SyncExportFailure ||
            uiState is BackupRestoreUiState.SyncPreviewFailure ||
            uiState is BackupRestoreUiState.SyncImportSuccess ||
            uiState is BackupRestoreUiState.SyncImportFailure) {
            viewModel.dismissResult()
            return@BackHandler
        }
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_backup_restore)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isBusy) onBack()
                        },
                        enabled = !isBusy,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            // Differential Sync Card
            DifferentialSyncCard(
                enabled = !isBusy,
                onExportSync = {
                    val defaultName = viewModel.generateDefaultSyncFilename()
                    createSyncLauncher.launch(defaultName)
                },
                onImportSync = {
                    openSyncLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                }
            )

            // Full Backup Card
            FullBackupCard(
                enabled = !isBusy,
                onCreateBackup = {
                    val defaultName = viewModel.generateDefaultBackupFilename()
                    createBackupLauncher.launch(defaultName)
                }
            )

            SafetyBackupCard(
                state = safetyBackupState,
                enabled = !isBusy,
                onRefresh = viewModel::refreshSafetyBackups,
                onPreview = viewModel::previewSafetyBackup
            )

            // External Restore Backup Card
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
                isSafetyBackup = state.isSafetyBackup,
                archiveSizeBytes = state.stagedFile.length(),
                selectedPackageIds = state.selectedPackageIds,
                onTogglePackage = { pkgId, checked -> viewModel.toggleRestorePackage(pkgId, checked) },
                onConfirm = {
                    viewModel.confirmRestore(
                        state.stagedFile,
                        state.selectedPackageIds,
                        state.deleteSourceAfterUse
                    )
                },
                onCancel = { viewModel.cancelPreview(state.stagedFile, state.deleteSourceAfterUse) }
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
                        Text(
                            "Restored ${state.restoredCounts.packages} package(s), " +
                                "${state.restoredCounts.contents} content records, " +
                                "${state.restoredCounts.learningItems} learning items, and " +
                                "${state.restoredCounts.mediaFiles} media files."
                        )
                        state.cleanupResult.failureMessage?.let {
                            Text("Khôi phục thành công, nhưng không thể dọn bản sao cũ: $it")
                        }
                        Text(
                            "Một bản sao an toàn mới của dữ liệu trước khôi phục đã được lưu trên thiết bị.",
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
        is BackupRestoreUiState.SyncExporting -> {
            ProgressDialog(title = "Xuất gói vi sai", message = state.message)
        }
        is BackupRestoreUiState.SyncExportSuccess -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Xuất gói đồng bộ vi sai thành công") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tệp: ${state.summary.fileName}", fontWeight = FontWeight.Bold)
                        Text("Dung lượng: ${state.summary.fileSizeFormatted}")
                        Text("• Thẻ từ vựng cập nhật: ${state.summary.contentDeltasCount}")
                        Text("• Lượt ôn tập: ${state.summary.reviewEventsCount}")
                        Text("• Tệp âm thanh/ảnh mới: ${state.summary.mediaCount}")
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("Xong")
                    }
                }
            )
        }
        is BackupRestoreUiState.SyncExportFailure -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Xuất gói thất bại", color = MaterialTheme.colorScheme.error) },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        is BackupRestoreUiState.SyncPreviewing -> {
            ProgressDialog(title = "Đang phân tích gói đồng bộ", message = state.message)
        }
        is BackupRestoreUiState.SyncPreviewReady -> {
            SyncPreviewDialog(
                report = state.report,
                strategy = state.conflictStrategy,
                onStrategyChanged = { viewModel.setSyncConflictStrategy(it) },
                onConfirm = { viewModel.confirmSyncImport(state.stagedFile, state.conflictStrategy) },
                onCancel = { viewModel.cancelPreview(state.stagedFile) }
            )
        }
        is BackupRestoreUiState.SyncPreviewFailure -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Không thể đọc gói đồng bộ", color = MaterialTheme.colorScheme.error) },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        is BackupRestoreUiState.SyncImporting -> {
            ProgressDialog(
                title = "Đang áp dụng đồng bộ",
                message = state.message,
                dismissible = false
            )
        }
        is BackupRestoreUiState.SyncImportSuccess -> {
            AlertDialog(
                onDismissRequest = {
                    viewModel.dismissResult()
                    onReload()
                },
                title = { Text("Đồng bộ hoàn tất") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.summary.message)
                        Text("• Cập nhật thẻ: ${state.summary.contentDeltasApplied}")
                        Text("• Media mới: ${state.summary.mediaAssetsAdded}")
                        Text("• Lượt ôn tập: ${state.summary.reviewEventsMerged} mới (${state.summary.reviewEventsDeduplicated} bỏ qua)")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.dismissResult()
                        onReload()
                    }) {
                        Text("OK")
                    }
                }
            )
        }
        is BackupRestoreUiState.SyncImportFailure -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResult() },
                title = { Text("Đồng bộ thất bại", color = MaterialTheme.colorScheme.error) },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.dismissResult() }) {
                        Text("OK")
                    }
                }
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
                    "File sao lưu bên ngoài",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Chọn một file .lebak từ bộ nhớ thiết bị hoặc thư mục Downloads.",
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
                    "File được xác minh và xem trước trước khi khôi phục. File bên ngoài không được đưa vào retention hoặc tự động xóa.",
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
                Text("Chọn file sao lưu (.lebak)")
            }
        }
    }
}

@Composable
private fun SafetyBackupCard(
    state: SafetyBackupListState,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onPreview: (SafetyBackupCandidate) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LearningSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
        ) {
            Text("Bản sao an toàn trên thiết bị", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Các bản sao này được Learning Engine tự động tạo trước khi khôi phục. Bạn có thể xem trước hoặc khôi phục một bản sao.",
                style = MaterialTheme.typography.bodySmall
            )
            when (state) {
                SafetyBackupListState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is SafetyBackupListState.Failed -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = enabled) { Text("Thử lại") }
                }
                is SafetyBackupListState.Ready -> {
                    val inventory = state.inventory
                    Text(
                        "Bản sao an toàn: ${inventory.validV2.size} bản • ${formatBytesHelper(inventory.totalValidV2Bytes)}"
                    )
                    Text("Giữ lại: 2 bản safety-v2 hợp lệ mới nhất", style = MaterialTheme.typography.bodySmall)
                    if (inventory.validV2.isEmpty()) Text("Chưa có bản sao an toàn khả dụng.")
                    inventory.validV2.forEach { candidate -> SafetyBackupRow(candidate, enabled, onPreview) }
                    if (inventory.invalidV2Count > 0) {
                        Text("${inventory.invalidV2Count} bản safety-v2 không khả dụng.", color = MaterialTheme.colorScheme.error)
                    }
                    if (inventory.legacy.isNotEmpty()) {
                        Text("Bản sao legacy: ${inventory.legacy.size} (không tự động dọn)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyBackupRow(
    candidate: SafetyBackupCandidate,
    enabled: Boolean,
    onPreview: (SafetyBackupCandidate) -> Unit
) {
    val timestamp = runCatching {
        java.time.Instant.parse(candidate.preview.createdAtUtc)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"))
    }.getOrDefault(candidate.preview.createdAtUtc)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text(timestamp, fontWeight = FontWeight.SemiBold)
        Text(
            "${formatBytesHelper(candidate.fileSizeBytes)} • ${candidate.preview.counts.packages} gói học • " +
                "${candidate.preview.counts.contents} Content • ${candidate.preview.counts.learningItems} LearningItems"
        )
        candidate.preview.packages.forEach { Text(it.packageName, style = MaterialTheme.typography.bodySmall) }
        Text(
            "Media: ${candidate.preview.counts.mediaFiles} file / ${formatBytesHelper(candidate.preview.bytes.mediaBytes)}",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            OutlinedButton(onClick = { onPreview(candidate) }, enabled = enabled) { Text("Xem trước") }
            Button(onClick = { onPreview(candidate) }, enabled = enabled) { Text("Khôi phục") }
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
    isSafetyBackup: Boolean,
    archiveSizeBytes: Long,
    selectedPackageIds: Set<String>,
    onTogglePackage: (String, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (isSafetyBackup) "BẢN SAO AN TOÀN" else "Khôi phục file sao lưu (.lebak)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Thông tin bản sao lưu:", fontWeight = FontWeight.Bold)
                Text("• Nền tảng tạo: ${preview.sourcePlatform.uppercase()} (${preview.appVersion})")
                val previewTime = runCatching {
                    java.time.Instant.parse(preview.createdAtUtc).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"))
                }.getOrDefault(preview.createdAtUtc)
                Text("• Thời gian: $previewTime")
                Text("• Kích thước file: ${formatBytesHelper(archiveSizeBytes)}")
                Text("• Content: ${preview.counts.contents}")
                Text("• LearningItems: ${preview.counts.learningItems}")
                Text("• Tệp media & âm thanh: ${preview.counts.mediaFiles} (${formatBytesHelper(preview.bytes.mediaBytes)})")
                Text("• Lịch sử ôn tập (FSRS): ${preview.counts.reviewEvents} lượt ôn")

                if (preview.packagePreviews.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Chọn gói cần khôi phục (${preview.packagePreviews.size} gói):", fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        preview.packagePreviews.forEach { pkg ->
                            val isChecked = pkg.packageId in selectedPackageIds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onTogglePackage(pkg.packageId, it) }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column {
                                        Text(pkg.packageName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("${pkg.contentCount} thẻ • ${pkg.mediaCount} media", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                val (statusText, statusColor) = when (pkg.status) {
                                    vn.loi.learning.domain.sync.model.PackageCompatibilityStatus.NEW -> "MỚI" to MaterialTheme.colorScheme.primary
                                    vn.loi.learning.domain.sync.model.PackageCompatibilityStatus.PRESENT -> "ĐÃ CÓ" to MaterialTheme.colorScheme.secondary
                                    vn.loi.learning.domain.sync.model.PackageCompatibilityStatus.CONFLICT -> "XUNG ĐỘT" to MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = statusColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Lưu ý: Quá trình khôi phục sẽ ghi đè các gói đã chọn. Một bản sao an toàn (safety backup) sẽ được tự động tạo trước khi áp dụng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedPackageIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.semantics { contentDescription = "Confirm restore" }
            ) {
                Text("Khôi phục (${selectedPackageIds.size} gói)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Hủy")
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

@Composable
private fun DifferentialSyncCard(
    enabled: Boolean,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit
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
                    Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Đồng bộ vi sai (.lesync)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Trao đổi siêu nhẹ: Chỉ chuyển các nội dung chỉnh sửa, audio TTS mới và lượt ôn tập FSRS. Tệp media có sẵn sẽ tự động được tái sử dụng mà không truyền lại.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Button(
                    onClick = onExportSync,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Xuất thay đổi")
                }
                OutlinedButton(
                    onClick = onImportSync,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nạp thay đổi")
                }
            }
        }
    }
}

@Composable
private fun SyncPreviewDialog(
    report: SyncPreviewReport,
    strategy: ConflictResolutionStrategy,
    onStrategyChanged: (ConflictResolutionStrategy) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "Xem trước gói đồng bộ vi sai",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Nguồn gửi: ${report.sourcePlatform.uppercase()}", fontWeight = FontWeight.SemiBold)
                Text("Gói ảnh hưởng: ${report.packagesAffected.joinToString().ifEmpty { "Tất cả" }}")
                Text("• Thẻ từ vựng cập nhật: ${report.contentChangesCount}")
                Text("• Media mới: ${report.newMediaCount} (${formatBytesHelper(report.newMediaBytes)})")
                Text("• Media có sẵn (tái sử dụng): ${report.existingMediaReusedCount} file", color = MaterialTheme.colorScheme.primary)
                Text("• Lượt ôn tập: ${report.reviewEventsCount} mới (${report.reviewEventsDeduplicatedCount} bỏ qua)")

                if (report.requiresFullBackup) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        report.warnings.firstOrNull() ?: "Thiết bị thiếu baseline cho gói này. Khuyến nghị Sao lưu toàn diện trước.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (report.conflicts.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Xung đột phát hiện (${report.conflicts.size}):",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    report.conflicts.forEach { conflict ->
                        Text("• Thẻ ${conflict.entityId}: \"${conflict.localValueSummary}\" ↔ \"${conflict.incomingValueSummary}\"", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Phương thức hòa giải:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = strategy == ConflictResolutionStrategy.MERGE_FIELD_LEVEL,
                            onClick = { onStrategyChanged(ConflictResolutionStrategy.MERGE_FIELD_LEVEL) }
                        )
                        Text("Hòa giải từng trường (Khuyến nghị)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = strategy == ConflictResolutionStrategy.PRESERVE_LOCAL,
                            onClick = { onStrategyChanged(ConflictResolutionStrategy.PRESERVE_LOCAL) }
                        )
                        Text("Giữ bản hiện tại", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = strategy == ConflictResolutionStrategy.APPLY_INCOMING,
                            onClick = { onStrategyChanged(ConflictResolutionStrategy.APPLY_INCOMING) }
                        )
                        Text("Ghi đè bản nhận được", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Áp dụng đồng bộ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Hủy")
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
