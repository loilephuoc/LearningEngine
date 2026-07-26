package vn.loi.learning.desktop.ui.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

/**
 * CP1 Tests — Editable Draft State.
 *
 * Kiểm tra toàn bộ vòng đời edit in-memory:
 * TC01: startEditContent tạo draft từ selected item
 * TC02: updateDraft* cập nhật từng field trong draft
 * TC03: discardEdits khôi phục state ban đầu, không mất selection
 * TC04: isDirty = false khi không edit, true khi có draftEdits
 * TC05: saveEditLocal cập nhật allItems, isDirty = false, selection preserved
 */
class PackageContentBrowserEditStateTest {

    // ---------------------------------------------------------------------------
    // TC01 — startEditContent creates draft from selected item
    // ---------------------------------------------------------------------------
    @Test
    fun `TC01 startEditContent creates draft matching selected item data`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)
        vm.selectPackageBrowserRow("cnt-1")

        assertNull(vm.packageBrowserUiState?.editingContentId)
        assertNull(vm.packageBrowserUiState?.draftEdits)

        vm.startEditContent()

        val state = vm.packageBrowserUiState!!
        assertEquals("cnt-1", state.editingContentId)
        assertNotNull(state.draftEdits)
        val draft = state.draftEdits!!
        assertEquals("cnt-1", draft.contentId)
        assertEquals("Question 1", draft.questionText)
        assertEquals("Answer 1", draft.answerText)
        assertEquals("pron-1", draft.pronunciation)
    }

    // ---------------------------------------------------------------------------
    // TC02 — updateDraft* updates individual fields
    // ---------------------------------------------------------------------------
    @Test
    fun `TC02 updateDraft functions update individual fields in draft`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 2)
        vm.selectPackageBrowserRow("cnt-1")
        vm.startEditContent()

        vm.updateDraftQuestion("New Question")
        assertEquals("New Question", vm.packageBrowserUiState?.draftEdits?.questionText)

        vm.updateDraftAnswer("New Answer")
        assertEquals("New Answer", vm.packageBrowserUiState?.draftEdits?.answerText)

        vm.updateDraftPronunciation("/njuː/")
        assertEquals("/njuː/", vm.packageBrowserUiState?.draftEdits?.pronunciation)

        vm.updateDraftPartOfSpeech("verb")
        assertEquals("verb", vm.packageBrowserUiState?.draftEdits?.partOfSpeech)

        vm.updateDraftExampleText("She is new here.")
        assertEquals("She is new here.", vm.packageBrowserUiState?.draftEdits?.exampleText)

        vm.updateDraftExampleTranslation("Cô ấy mới ở đây.")
        assertEquals("Cô ấy mới ở đây.", vm.packageBrowserUiState?.draftEdits?.exampleTranslation)

        // Other fields unaffected
        assertEquals("cnt-1", vm.packageBrowserUiState?.draftEdits?.contentId)
    }

    // ---------------------------------------------------------------------------
    // TC03 — discardEdits restores view mode, preserves selection
    // ---------------------------------------------------------------------------
    @Test
    fun `TC03 discardEdits clears draft and editing state but preserves selection`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)
        vm.selectPackageBrowserRow("cnt-2")
        vm.startEditContent()
        vm.updateDraftQuestion("Changed question")

        assertTrue(vm.packageBrowserUiState!!.isDirty)
        assertEquals("cnt-2", vm.packageBrowserUiState?.selectedContentId)

        vm.discardEdits()

        val state = vm.packageBrowserUiState!!
        assertNull(state.editingContentId)
        assertNull(state.draftEdits)
        assertFalse(state.isDirty)
        // Selection preserved
        assertEquals("cnt-2", state.selectedContentId)

        // Original data in allItems is unchanged
        val originalItem = state.allItems.first { it.contentId.value == "cnt-2" }
        assertEquals("Question 2", originalItem.questionText)
    }

    // ---------------------------------------------------------------------------
    // TC04 — isDirty flag semantics
    // ---------------------------------------------------------------------------
    @Test
    fun `TC04 isDirty is false when not editing and true when draft exists`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 2)
        vm.selectPackageBrowserRow("cnt-1")

        assertFalse(vm.packageBrowserUiState!!.isDirty)

        vm.startEditContent()
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.discardEdits()
        assertFalse(vm.packageBrowserUiState!!.isDirty)
    }

    // ---------------------------------------------------------------------------
    // TC05 — saveEditLocal updates allItems, clears dirty, preserves selection
    // ---------------------------------------------------------------------------
    @Test
    fun `TC05 saveEditLocal updates allItems and clears dirty state with selection preserved`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)
        vm.selectPackageBrowserRow("cnt-2")
        vm.startEditContent()
        vm.updateDraftQuestion("Updated Question 2")
        vm.updateDraftAnswer("Updated Answer 2")
        vm.updateDraftPronunciation("/ʌpˈdeɪtɪd/")

        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.saveEditLocal()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isDirty)
        assertNull(state.editingContentId)
        assertNull(state.draftEdits)
        // Selection preserved
        assertEquals("cnt-2", state.selectedContentId)

        // allItems updated
        val updatedItem = state.allItems.first { it.contentId.value == "cnt-2" }
        assertEquals("Updated Question 2", updatedItem.questionText)
        assertEquals("Updated Answer 2", updatedItem.answerText)
        assertEquals("/ʌpˈdeɪtɪd/", updatedItem.pronunciation)

        // Other items untouched
        val untouchedItem = state.allItems.first { it.contentId.value == "cnt-1" }
        assertEquals("Question 1", untouchedItem.questionText)
    }

    // ---------------------------------------------------------------------------
    // TC08 — Single click selects row without entering edit mode
    // ---------------------------------------------------------------------------
    @Test
    fun `TC08 single click selects row without entering edit mode`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        vm.selectPackageBrowserRow("cnt-2")

        assertEquals("cnt-2", vm.packageBrowserUiState?.selectedContentId)
        assertNull(vm.packageBrowserUiState?.editingContentId)
        assertNull(vm.packageBrowserUiState?.draftEdits)
        assertFalse(vm.packageBrowserUiState!!.isDirty)
    }

    // ---------------------------------------------------------------------------
    // TC09 — Double click selects row and immediately enters edit mode
    // ---------------------------------------------------------------------------
    @Test
    fun `TC09 double click selects row and immediately enters edit mode`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-2")

        val state = vm.packageBrowserUiState!!
        assertEquals("cnt-2", state.selectedContentId)
        assertEquals("cnt-2", state.editingContentId)
        val draft = assertNotNull(state.draftEdits)
        assertEquals("cnt-2", draft.contentId)
        assertEquals("Question 2", draft.questionText)
        assertTrue(state.isDirty)
    }

    // ---------------------------------------------------------------------------
    // TC10 — Double click same editing row preserves modified draft and dirty state
    // ---------------------------------------------------------------------------
    @Test
    fun `TC10 double click same editing row preserves modified draft and dirty state`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        // First double click to enter edit mode
        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Modified Question 1")

        assertEquals("Modified Question 1", vm.packageBrowserUiState?.draftEdits?.questionText)
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        // Double click same row again
        vm.doubleClickPackageBrowserRow("cnt-1")

        val state = vm.packageBrowserUiState!!
        assertEquals("cnt-1", state.selectedContentId)
        assertEquals("cnt-1", state.editingContentId)
        assertEquals("Modified Question 1", state.draftEdits?.questionText)
        assertTrue(state.isDirty)
    }

    // ---------------------------------------------------------------------------
    // TC11 — Double click another row while dirty does not lose changes
    // ---------------------------------------------------------------------------
    @Test
    fun `TC11 double click another row while dirty does not lose changes`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        // Enter edit mode on row 1 and modify
        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Unsaved Dirty Question 1")

        assertTrue(vm.packageBrowserUiState!!.isDirty)

        // Attempt to double click row 2
        vm.doubleClickPackageBrowserRow("cnt-2")

        val state = vm.packageBrowserUiState!!
        // Row 1 draft and editing state must be preserved
        assertEquals("cnt-1", state.editingContentId)
        assertEquals("Unsaved Dirty Question 1", state.draftEdits?.questionText)
        assertTrue(state.isDirty)
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun createViewModelWithPackage(contentCount: Int): Pair<ContentLibraryViewModel, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId = InstalledPackageId("inst-edit-test")
        val pkgId = PackageId("pkg-edit-test")
        val libId = ContentLibraryId("lib-edit-test")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = appContext.defaultLibraryId!!,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Edit Package", "OPD3"),
                name = PackageName("Edit Package"),
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
                descriptor = PackageDescriptor(name = "Edit Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Edit Library"),
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
                        exampleText = if (i % 2 == 0) "Example $i" else null,
                        exampleTranslation = if (i % 2 == 0) "Translation $i" else null
                    ),
                    metadata = ContentMetadata(lesson = "Lesson ${i % 3}")
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

        val facade = ContentLibraryFacade(appContext)
        val lessonBrowserFacade = LessonBrowserFacade(appContext)
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)

        val vm = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonBrowserFacade,
            packageBrowserFacade = packageBrowserFacade
        )

        vm.browsePackageLessons(instId, "Edit Package")

        return vm to instId
    }
}
