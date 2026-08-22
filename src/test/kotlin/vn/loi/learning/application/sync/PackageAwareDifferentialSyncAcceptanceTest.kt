package vn.loi.learning.application.sync

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
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
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.sync.model.ConflictResolutionStrategy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage
import vn.loi.learning.infrastructure.recovery.JvmLearningDataRecoveryManager
import vn.loi.learning.infrastructure.recovery.PortableBackupV2Descriptor

class PackageAwareDifferentialSyncAcceptanceTest {

    private lateinit var rootTempDir: Path
    private lateinit var pcDataDir: Path
    private lateinit var pcMediaDir: Path
    private lateinit var pcEngine: LearningApplicationContext
    private lateinit var pcMediaStorage: JvmContentMediaStorage
    private lateinit var pcRecovery: JvmLearningDataRecoveryManager

    private lateinit var androidDataDir: Path
    private lateinit var androidMediaDir: Path
    private lateinit var androidEngine: LearningApplicationContext
    private lateinit var androidMediaStorage: JvmContentMediaStorage
    private lateinit var androidRecovery: JvmLearningDataRecoveryManager

    @BeforeEach
    fun setUp() {
        rootTempDir = Files.createTempDirectory("package_aware_sync_acceptance_")

        // PC Node
        pcDataDir = rootTempDir.resolve("pc/data")
        pcMediaDir = pcDataDir.resolve("media")
        Files.createDirectories(pcDataDir)
        Files.createDirectories(pcMediaDir)
        pcEngine = LearningApplicationFactory.createPersisted(pcDataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        pcMediaStorage = JvmContentMediaStorage(pcMediaDir)
        pcRecovery = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to pcDataDir, "media" to pcMediaDir),
            safetyDirectory = rootTempDir.resolve("pc/safety")
        )

        // Android Node
        androidDataDir = rootTempDir.resolve("android/data")
        androidMediaDir = androidDataDir.resolve("media")
        Files.createDirectories(androidDataDir)
        Files.createDirectories(androidMediaDir)
        androidEngine = LearningApplicationFactory.createPersisted(androidDataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        androidMediaStorage = JvmContentMediaStorage(androidMediaDir)
        androidRecovery = JvmLearningDataRecoveryManager(
            roots = mapOf("data" to androidDataDir, "media" to androidMediaDir),
            safetyDirectory = rootTempDir.resolve("android/safety")
        )
    }

    @AfterEach
    fun tearDown() {
        rootTempDir.toFile().deleteRecursively()
    }

