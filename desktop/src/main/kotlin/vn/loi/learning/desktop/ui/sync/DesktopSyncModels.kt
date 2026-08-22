package vn.loi.learning.desktop.ui.sync

import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.domain.sync.model.SyncPreviewReport
import vn.loi.learning.domain.sync.model.SyncResultSummary
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview
import vn.loi.learning.infrastructure.recovery.PortableBackupCreationPlanV2
import vn.loi.learning.infrastructure.recovery.PortableBackupProgressV2

data class DesktopBackupSuccessReport(
    val path: String,
    val archiveBytes: Long,
    val packageCount: Long,
    val contentCount: Long,
    val mediaFileCount: Long,
    val mediaBytes: Long,
    val expandedBytes: Long,
    val compressionRatio: Double,
    val verificationPassed: Boolean
)

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
    val preview: PortableBackupCreationPlanV2? = null,
    val isPreviewing: Boolean = false,
    val isExporting: Boolean = false,
    val progress: PortableBackupProgressV2? = null,
    val elapsedMillis: Long = 0,
    val successReport: DesktopBackupSuccessReport? = null,
    val exportSuccessPath: String? = null,
    val errorMessage: String? = null
)

data class DesktopRestoreDialogState(
    val stagedFilePath: String? = null,
    val preview: PortableBackupV2Preview? = null,
    val packagePreviews: List<vn.loi.learning.domain.sync.model.PackageRestorePreviewItem> = emptyList(),
    val selectedPackageIds: Set<String> = emptySet(),
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
