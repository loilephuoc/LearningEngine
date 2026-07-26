package vn.loi.learning.desktop.ui.studio

import kotlin.test.Test
import kotlin.test.assertEquals
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
                        pronunciation = "pron-$i"
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

        vm.browsePackageLessons(instId, "Test Package")
        return vm
    }

    @Test
    fun `single click auto-edit enters edit mode immediately`() {
        val vm = createViewModelWithPackage(3)
        val targetId = vm.packageBrowserUiState!!.filteredItems[1].contentId.value

        // Single click target item
        vm.attemptSelectRowAutoEdit(targetId)

        val state = vm.packageBrowserUiState!!
        assertEquals(targetId, state.selectedContentId)
        assertEquals(targetId, state.editingContentId)
        assertNotNull(state.draftEdits)
    }

    @Test
    fun `keyboard navigation down and up moves selection and updates edit draft when clean`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.filteredItems
        val firstId = items[0].contentId.value
        val secondId = items[1].contentId.value
        val thirdId = items[2].contentId.value

        vm.selectPackageBrowserRow(firstId)

        // Navigate Down (+1) from first -> second
        vm.navigateExplorerByDelta(1)
        assertEquals(secondId, vm.packageBrowserUiState?.selectedContentId)
        assertEquals(secondId, vm.packageBrowserUiState?.editingContentId)

        // Discard edit to test clean navigation to third
        vm.discardEdits()
        vm.navigateExplorerByDelta(1)
        assertEquals(thirdId, vm.packageBrowserUiState?.selectedContentId)

        // Discard edit to test clean navigation back up to second
        vm.discardEdits()
        vm.navigateExplorerByDelta(-1)
        assertEquals(secondId, vm.packageBrowserUiState?.selectedContentId)
    }

    @Test
    fun `keyboard navigation home and end jump to boundary items when clean`() {
        val vm = createViewModelWithPackage(15)
        val items = vm.packageBrowserUiState!!.filteredItems
        val firstId = items.first().contentId.value
        val lastId = items.last().contentId.value

        vm.selectPackageBrowserRow(firstId)

        // Jump End (Int.MAX_VALUE)
        vm.navigateExplorerByDelta(Int.MAX_VALUE)
        assertEquals(lastId, vm.packageBrowserUiState?.selectedContentId)

        // Discard edit and Jump Home (Int.MIN_VALUE)
        vm.discardEdits()
        vm.navigateExplorerByDelta(Int.MIN_VALUE)
        assertEquals(firstId, vm.packageBrowserUiState?.selectedContentId)
    }

    @Test
    fun `saveNewItem failure preserves create mode and draft`() {
        val vm = createViewModelWithPackage(3)

        // Start new item draft
        vm.startNewItem()
        assertTrue(vm.packageBrowserUiState?.isCreatingNewItem == true)

        // Populate draft with valid text
        vm.updateDraftQuestion("New Question")
        vm.updateDraftAnswer("New Answer")

        // Calling saveNewItem (with dummy facade that fails createContent due to missing writable lib mapping if unlinked)
        vm.saveNewItem()

        // Verify state: stays in isCreatingNewItem, draft preserved
        val state = vm.packageBrowserUiState!!
        assertTrue(state.isCreatingNewItem, "Must preserve Create Mode on save failure")
        assertEquals("New Question", state.draftEdits?.questionText, "Must preserve draft question")
    }

    @Test
    fun `unsaved changes protection prevents losing dirty draft on navigation`() {
        val vm = createViewModelWithPackage(3)
        val items = vm.packageBrowserUiState!!.filteredItems
        val firstId = items[0].contentId.value
        val secondId = items[1].contentId.value

        // Edit first item and make it dirty
        vm.attemptSelectRowAutoEdit(firstId)
        vm.updateDraftQuestion("Question 1 Modified")

        // Attempt to select second item
        vm.attemptSelectRowAutoEdit(secondId)

        val state = vm.packageBrowserUiState!!
        assertEquals(firstId, state.selectedContentId, "Must remain on first item")
        assertTrue(state.showUnsavedChangesDialog, "Must show unsaved dialog")
    }
}