    @Test
    fun `full end-to-end multi-package real-world workflow with selective sync, deduplication and conflict resolution`() {
        val libId = LibraryId("main-library")
        val topicId = TopicId("general-topic")

        // Step 1: Desktop contains Packages A, B
        val pkgA = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-a-inst"),
            libraryId = libId,
            packageId = PackageId("pkg-a"),
            topicId = topicId,
            name = PackageName("Vocabulary Intermediate (Pkg A)"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 2,
            learningItemCount = 2
        )
        val pkgB = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-b-inst"),
            libraryId = libId,
            packageId = PackageId("pkg-b"),
            topicId = topicId,
            name = PackageName("Grammar Advanced (Pkg B)"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 1,
            learningItemCount = 1
        )
        pcEngine.installedPackageRepository!!.save(pkgA)
        pcEngine.installedPackageRepository!!.save(pkgB)

        val cardA1 = Content(
            id = ContentId("card-a-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "diligent", translatedText = "siêng năng"),
            media = ContentMedia(primaryAudio = "pkg-a/audio/diligent.mp3")
        )
        val cardA2 = Content(
            id = ContentId("card-a-2"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "meticulous", translatedText = "tỉ mỉ"),
            media = ContentMedia(primaryAudio = "pkg-a/audio/meticulous.mp3")
        )
        val cardB1 = Content(
            id = ContentId("card-b-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "hypothetical", translatedText = "giả thuyết"),
            media = ContentMedia(primaryAudio = "pkg-b/audio/hypothetical.mp3")
        )
        pcEngine.contentRepository!!.save(cardA1)
        pcEngine.contentRepository!!.save(cardA2)
        pcEngine.contentRepository!!.save(cardB1)

        val itemA1 = LearningItem(id = LearningItemId("item-a-1"), contentId = ContentId("card-a-1"), mode = LearningMode.MEANING_RECOGNITION)
        val itemA2 = LearningItem(id = LearningItemId("item-a-2"), contentId = ContentId("card-a-2"), mode = LearningMode.MEANING_RECOGNITION)
        val itemB1 = LearningItem(id = LearningItemId("item-b-1"), contentId = ContentId("card-b-1"), mode = LearningMode.MEANING_RECOGNITION)
        pcEngine.learningItemRepository!!.save(itemA1)
        pcEngine.learningItemRepository!!.save(itemA2)
        pcEngine.learningItemRepository!!.save(itemB1)

        // Store initial audio files on PC
        pcMediaStorage.storeStream("pkg-a", "audio/diligent.mp3", Files.write(rootTempDir.resolve("d.mp3"), byteArrayOf(1, 2, 3, 4)))
        pcMediaStorage.storeStream("pkg-a", "audio/meticulous.mp3", Files.write(rootTempDir.resolve("m.mp3"), byteArrayOf(5, 6, 7, 8)))
        pcMediaStorage.storeStream("pkg-b", "audio/hypothetical.mp3", Files.write(rootTempDir.resolve("h.mp3"), byteArrayOf(9, 9, 9, 9)))

        // Step 2: Desktop creates Full Portable Backup (.lebak) for baseline
        val baselineBackup = rootTempDir.resolve("baseline.lebak")
        pcRecovery.createPortableBackupV2(
            target = baselineBackup,
            descriptor = PortableBackupV2Descriptor(
                appVersion = "2.0.0",
                versionCode = 1,
                sourcePlatform = "desktop",
                learnerIds = listOf("default-learner")
            )
        )
        assertTrue(Files.exists(baselineBackup))

        // Step 3: Android restores baseline .lebak
        val androidRestoreResult = androidRecovery.restorePortableBackupV2(baselineBackup, operationActive = false)
        assertTrue(androidRestoreResult is vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreResult.Success)
        androidEngine = LearningApplicationFactory.createPersisted(androidDataDir, reconcilePartOfSpeechRegistryOnCreate = false)
        assertEquals(2, androidEngine.installedPackageRepository!!.findAll().size)
        assertNotNull(androidEngine.contentRepository!!.findById(ContentId("card-a-1")))

        // Step 4: Android studies Package A and records 2 review events
        val learner = LearnerId("default-learner")
        val reviewA1 = ReviewEvent(
            id = ReviewEventId("rev-android-1"),
            rating = ReviewRating.GOOD,
            reviewedAt = Moment(1700000000000L),
            responseTime = TimeSpan(2500L),
            stateBefore = MemoryState(learner, itemA1.id, LearningStage.LEARNING, 5.0, 1.0, Moment(1700000000000L), null, 0, 0),
            stateAfter = MemoryState(learner, itemA1.id, LearningStage.REVIEW, 4.8, 3.0, Moment(1700259200000L), Moment(1700000000000L), 1, 0),
            source = RatingSource.STANDARD_REVIEW
        )
        val reviewA2 = ReviewEvent(
            id = ReviewEventId("rev-android-2"),
            rating = ReviewRating.EASY,
            reviewedAt = Moment(1700000050000L),
            responseTime = TimeSpan(1800L),
            stateBefore = MemoryState(learner, itemA2.id, LearningStage.LEARNING, 5.0, 1.0, Moment(1700000050000L), null, 0, 0),
            stateAfter = MemoryState(learner, itemA2.id, LearningStage.REVIEW, 4.2, 5.0, Moment(1700432000000L), Moment(1700000050000L), 1, 0),
            source = RatingSource.STANDARD_REVIEW
        )
        androidEngine.reviewEventRepository!!.append(reviewA1)
        androidEngine.reviewEventRepository!!.append(reviewA2)
        androidEngine.memoryStateRepository!!.save(reviewA1.stateAfter)
        androidEngine.memoryStateRepository!!.save(reviewA2.stateAfter)

        // Step 6: Android exports Differential Sync (.lesync) for Package A with review events
        val androidSyncFile = rootTempDir.resolve("android_review_sync.lesync")
        androidEngine.syncEngine!!.exportSyncPackage(
            target = androidSyncFile,
            sourcePlatform = "android",
            specificPackageIds = setOf("pkg-a"),
            includeReviewEvents = true
        )
        assertTrue(Files.exists(androidSyncFile))

        // Step 7: PC previews and imports Android's review sync
        val pcPreview = pcEngine.syncEngine!!.generatePreviewReport(androidSyncFile)
        assertEquals(listOf("pkg-a"), pcPreview.packagesAffected)
        assertEquals(2, pcPreview.reviewEventsCount)
        assertFalse(pcPreview.requiresFullBackup)

        val pcImportResult = pcEngine.syncEngine!!.importSyncPackage(androidSyncFile, ConflictResolutionStrategy.MERGE_FIELD_LEVEL)
        assertTrue(pcImportResult.success)
        assertEquals(2, pcImportResult.reviewEventsMerged)
        // Check PC has Android's review events and FSRS memory state converged
        assertEquals(2, pcEngine.reviewEventRepository!!.findAll().size)
        assertEquals(3.0, pcEngine.memoryStateRepository!!.find(learner, itemA1.id)?.stabilityDays)

        // Step 8: Meanwhile on PC, user edits Package A (cardA1 translatedText) & creates a new TTS audio file
        val editedCardA1 = cardA1.copy(
            text = cardA1.text.copy(translatedText = "chăm chỉ, cần cù"),
            media = cardA1.media.copy(translatedAudio = "pkg-a/audio/diligent_vi.mp3")
        )
        pcEngine.contentRepository!!.save(editedCardA1)
        pcMediaStorage.storeStream("pkg-a", "audio/diligent_vi.mp3", Files.write(rootTempDir.resolve("d_vi.mp3"), byteArrayOf(10, 11, 12, 13)))

        // PC also modifies Package B to prove Package B is unaffected by Package A sync
        val editedCardB1 = cardB1.copy(text = cardB1.text.copy(exampleText = "This is purely hypothetical."))
        pcEngine.contentRepository!!.save(editedCardB1)

        // Step 9: PC exports Differential Sync (.lesync) for Package A to Android with knownRemoteMediaHashes
        val androidExistingHashes = androidEngine.syncEngine!!.getMediaHashesForPackages(setOf("pkg-a"))
        val pcSyncFile = rootTempDir.resolve("pc_content_sync.lesync")
        pcEngine.syncEngine!!.exportSyncPackage(
            target = pcSyncFile,
            sourcePlatform = "desktop",
            specificPackageIds = setOf("pkg-a"),
            knownRemoteMediaHashes = androidExistingHashes,
            includeReviewEvents = false
        )

        // Step 10: Android previews PC's .lesync
        val androidPreview = androidEngine.syncEngine!!.generatePreviewReport(pcSyncFile)
        assertEquals(listOf("pkg-a"), androidPreview.packagesAffected)
        assertEquals(1, androidPreview.newMediaCount) // only the new diligent_vi.mp3
        assertTrue(androidPreview.existingMediaReusedCount >= 2) // diligent.mp3 and meticulous.mp3 reused!

        // Step 10: Android imports PC's .lesync
        val androidImportResult = androidEngine.syncEngine!!.importSyncPackage(pcSyncFile, ConflictResolutionStrategy.MERGE_FIELD_LEVEL)
        assertTrue(androidImportResult.success)
        assertEquals(1, androidImportResult.mediaAssetsAdded)

        val androidUpdatedCardA1 = androidEngine.contentRepository!!.findById(ContentId("card-a-1"))
        assertNotNull(androidUpdatedCardA1)
        assertEquals("chăm chỉ, cần cù", androidUpdatedCardA1.text.translatedText)
        assertEquals("pkg-a/audio/diligent_vi.mp3", androidUpdatedCardA1.media.translatedAudio)
        assertTrue(androidMediaStorage.exists("pkg-a/audio/diligent_vi.mp3"))

        // Verify Package B on Android is unchanged and does not have the PC edit (since sync was scoped to pkg-a)
        val androidCardB1 = androidEngine.contentRepository!!.findById(ContentId("card-b-1"))
        assertNotNull(androidCardB1)
        assertNull(androidCardB1.text.exampleText)
    }

    @Test
    fun `first-baseline requirement flags missing package and recommends full backup`() {
        val libId = LibraryId("main-library")
        val topicId = TopicId("general-topic")

        // PC has Package Z
        val pkgZ = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-z-inst"),
            libraryId = libId,
            packageId = PackageId("pkg-z"),
            topicId = topicId,
            name = PackageName("New German Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 1,
            learningItemCount = 1
        )
        pcEngine.installedPackageRepository!!.save(pkgZ)
        pcEngine.contentRepository!!.save(
            Content(id = ContentId("card-z-1"), type = ContentType.WORD, text = ContentText("hallo", "xin chào"))
        )

        val syncPackage = rootTempDir.resolve("pkg-z.lesync")
        pcEngine.syncEngine!!.exportSyncPackage(
            target = syncPackage,
            sourcePlatform = "desktop",
            specificPackageIds = setOf("pkg-z")
        )

        // Android (which does not have pkg-z installed) previews it
        val preview = androidEngine.syncEngine!!.generatePreviewReport(syncPackage)
        assertTrue(preview.requiresFullBackup)
        assertEquals(listOf("pkg-z"), preview.missingBaselinePackageIds)
        assertTrue(preview.warnings.isNotEmpty())
        assertTrue(preview.warnings.first().contains("baseline"))
    }

    @Test
    fun `conflict preview and strategies preserve or apply accurately`() {
        // Setup shared card
        val card = Content(
            id = ContentId("shared-card-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "ubiquitous", translatedText = "phổ biến"),
            metadata = ContentMetadata(title = "Unit 1")
        )
        pcEngine.contentRepository!!.save(card)
        androidEngine.contentRepository!!.save(card)

        // PC edits primaryText
        val pcEdited = card.copy(text = card.text.copy(primaryText = "ubiquitous (adj)"))
        pcEngine.contentRepository!!.save(pcEdited)

        val pcSync = rootTempDir.resolve("conflict-test.lesync")
        pcEngine.syncEngine!!.exportSyncPackage(pcSync, "desktop")

        // Android previews PC sync -> detects conflict
        val preview = androidEngine.syncEngine!!.generatePreviewReport(pcSync)
        assertEquals(1, preview.conflicts.size)
        assertEquals("shared-card-1", preview.conflicts.first().entityId)
        assertEquals("primaryText", preview.conflicts.first().fieldName)
        assertEquals("ubiquitous", preview.conflicts.first().localValueSummary)
        assertEquals("ubiquitous (adj)", preview.conflicts.first().incomingValueSummary)

        // Strategy 1: PRESERVE_LOCAL keeps Android's local text
        val preserveResult = androidEngine.syncEngine!!.importSyncPackage(pcSync, ConflictResolutionStrategy.PRESERVE_LOCAL)
        assertTrue(preserveResult.success)
        val localCard = androidEngine.contentRepository!!.findById(ContentId("shared-card-1"))
        assertNotNull(localCard)
        assertEquals("ubiquitous", localCard.text.primaryText)

        // Strategy 2: APPLY_INCOMING overwrites with incoming change
        val applyResult = androidEngine.syncEngine!!.importSyncPackage(pcSync, ConflictResolutionStrategy.APPLY_INCOMING)
        assertTrue(applyResult.success)
        val updatedCard = androidEngine.contentRepository!!.findById(ContentId("shared-card-1"))
        assertNotNull(updatedCard)
        assertEquals("ubiquitous (adj)", updatedCard.text.primaryText)
    }
}
