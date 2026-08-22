package vn.loi.learning.desktop.ui.sync

import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.desktop.runtime.DesktopRecoveryManager
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopSyncViewModelTest {

    private lateinit var tempDir: java.nio.file.Path
    private lateinit var dataDir: java.nio.file.Path
    private lateinit var applicationContext: LearningApplicationContext
    private lateinit var recoveryManager: DesktopRecoveryManager
    private lateinit var viewModel: DesktopSyncViewModel

    @Before
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

    @After
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
    fun `executeBackup creates lebak archive and updates state`() {
        val targetPath = tempDir.resolve("backup.lebak")
        viewModel.executeBackup(targetPath)

        val state = viewModel.backupState.value
        assertFalse(state.isExporting)
        assertEquals(targetPath.toString(), state.exportSuccessPath)
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

        viewModel.executeRestore(validBackup)
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
