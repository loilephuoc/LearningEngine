package vn.loi.learning.desktop.ui.sync

import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.domain.sync.model.SyncPreviewReport
import vn.loi.learning.domain.sync.model.SyncResultSummary
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview

data class DesktopPackageItem(
    val id: String,
    val packageId: String,
    val name: String,
    val version: String,
    val cardCount: Int,
    val isSelected: Boolean = true
)

data class DesktopBackupDialogState(
    val availablePackages: List<DesktopPackageItem> = emptyList(),
    val selectAllPackages: Boolean = true,
    val includeLearningProgress: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccessPath: String? = null,
    val errorMessage: String? = null
)

data class DesktopRestoreDialogState(
    val stagedFilePath: String? = null,
    val preview: PortableBackupV2Preview? = null,
    val newPackages: List<String> = emptyList(),
    val existingPackages: List<String> = emptyList(),
    val isRestoring: Boolean = false,
    val restoreSuccessSummary: String? = null,
    val errorMessage: String? = null
)

data class DesktopSyncExportDialogState(
    val availablePackages: List<DesktopPackageItem> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
    val includeReviewEvents: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccessPath: String? = null,
    val errorMessage: String? = null
)

data class DesktopSyncImportDialogState(
    val stagedFilePath: String? = null,
    val previewReport: SyncPreviewReport? = null,
    val conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.MERGE_FIELD_LEVEL,
    val isImporting: Boolean = false,
    val importSuccessSummary: SyncResultSummary? = null,
    val errorMessage: String? = null
)
