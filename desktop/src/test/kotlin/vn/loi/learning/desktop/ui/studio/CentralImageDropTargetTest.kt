package vn.loi.learning.desktop.ui.studio

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class CentralImageDropTargetTest {

    private fun createHarness(initialImageRef: String? = null): Triple<ContentLibraryViewModel, JvmContentMediaStorage, String> {
        val appContext = LearningApplicationFactory.createInMemory()
        val tempMediaDir = Files.createTempDirectory("central-drop-test-media")
        val storage = JvmContentMediaStorage(tempMediaDir)

        val instId = InstalledPackageId("test-installed-pkg")
        val pkgId = PackageId("test-pkg")
        val libId = ContentLibraryId("test-lib")
        val contentId = ContentId("cnt-1")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = appContext.defaultLibraryId!!,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Central Drop Package", "OPD3"),
                name = PackageName("Central Drop Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
        )

        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Central Drop Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )

        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Test Library"),
                contentIds = setOf(contentId)
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = contentId,
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "Hello",
                    translatedText = "Xin chào",
                    pronunciation = "həˈloʊ",
                    exampleText = "Hello world",
                    exampleTranslation = "Chào thế giới"
                ),
                media = ContentMedia(
                    image = initialImageRef,
                    primaryAudio = null,
                    translatedAudio = null,
                    exampleAudio = null,
                    exampleTranslatedAudio = null
                ),
                metadata = ContentMetadata(lesson = "General")
            )
        )

        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("item-1-1"),
                contentId = contentId,
                mode = LearningMode.MEANING_RECOGNITION
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
        vm.browsePackageLessons(instId, "Central Drop Package")
        vm.attemptSelectRowAutoEdit(contentId.value)

        return Triple(vm, storage, contentId.value)
    }

    @Test
    fun `DragDropUtils validates supported image extensions and rejects non-images`() {
        val jpg = File.createTempFile("photo", ".jpg").apply { deleteOnExit() }
        val jpeg = File.createTempFile("photo", ".jpeg").apply { deleteOnExit() }
        val png = File.createTempFile("photo", ".png").apply { deleteOnExit() }
        val webp = File.createTempFile("photo", ".webp").apply { deleteOnExit() }

        val mp3 = File.createTempFile("audio", ".mp3").apply { deleteOnExit() }
        val txt = File.createTempFile("doc", ".txt").apply { deleteOnExit() }

        assertTrue(DragDropUtils.isSupportedImage(jpg))
        assertTrue(DragDropUtils.isSupportedImage(jpeg))
        assertTrue(DragDropUtils.isSupportedImage(png))
        assertTrue(DragDropUtils.isSupportedImage(webp))

        assertFalse(DragDropUtils.isSupportedImage(mp3))
        assertFalse(DragDropUtils.isSupportedImage(txt))
        assertFalse(DragDropUtils.isSupportedImage(File("non_existent.png")))
    }

    @Test
    fun `Dropping valid image onto empty central image area imports image and updates draft imageRef`() {
        val (vm, storage, _) = createHarness(initialImageRef = null)

        val tempImage = File.createTempFile("test_cat", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            deleteOnExit()
        }

        // Action invoked when file dropped on central image drop target
        vm.importDraftMediaFile(tempImage, "image")

        val state = vm.packageBrowserUiState!!
        val draft = state.draftEdits
        assertNotNull(draft)
        val imageRef = draft.imageRef
        assertNotNull(imageRef)
        assertTrue(imageRef.contains(tempImage.name))
        assertTrue(state.isDirty, "Draft must be marked dirty after drop")
        assertTrue(storage.exists(imageRef), "Imported file must exist in media storage")
    }

    @Test
    fun `Dropping valid image onto existing image replaces image reference with new imported file`() {
        val (vm, storage, _) = createHarness(initialImageRef = "media/old_image.png")

        val initialDraft = vm.packageBrowserUiState!!.draftEdits
        assertEquals("media/old_image.png", initialDraft?.imageRef)

        val replacementImage = File.createTempFile("test_dog", ".png").apply {
            writeBytes(byteArrayOf(9, 8, 7, 6))
            deleteOnExit()
        }

        // Action invoked on central drop target
        vm.importDraftMediaFile(replacementImage, "image")

        val updatedDraft = vm.packageBrowserUiState!!.draftEdits
        assertNotNull(updatedDraft)
        val updatedRef = updatedDraft.imageRef
        assertNotNull(updatedRef)
        assertTrue(updatedRef != "media/old_image.png")
        assertTrue(updatedRef.contains(replacementImage.name))
        assertTrue(storage.exists(updatedRef))
    }

    @Test
    fun `Dropping image preserves unsaved text changes in draft`() {
        val (vm, _, _) = createHarness(initialImageRef = null)

        // User makes text edits before dropping image
        vm.updateDraftQuestion("New Question Edited")
        vm.updateDraftExampleTranslation("Bản dịch mới chưa lưu")
        vm.updateDraftPartOfSpeech("verb")

        val imageFile = File.createTempFile("preserved_fields", ".webp").apply {
            writeBytes(byteArrayOf(42, 43))
            deleteOnExit()
        }

        vm.importDraftMediaFile(imageFile, "image")

        val draft = vm.packageBrowserUiState!!.draftEdits
        assertNotNull(draft)
        // Image is updated
        val imageRef = draft.imageRef
        assertNotNull(imageRef)
        assertTrue(imageRef.contains(imageFile.name))
        // All unsaved text fields remain intact
        assertEquals("New Question Edited", draft.questionText)
        assertEquals("Bản dịch mới chưa lưu", draft.exampleTranslation)
        assertEquals("verb", draft.partOfSpeech)
        assertEquals("Xin chào", draft.answerText)
        assertEquals("Hello world", draft.exampleText)
        assertEquals("həˈloʊ", draft.pronunciation)
    }

    @Test
    fun `Dropping unsupported file on central image slot performs zero draft mutation`() {
        val (vm, _, _) = createHarness(initialImageRef = null)

        val beforeDraft = vm.packageBrowserUiState!!.draftEdits
        val mp3File = File.createTempFile("audio", ".mp3").apply {
            writeBytes(byteArrayOf(10, 20))
            deleteOnExit()
        }

        // Validate that DragDropUtils rejects non-image
        assertFalse(DragDropUtils.isSupportedImage(mp3File))

        // Even if an unknown slot is called, zero draft mutation happens
        vm.importDraftMediaFile(mp3File, "unsupported_slot")

        assertEquals(beforeDraft, vm.packageBrowserUiState!!.draftEdits)
        assertTrue(vm.uiState.importError?.contains("Unknown media slot") == true)
    }
}
