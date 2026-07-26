package vn.loi.learning.desktop.ui.studio

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.browser.ContentDraftEdits
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
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContentStudioMediaTest {

    private fun setupViewModel(): Pair<ContentLibraryViewModel, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-media-test")
        val pkgId = PackageId("pkg-media-test")
        val libId = LibraryId("lib-media-test")
        val contentLibId = ContentLibraryId("lib-media-test")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Media Package", "OPD3"),
                name = PackageName("Media Package"),
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
                descriptor = PackageDescriptor(name = "Media Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(contentLibId)
            )
        )

        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = contentLibId,
                descriptor = LibraryDescriptor(name = "Media Library"),
                contentIds = setOf(ContentId("cnt-media-1"))
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("cnt-media-1"),
                type = ContentType.WORD,
                text = ContentText(primaryText = "Media Question", translatedText = "Media Answer"),
                media = ContentMedia(
                    image = "orig_image.jpg",
                    primaryAudio = "orig_question.mp3",
                    translatedAudio = "orig_answer.mp3",
                    exampleAudio = "orig_example.mp3",
                    exampleTranslatedAudio = "orig_translation.mp3"
                ),
                metadata = ContentMetadata(lesson = "General")
            )
        )

        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("item-media-1"),
                contentId = ContentId("cnt-media-1"),
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        val editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository
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

        vm.browsePackageLessons(instId, "Media Package")
        return vm to instId
    }

    @Test
    fun `TC01 - Image replace and remove update draft and mark dirty`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-media-1")
        vm.startEditContent()

        vm.updateDraftImageRef("new_hero.png")
        assertEquals("new_hero.png", vm.packageBrowserUiState?.draftEdits?.imageRef)
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.updateDraftImageRef(null)
        assertNull(vm.packageBrowserUiState?.draftEdits?.imageRef)
    }

    @Test
    fun `TC02 - Audio slots replace and remove update draft independently`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-media-1")
        vm.startEditContent()

        vm.updateDraftQuestionAudioRef("new_q.mp3")
        vm.updateDraftAnswerAudioRef("new_a.mp3")
        vm.updateDraftExampleAudioRef("new_ex.mp3")
        vm.updateDraftTranslationAudioRef("new_tr.mp3")

        val draft = vm.packageBrowserUiState?.draftEdits!!
        assertEquals("new_q.mp3", draft.questionAudioRef)
        assertEquals("new_a.mp3", draft.answerAudioRef)
        assertEquals("new_ex.mp3", draft.exampleAudioRef)
        assertEquals("new_tr.mp3", draft.translationAudioRef)

        // Remove Question Audio only
        vm.updateDraftQuestionAudioRef(null)
        assertNull(vm.packageBrowserUiState?.draftEdits?.questionAudioRef)
        assertEquals("new_a.mp3", vm.packageBrowserUiState?.draftEdits?.answerAudioRef)
    }

    @Test
    fun `TC03 - Discard restores original media references without repo mutation`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-media-1")
        vm.startEditContent()

        vm.updateDraftImageRef("replaced_image.png")
        vm.updateDraftQuestionAudioRef("replaced_q.mp3")

        vm.discardEdits()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isDirty)
        val item = state.selectedItemInView!!
        assertEquals("orig_image.jpg", item.imageRef)
        assertEquals("orig_question.mp3", item.questionAudioRef)
    }

    @Test
    fun `TC04 - Save persists updated media references canonically and survives reload`() {
        val (vm, instId) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-media-1")
        vm.startEditContent()

        vm.updateDraftImageRef("persisted_image.png")
        vm.updateDraftQuestionAudioRef("persisted_q.mp3")
        vm.updateDraftAnswerAudioRef("persisted_a.mp3")
        vm.updateDraftExampleAudioRef("persisted_ex.mp3")
        vm.updateDraftTranslationAudioRef("persisted_tr.mp3")

        vm.saveEdit()

        val stateAfterSave = vm.packageBrowserUiState!!
        assertFalse(stateAfterSave.isDirty)

        // Simulate app restart by reloading package from repository
        vm.browsePackageLessons(instId, "Media Package")
        val reloadedItem = vm.packageBrowserUiState!!.selectedItemAnywhere!!

        assertEquals("persisted_image.png", reloadedItem.imageRef)
        assertEquals("persisted_q.mp3", reloadedItem.questionAudioRef)
        assertEquals("persisted_a.mp3", reloadedItem.answerAudioRef)
        assertEquals("persisted_ex.mp3", reloadedItem.exampleAudioRef)
        assertEquals("persisted_tr.mp3", reloadedItem.translationAudioRef)
    }
}
