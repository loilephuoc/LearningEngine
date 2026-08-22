package vn.loi.learning.desktop.ui.sync

import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopRecoveryManager
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.recovery.PortableBackupPhaseV2
import vn.loi.learning.infrastructure.recovery.PortableBackupProgressV2

class DesktopSyncViewModelTest {

    @Test
    fun `backup progress is indeterminate until a real metric advances`() {
        assertFalse(PortableBackupProgressV2(
            phase = PortableBackupPhaseV2.CALCULATING_MEDIA,
            totalItems = 16_627
        ).hasMeasurableBackupProgress())
        assertTrue(PortableBackupProgressV2(
            phase = PortableBackupPhaseV2.CALCULATING_MEDIA,
            processedItems = 1,
            totalItems = 16_627
        ).hasMeasurableBackupProgress())
    }

    private lateinit var tempDir: java.nio.file.Path
    private lateinit var dataDir: java.nio.file.Path
    private lateinit var applicationContext: LearningApplicationContext
    private lateinit var recoveryManager: DesktopRecoveryManager
    private lateinit var viewModel: DesktopSyncViewModel

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("desktop_sync_vm_test_")
        dataDir = tempDir.resolve("data")
        val mediaDir = dataDir.resolve("media")
        Files.createDirectories(dataDir)
        Files.createDirectories(mediaDir)

        applicationContext = LearningApplicationFactory.createPersisted(dataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        recoveryManager = DesktopRecoveryManager(dataDir, tempDir.resolve("config"))
        viewModel = DesktopSyncViewModel(applicationContext, recoveryManager)
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `openBackupDialog initializes state and supports package selection toggles`() {
        viewModel.openBackupDialog()
        val initial = viewModel.backupState.value
        assertTrue(initial.selectAllPackages)
        assertTrue(initial.includeLearningProgress)
        assertFalse(initial.isExporting)
        assertNull(initial.exportSuccessPath)

        viewModel.toggleBackupSelectAll(false)
        assertFalse(viewModel.backupState.value.selectAllPackages)

        viewModel.toggleBackupIncludeProgress(false)
        assertFalse(viewModel.backupState.value.includeLearningProgress)
    }

    @Test
    fun `refreshBackupPreview publishes read only scope estimates`() {
        viewModel.openBackupDialog()
        viewModel.refreshBackupPreview()

        val state = viewModel.backupState.value
        assertFalse(state.isPreviewing)
        assertNotNull(state.preview)
        assertEquals(0, state.preview?.counts?.packages)
        assertEquals(0, state.preview?.estimatedTotalBytes)
        assertNull(state.errorMessage)
    }

    @Test
    fun `portable backup filename uses readable local timestamp and safe scope`() {
        assertEquals(
            "LearningEngine_Backup_Vocabulary_In_Use_Upper_Intermediate_20260822_095012.lebak",
            portableBackupFileName(
                listOf("Vocabulary_In_Use_Upper_Intermediate"),
                LocalDateTime.of(2026, 8, 22, 9, 50, 12)
            )
        )
        assertEquals(
            "LearningEngine_Backup_3Packages_20260822_095012.lebak",
            portableBackupFileName(listOf("A", "B", "C"), LocalDateTime.of(2026, 8, 22, 9, 50, 12))
        )
    }

    @Test
    fun `executeBackup creates lebak archive and updates state`() {
        val targetPath = tempDir.resolve("backup.lebak")
        viewModel.executeBackup(targetPath)

        val state = viewModel.backupState.value
        assertFalse(state.isExporting)
        assertEquals(targetPath.toString(), state.exportSuccessPath)
        assertNotNull(state.successReport)
        assertTrue(state.successReport?.verificationPassed == true)
        assertTrue(state.progress?.phase == vn.loi.learning.infrastructure.recovery.PortableBackupPhaseV2.COMPLETED)
        assertNull(state.errorMessage)
        assertTrue(Files.exists(targetPath))
    }

    @Test
    fun `openRestoreDialog and stageAndPreviewRestore loads valid backup preview`() {
        val validBackup = tempDir.resolve("preview-test.lebak")
        viewModel.executeBackup(validBackup)

        viewModel.openRestoreDialog()
        assertEquals(DesktopRestoreDialogState(), viewModel.restoreState.value)

        viewModel.stageAndPreviewRestore(validBackup)
        val state = viewModel.restoreState.value
        assertFalse(state.isRestoring)
        assertNotNull(state.preview)
        assertEquals("desktop", state.preview?.sourcePlatform)
        assertNull(state.errorMessage)

        viewModel.executeRestore(validBackup, fullReplacement = true)
        val restoreResultState = viewModel.restoreState.value
        assertNotNull(restoreResultState.restoreSuccessSummary)
    }

    @Test
    fun `openSyncExportDialog and executeSyncExport creates lesync changeset`() {
        viewModel.openSyncExportDialog()
        val initial = viewModel.syncExportState.value
        assertTrue(initial.includeReviewEvents)

        val syncTarget = tempDir.resolve("sync-test.lesync")
        viewModel.executeSyncExport(syncTarget)

        val state = viewModel.syncExportState.value
        assertFalse(state.isExporting)
        assertEquals(syncTarget.toString(), state.exportSuccessPath)
        assertNull(state.errorMessage)
        assertTrue(Files.exists(syncTarget))
    }

    @Test
    fun `stageAndPreviewSyncImport generates report and executeSyncImport merges safely`() {
        val syncTarget = tempDir.resolve("sync-import-test.lesync")
        viewModel.executeSyncExport(syncTarget)

        viewModel.openSyncImportDialog()
        viewModel.stageAndPreviewSyncImport(syncTarget)

        val previewState = viewModel.syncImportState.value
        assertNotNull(previewState.previewReport)
        assertEquals("desktop", previewState.previewReport?.sourcePlatform)

        viewModel.setConflictStrategy(ConflictResolutionStrategy.MERGE_FIELD_LEVEL)
        viewModel.executeSyncImport(syncTarget)

        val importState = viewModel.syncImportState.value
        assertNotNull(importState.importSuccessSummary)
        assertTrue(importState.importSuccessSummary!!.success)
    }
}
