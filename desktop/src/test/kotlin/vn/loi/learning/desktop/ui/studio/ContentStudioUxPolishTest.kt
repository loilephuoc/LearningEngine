package vn.loi.learning.desktop.ui.studio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.browser.toDraftEdits
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

class ContentStudioUxPolishTest {

    private fun createViewModelWithPackage(contentCount: Int): ContentLibraryViewModel {
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
            appContext.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.WORD,
                    text = ContentText(
                        primaryText = "Question $i",
                        translatedText = "Answer $i",
                        pronunciation = "pron-$i",
                        exampleText = "Example $i",
                        exampleTranslation = "Translation $i"
                    ),
                    media = ContentMedia(
                        image = "media/img_$i.png",
                        primaryAudio = "media/q_audio_$i.mp3",
                        translatedAudio = "media/a_audio_$i.mp3",
                        exampleAudio = "media/ex_audio_$i.mp3",
                        exampleTranslatedAudio = "media/tr_audio_$i.mp3"
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
            contentLibraryRepository = appContext.contentLibraryRepository
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

    @Test
    fun `1 loading and selecting item does NOT set isDirty`() {
        val vm = createViewModelWithPackage(3)
        val state = vm.packageBrowserUiState!!

        val firstItem = state.allItems.first()
        assertEquals(firstItem.contentId.value, state.selectedContentId)
        assertFalse(state.isDirty, "Newly loaded item must NOT evaluate isDirty=true")
        assertNotNull(state.loadedBaselineDraft)
        assertEquals(state.loadedBaselineDraft, state.draftEdits)
        assertEquals("media/img_1.png", state.draftEdits?.imageRef)
        assertEquals("media/q_audio_1.mp3", state.draftEdits?.questionAudioRef)
    }

    @Test
    fun `2 select A to B to C without editing navigates cleanly without unsaved dialog`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value
        val idB = items[1].contentId.value
        val idC = items[2].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        assertFalse(vm.packageBrowserUiState!!.isDirty)

        vm.attemptSelectRowAutoEdit(idB)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
        assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        assertEquals(idB, vm.packageBrowserUiState!!.selectedContentId)
        assertEquals("media/img_2.png", vm.packageBrowserUiState!!.draftEdits?.imageRef)

        vm.attemptSelectRowAutoEdit(idC)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
        assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        assertEquals(idC, vm.packageBrowserUiState!!.selectedContentId)
        assertEquals("media/img_3.png", vm.packageBrowserUiState!!.draftEdits?.imageRef)
    }

    @Test
    fun `3 navigate Up Down repeatedly without editing keeps isDirty false`() {
        val vm = createViewModelWithPackage(3)

        for (step in 1..5) {
            vm.navigateExplorerByDelta(1) // Down
            assertFalse(vm.packageBrowserUiState!!.isDirty, "Step $step down isDirty must be false")
            assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        }

        for (step in 1..5) {
            vm.navigateExplorerByDelta(-1) // Up
            assertFalse(vm.packageBrowserUiState!!.isDirty, "Step $step up isDirty must be false")
            assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        }
    }

    @Test
    fun `4 editing text field makes isDirty true and navigation requests unsaved dialog`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value
        val idB = items[1].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        assertFalse(vm.packageBrowserUiState!!.isDirty)

        vm.updateDraftQuestion("Question 1 Modified")
        assertTrue(vm.packageBrowserUiState!!.isDirty, "isDirty must be true after real text change")

        vm.attemptSelectRowAutoEdit(idB)
        val state = vm.packageBrowserUiState!!
        assertEquals(idA, state.selectedContentId, "Selection must stay on A when dialog opens")
        assertTrue(state.showUnsavedChangesDialog, "Unsaved dialog must pop up on dirty navigation")
    }

    @Test
    fun `5 dirty item discard rehydrates original item and retains media`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value
        val idB = items[1].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftQuestion("Question 1 Modified")
        vm.updateDraftImageRef(null) // dirty modification

        // Select B -> pops dialog -> click Discard
        vm.attemptSelectRowAutoEdit(idB)
        vm.confirmDiscardAndProceed()

        val state = vm.packageBrowserUiState!!
        assertEquals(idB, state.selectedContentId, "Must navigate to B after Discard")
        assertFalse(state.isDirty, "New item B must not be dirty")
        assertEquals("media/img_2.png", state.draftEdits?.imageRef, "B's image must be loaded")

        // Navigate back to A to check clean rehydration
        vm.attemptSelectRowAutoEdit(idA)
        val stateA = vm.packageBrowserUiState!!
        assertEquals("Question 1", stateA.draftEdits?.questionText, "A's original text must be rehydrated")
        assertEquals("media/img_1.png", stateA.draftEdits?.imageRef, "A's original media must be rehydrated")
    }

    @Test
    fun `6 dirty item save persists text and retains image and all 4 audio slots`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftQuestion("Question 1 Persisted")
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.saveEditLocal()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isDirty, "After save, isDirty must evaluate to false")
        val itemA = state.allItems.first { it.contentId.value == idA }
        assertEquals("Question 1 Persisted", itemA.questionText)
        assertEquals("media/img_1.png", itemA.imageRef)
        assertEquals("media/q_audio_1.mp3", itemA.questionAudioRef)
        assertEquals("media/a_audio_1.mp3", itemA.answerAudioRef)
        assertEquals("media/ex_audio_1.mp3", itemA.exampleAudioRef)
        assertEquals("media/tr_audio_1.mp3", itemA.translationAudioRef)
    }

    @Test
    fun `7 dirty item cancel keeps item selected and dirty draft intact`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value
        val idB = items[1].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftQuestion("Question 1 Modified")

        vm.attemptSelectRowAutoEdit(idB)
        assertTrue(vm.packageBrowserUiState!!.showUnsavedChangesDialog)

        vm.cancelUnsavedChangesDialog()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.showUnsavedChangesDialog, "Dialog must close on Cancel")
        assertEquals(idA, state.selectedContentId, "Selection must remain on item A")
        assertTrue(state.isDirty, "Draft must remain dirty on Cancel")
        assertEquals("Question 1 Modified", state.draftEdits?.questionText)
    }

    @Test
    fun `8 text-only save retains all media references in repository`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.allItems
        val idA = items[0].contentId.value

        vm.attemptSelectRowAutoEdit(idA)
        vm.updateDraftQuestion("Only Text Changed")

        // Save local update
        vm.saveEditLocal()

        val itemA = vm.packageBrowserUiState!!.allItems.first { it.contentId.value == idA }
        assertEquals("Only Text Changed", itemA.questionText)
        assertEquals("media/img_1.png", itemA.imageRef)
        assertEquals("media/q_audio_1.mp3", itemA.questionAudioRef)
        assertEquals("media/a_audio_1.mp3", itemA.answerAudioRef)
        assertEquals("media/ex_audio_1.mp3", itemA.exampleAudioRef)
        assertEquals("media/tr_audio_1.mp3", itemA.translationAudioRef)
    }

    @Test
    fun `9 load hydration round-trip compares clean against baseline`() {
        val vm = createViewModelWithPackage(3)
        val item = vm.packageBrowserUiState!!.allItems.first()
        val draftFromItem = item.toDraftEdits()

        assertEquals(vm.packageBrowserUiState!!.loadedBaselineDraft, draftFromItem)
        assertEquals(vm.packageBrowserUiState!!.draftEdits, draftFromItem)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
    }
}
