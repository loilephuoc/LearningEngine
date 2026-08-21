package vn.loi.learning.android.platform

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.android.recording.VoiceRecordingItem
import vn.loi.learning.application.contentpackaging.PackageImportResult
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidPortableBackupSnapshotTest {
    @Test
    fun `typed preferences preserve durable values and exclude transient runtime state`() {
        val encoded = AndroidPortableBackupSnapshot.encodePreferences(
            mapOf(
                "learning-engine-study" to mapOf("daily.new" to 42, "unknown.runtime" to "drop"),
                "learning_engine_reminder_difficult_prefs" to mapOf("difficult.content.ids" to setOf("c-2", "c-1")),
                "learning_engine_reminder_prefs" to mapOf(
                    "reminder.enabled" to true,
                    "reminder.paused_until_epoch_millis" to 99L,
                    "reminder.unlocked_paused_until_epoch_millis" to 88L,
                    "home_widget.current_candidate_id" to "candidate"
                ),
                "learning-engine-autoplay" to mapOf("autoplay.direction" to "FORWARD", "autoplay.sleep_timer_minutes" to 30)
            ),
            hasBackground = true
        ).toString()

        assertContains(encoded, "daily.new")
        assertContains(encoded, "difficult.content.ids")
        assertContains(encoded, "c-1")
        assertContains(encoded, "reminder.enabled")
        assertContains(encoded, AndroidPortableBackupSnapshot.BACKGROUND_PATH)
        listOf(
            "unknown.runtime", "paused_until_epoch_millis", "unlocked_paused_until_epoch_millis",
            "current_candidate_id", "sleep_timer_minutes", "shuffle"
        ).forEach { assertFalse(it in encoded, it) }
    }

    @Test
    fun `portable recording index preserves metadata without device absolute path`() {
        val deviceRoot = createTempDirectory("device-A-recording-")
        try {
            val absolute = deviceRoot.resolve("quick_voice/voice.m4a").toAbsolutePath().toString()
            val encoded = AndroidPortableBackupSnapshot.encodeRecordingIndex(
                listOf(VoiceRecordingItem("recording-id", "voice.m4a", absolute, 1234L, 5678L, 90L))
            ).toString()
            assertContains(encoded, "recording-id")
            assertContains(encoded, "files/voice.m4a")
            assertContains(encoded, "1234")
            assertContains(encoded, "5678")
            assertContains(encoded, "90")
            assertFalse(absolute in encoded)
            assertTrue("filePath" !in encoded)
        } finally {
            deviceRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `recording repository snapshots completed files safely and coordinates lock`() {
        val deviceRoot = createTempDirectory("device-repo-test-")
        try {
            val repoDir = deviceRoot.resolve("app_files")
            val targetStaging = deviceRoot.resolve("staging_target")
            val repository = vn.loi.learning.android.recording.QuickVoiceRecordingRepository.forTesting(repoDir.toFile())
            val audioFile = repoDir.resolve("recordings/quick_voice/rec_1.m4a")
            Files.createDirectories(audioFile.parent)
            Files.write(audioFile, byteArrayOf(1, 2, 3, 4, 5))

            kotlinx.coroutines.runBlocking {
                repository.save(VoiceRecordingItem("r-1", "rec_1.m4a", audioFile.toAbsolutePath().toString(), 100L, 500L, 5L))
            }

            val snapshotted = repository.snapshotCompletedForBackup(targetStaging)
            kotlin.test.assertEquals(1, snapshotted.size)
            kotlin.test.assertEquals("rec_1.m4a", snapshotted[0].filename)
            assertTrue(Files.exists(targetStaging.resolve("rec_1.m4a")))
            kotlin.test.assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), Files.readAllBytes(targetStaging.resolve("rec_1.m4a")))
        } finally {
            deviceRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `preference snapshot gate coordinates exclusive snapshot access`() {
        var counter = 0
        val result = AndroidPreferenceSnapshotGate.snapshot {
            counter += 1
            AndroidPreferenceSnapshotGate.mutation {
                counter += 2
            }
            counter
        }
        kotlin.test.assertEquals(3, result)
    }

    @Test
    fun `recording repository restores into new device root and reconstructs local absolute path`() {
        val deviceRoot = createTempDirectory("device-b-root-")
        val staging = createTempDirectory("staging-root-")
        try {
            val stagingFiles = staging.resolve("android/recordings/files")
            Files.createDirectories(stagingFiles)
            val audioFile = stagingFiles.resolve("rec_restore.m4a")
            Files.write(audioFile, byteArrayOf(9, 8, 7))

            val stagingIndex = staging.resolve("android/recordings/index.json")
            Files.write(
                stagingIndex,
                """
                {
                    "schemaVersion": 1,
                    "recordings": [
                        {
                            "id": "rec_restored_1",
                            "filename": "rec_restore.m4a",
                            "recordingFile": "files/rec_restore.m4a",
                            "createdAt": 1000,
                            "durationMs": 2000,
                            "sizeBytes": 3
                        }
                    ]
                }
                """.trimIndent().toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            )

            val repo = vn.loi.learning.android.recording.QuickVoiceRecordingRepository.forTesting(deviceRoot.toFile())
            repo.restoreRecordingsFromBackup(staging.resolve("android/recordings"))

            val all = kotlinx.coroutines.runBlocking { repo.getAll() }
            kotlin.test.assertEquals(1, all.size)
            val restored = all[0]
            kotlin.test.assertEquals("rec_restored_1", restored.id)
            kotlin.test.assertEquals("rec_restore.m4a", restored.filename)
            val expectedPath = java.io.File(deviceRoot.toFile(), "recordings/quick_voice/rec_restore.m4a").absolutePath
            kotlin.test.assertEquals(expectedPath, restored.filePath)
            assertTrue(restored.exists)
            kotlin.test.assertContentEquals(byteArrayOf(9, 8, 7), Files.readAllBytes(java.nio.file.Path.of(restored.filePath)))
        } finally {
            deviceRoot.toFile().deleteRecursively()
            staging.toFile().deleteRecursively()
        }
    }

    @Test
    fun `recording repository rollback restores original state`() {
        val deviceRoot = createTempDirectory("device-rollback-")
        try {
            val repo = vn.loi.learning.android.recording.QuickVoiceRecordingRepository.forTesting(deviceRoot.toFile())
            val audioFile = deviceRoot.resolve("recordings/quick_voice/original.m4a")
            Files.createDirectories(audioFile.parent)
            Files.write(audioFile, byteArrayOf(1, 1, 1))

            val originalItem = VoiceRecordingItem("orig_1", "original.m4a", audioFile.toAbsolutePath().toString(), 500L, 1000L, 3L)
            kotlinx.coroutines.runBlocking { repo.save(originalItem) }

            val (items, files) = repo.captureRecordingsState()
            kotlin.test.assertEquals(1, items.size)
            kotlin.test.assertEquals(1, files.size)

            // Mutate repository with another recording
            val modifiedAudio = deviceRoot.resolve("recordings/quick_voice/modified.m4a")
            Files.write(modifiedAudio, byteArrayOf(2, 2))
            val modifiedItem = VoiceRecordingItem("mod_1", "modified.m4a", modifiedAudio.toAbsolutePath().toString(), 600L, 1000L, 2L)
            kotlinx.coroutines.runBlocking { repo.save(modifiedItem) }
            kotlin.test.assertEquals(2, kotlinx.coroutines.runBlocking { repo.getAll() }.size)

            // Rollback
            repo.rollbackRecordings(items, files)
            val restoredList = kotlinx.coroutines.runBlocking { repo.getAll() }
            kotlin.test.assertEquals(1, restoredList.size)
            kotlin.test.assertEquals("orig_1", restoredList[0].id)
            assertTrue(Files.exists(audioFile))
        } finally {
            deviceRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `active recording prevents restore and throws IllegalStateException`() {
        val deviceRoot = createTempDirectory("device-busy-")
        val staging = createTempDirectory("staging-busy-")
        try {
            val repo = vn.loi.learning.android.recording.QuickVoiceRecordingRepository.forTesting(deviceRoot.toFile())
            repo.isRecordingActive = true
            assertFailsWith<IllegalStateException> {
                repo.restoreRecordingsFromBackup(staging.resolve("android/recordings"))
            }
        } finally {
            deviceRoot.toFile().deleteRecursively()
            staging.toFile().deleteRecursively()
        }
    }

    @Test
    fun `p0 regression installed packages A to A plus B to restore A proves canonical replacement`() {
        val temp = createTempDirectory("p0-canonical-test-")
        try {
            val dataDir = temp.resolve("learning-engine/data")
            val mediaDir = dataDir.resolve("media")
            val importsDir = temp.resolve("learning-engine/imports")
            val backupsDir = temp.resolve("learning-engine/backups")
            val directories = AndroidPlatformDirectories(dataDir, mediaDir, importsDir)
            directories.create()

            val engine = LearningApplicationFactory.createPersisted(dataDir, false)
            val recovery = vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager(
                roots = mapOf("data" to dataDir, "media" to mediaDir),
                safetyDirectory = backupsDir,
                gate = requireNotNull(engine.recoveryOperationGate),
                stagedDomainValidator = { roots ->
                    LearningApplicationFactory.validatePersisted(requireNotNull(roots["data"]))
                }
            )
            val graph = AndroidApplicationGraph(
                engine = engine,
                media = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaDir),
                recovery = recovery,
                directories = directories
            )

            // 1. Initial State A: Intermediate package only
            val pkgA = ContentPackage(
                PackageId("package-intermediate"),
                PackageDescriptor("Vocabulary_in_Use_Intermediate", "1.0.0", "OPD3"),
                setOf(ContentLibraryId("lib-intermediate")),
                topicId = TopicId("topic-intermediate")
            )
            engine.contentRepository!!.save(Content(ContentId("c-1"), ContentType.WORD, ContentText("Intermediate Word", "Từ trung cấp")))
            engine.learningItemRepository!!.save(LearningItem(LearningItemId("item-1"), ContentId("c-1"), LearningMode.MEANING_RECOGNITION))
            engine.contentLibraryRepository!!.save(ContentLibrary(ContentLibraryId("lib-intermediate"), LibraryDescriptor("Lib Int"), setOf(ContentId("c-1"))))
            engine.contentPackageRepository!!.save(pkgA)
            engine.completePackageImportLifecycle!!.execute(listOf(PackageImportResult(pkgA, 1, 1, 1)))

            val mediaFileA = mediaDir.resolve("package-intermediate/audio.mp3")
            Files.createDirectories(mediaFileA.parent)
            Files.write(mediaFileA, byteArrayOf(1, 2, 3))

            // 2. Create Backup A
            val backupA = backupsDir.resolve("Backup_A.lebak")
            graph.createPortableBackup(backupA)
            assertTrue(Files.exists(backupA))

            // 3. Mutate live state to State A + B (Add Elementary package)
            val pkgB = ContentPackage(
                PackageId("package-elementary"),
                PackageDescriptor("Vocabulary_In_Use_Elementary", "1.0.0", "OPD3"),
                setOf(ContentLibraryId("lib-elementary")),
                topicId = TopicId("topic-elementary")
            )
            engine.contentRepository!!.save(Content(ContentId("c-2"), ContentType.WORD, ContentText("Elementary Word", "Từ sơ cấp")))
            engine.learningItemRepository!!.save(LearningItem(LearningItemId("item-2"), ContentId("c-2"), LearningMode.MEANING_RECOGNITION))
            engine.contentLibraryRepository!!.save(ContentLibrary(ContentLibraryId("lib-elementary"), LibraryDescriptor("Lib Elem"), setOf(ContentId("c-2"))))
            engine.contentPackageRepository!!.save(pkgB)
            engine.completePackageImportLifecycle!!.execute(listOf(PackageImportResult(pkgB, 1, 1, 1)))

            val mediaFileB = mediaDir.resolve("package-elementary/audio.mp3")
            Files.createDirectories(mediaFileB.parent)
            Files.write(mediaFileB, byteArrayOf(4, 5, 6))

            val liveBeforeRestore = Files.readString(dataDir.resolve("installed-packages.json"))
            assertTrue(liveBeforeRestore.contains("Vocabulary_In_Use_Elementary"))

            // 4. Restore Backup A
            val result = graph.restorePortableBackup(backupA, operationActive = false)
            assertIs<vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult.Success>(result)

            // 5. Inspect live persisted file directly on disk
            val liveAfterRestore = Files.readString(dataDir.resolve("installed-packages.json"))
            assertTrue(liveAfterRestore.contains("Vocabulary_in_Use_Intermediate"))
            assertFalse(liveAfterRestore.contains("Vocabulary_In_Use_Elementary"), "Elementary package must NOT survive restore!")
            assertFalse(Files.exists(mediaFileB), "Elementary media directory must NOT survive restore!")
            assertTrue(Files.exists(mediaFileA), "Intermediate media file must be restored!")

            // 6. Recreate fresh application context and verify repository
            val freshEngine = LearningApplicationFactory.createPersisted(dataDir, false)
            val repo: vn.loi.learning.domain.library.repository.InstalledPackageRepository = freshEngine.installedPackageRepository!!
            val installedList = repo.findAll()
            kotlin.test.assertEquals(1, installedList.size)
            kotlin.test.assertEquals("Vocabulary_in_Use_Intermediate", installedList[0].name.value)
        } finally {
            temp.toFile().deleteRecursively()
        }
    }
}
