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

class ImageReuseExplicitTargetIntentTest {

    private fun setupTestEnvironment(): Triple<ContentLibraryViewModel, JvmContentMediaStorage, LearningApplicationContext> {
        val appContext = LearningApplicationFactory.createInMemory()
        val tempMediaDir = Files.createTempDirectory("imagereuse-intent-test")
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
            contentCount = 2,
            learningItemCount = 2
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
            contentCount = 2,
            learningItemCount = 2
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
                contentIds = setOf(ContentId("target_1"), ContentId("target_2"))
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
                contentIds = setOf(ContentId("source_1"), ContentId("source_2"))
            )
        )

        // Store source images in media storage
        val srcAppleAsset = storage.store("Source Package", "source_1.jpg", "source1_image_bytes".toByteArray())
        val srcBananaAsset = storage.store("Source Package", "source_2.jpg", "source2_image_bytes".toByteArray())
        val targetBananaAsset = storage.store("Target Package", "old_banana.jpg", "old_banana_image_bytes".toByteArray())

        // Create initial content records
        appContext.contentRepository!!.save(
            Content(
                id = ContentId("target_1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "apple",
                    translatedText = "quả táo",
                    pronunciation = "/ˈæp.əl/",
                    exampleText = "I eat an apple.",
                    exampleTranslation = "Tôi ăn một quả táo."
                ),
                media = ContentMedia(image = null),
                metadata = ContentMetadata(lesson = "Unit 1")
            )
        )
        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("learn_t1"),
                contentId = ContentId("target_1"),
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("target_2"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "banana",
                    translatedText = "quả chuối",
                    pronunciation = "/bəˈnæn.ə/",
                    exampleText = "I eat a banana.",
                    exampleTranslation = "Tôi ăn một quả chuối."
                ),
                media = ContentMedia(image = targetBananaAsset.relativePath),
                metadata = ContentMetadata(lesson = "Unit 1")
            )
        )
        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("learn_t2"),
                contentId = ContentId("target_2"),
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("source_1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "apple",
                    translatedText = "trái táo đỏ",
                    pronunciation = "/ˈæp.əl/",
                    exampleText = "An apple a day.",
                    exampleTranslation = "Mỗi ngày một quả táo."
                ),
                media = ContentMedia(image = srcAppleAsset.relativePath),
                metadata = ContentMetadata(lesson = "Unit 1")
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("source_2"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "banana",
                    translatedText = "trái chuối vàng",
                    pronunciation = "/bəˈnæn.ə/",
                    exampleText = "Sweet banana.",
                    exampleTranslation = "Quả chuối ngọt."
                ),
                media = ContentMedia(image = srcBananaAsset.relativePath),
                metadata = ContentMetadata(lesson = "Unit 1")
            )
        )

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
    fun `user Replace target image wins precedence over source candidate`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        // Open Image Reuse Review
        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        val reviewStage = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals(2, reviewStage.totalTargets)
        assertEquals("target_1", reviewStage.currentTarget?.targetContentId)

        // Create a user replacement image file
        val tempImg = File.createTempFile("user_custom_apple", ".png")
        val bi = java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB)
        javax.imageio.ImageIO.write(bi, "png", tempImg)

        // User Replaces target image
        viewModel.replaceImageReuseTargetImage(tempImg)

        val updatedStage = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertTrue(updatedStage.isTargetDraftDirty)
        assertTrue(updatedStage.effectiveTargetDraft.imageIntent is TargetImageIntent.Replace)
        val customImagePath = (updatedStage.effectiveTargetDraft.imageIntent as TargetImageIntent.Replace).imageRef
        assertTrue(customImagePath.contains("user_custom_apple"))

        // Click Save & Next (applyImageReuseAndNext)
        viewModel.applyImageReuseAndNext()

        // Verify target_1 in DB has custom image, NOT source candidate image
        val savedTarget1 = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        assertEquals(customImagePath, savedTarget1.media.image)
        assertFalse(savedTarget1.media.image!!.contains("source_1.jpg"))

        // Source package remains untouched
        val source1 = appContext.contentRepository!!.findById(ContentId("source_1"))!!
        assertTrue(source1.media.image!!.contains("source_1.jpg"))
    }

    @Test
    fun `user Remove target image sets image to null without falling back to source`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // Advance to target_2 which already has an old image ("Target Package/old_banana.jpg")
        viewModel.skipImageReuseItem()
        val reviewStage = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_2", reviewStage.currentTarget?.targetContentId)
        assertTrue(reviewStage.currentTarget?.currentImageRef!!.contains("old_banana.jpg"))

        // User explicitly clicks Remove
        viewModel.removeImageReuseTargetImage()

        val updatedStage = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertTrue(updatedStage.isTargetDraftDirty)
        assertEquals(TargetImageIntent.Remove, updatedStage.effectiveTargetDraft.imageIntent)
        assertNull(updatedStage.effectiveTargetImageRef)

        // Click Save & Next
        viewModel.applyImageReuseAndNext()

        // Verify target_2 in DB has image = null
        val savedTarget2 = appContext.contentRepository!!.findById(ContentId("target_2"))!!
        assertNull(savedTarget2.media.image)
    }

    @Test
    fun `user editing text fields auto-saves on Skip Candidate and preserves target`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        val reviewStage = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_1", reviewStage.currentTarget?.targetContentId)

        // User edits target question and answer
        viewModel.updateImageReuseTargetQuestion("red apple")
        viewModel.updateImageReuseTargetAnswer("quả táo đỏ tươi")
        viewModel.updateImageReuseTargetPartOfSpeech("idiom")

        // User clicks Skip Candidate instead of Use Image
        viewModel.skipImageReuseCandidate()

        // Verify target_1 in DB has updated text, while image remains null (since Unchanged)
        val savedTarget1 = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        assertEquals("red apple", savedTarget1.text.primaryText)
        assertEquals("quả táo đỏ tươi", savedTarget1.text.translatedText)
        assertEquals("idiom", savedTarget1.customFields[vn.loi.learning.domain.content.model.ContentFieldId("partOfSpeech")]?.value)
        assertNull(savedTarget1.media.image)
    }

    @Test
    fun `user editing text fields auto-saves on Close dialog`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // User edits target translation and example
        viewModel.updateImageReuseTargetTranslation("Bản dịch mới")
        viewModel.updateImageReuseTargetExample("New example sentence.")

        // User closes dialog
        viewModel.closeImageReuseReview()

        // Verify target_1 in DB has auto-saved text changes
        val savedTarget1 = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        assertEquals("Bản dịch mới", savedTarget1.text.exampleTranslation)
        assertEquals("New example sentence.", savedTarget1.text.exampleText)
        assertFalse(viewModel.imageReuseDialogState.visible)
    }

    @Test
    fun `user editing text fields auto-saves on Previous Item`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // Skip to target_2
        viewModel.skipImageReuseItem()
        val stageAt2 = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_2", stageAt2.currentTarget?.targetContentId)

        // Edit target_2
        viewModel.updateImageReuseTargetAnswer("chuối tươi ngọt")

        // Click Previous Item
        viewModel.previousImageReuseItem()

        // Verify target_2 was saved to DB
        val savedTarget2 = appContext.contentRepository!!.findById(ContentId("target_2"))!!
        assertEquals("chuối tươi ngọt", savedTarget2.text.translatedText)

        // Verify now on target_1
        val stageAt1 = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_1", stageAt1.currentTarget?.targetContentId)
    }

    @Test
    fun `user editing text fields auto-saves on Skip This Item`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // Edit target_1
        viewModel.updateImageReuseTargetAnswer("táo ngon")

        // Click Skip This Item
        viewModel.skipImageReuseItem()

        // Verify target_1 was saved to DB
        val savedTarget1 = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        assertEquals("táo ngon", savedTarget1.text.translatedText)

        // Verify now on target_2
        val stageAt2 = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_2", stageAt2.currentTarget?.targetContentId)
    }

    @Test
    fun `text-only edit on target item with existing image preserves target image and does not apply source image`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // Skip to target_2 which already has an image: "Target Package/old_banana.jpg"
        viewModel.skipImageReuseItem()
        val stageAt2 = viewModel.imageReuseDialogState.stage as ImageReuseReviewStage.Review
        assertEquals("target_2", stageAt2.currentTarget?.targetContentId)
        val originalTarget2Image = stageAt2.currentTarget?.currentImageRef
        assertNotNull(originalTarget2Image)
        assertTrue(originalTarget2Image.contains("old_banana.jpg"))

        // User edits Answer text only without touching image
        val localDraft = stageAt2.effectiveTargetDraft.copy(
            answer = "người độc thân"
        )
        assertTrue(localDraft.isDirty(stageAt2.currentTarget!!))
        assertEquals(TargetImageIntent.Unchanged, localDraft.imageIntent)

        // User clicks Save & Next (submitting local draft)
        viewModel.applyImageReuseAndNext(localDraft)

        // Verify target_2 in DB has updated answer AND retains original target image (Source candidate image is NOT applied)
        val savedTarget2 = appContext.contentRepository!!.findById(ContentId("target_2"))!!
        assertEquals("người độc thân", savedTarget2.text.translatedText)
        assertEquals(originalTarget2Image, savedTarget2.media.image)
        assertFalse(savedTarget2.media.image!!.contains("source_2.jpg"))

        // Source item remains untouched
        val source2 = appContext.contentRepository!!.findById(ContentId("source_2"))!!
        assertEquals("source_2.jpg", File(source2.media.image!!).name)
    }

    @Test
    fun `default Use Source Image copies candidate image and preserves text`() {
        val (viewModel, storage, appContext) = setupTestEnvironment()

        viewModel.openImageReuseReview(
            installedPackageId = InstalledPackageId("target_pkg"),
            packageName = "Target Package"
        )
        viewModel.updateImageReuseScope(ImageReuseScope.ALL_MATCHING_ITEMS)
        viewModel.startImageReuseScan()

        // Click Use Source Image & Next without any manual target image decision
        viewModel.applyImageReuseAndNext()

        // Verify target_1 in DB has copied image from Source Package
        val savedTarget1 = appContext.contentRepository!!.findById(ContentId("target_1"))!!
        assertNotNull(savedTarget1.media.image)
        assertTrue(savedTarget1.media.image!!.contains("source_1.jpg"))
    }
}
