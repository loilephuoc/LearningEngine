package vn.loi.learning.desktop.ui.studio

import java.io.File
import java.io.ByteArrayOutputStream
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.time.Instant
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertIs
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
    fun `Dropping valid image onto empty central image area imports image, auto-saves immediately, and reloads canonical item`() {
        val (vm, storage, contentId) = createHarness(initialImageRef = null)

        val tempImage = File.createTempFile("test_cat", ".jpg").apply {
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(40, 30, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", this)
            deleteOnExit()
        }

        // Action invoked when file dropped on central image drop target
        vm.importDraftMediaFile(tempImage, "image")

        val state = vm.packageBrowserUiState!!
        val draft = state.draftEdits
        assertNotNull(draft)
        val imageRef = draft.imageRef
        assertNotNull(imageRef)
        assertTrue(imageRef.endsWith(".jpg"))
        assertFalse(state.isDirty, "Existing item must be auto-saved and clean after image drop")
        assertTrue(storage.exists(imageRef), "Imported file must exist in media storage")
        assertEquals(imageRef, state.selectedItemAnywhere?.imageRef, "Canonical item must reflect new imageRef after auto-save")
    }

    @Test
    fun `Dropping valid image onto existing image replaces image reference, auto-saves, and reloads canonical item`() {
        val (vm, storage, contentId) = createHarness(initialImageRef = "media/old_image.png")

        val initialDraft = vm.packageBrowserUiState!!.draftEdits
        assertEquals("media/old_image.png", initialDraft?.imageRef)

        val replacementImage = File.createTempFile("test_dog", ".png").apply {
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(50, 35, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", this)
            deleteOnExit()
        }

        // Action invoked on central drop target
        vm.importDraftMediaFile(replacementImage, "image")

        val state = vm.packageBrowserUiState!!
        val updatedDraft = state.draftEdits
        assertNotNull(updatedDraft)
        val updatedRef = updatedDraft.imageRef
        assertNotNull(updatedRef)
        assertTrue(updatedRef != "media/old_image.png")
        assertTrue(updatedRef.endsWith(".jpg"))
        assertTrue(storage.exists(updatedRef))
        assertFalse(state.isDirty, "Existing item must be clean after image replace auto-save")
        assertEquals(updatedRef, state.selectedItemAnywhere?.imageRef)
    }

    @Test
    fun `Dropping image preserves unsaved text changes in draft and persists both`() {
        val (vm, _, _) = createHarness(initialImageRef = null)

        // User makes text edits before dropping image
        vm.updateDraftQuestion("New Question Edited")
        vm.updateDraftExampleTranslation("Bản dịch mới chưa lưu")
        vm.updateDraftPartOfSpeech("verb")

        val imageFile = File.createTempFile("preserved_fields", ".png").apply {
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(45, 35, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", this)
            deleteOnExit()
        }

        vm.importDraftMediaFile(imageFile, "image")

        val state = vm.packageBrowserUiState!!
        val draft = state.draftEdits
        assertNotNull(draft)
        // Image is updated
        val imageRef = draft.imageRef
        assertNotNull(imageRef)
        assertTrue(imageRef.endsWith(".jpg"))
        // All unsaved text fields remain intact and are persisted together
        assertEquals("New Question Edited", draft.questionText)
        assertEquals("Bản dịch mới chưa lưu", draft.exampleTranslation)
        assertEquals("verb", draft.partOfSpeech)
        assertEquals("Xin chào", draft.answerText)
        assertEquals("Hello world", draft.exampleText)
        assertEquals("həˈloʊ", draft.pronunciation)
        assertFalse(state.isDirty, "Draft is persisted and clean")
    }

    @Test
    fun `Importing image when creating new item does NOT auto-persist`() {
        val (vm, storage, _) = createHarness(initialImageRef = null)
        vm.startNewItem()

        val tempImage = File.createTempFile("new_item_cat", ".jpg").apply {
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(40, 30, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", this)
            deleteOnExit()
        }

        vm.importDraftMediaFile(tempImage, "image")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isCreatingNewItem, "Must still be creating new item")
        assertTrue(state.isDirty, "New item draft must remain dirty")
        assertNotNull(state.draftEdits?.imageRef)
        assertTrue(state.draftEdits!!.imageRef!!.endsWith(".jpg"))
    }

    @Test
    fun `Importing audio slot does NOT auto-persist and keeps draft dirty`() {
        val (vm, storage, _) = createHarness(initialImageRef = null)

        val audioFile = File.createTempFile("question_audio", ".mp3").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }

        vm.importDraftMediaFile(audioFile, "question")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isDirty, "Draft must remain dirty after audio import")
        assertNotNull(state.draftEdits?.questionAudioRef)
        assertTrue(state.draftEdits!!.questionAudioRef!!.contains(audioFile.name))
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

    @Test
    fun `browser data image uses canonical import and preserves existing unsaved text`() = runBlocking {
        val (vm, storage, _) = createHarness(initialImageRef = "media/old.png")
        vm.updateDraftQuestion("Unsaved browser-drop question")
        val beforeAnswer = vm.packageBrowserUiState!!.draftEdits!!.answerText

        BrowserImageDropExtractor().extractOwnedForCentralTest(browserDataTransferable()).use { extracted ->
            vm.importDraftMediaFile(extracted.file, "image")
        }

        val state = vm.packageBrowserUiState!!
        val imageRef = requireNotNull(state.draftEdits?.imageRef)
        assertTrue(imageRef.endsWith(".jpg"))
        assertTrue(storage.exists(imageRef))
        assertTrue(ImageIO.read(storage.resolve(imageRef)!!.toFile()) != null)
        assertEquals("Unsaved browser-drop question", state.draftEdits?.questionText)
        assertEquals(beforeAnswer, state.draftEdits?.answerText)
        assertEquals(imageRef, state.selectedItemAnywhere?.imageRef)
        assertFalse(state.isDirty)
    }

    @Test
    fun `browser data image keeps a new item unpersisted and acquisition failure is atomic`() = runBlocking {
        val (vm, _, _) = createHarness(initialImageRef = "media/original.jpg")
        vm.startNewItem()
        vm.updateDraftQuestion("New browser item")

        BrowserImageDropExtractor().extractOwnedForCentralTest(browserDataTransferable()).use { extracted ->
            vm.importDraftMediaFile(extracted.file, "image")
        }
        val imported = vm.packageBrowserUiState!!
        assertTrue(imported.isCreatingNewItem)
        assertTrue(imported.isDirty)
        assertEquals("New browser item", imported.draftEdits?.questionText)
        val importedRef = imported.draftEdits?.imageRef

        assertFailsWith<BrowserImageDropException> {
            BrowserImageDropExtractor().extractOwnedForCentralTest(TestTransferable(DataFlavor.stringFlavor to "javascript:alert(1)"))
        }
        val afterFailure = vm.packageBrowserUiState!!
        assertEquals(importedRef, afterFailure.draftEdits?.imageRef)
        assertEquals("New browser item", afterFailure.draftEdits?.questionText)
        assertTrue(afterFailure.isCreatingNewItem)
    }

    @Test
    fun `clipboard image paste preserves unsaved text auto-saves existing item and remains UUID unique`() = runBlocking {
        val (vm, storage, _) = createHarness(initialImageRef = "media/before-clipboard.jpg")
        vm.updateDraftQuestion("Clipboard preserved question")
        val extractor = ClipboardImageExtractor()
        val refs = (1..12).map {
            val ready = assertIs<ClipboardImageSnapshotResult.Ready>(extractor.snapshot(browserDataTransferable()))
            extractor.extract(ready.snapshot).use { extracted ->
                vm.importDraftMediaFile(extracted.file, "image")
            }
            requireNotNull(vm.packageBrowserUiState!!.draftEdits?.imageRef)
        }

        val state = vm.packageBrowserUiState!!
        assertEquals(12, refs.toSet().size)
        assertTrue(refs.all { it.endsWith(".jpg") && storage.exists(it) })
        assertEquals("Clipboard preserved question", state.draftEdits?.questionText)
        assertEquals(refs.last(), state.selectedItemAnywhere?.imageRef)
        assertFalse(state.isDirty)
    }

    @Test
    fun `clipboard image paste keeps new item as an unpersisted dirty draft`() = runBlocking {
        val (vm, _, _) = createHarness()
        vm.startNewItem()
        vm.updateDraftQuestion("Clipboard new draft")
        val extractor = ClipboardImageExtractor()
        val ready = assertIs<ClipboardImageSnapshotResult.Ready>(extractor.snapshot(browserDataTransferable()))
        extractor.extract(ready.snapshot).use { vm.importDraftMediaFile(it.file, "image") }

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isCreatingNewItem)
        assertTrue(state.isDirty)
        assertEquals("Clipboard new draft", state.draftEdits?.questionText)
        assertTrue(state.draftEdits?.imageRef?.endsWith(".jpg") == true)
    }

    private fun browserDataTransferable(): Transferable {
        val bytes = ByteArrayOutputStream().use { output ->
            ImageIO.write(BufferedImage(48, 36, BufferedImage.TYPE_INT_RGB), "png", output)
            output.toByteArray()
        }
        val value = "data:image/png;base64,${Base64.getEncoder().encodeToString(bytes)}"
        return TestTransferable(DataFlavor("text/html;class=java.lang.String") to "<img src=\"$value\">")
    }

    private class TestTransferable(vararg entries: Pair<DataFlavor, Any>) : Transferable {
        private val values = entries.toMap()
        override fun getTransferDataFlavors(): Array<DataFlavor> = values.keys.toTypedArray()
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor in values
        override fun getTransferData(flavor: DataFlavor): Any =
            values[flavor] ?: throw UnsupportedFlavorException(flavor)
    }
}

private suspend fun BrowserImageDropExtractor.extractOwnedForCentralTest(transferable: Transferable): ExtractedDroppedImage =
    extract(snapshot(transferable))
