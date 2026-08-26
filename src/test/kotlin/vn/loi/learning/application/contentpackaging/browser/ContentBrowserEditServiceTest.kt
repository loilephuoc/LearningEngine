package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.sync.protocol.*
import vn.loi.learning.application.sync.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.sync.InMemorySyncTransport
import java.nio.file.Files

/**
 * CP2 Tests — Persist Content Edit.
 *
 * TC01: updateTextFields persists mutable text fields
 * TC02: ContentId và ContentType không thay đổi sau save
 * TC03: save không xóa LearningItem
 * TC04: Media và Metadata (lesson/tags) không thay đổi sau save
 * TC05: updateTextFields với questionText trống → IllegalArgumentException
 * TC06: persistEdit qua Facade → reload state phản ánh data mới
 */
class ContentBrowserEditServiceTest {

    private enum class CreateFailureBoundary { CONTENT, LEARNING_ITEM, LIBRARY, INSTALLED_PACKAGE }

    // ---------------------------------------------------------------------------
    // TC01 — updateTextFields persists mutable text fields
    // ---------------------------------------------------------------------------
    @Test
    fun `TC01 updateTextFields persists question answer pronunciation and example`() {
        val (appContext, _) = createFixture(contentCount = 3)
        val service = ContentBrowserEditService(appContext.contentRepository!!)

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated Question",
            answerText = "Updated Answer",
            pronunciation = "/ʌpˈdeɪtɪd/",
            partOfSpeech = "verb",
            exampleText = "She updated it.",
            exampleTranslation = "Cô ấy đã cập nhật nó."
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))
        assertNotNull(saved)
        assertEquals("Updated Question", saved.text.primaryText)
        assertEquals("Updated Answer", saved.text.translatedText)
        assertEquals("/ʌpˈdeɪtɪd/", saved.text.pronunciation)
        assertEquals("She updated it.", saved.text.exampleText)
        assertEquals("Cô ấy đã cập nhật nó.", saved.text.exampleTranslation)
    }

    // ---------------------------------------------------------------------------
    // TC02 — ContentId và ContentType không thay đổi
    // ---------------------------------------------------------------------------
    @Test
    fun `TC02 ContentId and ContentType are preserved after updateTextFields`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val originalContent = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "New Question",
            answerText = "New Answer",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals(originalContent.id, saved.id)
        assertEquals(originalContent.type, saved.type)
    }

    // ---------------------------------------------------------------------------
    // TC03 — LearningItems không bị xóa sau khi save
    // ---------------------------------------------------------------------------
    @Test
    fun `TC03 save does not delete LearningItems`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val initialItems = appContext.learningItemRepository!!.findAllEnabled()

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated",
            answerText = "Updated",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val finalItems = appContext.learningItemRepository!!.findAllEnabled()
        assertEquals(initialItems.size, finalItems.size)
    }

    // ---------------------------------------------------------------------------
    // TC04 — Media và Metadata không thay đổi
    // ---------------------------------------------------------------------------
    @Test
    fun `TC04 media and metadata are preserved after save`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)
        val original = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!

        service.updateTextFields(
            contentId = ContentId("cnt-1"),
            questionText = "Updated",
            answerText = "",
            pronunciation = "",
            partOfSpeech = "",
            exampleText = "",
            exampleTranslation = ""
        )

        val saved = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals(original.media.primaryAudio, saved.media.primaryAudio)
        assertEquals(original.media.image, saved.media.image)
        assertEquals(original.metadata.lesson, saved.metadata.lesson)
        assertEquals(original.metadata.tags, saved.metadata.tags)
    }

    // ---------------------------------------------------------------------------
    // TC05 — blank questionText → IllegalArgumentException
    // ---------------------------------------------------------------------------
    @Test
    fun `TC05 updateTextFields with blank question throws IllegalArgumentException`() {
        val (appContext, _) = createFixture(contentCount = 1)
        val service = ContentBrowserEditService(appContext.contentRepository!!)

        assertFails {
            service.updateTextFields(
                contentId = ContentId("cnt-1"),
                questionText = "   ",
                answerText = "Answer",
                pronunciation = "",
                partOfSpeech = "",
                exampleText = "",
                exampleTranslation = ""
            )
        }

        // Original unchanged
        val unchanged = appContext.contentRepository!!.findById(ContentId("cnt-1"))!!
        assertEquals("Question 1", unchanged.text.primaryText)
    }

    // ---------------------------------------------------------------------------
    // TC06 — deleteContent removes content and owned learning items, leaves no orphans
    // ---------------------------------------------------------------------------
    @Test
    fun `TC06 deleteContent removes content and owned learning items while preserving unrelated records`() {
        val (appContext, _) = createFixture(contentCount = 3)
        val service = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository!!,
            installedPackageRepository = appContext.installedPackageRepository!!,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        val targetId = ContentId("cnt-2")
        val initialItems = appContext.learningItemRepository!!.findAllEnabled()
        val ownedItemIds = initialItems.filter { it.contentId == targetId }.map { it.id }.toSet()
        assertEquals(2, ownedItemIds.size)

        service.deleteContent(
            contentId = targetId,
            learningItemRepository = appContext.learningItemRepository!!,
            installedPackageId = InstalledPackageId("inst-edit-svc")
        )

        // Target content deleted
        val deletedContent = appContext.contentRepository!!.findById(targetId)
        assertNull(deletedContent)

        // Unrelated content remains
        assertNotNull(appContext.contentRepository!!.findById(ContentId("cnt-1")))
        assertNotNull(appContext.contentRepository!!.findById(ContentId("cnt-3")))

        // Owned learning items removed, no orphans remain
        val remainingItems = appContext.learningItemRepository!!.findAllEnabled()
        assertEquals(4, remainingItems.size) // 6 - 2 = 4
        assertTrue(remainingItems.none { it.contentId == targetId })
        assertTrue(remainingItems.none { it.id in ownedItemIds })
    }

    // ---------------------------------------------------------------------------
    // TC07 — deleteContent updates ContentLibrary and InstalledPackage
    // ---------------------------------------------------------------------------
    @Test
    fun `TC07 deleteContent updates ContentLibrary and InstalledPackage records`() {
        val (appContext, instId) = createFixture(contentCount = 3)
        val service = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository!!,
            installedPackageRepository = appContext.installedPackageRepository!!,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        val targetId = ContentId("cnt-1")
        service.deleteContent(
            contentId = targetId,
            learningItemRepository = appContext.learningItemRepository!!,
            installedPackageId = instId
        )

        // ContentLibrary updated
        val lib = appContext.contentLibraryRepository!!.findById(ContentLibraryId("lib-edit-svc"))!!
        assertFalse(lib.contains(targetId))
        assertEquals(2, lib.contentCount)

        // InstalledPackage updated
        val instPkg = appContext.installedPackageRepository!!.findById(instId)!!
        assertEquals(2, instPkg.contentCount)
        assertEquals(4, instPkg.learningItemCount)
    }

    // ---------------------------------------------------------------------------
    // TC08 — deleteContent with invalid contentId throws IllegalArgumentException
    // ---------------------------------------------------------------------------
    @Test
    fun `TC08 deleteContent with invalid contentId throws IllegalArgumentException`() {
        val (appContext, instId) = createFixture(contentCount = 2)
        val service = ContentBrowserEditService(appContext.contentRepository!!)

        assertFails {
            service.deleteContent(
                contentId = ContentId("cnt-non-existent"),
                learningItemRepository = appContext.learningItemRepository!!,
                installedPackageId = instId
            )
        }
    }

    @Test
    fun `delete then undo restores exact aggregate identities memberships media and truthful counts`() {
        val (appContext, instId) = createFixture(contentCount = 2)
        val contentId = ContentId("cnt-1")
        val disabled = LearningItem(
            id = LearningItemId("item-1-disabled"),
            contentId = contentId,
            mode = LearningMode.entries.last(),
            isEnabled = false
        )
        appContext.learningItemRepository!!.save(disabled)
        val secondLibraryId = ContentLibraryId("lib-edit-svc-secondary")
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(secondLibraryId, LibraryDescriptor(name = "Secondary"), setOf(contentId))
        )
        val packageBefore = appContext.installedPackageRepository!!.findById(instId)!!
        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                packageBefore.id, packageBefore.libraryId, packageBefore.packageId, packageBefore.topicId,
                packageBefore.name, packageBefore.version, packageBefore.state, packageBefore.installedAt,
                packageBefore.contentCount, packageBefore.learningItemCount + 1, packageBefore.contentChecksum
            )
        )
        val originalContent = appContext.contentRepository!!.findById(contentId)!!
        val originalItems = appContext.learningItemRepository!!.findByContentId(contentId).toSet()
        val service = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository!!,
            installedPackageRepository = appContext.installedPackageRepository!!,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        val snapshot = service.deleteContent(contentId, appContext.learningItemRepository!!, instId)
        assertEquals(originalItems.size, snapshot.learningItems.size)
        assertEquals(setOf(ContentLibraryId("lib-edit-svc"), secondLibraryId), snapshot.libraryIds)
        assertEquals(2, appContext.installedPackageRepository!!.findById(instId)!!.learningItemCount)

        val afterDelete = appContext.installedPackageRepository!!.findById(instId)!!
        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                afterDelete.id, afterDelete.libraryId, afterDelete.packageId, afterDelete.topicId,
                afterDelete.name, afterDelete.version, afterDelete.state, afterDelete.installedAt,
                afterDelete.contentCount, afterDelete.learningItemCount + 4, "later-change"
            )
        )
        service.restoreDeletedContent(snapshot, appContext.learningItemRepository!!)

        assertEquals(originalContent, appContext.contentRepository!!.findById(contentId))
        assertEquals(originalItems, appContext.learningItemRepository!!.findByContentId(contentId).toSet())
        assertTrue(appContext.contentLibraryRepository!!.findById(ContentLibraryId("lib-edit-svc"))!!.contains(contentId))
        assertTrue(appContext.contentLibraryRepository!!.findById(secondLibraryId)!!.contains(contentId))
        val restoredPackage = appContext.installedPackageRepository!!.findById(instId)!!
        assertEquals(9, restoredPackage.learningItemCount)
        assertEquals("later-change", restoredPackage.contentChecksum)
    }

    @Test
    fun `undo refuses conflicting exact content identity without overwriting it`() {
        val (appContext, instId) = createFixture(contentCount = 1)
        val service = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository!!,
            installedPackageRepository = appContext.installedPackageRepository!!,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )
        val snapshot = service.deleteContent(ContentId("cnt-1"), appContext.learningItemRepository!!, instId)
        val conflicting = snapshot.content.copy(text = ContentText(primaryText = "Conflict"))
        appContext.contentRepository!!.save(conflicting)

        assertFails { service.restoreDeletedContent(snapshot, appContext.learningItemRepository!!) }
        assertEquals(conflicting, appContext.contentRepository!!.findById(conflicting.id))
        assertTrue(appContext.learningItemRepository!!.findByContentId(conflicting.id).isEmpty())
    }

    @Test
    fun `create rolls back every canonical write boundary and retry commits exactly once`() {
        CreateFailureBoundary.entries.forEach { boundary ->
            val persistence = java.nio.file.Files.createTempDirectory("create-rollback-${boundary.name.lowercase()}")
            val context = LearningApplicationFactory.createPersisted(persistence)
            val installedId = InstalledPackageId("installed-${boundary.name.lowercase()}")
            val packageId = PackageId("package-${boundary.name.lowercase()}")
            val contentLibraryId = ContentLibraryId("library-${boundary.name.lowercase()}")
            context.installedPackageRepository!!.save(
                InstalledPackage.reconstitute(
                    id = installedId,
                    libraryId = context.defaultLibraryId!!,
                    packageId = packageId,
                    topicId = TopicId.deriveForLegacyPackage("Rollback Package", "OPD3"),
                    name = PackageName("Rollback Package"),
                    version = PackageVersion("1.0.0"),
                    state = PackageState.ACTIVE,
                    installedAt = java.time.Instant.EPOCH,
                    contentCount = 0,
                    learningItemCount = 0
                )
            )
            context.contentPackageRepository!!.save(
                ContentPackage(
                    id = packageId,
                    descriptor = PackageDescriptor("Rollback Package", "1.0.0", "OPD3"),
                    libraryIds = setOf(contentLibraryId)
                )
            )
            context.contentLibraryRepository!!.save(
                ContentLibrary(contentLibraryId, LibraryDescriptor("Rollback Library"), emptySet())
            )

            val contentRepo = context.contentRepository!!
            val itemRepo = context.learningItemRepository!!
            val libraryRepo = context.contentLibraryRepository!!
            val installedRepo = context.installedPackageRepository!!
            val failingContent = object : vn.loi.learning.application.port.ContentRepository by contentRepo {
                override fun save(content: Content) {
                    if (boundary == CreateFailureBoundary.CONTENT) error("injected content failure")
                    contentRepo.save(content)
                }
            }
            val failingItems = object : vn.loi.learning.application.port.LearningItemRepository by itemRepo {
                override fun saveAll(learningItems: List<LearningItem>) {
                    if (boundary == CreateFailureBoundary.LEARNING_ITEM) error("injected item failure")
                    itemRepo.saveAll(learningItems)
                }
            }
            val failingLibrary = object : vn.loi.learning.application.port.ContentLibraryRepository by libraryRepo {
                override fun save(library: ContentLibrary) {
                    if (boundary == CreateFailureBoundary.LIBRARY) error("injected library failure")
                    libraryRepo.save(library)
                }
            }
            val failingInstalled = object : vn.loi.learning.domain.library.repository.InstalledPackageRepository by installedRepo {
                override fun save(installedPackage: InstalledPackage) {
                    if (boundary == CreateFailureBoundary.INSTALLED_PACKAGE) error("injected package failure")
                    installedRepo.save(installedPackage)
                }
            }
            val failingService = ContentBrowserEditService(
                contentRepository = failingContent,
                contentLibraryRepository = failingLibrary,
                installedPackageRepository = failingInstalled,
                contentPackageRepository = context.contentPackageRepository,
                transactionRunner = requireNotNull(context.transactionRunner)
            )

            assertFails {
                failingService.createContent(
                    installedPackageId = installedId,
                    questionText = "Question",
                    answerText = "Answer",
                    learningItemRepository = failingItems
                )
            }

            val afterFailure = LearningApplicationFactory.createPersisted(persistence)
            assertTrue(afterFailure.contentRepository!!.findAll().isEmpty(), boundary.name)
            assertTrue(afterFailure.learningItemRepository!!.findAllEnabled().isEmpty(), boundary.name)
            assertEquals(0, afterFailure.contentLibraryRepository!!.findById(contentLibraryId)!!.contentCount, boundary.name)
            assertEquals(0, afterFailure.installedPackageRepository!!.findById(installedId)!!.contentCount, boundary.name)
            assertEquals(0, afterFailure.installedPackageRepository!!.findById(installedId)!!.learningItemCount, boundary.name)

            val retryService = ContentBrowserEditService(
                contentRepository = afterFailure.contentRepository!!,
                contentLibraryRepository = afterFailure.contentLibraryRepository,
                installedPackageRepository = afterFailure.installedPackageRepository,
                contentPackageRepository = afterFailure.contentPackageRepository,
                transactionRunner = requireNotNull(afterFailure.transactionRunner)
            )
            retryService.createContent(
                installedPackageId = installedId,
                questionText = "Question",
                answerText = "Answer",
                learningItemRepository = afterFailure.learningItemRepository
            )
            val afterRetry = LearningApplicationFactory.createPersisted(persistence)
            assertEquals(1, afterRetry.contentRepository!!.findAll().size, boundary.name)
            assertEquals(1, afterRetry.learningItemRepository!!.findAllEnabled().size, boundary.name)
            assertEquals(1, afterRetry.contentLibraryRepository!!.findById(contentLibraryId)!!.contentCount, boundary.name)
            assertEquals(1, afterRetry.installedPackageRepository!!.findById(installedId)!!.contentCount, boundary.name)
            assertEquals(1, afterRetry.installedPackageRepository!!.findById(installedId)!!.learningItemCount, boundary.name)
        }
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun createFixture(contentCount: Int): Pair<LearningApplicationContext, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-edit-svc")
        val pkgId = PackageId("pkg-edit-svc")
        val libId = ContentLibraryId("lib-edit-svc")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = appContext.defaultLibraryId!!,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Test Package", "OPD3"),
                name = PackageName("Test Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = java.time.Instant.now(),
                contentCount = contentCount,
                learningItemCount = contentCount * 2
            )
        )

        val contentIds = (1..contentCount).map { ContentId("cnt-$it") }.toSet()
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Test Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Test Library"),
                contentIds = contentIds
            )
        )

        for (i in 1..contentCount) {
            val cid = ContentId("cnt-$i")
            appContext.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "Question $i",
                        translatedText = "Answer $i",
                        pronunciation = "pron-$i"
                    ),
                    media = ContentMedia(
                        primaryAudio = if (i % 2 == 0) "audio-$i.mp3" else null,
                        image = if (i % 3 == 0) "img-$i.jpg" else null
                    ),
                    metadata = ContentMetadata(lesson = "Lesson $i", tags = setOf("tag-$i"))
                )
            )
            for (m in 1..2) {
                appContext.learningItemRepository!!.save(
                    LearningItem(
                        id = LearningItemId("item-$i-$m"),
                        contentId = cid,
                        mode = LearningMode.entries[(m - 1) % LearningMode.entries.size]
                    )
                )
            }
        }

        return appContext to instId
    }

    @Test
    fun `importMediaAsset stores a real image as canonical JPEG`() {
        val (appContext, _) = createFixture(contentCount = 1)
        val tempFile = java.io.File.createTempFile("test_source_", ".jpg")
        try {
            javax.imageio.ImageIO.write(
                java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_RGB),
                "jpg",
                tempFile
            )
            val tempDir = java.nio.file.Files.createTempDirectory("media_storage_test")
            try {
                val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempDir)
                val service = ContentBrowserEditService(appContext.contentRepository!!)

                val relativePath = service.importMediaAsset("TestPkg", tempFile, mediaStorage)
                assertTrue(relativePath.startsWith("TestPkg/"))
                assertTrue(relativePath.endsWith(".jpg"))
                assertFalse(relativePath.contains("no_image.jpg"))
                assertNotNull(javax.imageio.ImageIO.read(mediaStorage.resolve(relativePath)!!.toFile()))
            } finally {
                tempDir.toFile().deleteRecursively()
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `repairPackageMediaReferences clears no_image sentinels and canonicalizes extensions without touching items`() {
        val (appContext, instId) = createFixture(contentCount = 2)
        val contentRepo = appContext.contentRepository!!

        // Content 1: has no_image.jpg sentinel
        val c1 = contentRepo.findById(ContentId("cnt-1"))!!
        contentRepo.save(c1.copy(media = ContentMedia(image = "no_image.jpg")))

        // Content 2: has .png reference, but on disk it's .jpg
        val tempDir = java.nio.file.Files.createTempDirectory("media_repair_test")
        try {
            val pkgDir = tempDir.resolve("Test Package")
            java.nio.file.Files.createDirectories(pkgDir)
            val realJpg = pkgDir.resolve("real_photo.jpg")
            java.nio.file.Files.write(realJpg, "jpg-data".toByteArray())

            val c2 = contentRepo.findById(ContentId("cnt-2"))!!
            contentRepo.save(c2.copy(media = ContentMedia(image = "Test Package/real_photo.png")))

            val mediaStorage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempDir)
            val service = ContentBrowserEditService(
                contentRepository = contentRepo,
                contentLibraryRepository = appContext.contentLibraryRepository,
                installedPackageRepository = appContext.installedPackageRepository,
                contentPackageRepository = appContext.contentPackageRepository,
                mediaStorage = mediaStorage
            )

            val learningItemsBefore = appContext.learningItemRepository!!.findAll()

            val result = service.repairPackageMediaReferences(instId)
            assertEquals(2, result.totalInspected)
            assertEquals(1, result.noImageSentinelsCleared)
            assertEquals(1, result.extensionsCanonicalized)
            assertEquals(2, result.repairedContentIds.size)

            val updatedC1 = contentRepo.findById(ContentId("cnt-1"))!!
            assertNull(updatedC1.media.image)

            val updatedC2 = contentRepo.findById(ContentId("cnt-2"))!!
            assertEquals("Test Package/real_photo.jpg", updatedC2.media.image)

            // Verify learning items and text fields are strictly untouched
            val learningItemsAfter = appContext.learningItemRepository!!.findAll()
            assertEquals(learningItemsBefore, learningItemsAfter)
            assertEquals("Question 1", updatedC1.text.primaryText)
            assertEquals("Question 2", updatedC2.text.primaryText)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `updatePartOfSpeechMultiBatch updates multiple contents to distinct POS values and leaves others untouched`() {
        val (appContext, _) = createFixture(contentCount = 3)
        val contentRepo = appContext.contentRepository!!
        val service = ContentBrowserEditService(
            contentRepository = contentRepo,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        // Preset cnt-3 to already have "WORD" and "USER_CONFIRMED"
        val c3 = contentRepo.findById(ContentId("cnt-3"))!!
        contentRepo.save(c3.copy(customFields = ContentCustomFields(setOf(
            ContentCustomField(ContentFieldId("partOfSpeech"), "WORD"),
            ContentCustomField(ContentFieldId("partOfSpeechReviewStatus"), "USER_CONFIRMED")
        ))))

        val updates = mapOf(
            ContentId("cnt-1") to "NOUN",
            ContentId("cnt-2") to "VERB",
            ContentId("cnt-3") to "WORD" // Already "WORD" and USER_CONFIRMED
        )

        val result = service.updatePartOfSpeechMultiBatch(updates)
        assertEquals(3, result.selectedCount)
        assertEquals(2, result.changedCount)
        assertEquals(1, result.unchangedCount)

        val updated1 = contentRepo.findById(ContentId("cnt-1"))!!
        assertEquals("NOUN", updated1.customFields[ContentFieldId("partOfSpeech")]?.value)
        assertEquals("USER_CONFIRMED", updated1.customFields[ContentFieldId("partOfSpeechReviewStatus")]?.value)

        val updated2 = contentRepo.findById(ContentId("cnt-2"))!!
        assertEquals("VERB", updated2.customFields[ContentFieldId("partOfSpeech")]?.value)
        assertEquals("USER_CONFIRMED", updated2.customFields[ContentFieldId("partOfSpeechReviewStatus")]?.value)

        val updated3 = contentRepo.findById(ContentId("cnt-3"))!!
        assertEquals("WORD", updated3.customFields[ContentFieldId("partOfSpeech")]?.value)
        assertEquals("USER_CONFIRMED", updated3.customFields[ContentFieldId("partOfSpeechReviewStatus")]?.value)
    }

    @Test
    fun `unlockPartOfSpeechReviewBatch removes USER_CONFIRMED status and leaves POS and learning progress unchanged`() {
        val (appContext, _) = createFixture(contentCount = 2)
        val contentRepo = appContext.contentRepository!!
        val learningItemRepo = appContext.learningItemRepository!!
        val service = ContentBrowserEditService(
            contentRepository = contentRepo,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        // Mark cnt-1 as USER_CONFIRMED with NOUN
        val c1 = contentRepo.findById(ContentId("cnt-1"))!!
        contentRepo.save(c1.copy(customFields = ContentCustomFields(setOf(
            ContentCustomField(ContentFieldId("partOfSpeech"), "NOUN"),
            ContentCustomField(ContentFieldId("partOfSpeechReviewStatus"), "USER_CONFIRMED")
        ))))

        val learningItemsBefore = learningItemRepo.findAll()

        val result = service.unlockPartOfSpeechReviewBatch(listOf(ContentId("cnt-1"), ContentId("cnt-2")))
        assertEquals(2, result.selectedCount)
        assertEquals(1, result.changedCount) // only cnt-1 was USER_CONFIRMED

        val unlocked1 = contentRepo.findById(ContentId("cnt-1"))!!
        assertEquals("NOUN", unlocked1.customFields[ContentFieldId("partOfSpeech")]?.value)
        assertNull(unlocked1.customFields[ContentFieldId("partOfSpeechReviewStatus")])

        val untouched2 = contentRepo.findById(ContentId("cnt-2"))!!
        assertNull(untouched2.customFields[ContentFieldId("partOfSpeechReviewStatus")])

        val learningItemsAfter = learningItemRepo.findAll()
        assertEquals(learningItemsBefore, learningItemsAfter)
    }

    // ---------------------------------------------------------------------------
    // Sync Outbox Regression Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `Desktop canonical Question edit generates persistent outbox event`() {
        val root = Files.createTempDirectory("edit-sync-outbox-question-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            val contentId = ContentId("content-live-1")
            val original = Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "10 percent",
                    translatedText = "10 phan tram",
                    exampleText = "He gave a 10 percent discount.",
                    exampleTranslation = "Anh ay giam gia 10 phan tram."
                ),
                customFields = ContentCustomFields(setOf(
                    ContentCustomField(ContentFieldId("custom"), "preserve me")
                ))
            )
            context.contentRepository!!.save(original)

            val account = SyncAccountId("user-uat-account")
            val service = ContentBrowserEditService(
                contentRepository = context.contentRepository,
                transactionRunner = requireNotNull(context.transactionRunner),
                localSyncStateRepository = context.localSyncStateRepository,
                syncAccountProvider = { account },
                syncDeviceIdProvider = { SyncDeviceId("desktop-test") }
            )

            service.updateTextFields(
                contentId = contentId,
                questionText = "10 percent CHANGE TO TEST SYNS",
                answerText = "10 phan tram",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "He gave a 10 percent discount.",
                exampleTranslation = "Anh ay giam gia 10 phan tram."
            )

            val persisted = context.contentRepository.findById(contentId)!!
            assertEquals("10 percent CHANGE TO TEST SYNS", persisted.text.primaryText)
            assertEquals("preserve me", persisted.customFields[ContentFieldId("custom")]?.value)

            val pending = context.localSyncStateRepository!!.pendingOutbox(account)
            assertEquals(1, pending.size)
            val event = pending.single()
            assertEquals(account, event.accountId)
            assertEquals(SyncDeviceId("desktop-test"), event.sourceDeviceId)
            assertEquals(SyncEntityId("content-live-1"), event.entityId)
            val delta = event.delta as ContentFieldDelta
            assertEquals(ContentField.QUESTION, delta.field)
            assertEquals(DeltaOperation.SET, delta.operation)
            assertEquals("10 percent CHANGE TO TEST SYNS", delta.value)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Answer Example Translation field edits generate expected deltas and REMOVE when cleared`() {
        val root = Files.createTempDirectory("edit-sync-outbox-fields-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            val contentId = ContentId("content-multi-fields")
            val original = Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "Word",
                    translatedText = "Tu",
                    exampleText = "This is a word.",
                    exampleTranslation = "Day la mot tu."
                )
            )
            context.contentRepository!!.save(original)

            val account = SyncAccountId("user-multi")
            val service = ContentBrowserEditService(
                contentRepository = context.contentRepository,
                transactionRunner = requireNotNull(context.transactionRunner),
                localSyncStateRepository = context.localSyncStateRepository,
                syncAccountProvider = { account },
                syncDeviceIdProvider = { SyncDeviceId("desktop-test") }
            )

            // Edit Answer and Translation, clear Example
            service.updateTextFields(
                contentId = contentId,
                questionText = "Word",
                answerText = "Tu ngu",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "",
                exampleTranslation = "Day la mot tu ngu."
            )

            val pending = context.localSyncStateRepository!!.pendingOutbox(account)
            assertEquals(3, pending.size)
            assertEquals(listOf(ContentField.ANSWER, ContentField.EXAMPLE, ContentField.TRANSLATION), pending.map { (it.delta as ContentFieldDelta).field })

            val answerDelta = pending[0].delta as ContentFieldDelta
            assertEquals(DeltaOperation.SET, answerDelta.operation)
            assertEquals("Tu ngu", answerDelta.value)

            val exampleDelta = pending[1].delta as ContentFieldDelta
            assertEquals(DeltaOperation.REMOVE, exampleDelta.operation)
            assertNull(exampleDelta.value)

            val translationDelta = pending[2].delta as ContentFieldDelta
            assertEquals(DeltaOperation.SET, translationDelta.operation)
            assertEquals("Day la mot tu ngu.", translationDelta.value)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `unchanged save produces no outbox event and retry is idempotent`() {
        val root = Files.createTempDirectory("edit-sync-outbox-noop-")
        try {
            val context = LearningApplicationFactory.createPersisted(root, false)
            val contentId = ContentId("content-noop")
            val original = Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(primaryText = "Noop", translatedText = "Khong doi")
            )
            context.contentRepository!!.save(original)

            val account = SyncAccountId("user-noop")
            val service = ContentBrowserEditService(
                contentRepository = context.contentRepository,
                transactionRunner = requireNotNull(context.transactionRunner),
                localSyncStateRepository = context.localSyncStateRepository,
                syncAccountProvider = { account }
            )

            // Unchanged save
            service.updateTextFields(
                contentId = contentId,
                questionText = "Noop",
                answerText = "Khong doi",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "",
                exampleTranslation = ""
            )

            assertTrue(context.localSyncStateRepository!!.pendingOutbox(account).isEmpty())

            // Mutation 1
            service.updateTextFields(
                contentId = contentId,
                questionText = "Noop Changed",
                answerText = "Khong doi",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "",
                exampleTranslation = ""
            )
            assertEquals(1, context.localSyncStateRepository!!.pendingOutbox(account).size)

            // Retry with exact same mutated data -> no duplicate event
            service.updateTextFields(
                contentId = contentId,
                questionText = "Noop Changed",
                answerText = "Khong doi",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "",
                exampleTranslation = ""
            )
            assertEquals(1, context.localSyncStateRepository!!.pendingOutbox(account).size)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `local content mutation and outbox event survive restart and are pushed and acknowledged by sync`() {
        val root = Files.createTempDirectory("edit-sync-lifecycle-")
        try {
            val context1 = LearningApplicationFactory.createPersisted(root, false)
            val contentId = ContentId("content-lifecycle")
            val original = Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(primaryText = "Initial Question", translatedText = "Dap an")
            )
            context1.contentRepository!!.save(original)

            val account = SyncAccountId("user-synced")
            val service1 = ContentBrowserEditService(
                contentRepository = context1.contentRepository,
                transactionRunner = requireNotNull(context1.transactionRunner),
                localSyncStateRepository = context1.localSyncStateRepository,
                syncAccountProvider = { account },
                syncDeviceIdProvider = { SyncDeviceId("desktop-life") }
            )

            service1.updateTextFields(
                contentId = contentId,
                questionText = "Modified Question",
                answerText = "Dap an",
                pronunciation = "",
                partOfSpeech = "WORD",
                exampleText = "",
                exampleTranslation = ""
            )

            // Restart application
            val context2 = LearningApplicationFactory.createPersisted(root, false)
            val persistedContent = context2.contentRepository!!.findById(contentId)!!
            assertEquals("Modified Question", persistedContent.text.primaryText)

            val pending = context2.localSyncStateRepository!!.pendingOutbox(account)
            assertEquals(1, pending.size)

            // Manual sync push
            val transport = InMemorySyncTransport()
            val coordinator = SyncSessionCoordinator(context2.localSyncStateRepository!!, transport)
            val syncResult = coordinator.synchronize(account, SyncDeviceId("desktop-life"), 100) { remote ->
                context2.contentFieldSyncService!!.applyRemote(remote)
            }

            assertEquals(1, syncResult.pushed)
            assertTrue(context2.localSyncStateRepository!!.pendingOutbox(account).isEmpty())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
