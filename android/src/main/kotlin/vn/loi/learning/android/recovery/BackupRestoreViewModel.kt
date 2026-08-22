package vn.loi.learning.android.recovery

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.domain.sync.model.SyncPreviewReport
import vn.loi.learning.domain.sync.model.SyncResultSummary
import vn.loi.learning.infrastructure.recovery.PortableBackupCountsV2
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

data class BackupSuccessSummary(
    val fileName: String,
    val fileSizeFormatted: String,
    val counts: PortableBackupCountsV2,
    val createdAtUtc: String
)

data class SyncExportSuccessSummary(
    val fileName: String,
    val fileSizeFormatted: String,
    val contentDeltasCount: Int,
    val reviewEventsCount: Int,
    val mediaCount: Int
)

sealed interface BackupRestoreUiState {
    data object Idle : BackupRestoreUiState
    data class BackingUp(val message: String = "Creating full backup...") : BackupRestoreUiState
    data class BackupSuccess(val summary: BackupSuccessSummary) : BackupRestoreUiState
    data class BackupFailure(val message: String) : BackupRestoreUiState

    data class Previewing(val message: String = "Analyzing backup file...") : BackupRestoreUiState
    data class PreviewReady(val preview: PortableBackupV2Preview, val stagedFile: File) : BackupRestoreUiState
    data class PreviewFailure(val message: String) : BackupRestoreUiState

    data class Restoring(val message: String = "Restoring backup... Please do not close the app.") : BackupRestoreUiState
    data class RestoreSuccess(
        val restoredEntriesCount: Int,
        val safetyBackupPath: String,
        val appVersion: String
    ) : BackupRestoreUiState
    data class RestoreFailure(val result: PortableBackupV2RestoreResult) : BackupRestoreUiState

    // Differential Sync States
    data class SyncExporting(val message: String = "Exporting differential sync changeset...") : BackupRestoreUiState
    data class SyncExportSuccess(val summary: SyncExportSuccessSummary) : BackupRestoreUiState
    data class SyncExportFailure(val message: String) : BackupRestoreUiState

    data class SyncPreviewing(val message: String = "Analyzing sync changeset...") : BackupRestoreUiState
    data class SyncPreviewReady(
        val report: SyncPreviewReport,
        val stagedFile: File,
        val conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.MERGE_FIELD_LEVEL
    ) : BackupRestoreUiState
    data class SyncPreviewFailure(val message: String) : BackupRestoreUiState

    data class SyncImporting(val message: String = "Applying sync changeset...") : BackupRestoreUiState
    data class SyncImportSuccess(val summary: SyncResultSummary) : BackupRestoreUiState
    data class SyncImportFailure(val message: String) : BackupRestoreUiState
}

