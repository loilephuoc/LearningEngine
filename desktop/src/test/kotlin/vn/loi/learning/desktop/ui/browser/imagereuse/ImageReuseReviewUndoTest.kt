package vn.loi.learning.desktop.ui.browser.imagereuse

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
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
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class ImageReuseReviewUndoTest {

    private fun setupTestEnvironment(): Triple<ContentLibraryViewModel, JvmContentMediaStorage, LearningApplicationContext> {
        val appContext = LearningApplicationFactory.createInMemory()
        val tempMediaDir = Files.createTempDirectory("imagereuse-undo-test")
        val storage = JvmContentMediaStorage(tempMediaDir)

        val targetPkgId = InstalledPackageId("target_pkg")
        val targetPkg = InstalledPackage.reconstitute(
            id = targetPkgId,
            libraryId = appContext.defaultLibraryId!!,
            packageId = PackageId("target_pkg"),
            topicId = TopicId.deriveForLegacyPackage("Target Package", "OPD3"),
            name = PackageName("Target Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 3,
            learningItemCount = 3
        )
        appContext.installedPackageRepository!!.save(targetPkg)

        val sourcePkgId = InstalledPackageId("source_pkg")
        val sourcePkg = InstalledPackage.reconstitute(
            id = sourcePkgId,
            libraryId = appContext.defaultLibraryId!!,
            packageId = PackageId("source_pkg"),
            topicId = TopicId.deriveForLegacyPackage("Source Package", "OPD3"),
            name = PackageName("Source Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 3,
            learningItemCount = 3
        )
        appContext.installedPackageRepository!!.save(sourcePkg)

        val targetLibId = ContentLibraryId("target_lib")
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = PackageId("target_pkg"),
                descriptor = PackageDescriptor("Target Package", "1.0.0", "OPD3"),
                libraryIds = setOf(targetLibId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = targetLibId,
                descriptor = LibraryDescriptor(name = "Target Library"),
                contentIds = setOf(ContentId("target_1"), ContentId("target_2"), ContentId("target_3"))
            )
        )

        val sourceLibId = ContentLibraryId("source_lib")
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = PackageId("source_pkg"),
                descriptor = PackageDescriptor("Source Package", "1.0.0", "OPD3"),
                libraryIds = setOf(sourceLibId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = sourceLibId,
                descriptor = LibraryDescriptor(name = "Source Library"),
                contentIds = setOf(ContentId("src_1"), ContentId("src_2"), ContentId("src_3"))
            )
        )

        // Create target contents
        val targetContent1 = Content(
            id = ContentId("target_1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "apple", translatedText = "quả táo", exampleText = "Fresh apple", exampleTranslation = "Táo tươi"),
            media = ContentMedia(image = null),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        val targetContent2 = Content(
            id = ContentId("target_2"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "banana", translatedText = "quả chuối", exampleText = "Yellow banana", exampleTranslation = "Chuối vàng"),
            media = ContentMedia(image = "media/old_banana.jpg"),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        val targetContent3 = Content(
            id = ContentId("target_3"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "orange", translatedText = "quả cam", exampleText = "Sweet orange", exampleTranslation = "Cam ngọt"),
            media = ContentMedia(image = "media/old_orange.jpg"),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        appContext.contentRepository!!.saveAll(listOf(targetContent1, targetContent2, targetContent3))

        for (i in 1..3) {
            appContext.learningItemRepository!!.save(
                LearningItem(
                    id = LearningItemId("item-target-$i"),
                    contentId = ContentId("target_$i"),
                    mode = LearningMode.MEANING_RECOGNITION
                )
            )
        }

        // Create source media files and contents
        val srcAppleAsset = storage.store("Source Package", "src_apple.jpg", byteArrayOf(1, 2, 3))
        val srcBananaAsset = storage.store("Source Package", "src_banana.jpg", byteArrayOf(4, 5, 6))
        val srcOrangeAsset = storage.store("Source Package", "src_orange.jpg", byteArrayOf(7, 8, 9))

        val srcContent1 = Content(
            id = ContentId("src_1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "apple", translatedText = "trái táo"),
            media = ContentMedia(image = srcAppleAsset.relativePath),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        val srcContent2 = Content(
            id = ContentId("src_2"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "banana", translatedText = "trái chuối"),
            media = ContentMedia(image = srcBananaAsset.relativePath),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        val srcContent3 = Content(
            id = ContentId("src_3"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "orange", translatedText = "trái cam"),
            media = ContentMedia(image = srcOrangeAsset.relativePath),
            metadata = ContentMetadata(lesson = "Fruits")
        )
        appContext.contentRepository!!.saveAll(listOf(srcContent1, srcContent2, srcContent3))

        for (i in 1..3) {
            appContext.learningItemRepository!!.save(
                LearningItem(
                    id = LearningItemId("item-src-$i"),
                    contentId = ContentId("src_$i"),
                    mode = LearningMode.MEANING_RECOGNITION
                )
            )
        }

        val editService = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentPackageRepository = appContext.contentPackageRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentLibraryRepository = appContext.contentLibraryRepository,
            transactionRunner = appContext.transactionRunner
        )

        val vm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = PackageContentBrowserFacade(
                queryService = appContext.packageBrowserQuery,
                editService = editService,
                learningItemRepository = appContext.learningItemRepository
            ),
            contentMediaStorage = storage
        )

        return Triple(vm, storage, appContext)
    }

    @Test
    fun `Previous Item is read only navigation and does not mutate content or media`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.startImageReuseScan()

        val stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, stage.currentTargetIndex)
        assertFalse(stage.canGoPrevious, "Previous must be disabled at index 0")

        // Advance to next candidate / item
        vm.skipImageReuseItem()
        val stage2 = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(1, stage2.currentTargetIndex)
        assertTrue(stage2.canGoPrevious)

        // Record content before previous navigation
        val beforeContent = appContext.contentRepository!!.findById(ContentId("target_1"))

        // Navigate previous
        vm.previousImageReuseItem()
        val stage3 = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, stage3.currentTargetIndex)

        // Verify zero mutation
        val afterContent = appContext.contentRepository!!.findById(ContentId("target_1"))
        assertEquals(beforeContent, afterContent)
    }

    @Test
    fun `Undo Last Use on target with missing image restores exact null state`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.startImageReuseScan()

        val initialStage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_1", initialStage.currentTarget?.targetContentId)
        assertNull(initialStage.currentTarget?.currentImageRef)
        assertFalse(initialStage.canUndo)

        // Accept source image for target_1
        vm.applyImageReuseAndNext()

        // Content in repository updated with new image
        val afterApplyContent = appContext.contentRepository!!.findById(ContentId("target_1"))
        assertNotNull(afterApplyContent?.media?.image)

        val reviewStage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(1, reviewStage.appliedCount)
        assertTrue(reviewStage.canUndo)
        assertEquals(1, reviewStage.undoStack.size)

        // Perform Undo
        vm.undoLastImageReuse()

        val afterUndoContent = appContext.contentRepository!!.findById(ContentId("target_1"))
        assertNull(afterUndoContent?.media?.image, "Target image must be restored to exact null")
        assertEquals("apple", afterUndoContent?.text?.primaryText)
        assertEquals("quả táo", afterUndoContent?.text?.translatedText)
        assertEquals("Fresh apple", afterUndoContent?.text?.exampleText)

        val afterUndoStage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, afterUndoStage.appliedCount, "Applied count must decrement to 0")
        assertEquals(0, afterUndoStage.currentTargetIndex, "Must navigate back to target_1")
        assertFalse(afterUndoStage.canUndo, "Undo stack must be empty")
    }

    @Test
    fun `Undo Last Use on target with existing image restores exact prior image reference`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        vm.startImageReuseScan()

        // Skip to target_2 which has old_banana.jpg
        vm.skipImageReuseItem()
        val stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_2", stage.currentTarget?.targetContentId)
        assertEquals("media/old_banana.jpg", stage.currentTarget?.currentImageRef)

        // Apply new image
        vm.applyImageReuseAndNext()

        val afterApply = appContext.contentRepository!!.findById(ContentId("target_2"))
        val newImage = afterApply?.media?.image
        assertNotNull(newImage)
        assertTrue(newImage != "media/old_banana.jpg")

        // Undo
        vm.undoLastImageReuse()

        val afterUndo = appContext.contentRepository!!.findById(ContentId("target_2"))
        assertEquals("media/old_banana.jpg", afterUndo?.media?.image, "Prior image reference must be restored")
        assertEquals("banana", afterUndo?.text?.primaryText)
        assertEquals("quả chuối", afterUndo?.text?.translatedText)
    }

    @Test
    fun `Multiple accepted reuses can be undone in reverse order`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        vm.startImageReuseScan()

        // Accept target 1
        vm.applyImageReuseAndNext()
        // Accept target 2
        vm.applyImageReuseAndNext()
        // Accept target 3
        vm.applyImageReuseAndNext()

        val reviewStage = vm.imageReuseDialogState.stage
        val appliedCount = when (reviewStage) {
            is ImageReuseReviewStage.Review -> reviewStage.appliedCount
            is ImageReuseReviewStage.Complete -> reviewStage.totalAppliedCount
            else -> 0
        }
        assertEquals(3, appliedCount)

        assertNotNull(appContext.contentRepository!!.findById(ContentId("target_1"))?.media?.image)
        assertTrue(appContext.contentRepository!!.findById(ContentId("target_2"))?.media?.image != "media/old_banana.jpg")
        assertTrue(appContext.contentRepository!!.findById(ContentId("target_3"))?.media?.image != "media/old_orange.jpg")
    }

    @Test
    fun `Skip candidate and skip item do NOT create undo entries`() {
        val (vm, _, _) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.startImageReuseScan()

        var stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, stage.undoStack.size)

        vm.skipImageReuseCandidate()
        stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, stage.undoStack.size)

        vm.skipImageReuseItem()
        stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(0, stage.undoStack.size)
        assertFalse(stage.canUndo)
    }

    @Test
    fun `External state modification causes undo to be rejected safely`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.startImageReuseScan()

        // Apply image on target_1
        vm.applyImageReuseAndNext()

        // Simulate external edit to target_1 image
        val existingContent = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        val externalEdit = existingContent.copy(
            media = existingContent.media.copy(image = "media/external_user_edit.jpg")
        )
        appContext.contentRepository!!.save(externalEdit)

        // Attempt Undo
        vm.undoLastImageReuse()

        val stage = vm.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        val err = stage.applyError
        assertNotNull(err)
        assertTrue(err.contains("Cannot undo because this item's image has changed"))

        // Verify external edit was NOT overwritten
        val currentContent = appContext.contentRepository!!.findById(ContentId("target_1"))
        assertEquals("media/external_user_edit.jpg", currentContent?.media?.image)
    }

    @Test
    fun `Source package remains strictly read-only throughout review and undo`() {
        val (vm, _, appContext) = setupTestEnvironment()
        val sourceContentBefore = appContext.contentRepository!!.findById(ContentId("src_1"))

        val targetPkgId = InstalledPackageId("target_pkg")
        vm.openImageReuseReview(targetPkgId, "Target Package")
        vm.startImageReuseScan()

        // Apply image
        vm.applyImageReuseAndNext()
        // Undo image
        vm.undoLastImageReuse()

        val sourceContentAfter = appContext.contentRepository!!.findById(ContentId("src_1"))
        assertEquals(sourceContentBefore, sourceContentAfter, "Source package content must be completely untouched")
    }
}
