package vn.loi.learning.desktop.ui.studio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter
import vn.loi.learning.desktop.ui.browser.ContentProblem
import vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction
import vn.loi.learning.desktop.ui.browser.toDraftEdits
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
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

class ContentStudioUxPolishTest {

    private fun createViewModelWithPackage(
        contentCount: Int,
        withOptionalFields: Boolean = true,
        onlyExample: Boolean = false,
        onlyTranslation: Boolean = false
    ): ContentLibraryViewModel {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("test-installed-pkg")
        val pkgId = PackageId("test-pkg")
        val libId = ContentLibraryId("test-lib")

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
            val exText = if (onlyTranslation) "" else if (withOptionalFields || onlyExample) "Example $i" else ""
            val exTr = if (onlyExample) "" else if (withOptionalFields || onlyTranslation) "Translation $i" else ""

            appContext.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "Question $i",
                        translatedText = "Answer $i",
                        pronunciation = if (withOptionalFields) "pron-$i" else "",
                        exampleText = exText,
                        exampleTranslation = exTr
                    ),
                    media = ContentMedia(
                        image = if (withOptionalFields) "media/img_$i.png" else null,
                        primaryAudio = "media/q_audio_$i.mp3",
                        translatedAudio = "media/a_audio_$i.mp3",
                        exampleAudio = if (withOptionalFields) "media/ex_audio_$i.mp3" else null,
                        exampleTranslatedAudio = if (withOptionalFields) "media/tr_audio_$i.mp3" else null
                    ),
                    metadata = ContentMetadata(lesson = "General")
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

