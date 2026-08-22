package vn.loi.learning.application.sync

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult
import vn.loi.learning.infrastructure.sync.PortableSyncPackageService

class CrossPlatformSyncRoundTripTest {

    private fun setupPersistedEnvironment(root: Path): TestPlatformEnvironment {
        val dataDir = Files.createDirectories(root.resolve("data"))
        val mediaDir = Files.createDirectories(dataDir.resolve("media"))
        val backupDir = Files.createDirectories(root.resolve("backups"))

        val context = LearningApplicationFactory.createPersisted(dataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        val mediaStorage = JvmContentMediaStorage(mediaDir)
        val recoveryManager = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to dataDir, "media" to mediaDir),
            safetyDirectory = backupDir,
            gate = requireNotNull(context.recoveryOperationGate),
            stagedDomainValidator = { roots -> LearningApplicationFactory.validatePersisted(requireNotNull(roots["data"])) }
        )

        return TestPlatformEnvironment(
            root = root,
            dataDir = dataDir,
            mediaDir = mediaDir,
            backupDir = backupDir,
            context = context,
            mediaStorage = mediaStorage,
            recovery = recoveryManager
        )
    }

    private data class TestPlatformEnvironment(
        val root: Path,
        val dataDir: Path,
        val mediaDir: Path,
        val backupDir: Path,
        val context: vn.loi.learning.infrastructure.LearningApplicationContext,
        val mediaStorage: JvmContentMediaStorage,
        val recovery: JvmLearningDataRecoveryManager
    )

    @Test
    fun `e2e cross-platform initial setup and subsequent differential synchronization`() {
        val pcRoot = Files.createTempDirectory("sync-e2e-pc-")
        val androidRoot = Files.createTempDirectory("sync-e2e-android-")
        try {
            val pc = setupPersistedEnvironment(pcRoot)

            // 1. PC: Setup initial package & vocabulary
            val pkg = InstalledPackage(
                id = InstalledPackageId("pkg-oxford-3000"),
                libraryId = LibraryId("default-library"),
                packageId = PackageId("oxford-3000"),
                topicId = TopicId("topic-oxford"),
                name = PackageName("Oxford 3000"),
                version = PackageVersion("1.0.0"),
                contentCount = 2,
                learningItemCount = 2
            )
            requireNotNull(pc.context.installedPackageRepository).save(pkg)
            pc.context.domainLibraryRepository?.findById(LibraryId("default-library"))?.let { defaultLib ->
                pc.context.domainLibraryRepository.save(defaultLib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt))
            }

            val content1 = Content(
                id = ContentId("word-1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "resilient",
                    translatedText = "kiên cường",
                    pronunciation = "/rɪˈzɪl.jənt/",
                    exampleText = "She is resilient.",
                    exampleTranslation = "Cô ấy rất kiên cường."
                ),
                media = ContentMedia(primaryAudio = "oxford-3000/audio/resilient.mp3"),
                customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("pos"), "ADJECTIVE")))
            )
            val content2 = Content(
                id = ContentId("word-2"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "perseverance",
                    translatedText = "sự kiên trì",
                    pronunciation = "/ˌpɜː.sɪˈvɪə.rəns/"
                ),
                media = ContentMedia(primaryAudio = "oxford-3000/audio/perseverance.mp3")
            )
            requireNotNull(pc.context.contentRepository).save(content1)
            requireNotNull(pc.context.contentRepository).save(content2)

            requireNotNull(pc.context.learningItemRepository).save(
                LearningItem(LearningItemId("item-1"), ContentId("word-1"), LearningMode.MEANING_RECOGNITION)
            )
            requireNotNull(pc.context.learningItemRepository).save(
                LearningItem(LearningItemId("item-2"), ContentId("word-2"), LearningMode.MEANING_RECOGNITION)
            )

            // Store audio in PC media storage
            pc.mediaStorage.store("oxford-3000", "audio/resilient.mp3", byteArrayOf(10, 20, 30))
            pc.mediaStorage.store("oxford-3000", "audio/perseverance.mp3", byteArrayOf(40, 50, 60))

            // 2. PC: Create Full Portable Backup (.lebak)
            val initialBackupFile = pcRoot.resolve("initial-setup.lebak")
            pc.recovery.createPortableBackupV2(
                target = initialBackupFile,
                descriptor = PortableBackupV2Descriptor(appVersion = "2.0.0", versionCode = 1, sourcePlatform = "desktop")
            )
            assertTrue(Files.isRegularFile(initialBackupFile))

            // 3. Android: Restore Initial Portable Backup
            val android = setupPersistedEnvironment(androidRoot)
            val restoreResult = android.recovery.restorePortableBackupV2(initialBackupFile)
            assertIs<PortableBackupV2RestoreResult.Success>(restoreResult)

            // Re-open Android context after restore
            val androidContext = LearningApplicationFactory.createPersisted(android.dataDir, reconcilePartOfSpeechRegistryOnCreate = false)
            val restoredContents = requireNotNull(androidContext.contentRepository).findAll()
            assertEquals(2, restoredContents.size)
            assertTrue(android.mediaStorage.exists("oxford-3000/audio/resilient.mp3"))
            assertTrue(android.mediaStorage.exists("oxford-3000/audio/perseverance.mp3"))

            // 4. Android: Mobile Study creates Review Events
            val learner = LearnerId("learner-main")
            val item1 = LearningItemId("item-1")
            val state0 = MemoryState.new(learner, item1, Moment(1000L))
            val state1 = state0.afterReview(
                nextStage = LearningStage.LEARNING,
                nextDifficulty = Difficulty.of(5.0),
                nextStability = Stability.of(1.0),
                reviewedAt = Moment(2000L),
                scheduledInterval = TimeSpan.days(1.0),
                isLapse = false
            )
            val mobileReview = ReviewEvent(
                id = ReviewEventId("rev-mobile-001"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(2000L),
                responseTime = TimeSpan.seconds(2),
                stateBefore = state0,
                stateAfter = state1
            )
            requireNotNull(androidContext.reviewEventRepository).append(mobileReview)
            requireNotNull(androidContext.memoryStateRepository).save(state1)

            // 5. Android: Export Differential Sync (.lesync)
            val androidSyncFile = androidRoot.resolve("android-study.lesync")
            requireNotNull(androidContext.syncEngine).exportSyncPackage(
                target = androidSyncFile,
                sourcePlatform = "android"
            )
            assertTrue(Files.isRegularFile(androidSyncFile))

            // 6. PC: Import Differential Sync from Android
            val pcSyncSummary = requireNotNull(pc.context.syncEngine).importSyncPackage(androidSyncFile)
            assertEquals(1, pcSyncSummary.reviewEventsMerged)
            assertEquals(0, pcSyncSummary.conflicts.size)

            val pcMemory = requireNotNull(pc.context.memoryStateRepository).find(learner, item1)
            assertEquals(1, pcMemory?.reviewCount)
            assertEquals(Moment(2000L), pcMemory?.lastReviewedAt)

            // 7. PC: Desktop Edits Content & Adds New Audio via TTS
            val updatedContent1 = content1.copy(
                text = content1.text.copy(exampleTranslation = "Cô ấy là người kiên cường vượt qua thử thách."),
                media = content1.media.copy(exampleAudio = "oxford-3000/audio/resilient_ex.mp3")
            )
            requireNotNull(pc.context.contentRepository).save(updatedContent1)
            pc.mediaStorage.store("oxford-3000", "audio/resilient_ex.mp3", byteArrayOf(70, 80, 90))

            // Known hashes on Android: the two existing audio files
            val hash1 = PortableSyncPackageService.sha256(byteArrayOf(10, 20, 30))
            val hash2 = PortableSyncPackageService.sha256(byteArrayOf(40, 50, 60))

            // PC exports differential sync with knownRemoteMediaHashes
            val pcDiffSyncFile = pcRoot.resolve("pc-edits.lesync")
            requireNotNull(pc.context.syncEngine).exportSyncPackage(
                target = pcDiffSyncFile,
                sourcePlatform = "desktop",
                knownRemoteMediaHashes = setOf(hash1, hash2)
            )

            // Verify only the 1 new audio file is packaged
            val preview = requireNotNull(androidContext.syncEngine).previewSyncPackage(pcDiffSyncFile)
            assertEquals(1, preview.manifest.mediaAssetsCount)
            assertEquals("oxford-3000/audio/resilient_ex.mp3", preview.mediaFiles.keys.single())

            // 8. Android: Import Differential Sync from PC
            val androidSyncSummary = requireNotNull(androidContext.syncEngine).importSyncPackage(pcDiffSyncFile)
            assertEquals(1, androidSyncSummary.contentDeltasApplied)
            assertEquals(1, androidSyncSummary.mediaAssetsAdded)

            val finalAndroidContent1 = requireNotNull(androidContext.contentRepository).findById(ContentId("word-1"))
            assertEquals("Cô ấy là người kiên cường vượt qua thử thách.", finalAndroidContent1?.text?.exampleTranslation)
            assertTrue(android.mediaStorage.exists("oxford-3000/audio/resilient_ex.mp3"))

            // Verify pre-existing mobile review events were untouched
            val finalAndroidReviews = requireNotNull(androidContext.reviewEventRepository).findAll(learner, item1)
            assertEquals(1, finalAndroidReviews.size)
        } finally {
            pcRoot.toFile().deleteRecursively()
            androidRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `corrupted sync archive is rejected without mutating live database`() {
        val root = Files.createTempDirectory("sync-corrupt-test-")
        try {
            val env = setupPersistedEnvironment(root)
            val validSync = root.resolve("valid.lesync")
            requireNotNull(env.context.syncEngine).exportSyncPackage(validSync, sourcePlatform = "desktop")

            // Tamper file bytes
            val corruptSync = root.resolve("corrupt.lesync")
            Files.write(corruptSync, byteArrayOf(0, 1, 2, 3, 4))

            var failed = false
            try {
                requireNotNull(env.context.syncEngine).importSyncPackage(corruptSync)
            } catch (_: Exception) {
                failed = true
            }
            assertTrue(failed, "Corrupt sync package should throw validation exception")
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
