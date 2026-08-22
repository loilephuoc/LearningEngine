package vn.loi.learning.android.recovery

import androidx.lifecycle.SavedStateHandle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.platform.AndroidPlatformDirectories
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager
import vn.loi.learning.infrastructure.recovery.PortableBackupCountsV2
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Preview
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateDefaultBackupFilename generates timestamped lebak format`() {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            val filename = viewModel.generateDefaultBackupFilename()
            assertTrue(filename.startsWith("LearningEngine_Backup_"))
            assertTrue(filename.endsWith(".lebak"))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `createBackup creates archive, reads preview, writes output, and transitions to BackupSuccess`() = runTest {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            val outputStream = ByteArrayOutputStream()
            viewModel.createBackup(fixture.contextDir) { outputStream }
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.BackupSuccess>(state)
            assertTrue(outputStream.size() > 0)
            assertEquals("LearningEngine_Backup_", state.summary.fileName.substring(0, 22))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `createBackup failure transitions to BackupFailure`() = runTest {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            // Passing null output stream causes failure
            viewModel.createBackup(fixture.contextDir) { null }
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.BackupFailure>(state)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `stageAndPreviewRestore copies stream to staged file and transitions to PreviewReady`() = runTest {
        val fixture = createFixture()
        try {
            val validBackup = fixture.tempDir.resolve("valid.lebak")
            fixture.graph.createPortableBackup(validBackup)
            val backupBytes = Files.readAllBytes(validBackup)

            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.stageAndPreviewRestore(fixture.contextDir) { ByteArrayInputStream(backupBytes) }
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.PreviewReady>(state)
            assertEquals(2, state.preview.backupSchemaVersion)
            assertTrue(state.stagedFile.exists())

            // Cancel preview deletes staged file
            viewModel.cancelPreview(state.stagedFile)
            assertFalse(state.stagedFile.exists())
            assertEquals(BackupRestoreUiState.Idle, viewModel.state.value)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `stageAndPreviewRestore failure on corrupted stream transitions to PreviewFailure`() = runTest {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.stageAndPreviewRestore(fixture.contextDir) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.PreviewFailure>(state)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `safety backup discovery and preview use validated internal source without deleting it`() = runTest {
        val fixture = createFixture()
        try {
            val safetyFile = fixture.graph.directories.backupDirectory.resolve("safety-v2-1000.lebak")
            fixture.graph.recovery.createPortableBackupV2(
                safetyFile,
                PortableBackupV2Descriptor("safety-pre-restore", sourcePlatform = "safety")
            )
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            testScheduler.advanceUntilIdle()

            val list = assertIs<SafetyBackupListState.Ready>(viewModel.safetyBackups.value)
            val candidate = list.inventory.validV2.single()
            viewModel.previewSafetyBackup(candidate)
            testScheduler.advanceUntilIdle()

            val preview = assertIs<BackupRestoreUiState.PreviewReady>(viewModel.state.value)
            assertTrue(preview.isSafetyBackup)
            assertFalse(preview.deleteSourceAfterUse)
            assertEquals(safetyFile.toAbsolutePath().normalize(), preview.stagedFile.toPath().toAbsolutePath().normalize())
            viewModel.cancelPreview(preview.stagedFile, preview.deleteSourceAfterUse)
            assertTrue(Files.exists(safetyFile))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `confirmRestore invokes restore and transitions to RestoreSuccess`() = runTest {
        val fixture = createFixture()
        try {
            val validBackup = fixture.tempDir.resolve("to-restore.lebak")
            fixture.graph.createPortableBackup(validBackup)
            val expectedCounts = fixture.graph.previewPortableBackup(validBackup).counts
            val stagedFile = File.createTempFile("staged_", ".lebak", fixture.contextDir)
            Files.copy(validBackup, stagedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.confirmRestore(stagedFile)
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.RestoreSuccess>(state)
            assertEquals(expectedCounts, state.restoredCounts)
            assertFalse(stagedFile.exists())
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `confirmRestore maps typed failure results accurately`() = runTest {
        val fixture = createFixture()
        try {
            val dummyFile = File.createTempFile("dummy_", ".lebak", fixture.contextDir)
            Files.write(dummyFile.toPath(), byteArrayOf(0, 0, 0))

            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.confirmRestore(dummyFile)
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.RestoreFailure>(state)
            assertIs<PortableBackupV2RestoreResult.ValidationFailed>(state.result)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `exportSync creates lesync archive and transitions to SyncExportSuccess`() = runTest {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            val outputStream = ByteArrayOutputStream()
            viewModel.exportSync(fixture.contextDir) { outputStream }
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertIs<BackupRestoreUiState.SyncExportSuccess>(state)
            assertTrue(outputStream.size() > 0)
            assertTrue(state.summary.fileName.startsWith("LearningEngine_Sync_"))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `stageAndPreviewSync reads report and confirmSyncImport applies changeset`() = runTest {
        val fixture = createFixture()
        try {
            val syncFile = fixture.tempDir.resolve("test.lesync")
            fixture.graph.exportSync(syncFile)
            val syncBytes = Files.readAllBytes(syncFile)

            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.stageAndPreviewSync(fixture.contextDir) { ByteArrayInputStream(syncBytes) }
            testScheduler.advanceUntilIdle()

            val previewState = viewModel.state.value
            assertIs<BackupRestoreUiState.SyncPreviewReady>(previewState)
            assertTrue(previewState.stagedFile.exists())

            viewModel.confirmSyncImport(previewState.stagedFile, previewState.conflictStrategy)
            testScheduler.advanceUntilIdle()

            val importState = viewModel.state.value
            assertIs<BackupRestoreUiState.SyncImportSuccess>(importState)
            assertTrue(importState.summary.success)
            assertFalse(previewState.stagedFile.exists())
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun `dismissResult resets state to Idle`() {
        val fixture = createFixture()
        try {
            val viewModel = BackupRestoreViewModel(
                graphProvider = { fixture.graph },
                savedState = SavedStateHandle(),
                ioDispatcher = dispatcher
            )
            viewModel.dismissResult()
            assertEquals(BackupRestoreUiState.Idle, viewModel.state.value)
        } finally {
            fixture.cleanup()
        }
    }

    private class TestFixture(
        val tempDir: Path,
        val contextDir: File,
        val graph: AndroidApplicationGraph
    ) {
        fun cleanup() {
            tempDir.toFile().deleteRecursively()
            contextDir.deleteRecursively()
        }
    }

    private fun createFixture(): TestFixture {
        val temp = Files.createTempDirectory("backup_restore_vm_test_")
        val contextDir = Files.createTempDirectory("context_dir_").toFile()
        val dataDir = temp.resolve("data")
        val mediaDir = dataDir.resolve("media")
        val importsDir = temp.resolve("imports")
        Files.createDirectories(dataDir)
        Files.createDirectories(mediaDir)
        Files.createDirectories(importsDir)

        val directories = AndroidPlatformDirectories(dataDir, mediaDir, importsDir)
        val engine = LearningApplicationFactory.createPersisted(dataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        val media = JvmContentMediaStorage(mediaDir)
        val recovery = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to dataDir, "media" to mediaDir),
            safetyDirectory = directories.backupDirectory
        )
        val graph = AndroidApplicationGraph(engine, media, recovery, directories)
        return TestFixture(temp, contextDir, graph)
    }
}
