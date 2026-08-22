package vn.loi.learning.desktop.ui.sync

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.desktop.runtime.DesktopRecoveryManager
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

class DesktopSyncViewModel(
    private val applicationContext: LearningApplicationContext,
    private val recoveryManager: DesktopRecoveryManager
) {
    private val _backupState = MutableStateFlow(DesktopBackupDialogState())
    val backupState: StateFlow<DesktopBackupDialogState> = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow(DesktopRestoreDialogState())
    val restoreState: StateFlow<DesktopRestoreDialogState> = _restoreState.asStateFlow()

    private val _syncExportState = MutableStateFlow(DesktopSyncExportDialogState())
    val syncExportState: StateFlow<DesktopSyncExportDialogState> = _syncExportState.asStateFlow()

    private val _syncImportState = MutableStateFlow(DesktopSyncImportDialogState())
    val syncImportState: StateFlow<DesktopSyncImportDialogState> = _syncImportState.asStateFlow()

    fun openBackupDialog() {
        val packages = applicationContext.installedPackageRepository?.findAll().orEmpty().map { pkg ->
            val cardCount = pkg.contentCount
            DesktopPackageItem(
                id = pkg.id.value,
                packageId = pkg.packageId.value,
                name = pkg.name.value,
                version = pkg.version.value,
                cardCount = cardCount,
                isSelected = true
            )
        }
        _backupState.value = DesktopBackupDialogState(
            availablePackages = packages,
            selectAllPackages = true,
            includeLearningProgress = true
        )
    }

    fun toggleBackupSelectAll(selectAll: Boolean) {
        _backupState.value = _backupState.value.copy(
            selectAllPackages = selectAll,
            availablePackages = _backupState.value.availablePackages.map { it.copy(isSelected = selectAll) }
        )
    }

    fun toggleBackupPackage(pkgId: String, selected: Boolean) {
        val updated = _backupState.value.availablePackages.map {
            if (it.id == pkgId || it.packageId == pkgId) it.copy(isSelected = selected) else it
        }
        _backupState.value = _backupState.value.copy(
            availablePackages = updated,
            selectAllPackages = updated.all { it.isSelected }
        )
    }

    fun toggleBackupIncludeProgress(include: Boolean) {
        _backupState.value = _backupState.value.copy(includeLearningProgress = include)
    }

    fun executeBackup(targetPath: Path) {
        _backupState.value = _backupState.value.copy(isExporting = true, errorMessage = null)
        try {
            recoveryManager.createPortableBackupV2(
                target = targetPath,
                descriptor = PortableBackupV2Descriptor(
                    appVersion = "2.0.0",
                    versionCode = 1,
                    sourcePlatform = "desktop",
                    learnerIds = listOf("default-learner")
                )
            )
            _backupState.value = _backupState.value.copy(
                isExporting = false,
                exportSuccessPath = targetPath.toString()
            )
        } catch (e: Exception) {
            _backupState.value = _backupState.value.copy(
                isExporting = false,
                errorMessage = e.message ?: "Failed to create backup."
            )
        }
    }

    fun openRestoreDialog() {
        _restoreState.value = DesktopRestoreDialogState()
    }

    fun stageAndPreviewRestore(sourcePath: Path) {
        _restoreState.value = _restoreState.value.copy(isRestoring = true, errorMessage = null)
        try {
            val preview = recoveryManager.previewPortableBackupV2(sourcePath)
            _restoreState.value = _restoreState.value.copy(
                isRestoring = false,
                stagedFilePath = sourcePath.toString(),
                preview = preview
            )
        } catch (e: Exception) {
            _restoreState.value = _restoreState.value.copy(
                isRestoring = false,
                errorMessage = e.message ?: "Failed to preview backup archive."
            )
        }
    }

    fun executeRestore(sourcePath: Path) {
        _restoreState.value = _restoreState.value.copy(isRestoring = true, errorMessage = null)
        try {
            val result = recoveryManager.restorePortableBackupV2(sourcePath, operationActive = false)
            when (result) {
                is PortableBackupV2RestoreResult.Success -> {
                    _restoreState.value = _restoreState.value.copy(
                        isRestoring = false,
                        restoreSuccessSummary = "Khôi phục thành công ${result.restoredEntriesCount} mục. Bản sao an toàn lưu tại: ${result.safetyBackupPath}"
                    )
                }
                else -> {
                    _restoreState.value = _restoreState.value.copy(
                        isRestoring = false,
                        errorMessage = "Khôi phục thất bại: ${result.message}"
                    )
                }
            }
        } catch (e: Exception) {
            _restoreState.value = _restoreState.value.copy(
                isRestoring = false,
                errorMessage = e.message ?: "Failed to restore backup."
            )
        }
    }

    fun openSyncExportDialog() {
        val packages = applicationContext.installedPackageRepository?.findAll().orEmpty().map { pkg ->
            DesktopPackageItem(
                id = pkg.id.value,
                packageId = pkg.packageId.value,
                name = pkg.name.value,
                version = pkg.version.value,
                cardCount = pkg.contentCount,
                isSelected = true
            )
        }
        _syncExportState.value = DesktopSyncExportDialogState(
            availablePackages = packages,
            selectedPackageIds = packages.map { it.packageId }.toSet(),
            includeReviewEvents = true
        )
    }

    fun toggleSyncExportPackage(pkgId: String) {
        val current = _syncExportState.value.selectedPackageIds
        val updated = if (pkgId in current) current - pkgId else current + pkgId
        _syncExportState.value = _syncExportState.value.copy(selectedPackageIds = updated)
    }

    fun toggleSyncExportIncludeReviews(include: Boolean) {
        _syncExportState.value = _syncExportState.value.copy(includeReviewEvents = include)
    }

    fun executeSyncExport(targetPath: Path) {
        _syncExportState.value = _syncExportState.value.copy(isExporting = true, errorMessage = null)
        try {
            val engine = requireNotNull(applicationContext.syncEngine) { "SyncEngine is not initialized." }
            val selected = _syncExportState.value.selectedPackageIds
            engine.exportSyncPackage(
                target = targetPath,
                sourcePlatform = "desktop",
                specificPackageIds = selected,
                includeReviewEvents = _syncExportState.value.includeReviewEvents
            )
            _syncExportState.value = _syncExportState.value.copy(
                isExporting = false,
                exportSuccessPath = targetPath.toString()
            )
        } catch (e: Exception) {
            _syncExportState.value = _syncExportState.value.copy(
                isExporting = false,
                errorMessage = e.message ?: "Failed to export sync package."
            )
        }
    }

    fun openSyncImportDialog() {
        _syncImportState.value = DesktopSyncImportDialogState()
    }

    fun stageAndPreviewSyncImport(sourcePath: Path) {
        _syncImportState.value = _syncImportState.value.copy(isImporting = true, errorMessage = null)
        try {
            val engine = requireNotNull(applicationContext.syncEngine) { "SyncEngine is not initialized." }
            val report = engine.generatePreviewReport(sourcePath)
            _syncImportState.value = _syncImportState.value.copy(
                isImporting = false,
                stagedFilePath = sourcePath.toString(),
                previewReport = report
            )
        } catch (e: Exception) {
            _syncImportState.value = _syncImportState.value.copy(
                isImporting = false,
                errorMessage = e.message ?: "Failed to preview sync package."
            )
        }
    }

    fun setConflictStrategy(strategy: ConflictResolutionStrategy) {
        _syncImportState.value = _syncImportState.value.copy(conflictStrategy = strategy)
    }

    fun executeSyncImport(sourcePath: Path) {
        _syncImportState.value = _syncImportState.value.copy(isImporting = true, errorMessage = null)
        try {
            val engine = requireNotNull(applicationContext.syncEngine) { "SyncEngine is not initialized." }
            val result = engine.importSyncPackage(sourcePath, _syncImportState.value.conflictStrategy)
            _syncImportState.value = _syncImportState.value.copy(
                isImporting = false,
                importSuccessSummary = result
            )
        } catch (e: Exception) {
            _syncImportState.value = _syncImportState.value.copy(
                isImporting = false,
                errorMessage = e.message ?: "Failed to import sync package."
            )
        }
    }
}