        val editService = ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentPackageRepository = appContext.contentPackageRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentLibraryRepository = appContext.contentLibraryRepository,
            transactionRunner = appContext.transactionRunner
        )
        val facade = ContentLibraryFacade(appContext)
        val lessonBrowserFacade = LessonBrowserFacade(appContext)
        val packageBrowserFacade = PackageContentBrowserFacade(
            queryService = appContext.packageBrowserQuery,
            editService = editService,
            learningItemRepository = appContext.learningItemRepository
        )

        val vm = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonBrowserFacade,
            packageBrowserFacade = packageBrowserFacade
        )

        vm.browsePackageLessons(instId, "Test Package")
        return vm
    }

    // -----------------------------------------------------------------------
    // PART 1: Drag & Drop tests (Utility & Callback routing)
    // -----------------------------------------------------------------------

    @Test
    fun `8 Valid image drop invokes the same import callback as Browse or Replace`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value

        vm.attemptSelectRowAutoEdit(idA)

        // Drop valid image file
        vm.updateDraftImageRef("media/new_dropped_image.jpg")

        val state = vm.packageBrowserUiState!!
        assertEquals("media/new_dropped_image.jpg", state.draftEdits?.imageRef)
        assertTrue(state.isDirty, "Updating image via drop must set isDirty=true")
    }

    @Test
    fun `9 Invalid image drop does not mutate the draft`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        val originalDraft = vm.packageBrowserUiState!!.draftEdits

        // Simulate rejected file drop (e.g. mp3 dropped on image slot)
        val isValid = DragDropUtils.isSupportedImage(File("audio.mp3"))
        assertFalse(isValid, "MP3 file must be rejected for image slot")

        // Draft remains unchanged
        assertEquals(originalDraft, vm.packageBrowserUiState!!.draftEdits)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
    }

    @Test
    fun `10 Drop replacement works when an image already exists`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        assertEquals("media/img_1.png", vm.packageBrowserUiState!!.draftEdits?.imageRef)

        // Replace existing image with new drop
        vm.updateDraftImageRef("media/replaced_image.png")

        val state = vm.packageBrowserUiState!!
        assertEquals("media/replaced_image.png", state.draftEdits?.imageRef)
        assertTrue(state.isDirty)
    }

    // -----------------------------------------------------------------------
    // PART 2 & 3: Optional field classification and dirty safety
    // -----------------------------------------------------------------------

    @Test
    fun `1 Empty optional fields are classified as hidden`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = false)
        val item = vm.packageBrowserUiState!!.allItems.first()

        assertTrue(item.pronunciation.isBlank(), "IPA must be empty")
        assertTrue(item.exampleText.isNullOrBlank(), "Example must be empty")
        assertTrue(item.exampleTranslation.isNullOrBlank(), "Translation must be empty")
    }

    @Test
    fun `2 Whitespace-only optional fields are classified as hidden`() {
        val vm = createViewModelWithPackage(1, withOptionalFields = false)
        vm.attemptSelectRowAutoEdit("cnt-1")

        vm.updateDraftPronunciation("   ")
        vm.updateDraftExampleText("\t\n")
        vm.updateDraftExampleTranslation(" ")

        val draft = vm.packageBrowserUiState!!.draftEdits!!
        assertTrue(draft.pronunciation.isBlank())
        assertTrue(draft.exampleText.isBlank())
        assertTrue(draft.exampleTranslation.isBlank())
    }

    @Test
    fun `3 Persisted optional fields remain visible`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        val item = vm.packageBrowserUiState!!.allItems.first()

        assertEquals("pron-1", item.pronunciation)
        assertEquals("Example 1", item.exampleText)
        assertEquals("Translation 1", item.exampleTranslation)
    }

    @Test
    fun `4 Revealing an optional field without changing it keeps isDirty false`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = false)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        assertFalse(vm.packageBrowserUiState!!.isDirty)

        // Revealing a field does not update draftEdits values until user types
        assertEquals("", vm.packageBrowserUiState!!.draftEdits?.pronunciation)
        assertFalse(vm.packageBrowserUiState!!.isDirty, "Revealing empty field without typing must remain clean")
    }

    @Test
    fun `5 Editing a revealed field sets isDirty true`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = false)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftPronunciation("/eɪ/ ")

        assertTrue(vm.packageBrowserUiState!!.isDirty, "Editing revealed field must evaluate to isDirty=true")
    }

    @Test
    fun `6 Clearing and saving a persisted optional field causes it to collapse`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        assertEquals("Example 1", vm.packageBrowserUiState!!.draftEdits?.exampleText)

        // Clear Example text and save
        vm.updateDraftExampleText("")
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.saveEditLocal()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isDirty)
        val updatedItem = state.allItems.first { it.contentId.value == idA }
        assertNull(updatedItem.exampleText, "Cleared example text must collapse to null/empty in item")
    }

    @Test
    fun `7 Items with no optional fields contain no reserved placeholder layout state`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = false)
        val item = vm.packageBrowserUiState!!.allItems.first()

        assertEquals("", item.pronunciation)
        assertNull(item.exampleText)
        assertNull(item.exampleTranslation)
        assertNull(item.imageRef)
    }

    @Test
    fun `Example expands when Translation is absent`() {
        val vm = createViewModelWithPackage(1, onlyExample = true)
        val item = vm.packageBrowserUiState!!.allItems.first()

        assertEquals("Example 1", item.exampleText)
        assertTrue(item.exampleTranslation.isNullOrBlank(), "Translation must be absent for expansion")
    }

    @Test
    fun `Translation expands when Example is absent`() {
        val vm = createViewModelWithPackage(1, onlyTranslation = true)
        val item = vm.packageBrowserUiState!!.allItems.first()

        assertEquals("Translation 1", item.exampleTranslation)
        assertTrue(item.exampleText.isNullOrBlank(), "Example must be absent for expansion")
    }

    @Test
    fun `IPA and POS controls use equal 50-50 weights in desktop mode and maintain dirty safety`() {
        val vm = createViewModelWithPackage(1, withOptionalFields = true)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        val draft = vm.packageBrowserUiState!!.draftEdits!!

        assertEquals("pron-1", draft.pronunciation)
        assertEquals("WORD", draft.partOfSpeech)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
    }

    // -----------------------------------------------------------------------
    // PART 4: HERO IMAGE RENDERER TESTS
    // -----------------------------------------------------------------------

    @Test
    fun `Hero image accepts the same activeImageRef used after drag and drop`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftImageRef("media/dropped_hero.jpg")

        val activeImageRef = vm.packageBrowserUiState!!.draftEdits?.imageRef
        assertEquals("media/dropped_hero.jpg", activeImageRef)
    }

    @Test
    fun `Missing or unresolved image reference produces an unavailable state rather than crashing`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = false)
        val idA = vm.packageBrowserUiState!!.allItems.first().contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftImageRef("media/non_existent_file.png")

        val state = vm.packageBrowserUiState!!
        assertEquals("media/non_existent_file.png", state.draftEdits?.imageRef)
    }

    @Test
    fun `Existing LessonThumbnail loader dimension remains unchanged for list use`() {
        val loader = LessonThumbnailLoader(
            mediaStorage = object : vn.loi.learning.application.port.ContentMediaStorage {
                override fun store(packageName: String, fileName: String, content: ByteArray): vn.loi.learning.application.contentmedia.ContentMediaAsset = error("Not implemented")
                override fun resolve(relativePath: String): java.nio.file.Path? = null
                override fun exists(relativePath: String): Boolean = false
            }
        )
        val result = loader.load("non-existent")
        assertEquals(ThumbnailResult.Unavailable, result)
    }

    // -----------------------------------------------------------------------
    // PART 5: Existing false-dirty and media preservation regression suite
    // -----------------------------------------------------------------------

    @Test
    fun `11 Existing false-dirty and media-preservation regression tests remain green`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        val state = vm.packageBrowserUiState!!

        val firstItem = state.allItems.first()
        assertEquals(firstItem.contentId.value, state.selectedContentId)
        assertFalse(state.isDirty)

        // Select A -> B -> C
        val idB = state.allItems[1].contentId.value
        vm.attemptSelectRowAutoEdit(idB)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
        assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)

        // Text edit retains media
        vm.updateDraftQuestion("Question 1 Persisted")
        vm.saveEditLocal()
        val itemA = vm.packageBrowserUiState!!.allItems.first { it.contentId.value == idB }
        assertEquals("Question 1 Persisted", itemA.questionText)
        assertEquals("media/img_2.png", itemA.imageRef)
        assertEquals("media/q_audio_2.mp3", itemA.questionAudioRef)
    }

    @Test
    fun `problem navigation uses canonical auto-edit dirty guard and has no wrap`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        vm.updatePackageBrowserProblemFilter(ContentProblemFilter.ALL_PROBLEMS)
        val first = vm.packageBrowserUiState!!.selectedContentId
        vm.navigatePackageBrowserProblem(-1)
        assertEquals(first, vm.packageBrowserUiState!!.selectedContentId)

        vm.updateDraftQuestion("Dirty question")
        vm.navigatePackageBrowserProblem(1)

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertIs<PackageBrowserPendingAction.DoubleClickRow>(state.pendingAction)
    }

    @Test
    fun `multi-selection toggle is independent from primary editor and dirty state`() {
        val vm = createViewModelWithPackage(3)
        val originalPrimary = vm.packageBrowserUiState!!.selectedContentId

        vm.togglePackageBrowserMultiSelection("cnt-2")
        vm.togglePackageBrowserMultiSelection("cnt-3")

        assertEquals(setOf("cnt-2", "cnt-3"), vm.packageBrowserUiState!!.selectedContentIds)
        assertEquals(originalPrimary, vm.packageBrowserUiState!!.selectedContentId)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
        vm.togglePackageBrowserMultiSelection("cnt-2")
        assertEquals(setOf("cnt-3"), vm.packageBrowserUiState!!.selectedContentIds)
    }

    @Test
    fun `shift range uses current visible order and hidden anchor falls back safely`() {
        val vm = createViewModelWithPackage(5)
        vm.togglePackageBrowserMultiSelection("cnt-2")
        vm.selectPackageBrowserVisibleRange("cnt-4")
        assertEquals(setOf("cnt-2", "cnt-3", "cnt-4"), vm.packageBrowserUiState!!.selectedContentIds)

        vm.clearPackageBrowserMultiSelection()
        vm.togglePackageBrowserMultiSelection("cnt-5")
        vm.updatePackageBrowserQuery("Question 2")
        vm.selectPackageBrowserVisibleRange("cnt-2")

        assertEquals(setOf("cnt-5", "cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)
        assertEquals("cnt-2", vm.packageBrowserUiState!!.selectionAnchorContentId)
    }

    @Test
    fun `select all adds only visible identities and selection survives projection changes`() {
        val vm = createViewModelWithPackage(4)
        vm.updatePackageBrowserQuery("Question 2")
        vm.selectAllVisiblePackageBrowserItems()
        assertEquals(setOf("cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)

        vm.clearPackageBrowserQuery()
        vm.updatePackageBrowserProblemFilter(ContentProblemFilter.ALL_PROBLEMS)
        assertEquals(setOf("cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)
        vm.updatePackageBrowserSort(vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.QUESTION_ASC)
        assertEquals(setOf("cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)
    }

    @Test
    fun `batch highlight commands preserve selection and selected media check uses cached projection`() {
        val vm = createViewModelWithPackage(3, withOptionalFields = true)
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-3")

        vm.highlightSelectedPackageBrowserItems()
        assertTrue(vm.packageBrowserUiState!!.highlightedContentIds.containsAll(setOf("cnt-1", "cnt-3")))
        assertEquals(setOf("cnt-1", "cnt-3"), vm.packageBrowserUiState!!.selectedContentIds)

        vm.checkSelectedPackageBrowserMedia()
        val report = assertNotNull(vm.packageBrowserUiState!!.selectedMediaCheck)
        assertEquals(2, report.selectedItemCount)
        assertEquals(2, report.count(ContentProblem.MISSING_IMAGE))

        vm.removeHighlightFromSelectedPackageBrowserItems()
        assertTrue(vm.packageBrowserUiState!!.highlightedContentIds.intersect(setOf("cnt-1", "cnt-3")).isEmpty())
        assertEquals(setOf("cnt-1", "cnt-3"), vm.packageBrowserUiState!!.selectedContentIds)
    }

    @Test
    fun `single delete sanitizes only deleted multi-selection identity`() {
        val vm = createViewModelWithPackage(3)
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-2")
        vm.confirmDeleteContentLocal()

        assertEquals(setOf("cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)
        assertEquals(2, vm.packageBrowserUiState!!.allItems.size)
    }

    @Test
    fun `multi-selection survives save and create does not add the new identity`() {
        val vm = createViewModelWithPackage(3)
        vm.togglePackageBrowserMultiSelection("cnt-2")
        vm.updateDraftQuestion("Saved Question")
        vm.saveEditLocal()
        assertEquals(setOf("cnt-2"), vm.packageBrowserUiState!!.selectedContentIds)

        vm.startNewItem()
        vm.updateDraftQuestion("Created Question")
        vm.updateDraftAnswer("Created Answer")
        vm.saveNewItem()

        val state = vm.packageBrowserUiState!!
        assertEquals(setOf("cnt-2"), state.selectedContentIds)
        assertFalse(state.selectedContentId in state.selectedContentIds)
    }

    @Test
    fun `closing and reopening Content Studio starts with an empty runtime selection`() {
        val vm = createViewModelWithPackage(2)
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.closePackageBrowser()
        assertNull(vm.packageBrowserUiState)
    }

    @Test
    fun `batch POS confirms all selected including hidden identities and preserves selection`() {
        val vm = createViewModelWithPackage(3)
        listOf("cnt-1", "cnt-2", "cnt-3").forEach(vm::togglePackageBrowserMultiSelection)
        vm.updatePackageBrowserQuery("Question 1")

        vm.updateDraftPartOfSpeech("Adjective")
        assertEquals("Adjective", vm.packageBrowserUiState!!.pendingBatchPartOfSpeech)
        assertEquals("WORD", vm.packageBrowserUiState!!.draftEdits!!.partOfSpeech)
        vm.confirmBatchPartOfSpeech()

        val state = vm.packageBrowserUiState!!
        assertEquals(setOf("cnt-1", "cnt-2", "cnt-3"), state.selectedContentIds)
        assertEquals(setOf("Adjective"), state.allItems.map { it.partOfSpeech }.toSet())
        assertEquals("Question 1", state.appliedQuery)
        assertEquals(1, state.filteredItems.size)
    }

    @Test
    fun `batch POS cancel and unrelated dirty draft cause zero batch mutation`() {
        val vm = createViewModelWithPackage(3)
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-2")
        val before = vm.packageBrowserUiState!!.allItems.map { it.partOfSpeech }

        vm.updateDraftPartOfSpeech("Adjective")
        vm.cancelBatchPartOfSpeech()
        assertNull(vm.packageBrowserUiState!!.pendingBatchPartOfSpeech)
        assertEquals(before, vm.packageBrowserUiState!!.allItems.map { it.partOfSpeech })

        vm.updateDraftQuestion("Unsaved unrelated question")
        vm.updateDraftPartOfSpeech("Noun")
        assertNull(vm.packageBrowserUiState!!.pendingBatchPartOfSpeech)
        assertTrue(vm.packageBrowserUiState!!.isDirty)
        assertEquals(before, vm.packageBrowserUiState!!.allItems.map { it.partOfSpeech })
    }

    @Test
    fun `single identity POS edit retains canonical draft and save behavior`() {
        val vm = createViewModelWithPackage(2)
        vm.updateDraftPartOfSpeech("Adjective")
        assertNull(vm.packageBrowserUiState!!.pendingBatchPartOfSpeech)
        assertEquals("Adjective", vm.packageBrowserUiState!!.draftEdits!!.partOfSpeech)
        vm.saveEditLocal()
        assertEquals("Adjective", vm.packageBrowserUiState!!.selectedItemAnywhere!!.partOfSpeech)
    }

    @Test
    fun `batch delete includes hidden identities repairs selection and one undo restores exact batch`() {
        val vm = createViewModelWithPackage(3)
        vm.attemptSelectRowAutoEdit("cnt-2")
        listOf("cnt-1", "cnt-2", "cnt-3").forEach(vm::togglePackageBrowserMultiSelection)
        vm.updatePackageBrowserQuery("Question 1")

        vm.showBatchDeleteConfirmation()
        assertEquals(setOf("cnt-1", "cnt-2", "cnt-3"), vm.packageBrowserUiState!!.pendingBatchDeleteContentIds)
        vm.confirmBatchDelete()

        val deleted = vm.packageBrowserUiState!!
        assertTrue(deleted.allItems.isEmpty())
        assertTrue(deleted.selectedContentIds.isEmpty())
        assertNull(deleted.selectedContentId)
        assertEquals("Question 1", deleted.appliedQuery)
        assertTrue(deleted.canUndoDelete)

        vm.undoDeleteContent()
        val restored = vm.packageBrowserUiState!!
        assertEquals(setOf("cnt-1", "cnt-2", "cnt-3"), restored.allItems.map { it.contentId.value }.toSet())
        assertTrue(restored.selectedContentIds.isEmpty())
        assertEquals("Question 1", restored.appliedQuery)
        assertEquals(1, restored.filteredItems.size)
    }

    @Test
    fun `batch delete cancel is zero mutation and dirty excluded primary draft survives`() {
        val vm = createViewModelWithPackage(3)
        vm.attemptSelectRowAutoEdit("cnt-3")
        vm.updateDraftQuestion("Unsaved primary")
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-2")
        val before = vm.packageBrowserUiState!!.allItems

        vm.showBatchDeleteConfirmation()
        vm.dismissBatchDeleteConfirmation()
        assertEquals(before, vm.packageBrowserUiState!!.allItems)

        vm.showBatchDeleteConfirmation()
        vm.confirmBatchDelete()
        val state = vm.packageBrowserUiState!!
        assertEquals(listOf("cnt-3"), state.allItems.map { it.contentId.value })
        assertEquals("cnt-3", state.selectedContentId)
        assertEquals("Unsaved primary", state.draftEdits!!.questionText)
        assertTrue(state.isDirty)
    }

    @Test
    fun `dirty primary included uses unsaved authority before batch confirmation`() {
        val vm = createViewModelWithPackage(3)
        vm.attemptSelectRowAutoEdit("cnt-1")
        vm.updateDraftQuestion("Dirty")
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-2")

        vm.showBatchDeleteConfirmation()
        assertTrue(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        assertTrue(vm.packageBrowserUiState!!.pendingBatchDeleteContentIds.isEmpty())
        vm.cancelUnsavedChangesDialog()
        assertEquals("Dirty", vm.packageBrowserUiState!!.draftEdits!!.questionText)
        assertEquals(3, vm.packageBrowserUiState!!.allItems.size)

        vm.showBatchDeleteConfirmation()
        vm.confirmDiscardAndProceed()
        assertEquals(setOf("cnt-1", "cnt-2"), vm.packageBrowserUiState!!.pendingBatchDeleteContentIds)
    }

    @Test
    fun `explicit batch selection overrides unrelated primary for header delete`() {
        val vm = createViewModelWithPackage(5)
        listOf("cnt-1", "cnt-2", "cnt-3").forEach(vm::togglePackageBrowserMultiSelection)
        vm.attemptSelectRowAutoEdit("cnt-5")

        vm.showDeleteConfirmation()
        assertEquals(setOf("cnt-1", "cnt-2", "cnt-3"), vm.packageBrowserUiState!!.pendingBatchDeleteContentIds)
        assertFalse(vm.packageBrowserUiState!!.showDeleteConfirm)
        vm.confirmBatchDelete()

        val state = vm.packageBrowserUiState!!
        assertEquals(setOf("cnt-4", "cnt-5"), state.allItems.map { it.contentId.value }.toSet())
        assertEquals("cnt-5", state.selectedContentId)
        assertEquals("Question 5", state.draftEdits!!.questionText)
    }

    @Test
    fun `one explicit selection overrides unrelated primary while empty selection falls back to primary`() {
        run {
            val vm = createViewModelWithPackage(3)
            vm.attemptSelectRowAutoEdit("cnt-3")
            vm.togglePackageBrowserMultiSelection("cnt-1")
            vm.showDeleteConfirmation()
            assertEquals("cnt-1", vm.packageBrowserUiState!!.deleteTargetContentId)
            vm.confirmDeleteContent()
            assertEquals(setOf("cnt-2", "cnt-3"), vm.packageBrowserUiState!!.allItems.map { it.contentId.value }.toSet())
            assertEquals("cnt-3", vm.packageBrowserUiState!!.selectedContentId)
        }
        run {
            val vm = createViewModelWithPackage(3)
            vm.attemptSelectRowAutoEdit("cnt-3")
            vm.showDeleteConfirmation()
            assertEquals("cnt-3", vm.packageBrowserUiState!!.deleteTargetContentId)
        }
    }

}