class BackupRestoreViewModel(
    private val graphProvider: () -> AndroidApplicationGraph,
    private val savedState: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val mutableState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val state: StateFlow<BackupRestoreUiState> = mutableState.asStateFlow()

    private var activeJob: Job? = null

    fun generateDefaultBackupFilename(): String {
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))
        return "LearningEngine_Backup_$formatted.lebak"
    }

    fun generateDefaultSyncFilename(): String {
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))
        return "LearningEngine_Sync_$formatted.lesync"
    }

    fun createBackup(cacheDir: File, openOutputStream: () -> OutputStream?) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.BackingUp || currentState is BackupRestoreUiState.Restoring || currentState is BackupRestoreUiState.SyncExporting || currentState is BackupRestoreUiState.SyncImporting) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.BackingUp("Preparing snapshot and creating backup...")
        activeJob = viewModelScope.launch {
            try {
                val summary = withContext(ioDispatcher) {
                    val tempFile = File.createTempFile("backup_v2_", ".lebak", cacheDir)
                    tempFile.delete()
                    try {
                        val graph = graphProvider()
                        graph.createPortableBackup(tempFile.toPath())
                        val preview = graph.previewPortableBackup(tempFile.toPath())
                        val out = openOutputStream() ?: throw IllegalStateException("Cannot open destination file.")
                        out.use { output ->
                            Files.newInputStream(tempFile.toPath()).use { input ->
                                input.copyTo(output)
                            }
                        }
                        val formattedSize = formatBytes(tempFile.length())
                        BackupSuccessSummary(
                            fileName = generateDefaultBackupFilename(),
                            fileSizeFormatted = formattedSize,
                            counts = preview.counts,
                            createdAtUtc = preview.createdAtUtc
                        )
                    } finally {
                        tempFile.delete()
                    }
                }
                mutableState.value = BackupRestoreUiState.BackupSuccess(summary)
            } catch (e: Exception) {
                mutableState.value = BackupRestoreUiState.BackupFailure(e.message ?: "Failed to create backup.")
            }
        }
    }

    fun stageAndPreviewRestore(cacheDir: File, openInputStream: () -> InputStream?) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.Restoring || currentState is BackupRestoreUiState.Previewing || currentState is BackupRestoreUiState.SyncImporting) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.Previewing("Reading and verifying backup archive...")
        activeJob = viewModelScope.launch {
            var stagedFile: File? = null
            try {
                val preview = withContext(ioDispatcher) {
                    val input = openInputStream() ?: throw IllegalStateException("Cannot read selected file.")
                    stagedFile = File.createTempFile("restore_staged_", ".lebak", cacheDir)
                    val target = stagedFile!!.toPath()
                    input.use { inputStream ->
                        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    val graph = graphProvider()
                    graph.previewPortableBackup(target)
                }
                mutableState.value = BackupRestoreUiState.PreviewReady(preview, stagedFile!!)
            } catch (e: Exception) {
                stagedFile?.delete()
                mutableState.value = BackupRestoreUiState.PreviewFailure(e.message ?: "Invalid or corrupted backup file.")
            }
        }
    }

    fun confirmRestore(stagedFile: File) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.Restoring) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.Restoring("Restoring learning data... Please do not close or exit.")
        activeJob = viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    try {
                        val graph = graphProvider()
                        graph.restorePortableBackup(stagedFile.toPath(), operationActive = false)
                    } finally {
                        stagedFile.delete()
                    }
                }
                when (result) {
                    is PortableBackupV2RestoreResult.Success -> {
                        mutableState.value = BackupRestoreUiState.RestoreSuccess(
                            restoredEntriesCount = result.restoredEntriesCount,
                            safetyBackupPath = result.safetyBackupPath,
                            appVersion = result.appVersion
                        )
                    }
                    else -> {
                        mutableState.value = BackupRestoreUiState.RestoreFailure(result)
                    }
                }
            } catch (e: Exception) {
                mutableState.value = BackupRestoreUiState.RestoreFailure(
                    PortableBackupV2RestoreResult.RestoreFailedRolledBack(
                        message = "Restore failed: ${e.message}",
                        safetyBackupPath = "",
                        failureReason = e.message ?: "Unknown error"
                    )
                )
            }
        }
    }

    // --- Differential Sync Actions ---

    fun exportSync(
        cacheDir: File,
        specificPackageIds: Set<String>? = null,
        knownRemoteMediaHashes: Set<String> = emptySet(),
        openOutputStream: () -> OutputStream?
    ) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.SyncExporting || currentState is BackupRestoreUiState.Restoring || currentState is BackupRestoreUiState.SyncImporting) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.SyncExporting("Packaging differential changes (.lesync)...")
        activeJob = viewModelScope.launch {
            try {
                val summary = withContext(ioDispatcher) {
                    val tempFile = File.createTempFile("sync_export_", ".lesync", cacheDir)
                    tempFile.delete()
                    try {
                        val graph = graphProvider()
                        graph.exportSync(
                            target = tempFile.toPath(),
                            specificPackageIds = specificPackageIds,
                            knownRemoteMediaHashes = knownRemoteMediaHashes,
                            includeReviewEvents = true
                        )
                        val preview = graph.previewSync(tempFile.toPath())
                        val out = openOutputStream() ?: throw IllegalStateException("Cannot open destination file.")
                        out.use { output ->
                            Files.newInputStream(tempFile.toPath()).use { input ->
                                input.copyTo(output)
                            }
                        }
                        SyncExportSuccessSummary(
                            fileName = generateDefaultSyncFilename(),
                            fileSizeFormatted = formatBytes(tempFile.length()),
                            contentDeltasCount = preview.manifest.contentDeltasCount,
                            reviewEventsCount = preview.manifest.reviewEventsCount,
                            mediaCount = preview.manifest.mediaAssetsCount
                        )
                    } finally {
                        tempFile.delete()
                    }
                }
                mutableState.value = BackupRestoreUiState.SyncExportSuccess(summary)
            } catch (e: Exception) {
                mutableState.value = BackupRestoreUiState.SyncExportFailure(e.message ?: "Failed to export sync package.")
            }
        }
    }

    fun stageAndPreviewSync(cacheDir: File, openInputStream: () -> InputStream?) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.SyncPreviewing || currentState is BackupRestoreUiState.SyncImporting || currentState is BackupRestoreUiState.Restoring) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.SyncPreviewing("Analyzing sync package (.lesync)...")
        activeJob = viewModelScope.launch {
            var stagedFile: File? = null
            try {
                val report = withContext(ioDispatcher) {
                    val input = openInputStream() ?: throw IllegalStateException("Cannot read selected sync file.")
                    stagedFile = File.createTempFile("sync_staged_", ".lesync", cacheDir)
                    val target = stagedFile!!.toPath()
                    input.use { inputStream ->
                        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    val graph = graphProvider()
                    val engine = requireNotNull(graph.engine.syncEngine) { "SyncEngine is not initialized." }
                    engine.generatePreviewReport(target)
                }
                mutableState.value = BackupRestoreUiState.SyncPreviewReady(
                    report = report,
                    stagedFile = stagedFile!!,
                    conflictStrategy = ConflictResolutionStrategy.MERGE_FIELD_LEVEL
                )
            } catch (e: Exception) {
                stagedFile?.delete()
                mutableState.value = BackupRestoreUiState.SyncPreviewFailure(e.message ?: "Invalid or corrupted sync file.")
            }
        }
    }

    fun setSyncConflictStrategy(strategy: ConflictResolutionStrategy) {
        val current = mutableState.value
        if (current is BackupRestoreUiState.SyncPreviewReady) {
            mutableState.value = current.copy(conflictStrategy = strategy)
        }
    }

    fun confirmSyncImport(stagedFile: File, strategy: ConflictResolutionStrategy) {
        val currentState = mutableState.value
        if (currentState is BackupRestoreUiState.SyncImporting || currentState is BackupRestoreUiState.Restoring) {
            return
        }
        if (activeJob?.isActive == true) return

        mutableState.value = BackupRestoreUiState.SyncImporting("Applying differential sync changes...")
        activeJob = viewModelScope.launch {
            try {
                val summary = withContext(ioDispatcher) {
                    try {
                        val graph = graphProvider()
                        graph.importSync(stagedFile.toPath(), strategy)
                    } finally {
                        stagedFile.delete()
                    }
                }
                mutableState.value = BackupRestoreUiState.SyncImportSuccess(summary)
            } catch (e: Exception) {
                mutableState.value = BackupRestoreUiState.SyncImportFailure(e.message ?: "Failed to apply sync changes.")
            }
        }
    }

    fun cancelPreview(stagedFile: File?) {
        stagedFile?.delete()
        if (mutableState.value is BackupRestoreUiState.PreviewReady || mutableState.value is BackupRestoreUiState.PreviewFailure ||
            mutableState.value is BackupRestoreUiState.SyncPreviewReady || mutableState.value is BackupRestoreUiState.SyncPreviewFailure) {
            mutableState.value = BackupRestoreUiState.Idle
        }
    }

    fun dismissResult() {
        mutableState.value = BackupRestoreUiState.Idle
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
