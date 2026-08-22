package vn.loi.learning.application.sync

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentMediaStorage
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
import vn.loi.learning.domain.library.model.PackageState
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
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.sync.SyncEngine
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class SyncEngineTest {

    private fun createDevice(root: Path): DeviceContext {
        val dataDir = Files.createDirectories(root.resolve("data"))
        val mediaDir = Files.createDirectories(dataDir.resolve("media"))
        val mediaStorage = JvmContentMediaStorage(mediaDir)
        val contentRepo = InMemoryContentRepository()
        val itemRepo = InMemoryLearningItemRepository()
        val memoryRepo = InMemoryMemoryStateRepository()
        val reviewRepo = InMemoryReviewEventRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val txRunner = InMemoryTransactionRunner()

        val engine = SyncEngine(
            contentRepository = contentRepo,
            learningItemRepository = itemRepo,
            memoryStateRepository = memoryRepo,
            reviewEventRepository = reviewRepo,
            installedPackageRepository = pkgRepo,
            contentLibraryRepository = InMemoryContentLibraryRepository(),
            contentPackageRepository = InMemoryContentPackageRepository(),
            mediaStorage = mediaStorage,
            transactionRunner = txRunner
        )

        return DeviceContext(
            root = root,
            dataDir = dataDir,
            mediaDir = mediaDir,
            mediaStorage = mediaStorage,
            contentRepo = contentRepo,
            itemRepo = itemRepo,
            memoryRepo = memoryRepo,
            reviewRepo = reviewRepo,
            pkgRepo = pkgRepo,
            syncEngine = engine
        )
    }

    private data class DeviceContext(
        val root: Path,
        val dataDir: Path,
        val mediaDir: Path,
        val mediaStorage: ContentMediaStorage,
        val contentRepo: InMemoryContentRepository,
        val itemRepo: InMemoryLearningItemRepository,
        val memoryRepo: InMemoryMemoryStateRepository,
        val reviewRepo: InMemoryReviewEventRepository,
        val pkgRepo: InMemoryInstalledPackageRepository,
        val syncEngine: SyncEngine
    )

    @Test
    fun `export and import synchronizes new content and media across devices`() {
        val rootA = Files.createTempDirectory("sync-test-devA-")
        val rootB = Files.createTempDirectory("sync-test-devB-")
        try {
            val devA = createDevice(rootA)
            val devB = createDevice(rootB)

            // Setup Package & Content on Device A
            val pkg = InstalledPackage(
                id = InstalledPackageId("pkg-oxford"),
                libraryId = LibraryId("lib-1"),
                packageId = PackageId("oxford-3000"),
                topicId = TopicId("topic-1"),
                name = PackageName("Oxford 3000"),
                version = PackageVersion("1.0.0"),
                contentCount = 1,
                learningItemCount = 1
            )
            devA.pkgRepo.save(pkg)

            // Create Audio File in Device A media
            devA.mediaStorage.store("oxford-3000", "audio/serendipity.mp3", byteArrayOf(1, 2, 3, 4, 5))

            val contentA = Content(
                id = ContentId("c-1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "serendipity",
                    translatedText = "sự tình cờ may mắn",
                    pronunciation = "/ˌser.ənˈdɪp.ə.ti/",
                    exampleText = "A fortunate stroke of serendipity.",
                    exampleTranslation = "Một sự may mắn tình cờ."
                ),
                media = ContentMedia(
                    primaryAudio = "oxford-3000/audio/serendipity.mp3"
                ),
                customFields = ContentCustomFields(setOf(ContentCustomField(ContentFieldId("pos"), "NOUN")))
            )
            devA.contentRepo.save(contentA)
            devA.itemRepo.save(LearningItem(LearningItemId("item-c-1"), ContentId("c-1"), LearningMode.MEANING_RECOGNITION))

            // Export Sync from Device A
            val syncFile = rootA.resolve("changeset-A.lesync")
            devA.syncEngine.exportSyncPackage(syncFile, sourcePlatform = "desktop")

            assertTrue(Files.isRegularFile(syncFile))

            // Import Sync on Device B
            val summary = devB.syncEngine.importSyncPackage(syncFile)

            assertEquals(1, summary.contentDeltasApplied)
            assertEquals(1, summary.mediaAssetsAdded)
            assertEquals(0, summary.conflicts.size)

            val contentOnB = devB.contentRepo.findById(ContentId("c-1"))
            assertEquals("serendipity", contentOnB?.text?.primaryText)
            assertEquals("sự tình cờ may mắn", contentOnB?.text?.translatedText)
            assertEquals("NOUN", contentOnB?.customFields?.get(ContentFieldId("pos"))?.value)

            // Verify Media on Device B
            val mediaPathB = devB.mediaStorage.resolve("oxford-3000/audio/serendipity.mp3")
            assertTrue(mediaPathB != null && Files.isRegularFile(mediaPathB))
            assertEquals(5, Files.size(mediaPathB))
        } finally {
            rootA.toFile().deleteRecursively()
            rootB.toFile().deleteRecursively()
        }
    }

    @Test
    fun `export skips unchanged media when remote hashes are known`() {
        val rootA = Files.createTempDirectory("sync-skip-media-devA-")
        val rootB = Files.createTempDirectory("sync-skip-media-devB-")
        try {
            val devA = createDevice(rootA)
            val devB = createDevice(rootB)

            val audioBytes = byteArrayOf(10, 20, 30, 40)
            devA.mediaStorage.store("pkg-1", "audio/word.mp3", audioBytes)
            val audioSha = vn.loi.learning.infrastructure.sync.PortableSyncPackageService.sha256(audioBytes)

            devA.contentRepo.save(
                Content(
                    id = ContentId("c-word"),
                    type = ContentType.WORD,
                    text = ContentText(primaryText = "word", translatedText = "từ"),
                    media = ContentMedia(primaryAudio = "pkg-1/audio/word.mp3")
                )
            )

            // Device B already has this audio hash
            val syncFile = rootA.resolve("lightweight.lesync")
            devA.syncEngine.exportSyncPackage(
                target = syncFile,
                sourcePlatform = "desktop",
                knownRemoteMediaHashes = setOf(audioSha)
            )

            val preview = devB.syncEngine.previewSyncPackage(syncFile)
            assertEquals(1, preview.manifest.contentDeltasCount)
            assertEquals(0, preview.manifest.mediaAssetsCount) // Media was NOT packaged!
            assertEquals(0, preview.mediaFiles.size)
        } finally {
            rootA.toFile().deleteRecursively()
            rootB.toFile().deleteRecursively()
        }
    }

    @Test
    fun `review events merge and deduplicate across mobile and desktop without history loss`() {
        val rootA = Files.createTempDirectory("sync-review-devA-")
        val rootB = Files.createTempDirectory("sync-review-devB-")
        try {
            val devA = createDevice(rootA)
            val devB = createDevice(rootB)

            val learner = LearnerId("user-1")
            val itemId = LearningItemId("item-vocab-1")

            val state0 = MemoryState.new(learner, itemId, Moment(1000L))
            val state1 = state0.afterReview(
                nextStage = LearningStage.LEARNING,
                nextDifficulty = Difficulty.of(5.0),
                nextStability = Stability.of(1.0),
                reviewedAt = Moment(2000L),
                scheduledInterval = TimeSpan.days(1.0),
                isLapse = false
            )
            val state2 = state1.afterReview(
                nextStage = LearningStage.REVIEW,
                nextDifficulty = Difficulty.of(4.8),
                nextStability = Stability.of(3.0),
                reviewedAt = Moment(5000L),
                scheduledInterval = TimeSpan.days(3.0),
                isLapse = false
            )

            val rev1 = ReviewEvent(
                id = ReviewEventId("rev-001"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(2000L),
                responseTime = TimeSpan.seconds(2),
                stateBefore = state0,
                stateAfter = state1
            )
            val rev2 = ReviewEvent(
                id = ReviewEventId("rev-002"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(5000L),
                responseTime = TimeSpan.seconds(3),
                stateBefore = state1,
                stateAfter = state2
            )

            // Device A has rev1
            devA.reviewRepo.append(rev1)
            devA.memoryRepo.save(state1)

            // Device B (phone) has rev1 AND rev2 (done while traveling)
            devB.reviewRepo.append(rev1)
            devB.reviewRepo.append(rev2)
            devB.memoryRepo.save(state2)

            // Export from Device B -> Import to Device A
            val syncPhone = rootB.resolve("phone-study.lesync")
            devB.syncEngine.exportSyncPackage(syncPhone, sourcePlatform = "android")

            val summary = devA.syncEngine.importSyncPackage(syncPhone)

            assertEquals(1, summary.reviewEventsMerged) // rev2 merged
            assertEquals(1, summary.reviewEventsDeduplicated) // rev1 deduplicated

            val allOnA = devA.reviewRepo.findAll(learner, itemId)
            assertEquals(2, allOnA.size)
            assertEquals(listOf("rev-001", "rev-002"), allOnA.map { it.id.value })

            // Memory state on Device A updated to state2
            val finalMemory = devA.memoryRepo.find(learner, itemId)
            assertEquals(2, finalMemory?.reviewCount)
            assertEquals(Moment(5000L), finalMemory?.lastReviewedAt)
            assertEquals(LearningStage.REVIEW, finalMemory?.stage)
        } finally {
            rootA.toFile().deleteRecursively()
            rootB.toFile().deleteRecursively()
        }
    }

    @Test
    fun `field-level content conflict is detected and reported cleanly`() {
        val rootA = Files.createTempDirectory("sync-conflict-devA-")
        val rootB = Files.createTempDirectory("sync-conflict-devB-")
        try {
            val devA = createDevice(rootA)
            val devB = createDevice(rootB)

            // Content on Device A
            val contentA = Content(
                id = ContentId("c-ambiguous"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "bark",
                    translatedText = "sủa (chó)",
                    exampleText = "The dog barks."
                )
            )
            devA.contentRepo.save(contentA)

            // Content on Device B (divergent edit)
            val contentB = Content(
                id = ContentId("c-ambiguous"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "bark",
                    translatedText = "vỏ cây",
                    exampleText = "The bark of the oak tree."
                )
            )
            devB.contentRepo.save(contentB)

            // Export Device A -> Import to Device B
            val syncA = rootA.resolve("pc-edit.lesync")
            devA.syncEngine.exportSyncPackage(syncA, sourcePlatform = "desktop")

            val summary = devB.syncEngine.importSyncPackage(
                syncA,
                conflictStrategy = ConflictResolutionStrategy.MERGE_FIELD_LEVEL
            )

            assertEquals(1, summary.contentDeltasApplied)
            // Verify translatedText was merged and data was not destroyed
            val updatedB = devB.contentRepo.findById(ContentId("c-ambiguous"))
            assertEquals("sủa (chó)", updatedB?.text?.translatedText)
        } finally {
            rootA.toFile().deleteRecursively()
            rootB.toFile().deleteRecursively()
        }
    }
}
