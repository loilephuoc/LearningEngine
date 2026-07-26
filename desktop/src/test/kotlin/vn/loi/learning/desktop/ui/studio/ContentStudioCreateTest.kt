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

class ContentStudioCreateTest {

    private fun setupViewModel(): Pair<ContentLibraryViewModel, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-create-test")
        val pkgId = PackageId("pkg-create-test")
        val libId = LibraryId("lib-create-test")
        val contentLibId = ContentLibraryId("lib-create-test")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = libId,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Create Package", "OPD3"),
                name = PackageName("Create Package"),
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
                descriptor = PackageDescriptor(name = "Create Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(contentLibId)
            )
        )

        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = contentLibId,
                descriptor = LibraryDescriptor(name = "Create Library"),
                contentIds = setOf(ContentId("cnt-1"))
            )
        )

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("cnt-1"),
                type = ContentType.WORD,
                text = ContentText(primaryText = "Question 1", translatedText = "Answer 1"),
                metadata = ContentMetadata(lesson = "General")
            )
        )

        appContext.learningItemRepository!!.save(
            LearningItem(
                id = LearningItemId("item-1-1"),
                contentId = ContentId("cnt-1"),
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        val editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentPackageRepository = appContext.contentPackageRepository
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

        vm.browsePackageLessons(instId, "Create Package")
        return vm to instId
    }

    @Test
    fun `TC01 - startNewItem enters create mode with empty draft`() {
        val (vm, _) = setupViewModel()
        vm.startNewItem()

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isCreatingNewItem)
        assertTrue(state.isDirty)
        assertEquals("new_item_draft", state.editingContentId)
        assertNotNull(state.draftEdits)
        assertEquals("", state.draftEdits?.questionText)
        assertEquals("", state.draftEdits?.answerText)
    }

    @Test
    fun `TC02 - cancelNewItem restores previous state without repo mutation`() {
        val (vm, _) = setupViewModel()
        vm.selectPackageBrowserRow("cnt-1")
        vm.startNewItem()

        vm.cancelNewItem()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isCreatingNewItem)
        assertFalse(state.isDirty)
        assertNull(state.editingContentId)
        assertEquals("cnt-1", state.selectedContentId)
    }

    @Test
    fun `TC03 - saveNewItem persists new content canonically and selects it`() {
        val (vm, _) = setupViewModel()
        vm.startNewItem()
        vm.updateDraftQuestion("Brand New Question")
        vm.updateDraftAnswer("Brand New Answer")
        vm.updateDraftPronunciation("brand-new-ipa")
        vm.updateDraftPartOfSpeech("NOUN")

        vm.saveNewItem()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isCreatingNewItem)
        assertEquals(2, state.allItems.size)
        val newSelectedId = state.selectedContentId
        assertNotNull(newSelectedId)
        assertTrue(newSelectedId.startsWith("content_"))

        val createdItem = state.allItems.first { it.contentId.value == newSelectedId }
        assertEquals("Brand New Question", createdItem.questionText)
        assertEquals("Brand New Answer", createdItem.answerText)
    }
}
