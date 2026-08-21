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
        assertFalse(vm.packageBrowserUiState!!.isDirty)

        vm.updateDraftQuestion("Question 1 Modified")
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
        assertFalse(state.isDirty)
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
    // TC06 — saveEdit persists to repository (CP2)
    // ---------------------------------------------------------------------------
    @Test
    fun `TC06 saveEdit persists edits to repository and reloads state`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-2")
        vm.startEditContent()
        vm.updateDraftQuestion("Persisted Question")
        vm.updateDraftAnswer("Persisted Answer")
        vm.updateDraftPronunciation("/pɜːˈsɪstɪd/")

        vm.saveEdit()

        // State is clean: no dirty
        assertFalse(vm.packageBrowserUiState!!.isDirty)

        // Persisted in repository
        val saved = appContext.contentRepository!!.findById(
            vn.loi.learning.domain.content.model.ContentId("cnt-2")
        )
        assertNotNull(saved)
        assertEquals("Persisted Question", saved.text.primaryText)
        assertEquals("Persisted Answer", saved.text.translatedText)
        assertEquals("/pɜːˈsɪstɪd/", saved.text.pronunciation)
    }

    // ---------------------------------------------------------------------------
    // TC12 — saveEdit failure preserves draft and dirty state (CP2)
    // ---------------------------------------------------------------------------
    @Test
    fun `TC12 saveEdit failure preserves draft and dirty state and surfaces error`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-2")
        vm.startEditContent()
        vm.updateDraftQuestion("   ") // Blank question invalid for saveEdit

        vm.saveEdit()

        // State remains dirty and draft preserved
        assertTrue(vm.packageBrowserUiState!!.isDirty)
        assertEquals("cnt-2", vm.packageBrowserUiState!!.editingContentId)
        assertEquals("   ", vm.packageBrowserUiState!!.draftEdits?.questionText)

        // Error surfaced
        assertTrue(vm.uiState.importError?.contains("Save failed") == true)
    }

    // ---------------------------------------------------------------------------
    // TC07 — deleteContent via Facade deletes content and LearningItems (CP3)
    // ---------------------------------------------------------------------------
    @Test
    fun `TC07 confirmDeleteContent removes content and learning items from repository`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-1")
        val initialContentCount = appContext.contentRepository!!.findAll().size
        val initialItemCount = appContext.learningItemRepository!!.findAllEnabled().size

        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()

        // Browser state updated
        assertNotNull(vm.packageBrowserUiState)
        assertEquals(2, vm.packageBrowserUiState!!.totalCount)

        // Deleted from repository
        val finalContentCount = appContext.contentRepository!!.findAll().size
        val finalItemCount = appContext.learningItemRepository!!.findAllEnabled().size
        assertEquals(initialContentCount - 1, finalContentCount)
        // LearningItems for cnt-1 also deleted (2 per content in fixture)
        assertEquals(initialItemCount - 2, finalItemCount)
        assertNull(appContext.contentRepository!!.findById(vn.loi.learning.domain.content.model.ContentId("cnt-1")))
    }

    // ---------------------------------------------------------------------------
    // TC13 — Cancel delete keeps content, learning items, and count unchanged
    // ---------------------------------------------------------------------------
    @Test
    fun `TC13 cancel delete keeps content learning items and count unchanged`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-2")
        vm.showDeleteConfirmation()
        assertTrue(vm.packageBrowserUiState!!.showDeleteConfirm)

        vm.dismissDeleteConfirmation()

        assertFalse(vm.packageBrowserUiState!!.showDeleteConfirm)
        assertEquals(3, vm.packageBrowserUiState!!.totalCount)
        assertNotNull(appContext.contentRepository!!.findById(vn.loi.learning.domain.content.model.ContentId("cnt-2")))
        assertEquals(6, appContext.learningItemRepository!!.findAllEnabled().size)
    }

    // ---------------------------------------------------------------------------
    // TC14 — Delete middle row selects next row
    // ---------------------------------------------------------------------------
    @Test
    fun `TC14 delete middle row selects next row`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-2")
        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()

        val state = vm.packageBrowserUiState!!
        assertEquals(2, state.totalCount)
        // Middle row (cnt-2) deleted -> next row (cnt-3) selected
        assertEquals("cnt-3", state.selectedContentId)
    }

    // ---------------------------------------------------------------------------
    // TC15 — Delete last row selects previous row
    // ---------------------------------------------------------------------------
    @Test
    fun `TC15 delete last row selects previous row`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.selectPackageBrowserRow("cnt-3")
        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()

        val state = vm.packageBrowserUiState!!
        assertEquals(2, state.totalCount)
        // Last row (cnt-3) deleted -> previous row (cnt-2) selected
        assertEquals("cnt-2", state.selectedContentId)
    }

    // ---------------------------------------------------------------------------
    // TC16 — Delete only row clears preview and selectedContentId
    // ---------------------------------------------------------------------------
    @Test
    fun `TC16 delete only row clears preview and selectedContentId`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 1)

        vm.selectPackageBrowserRow("cnt-1")
        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()

        val state = vm.packageBrowserUiState!!
        assertEquals(0, state.totalCount)
        assertNull(state.selectedContentId)
        assertNull(state.selectedItemInView)
    }

    @Test
    fun `delete exposes one-level undo and undo restores exact item then consumes snapshot`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)
        vm.selectPackageBrowserRow("cnt-2")
        val before = vm.packageBrowserUiState!!.allItems.first { it.contentId.value == "cnt-2" }

        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()
        assertTrue(vm.packageBrowserUiState!!.canUndoDelete)
        assertEquals("Question 2", vm.packageBrowserUiState!!.undoDeleteLabel)

        vm.undoDeleteContent()
        val restored = vm.packageBrowserUiState!!
        assertFalse(restored.canUndoDelete)
        assertEquals("cnt-2", restored.selectedContentId)
        assertEquals(before, restored.allItems.first { it.contentId.value == "cnt-2" })
    }

    @Test
    fun `problem filtering and navigation are presentation-only and mutate no repositories`() {
        val context = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(context, contentCount = 3)
        val before = listOf(
            context.contentRepository!!.findAll(),
            context.learningItemRepository!!.findAll(),
            context.contentLibraryRepository!!.findAll(),
            context.installedPackageRepository!!.findAll(),
            context.memoryStateRepository!!.findAll(),
            context.reviewEventRepository!!.findAll(),
            context.learningTrajectoryRepository!!.findAll(),
            context.studySessionRepository!!.findAll(),
            context.studyQueueRepository!!.findAll()
        )

        vm.updatePackageBrowserProblemFilter(ContentProblemFilter.ALL_PROBLEMS)
        vm.navigatePackageBrowserProblem(1)
        vm.updatePackageBrowserProblemFilter(ContentProblemFilter.NONE)
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.selectAllVisiblePackageBrowserItems()
        vm.highlightSelectedPackageBrowserItems()
        vm.removeHighlightFromSelectedPackageBrowserItems()
        vm.checkSelectedPackageBrowserMedia()

        val after = listOf(
            context.contentRepository!!.findAll(),
            context.learningItemRepository!!.findAll(),
            context.contentLibraryRepository!!.findAll(),
            context.installedPackageRepository!!.findAll(),
            context.memoryStateRepository!!.findAll(),
            context.reviewEventRepository!!.findAll(),
            context.learningTrajectoryRepository!!.findAll(),
            context.studySessionRepository!!.findAll(),
            context.studyQueueRepository!!.findAll()
        )
        assertEquals(before, after)
    }

    @Test
    fun `batch POS preserves learning history and every Content and LearningItem identity`() {
        val context = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(context, contentCount = 3)
        val contentIdsBefore = context.contentRepository!!.findAll().map { it.id }
        val learningItemsBefore = context.learningItemRepository!!.findAll()
        val learningBefore = listOf(
            context.memoryStateRepository!!.findAll(),
            context.reviewEventRepository!!.findAll(),
            context.learningTrajectoryRepository!!.findAll(),
            context.studySessionRepository!!.findAll(),
            context.studyQueueRepository!!.findAll()
        )

        vm.selectAllVisiblePackageBrowserItems()
        vm.updateDraftPartOfSpeech("Adjective")
        vm.confirmBatchPartOfSpeech()

        assertEquals(contentIdsBefore, context.contentRepository!!.findAll().map { it.id })
        assertEquals(learningItemsBefore, context.learningItemRepository!!.findAll())
        assertEquals(learningBefore, listOf(
            context.memoryStateRepository!!.findAll(),
            context.reviewEventRepository!!.findAll(),
            context.learningTrajectoryRepository!!.findAll(),
            context.studySessionRepository!!.findAll(),
            context.studyQueueRepository!!.findAll()
        ))
        assertEquals(setOf("Adjective"), vm.packageBrowserUiState!!.allItems.map { it.partOfSpeech }.toSet())
    }

    @Test
    fun `second successful delete replaces first undo snapshot`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)
        vm.selectPackageBrowserRow("cnt-1")
        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()
        vm.selectPackageBrowserRow("cnt-2")
        vm.showDeleteConfirmation()
        vm.confirmDeleteContent()

        vm.undoDeleteContent()
        val ids = vm.packageBrowserUiState!!.allItems.map { it.contentId.value }
        assertFalse("cnt-1" in ids)
        assertTrue("cnt-2" in ids)
    }

    // ---------------------------------------------------------------------------
    // TC17 — Dirty draft blocks delete and does not silently discard edits
    // ---------------------------------------------------------------------------
    @Test
    fun `TC17 dirty draft blocks delete and does not silently discard edits`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Question 1")
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        // Attempting to show delete confirmation while dirty should be blocked
        vm.showDeleteConfirmation()
        assertFalse(vm.packageBrowserUiState!!.showDeleteConfirm)

        // Attempting confirmDeleteContent while dirty is also blocked
        vm.confirmDeleteContent()

        // Row remains intact with modified draft
        val state = vm.packageBrowserUiState!!
        assertEquals(3, state.totalCount)
        assertTrue(state.isDirty)
        assertEquals("Dirty Question 1", state.draftEdits?.questionText)
        assertNotNull(appContext.contentRepository!!.findById(vn.loi.learning.domain.content.model.ContentId("cnt-1")))
    }

    // ---------------------------------------------------------------------------
    // TC19 — Close browser while dirty presents dialog
    // ---------------------------------------------------------------------------
    @Test
    fun `TC19 close browser while dirty presents dialog cancel stays discard closes save persists`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Q1")
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        vm.closePackageBrowser()

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.CloseBrowser, state.pendingAction)

        // Cancel
        vm.cancelUnsavedChangesDialog()
        assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        assertNull(vm.packageBrowserUiState!!.pendingAction)
        assertTrue(vm.packageBrowserUiState!!.isDirty)

        // Discard closes browser
        vm.closePackageBrowser()
        vm.confirmDiscardAndProceed()
        assertNull(vm.packageBrowserUiState)
    }

    // ---------------------------------------------------------------------------
    // TC20 — Single click another row while dirty presents dialog discard selects target
    // ---------------------------------------------------------------------------
    @Test
    fun `TC20 single click another row while dirty presents dialog discard selects target`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Q1")

        vm.attemptSelectRow("cnt-2")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.SelectRow("cnt-2"), state.pendingAction)
        // Current row remains selected until resolution
        assertEquals("cnt-1", state.selectedContentId)

        vm.confirmDiscardAndProceed()

        val resolvedState = vm.packageBrowserUiState!!
        assertFalse(resolvedState.isDirty)
        assertEquals("cnt-2", resolvedState.selectedContentId)
        assertNull(resolvedState.editingContentId)
    }

    // ---------------------------------------------------------------------------
    // TC21 — Double click another row while dirty presents dialog save persists then enters edit
    // ---------------------------------------------------------------------------
    @Test
    fun `TC21 double click another row while dirty presents dialog save persists then enters edit`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Persisted Q1")

        vm.doubleClickPackageBrowserRow("cnt-2")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DoubleClickRow("cnt-2"), state.pendingAction)

        vm.confirmSaveAndProceed()

        val resolvedState = vm.packageBrowserUiState!!
        assertEquals("cnt-2", resolvedState.selectedContentId)
        assertEquals("cnt-2", resolvedState.editingContentId)
        assertEquals("Question 2", resolvedState.draftEdits?.questionText)

        // Repository updated for cnt-1
        val saved1 = appContext.contentRepository!!.findById(vn.loi.learning.domain.content.model.ContentId("cnt-1"))!!
        assertEquals("Persisted Q1", saved1.text.primaryText)
    }

    // ---------------------------------------------------------------------------
    // TC22 — Double click current row while dirty preserves draft without dialog
    // ---------------------------------------------------------------------------
    @Test
    fun `TC22 double click current row while dirty preserves draft without dialog`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Modified Q1")

        vm.doubleClickPackageBrowserRow("cnt-1")

        val state = vm.packageBrowserUiState!!
        assertFalse(state.showUnsavedChangesDialog)
        assertEquals("cnt-1", state.selectedContentId)
        assertEquals("cnt-1", state.editingContentId)
        assertEquals("Modified Q1", state.draftEdits?.questionText)
    }

    // ---------------------------------------------------------------------------
    // TC23 — Package switch while dirty presents dialog
    // ---------------------------------------------------------------------------
    @Test
    fun `TC23 package switch while dirty presents dialog and executes pending browse package`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)
        val inst2 = InstalledPackageId("inst-2")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = inst2,
                libraryId = appContext.defaultLibraryId!!,
                packageId = PackageId("pkg-2"),
                topicId = TopicId.deriveForLegacyPackage("Other Pkg", "OPD3"),
                name = PackageName("Other Pkg"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = java.time.Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
        )
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = PackageId("pkg-2"),
                descriptor = PackageDescriptor(name = "Other Pkg", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(ContentLibraryId("lib-2"))
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = ContentLibraryId("lib-2"),
                descriptor = LibraryDescriptor(name = "Other Lib"),
                contentIds = setOf(ContentId("other-cnt-1"))
            )
        )
        appContext.contentRepository!!.save(
            Content(
                id = ContentId("other-cnt-1"),
                type = ContentType.WORD,
                text = ContentText(primaryText = "Other Q1"),
                metadata = ContentMetadata(lesson = "General")
            )
        )

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Q1")

        vm.browsePackageLessons(inst2, "Other Pkg")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BrowsePackage(inst2, "Other Pkg"), state.pendingAction)

        vm.confirmDiscardAndProceed()

        val nextState = vm.packageBrowserUiState!!
        assertEquals("Other Pkg", nextState.packageName)
        assertEquals("other-cnt-1", nextState.selectedContentId)
    }

    // ---------------------------------------------------------------------------
    // TC24 — Delete interaction while dirty resolves unsaved changes first
    // ---------------------------------------------------------------------------
    @Test
    fun `TC24 delete interaction while dirty resolves unsaved changes then opens delete confirm`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Q1")

        vm.showDeleteConfirmation()

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertFalse(state.showDeleteConfirm)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DeleteContent("cnt-1"), state.pendingAction)

        vm.confirmDiscardAndProceed()

        val nextState = vm.packageBrowserUiState!!
        assertFalse(nextState.showUnsavedChangesDialog)
        assertTrue(nextState.showDeleteConfirm)
        assertEquals("cnt-1", nextState.selectedContentId)
    }

    // ---------------------------------------------------------------------------
    // TC25 — Filter removes edited row presents dialog and applies query after resolution
    // ---------------------------------------------------------------------------
    @Test
    fun `TC25 filter removes edited row presents dialog and applies query after resolution`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Dirty Q1")

        vm.updatePackageBrowserQuery("NonExistentQuery")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertEquals(vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyQuery("NonExistentQuery"), state.pendingAction)

        vm.confirmDiscardAndProceed()

        val nextState = vm.packageBrowserUiState!!
        assertEquals("NonExistentQuery", nextState.query)
        assertEquals(0, nextState.filteredItems.size)
    }

    // ---------------------------------------------------------------------------
    // TC26 — Filter retaining edited row applies directly without dialog
    // ---------------------------------------------------------------------------
    @Test
    fun `TC26 filter retaining edited row applies directly without dialog`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("Question 1 Modified")

        vm.updatePackageBrowserQuery("Question")

        val state = vm.packageBrowserUiState!!
        assertFalse(state.showUnsavedChangesDialog)
        assertTrue(state.isDirty)
        assertEquals("Question", state.query)
    }

    // ---------------------------------------------------------------------------
    // TC27 — Failed save keeps dialog dirty state and blocks pending action
    // ---------------------------------------------------------------------------
    @Test
    fun `TC27 failed save keeps dialog dirty state and blocks pending action`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(appContext, contentCount = 3)

        vm.doubleClickPackageBrowserRow("cnt-1")
        vm.updateDraftQuestion("   ") // Invalid blank question

        vm.attemptSelectRow("cnt-2")
        assertTrue(vm.packageBrowserUiState!!.showUnsavedChangesDialog)

        vm.confirmSaveAndProceed()

        val state = vm.packageBrowserUiState!!
        assertTrue(state.showUnsavedChangesDialog)
        assertTrue(state.isDirty)
        assertEquals("cnt-1", state.selectedContentId)
        assertTrue(vm.uiState.importError?.contains("Save failed") == true)
    }


    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    @Test
    fun `importDraftMediaFile stores file and updates draft slot with resolvable relative path`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vmWithoutStorage, instId) = createViewModelWithPackageInContext(appContext, 2)
        val tempDir = java.nio.file.Files.createTempDirectory("media-test")
        val storage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempDir)

        val editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository!!,
            installedPackageRepository = appContext.installedPackageRepository!!
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
        vm.browsePackageLessons(instId, "Persist Package")
        vm.startNewItem()

        val sampleFile = java.io.File.createTempFile("sample", ".png")
        sampleFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        sampleFile.deleteOnExit()

        vm.importDraftMediaFile(sampleFile, "image")

        val state = vm.packageBrowserUiState!!
        assertNotNull(state.draftEdits?.imageRef)
        assertTrue(state.draftEdits!!.imageRef!!.endsWith(sampleFile.name))
        assertNotNull(storage.resolve(state.draftEdits!!.imageRef!!))
        assertTrue(storage.exists(state.draftEdits!!.imageRef!!))
    }

    @Test
    fun `same filename media imports preserve existing bytes and receive distinct references`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (_, instId) = createViewModelWithPackageInContext(appContext, 2)
        val mediaRoot = java.nio.file.Files.createTempDirectory("media-collision-test")
        val storage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaRoot)
        val vm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = PackageContentBrowserFacade(
                queryService = appContext.packageBrowserQuery,
                editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
                    appContext.contentRepository!!
                ),
                learningItemRepository = appContext.learningItemRepository
            ),
            contentMediaStorage = storage
        )
        vm.browsePackageLessons(instId, "Persist Package")
        vm.startNewItem()
        val firstDir = java.nio.file.Files.createTempDirectory("media-source-old")
        val secondDir = java.nio.file.Files.createTempDirectory("media-source-new")
        val first = firstDir.resolve("shared.mp3").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val second = secondDir.resolve("shared.mp3").toFile().apply { writeBytes(byteArrayOf(9, 8, 7)) }

        vm.importDraftMediaFile(first, "question")
        val firstRef = requireNotNull(vm.packageBrowserUiState!!.draftEdits!!.questionAudioRef)
        vm.importDraftMediaFile(second, "question")
        val secondRef = requireNotNull(vm.packageBrowserUiState!!.draftEdits!!.questionAudioRef)

        assertTrue(firstRef != secondRef)
        assertEquals(listOf<Byte>(1, 2, 3), storage.resolve(firstRef)!!.toFile().readBytes().toList())
        assertEquals(listOf<Byte>(9, 8, 7), storage.resolve(secondRef)!!.toFile().readBytes().toList())
    }

    @Test
    fun `unknown media slot performs zero physical write and zero draft mutation`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (_, instId) = createViewModelWithPackageInContext(appContext, 2)
        val mediaRoot = java.nio.file.Files.createTempDirectory("media-unknown-slot-test")
        val storage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(mediaRoot)
        val vm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = PackageContentBrowserFacade(
                queryService = appContext.packageBrowserQuery,
                editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
                    appContext.contentRepository!!
                ),
                learningItemRepository = appContext.learningItemRepository
            ),
            contentMediaStorage = storage
        )
        vm.browsePackageLessons(instId, "Persist Package")
        vm.startNewItem()
        val before = vm.packageBrowserUiState!!.draftEdits
        val source = java.io.File.createTempFile("unknown-slot", ".png").apply { writeBytes(byteArrayOf(4)) }

        vm.importDraftMediaFile(source, "unsupported")

        assertEquals(before, vm.packageBrowserUiState!!.draftEdits)
        assertEquals(0L, java.nio.file.Files.walk(mediaRoot).use { paths ->
            paths.filter { java.nio.file.Files.isRegularFile(it) }.count()
        })
        assertTrue(vm.uiState.importError?.contains("Unknown media slot") == true)
    }

    @Test
    fun `confirmDiscardAndProceed in Create Mode clears isCreatingNewItem`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 2)
        vm.startNewItem()
        assertTrue(vm.packageBrowserUiState?.isCreatingNewItem == true)

        vm.attemptSelectRow("cnt-2")
        assertTrue(vm.packageBrowserUiState?.showUnsavedChangesDialog == true)

        vm.confirmDiscardAndProceed()

        val state = vm.packageBrowserUiState!!
        assertFalse(state.isCreatingNewItem)
        assertNull(state.editingContentId)
        assertNull(state.draftEdits)
        assertEquals("cnt-2", state.selectedContentId)
    }

    @Test
    fun `highlight toggles multiple identities and survives selection search and sort`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 4)

        vm.togglePackageBrowserHighlight("cnt-1")
        vm.togglePackageBrowserHighlight("cnt-3")
        vm.attemptSelectRowAutoEdit("cnt-2")
        vm.updatePackageBrowserQuery("Question 2")
        vm.updatePackageBrowserSort(vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.QUESTION_ASC)

        assertEquals(setOf("cnt-1", "cnt-3"), vm.packageBrowserUiState!!.highlightedContentIds)
        vm.togglePackageBrowserHighlight("cnt-1")
        assertEquals(setOf("cnt-3"), vm.packageBrowserUiState!!.highlightedContentIds)
    }

    @Test
    fun `highlight state clears when Content Studio session closes`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 2)
        vm.togglePackageBrowserHighlight("cnt-1")

        vm.closePackageBrowser()

        assertNull(vm.packageBrowserUiState)
    }

    @Test
    fun `search Enter selects one exact question before partial matches`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 4)
        vm.updatePackageBrowserQuery("  question 2  ")

        vm.selectPackageBrowserSearchResult("  question 2  ")

        assertEquals("cnt-2", vm.packageBrowserUiState!!.selectedContentId)
        assertEquals("cnt-2", vm.packageBrowserUiState!!.editingContentId)
    }

    @Test
    fun `search Enter selects a single result and ignores ambiguous partial results`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 4)
        vm.updatePackageBrowserQuery("Question 4")
        vm.selectPackageBrowserSearchResult("Question 4")
        assertEquals("cnt-4", vm.packageBrowserUiState!!.selectedContentId)

        vm.updatePackageBrowserQuery("Question")
        vm.selectPackageBrowserSearchResult("Question")
        assertEquals("cnt-4", vm.packageBrowserUiState!!.selectedContentId)
    }

    @Test
    fun `search Enter preserves dirty navigation protection`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)
        vm.attemptSelectRowAutoEdit("cnt-1")
        vm.updateDraftQuestion("Dirty")
        vm.updatePackageBrowserQuery("Question 2")

        vm.selectPackageBrowserSearchResult("Question 2")

        val state = vm.packageBrowserUiState!!
        assertEquals("cnt-1", state.selectedContentId)
        assertTrue(state.showUnsavedChangesDialog)
    }

    @Test
    fun `search Enter dirty Save Discard and Cancel use canonical pending navigation`() {
        run {
            val (vm, _) = createViewModelWithPackageInContext(LearningApplicationFactory.createInMemory(), contentCount = 3)
            vm.attemptSelectRowAutoEdit("cnt-1")
            vm.updateDraftQuestion("Dirty saved")
            vm.selectPackageBrowserSearchResult("Question 2")
            assertTrue(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
            assertEquals(
                vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DoubleClickRow("cnt-2"),
                vm.packageBrowserUiState!!.pendingAction
            )
            vm.confirmSaveAndProceed()
            assertEquals("cnt-2", vm.packageBrowserUiState!!.selectedContentId)
        }
        run {
            val (vm, _) = createViewModelWithPackage(contentCount = 3)
            vm.attemptSelectRowAutoEdit("cnt-1")
            vm.updateDraftQuestion("Dirty discarded")
            vm.selectPackageBrowserSearchResult("Question 2")
            vm.confirmDiscardAndProceed()
            assertEquals("cnt-2", vm.packageBrowserUiState!!.selectedContentId)
        }
        run {
            val (vm, _) = createViewModelWithPackage(contentCount = 3)
            vm.attemptSelectRowAutoEdit("cnt-1")
            vm.updateDraftQuestion("Dirty kept")
            vm.selectPackageBrowserSearchResult("Question 2")
            vm.cancelUnsavedChangesDialog()
            assertEquals("cnt-1", vm.packageBrowserUiState!!.selectedContentId)
            assertEquals("Dirty kept", vm.packageBrowserUiState!!.draftEdits!!.questionText)
        }
    }

    @Test
    fun `ambiguous Search Enter while dirty is a no-op and unique target preserves multi-selection`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 3)
        vm.attemptSelectRowAutoEdit("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-1")
        vm.togglePackageBrowserMultiSelection("cnt-3")
        vm.updateDraftQuestion("Dirty")
        vm.selectPackageBrowserSearchResult("Question")
        assertFalse(vm.packageBrowserUiState!!.showUnsavedChangesDialog)
        assertEquals("cnt-1", vm.packageBrowserUiState!!.selectedContentId)

        vm.discardEdits()
        vm.selectPackageBrowserSearchResult("Question 2")
        assertEquals("cnt-2", vm.packageBrowserUiState!!.selectedContentId)
        assertEquals(setOf("cnt-1", "cnt-3"), vm.packageBrowserUiState!!.selectedContentIds)
    }

    @Test
    fun `multiple identical exact questions keep unrelated editor and repositories unchanged`() {
        val context = LearningApplicationFactory.createInMemory()
        val (vm, installedPackageId) = createViewModelWithPackageInContext(context, contentCount = 4)
        listOf("cnt-1", "cnt-2", "cnt-3").forEachIndexed { index, id ->
            val contentId = ContentId(id)
            val original = context.contentRepository!!.findById(contentId)!!
            context.contentRepository!!.save(
                original.copy(text = original.text.copy(primaryText = "to make", translatedText = "Answer ${index + 1}"))
            )
        }
        vm.closePackageBrowser()
        vm.browsePackageLessons(installedPackageId, "Persist Package")
        vm.attemptSelectRowAutoEdit("cnt-4")
        val repositoryBefore = context.contentRepository!!.findAll()

        vm.selectPackageBrowserSearchResult("to make")

        val state = vm.packageBrowserUiState!!
        assertEquals("cnt-4", state.selectedContentId)
        assertEquals("cnt-4", state.editingContentId)
        assertEquals("Question 4", state.draftEdits!!.questionText)
        assertFalse(state.showUnsavedChangesDialog)
        assertEquals(repositoryBefore, context.contentRepository!!.findAll())
    }

    @Test
    fun `clear search preserves editor and requests one center scroll`() {
        val (vm, _) = createViewModelWithPackage(contentCount = 4)
        vm.attemptSelectRowAutoEdit("cnt-3")
        vm.updatePackageBrowserQuery("Question 3")
        val before = vm.packageBrowserUiState!!.centerSelectedRowRequest

        vm.clearPackageBrowserQuery()

        val state = vm.packageBrowserUiState!!
        assertEquals("", state.appliedQuery)
        assertEquals("cnt-3", state.selectedContentId)
        assertEquals("cnt-3", state.editingContentId)
        assertEquals(before + 1L, state.centerSelectedRowRequest)
    }

    @Test
    fun `successful delete removes highlight and undo does not restore it`() {
        val context = LearningApplicationFactory.createInMemory()
        val (vm, _) = createViewModelWithPackageInContext(context, contentCount = 3)
        vm.attemptSelectRowAutoEdit("cnt-2")
        vm.togglePackageBrowserHighlight("cnt-2")
        vm.showDeleteConfirmation()

        vm.confirmDeleteContent()

        assertFalse("cnt-2" in vm.packageBrowserUiState!!.highlightedContentIds)
        vm.undoDeleteContent()
        assertFalse("cnt-2" in vm.packageBrowserUiState!!.highlightedContentIds)
    }

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

    /**
     * Tạo ViewModel có wiring đầy đủ (editService + learningItemRepository)
     * để test CP2/CP3 persist.
     */
    private fun createViewModelWithPackageInContext(
        appContext: LearningApplicationContext,
        contentCount: Int
    ): Pair<ContentLibraryViewModel, InstalledPackageId> {
        val instId = InstalledPackageId("inst-persist-test")
        val pkgId = PackageId("pkg-persist-test")
        val libId = ContentLibraryId("lib-persist-test")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = appContext.defaultLibraryId!!,
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Persist Package", "OPD3"),
                name = PackageName("Persist Package"),
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
                descriptor = PackageDescriptor(name = "Persist Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Persist Library"),
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

        val editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = appContext.contentRepository!!,
            contentLibraryRepository = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentPackageRepository = appContext.contentPackageRepository,
            transactionRunner = requireNotNull(appContext.transactionRunner),
            studySessionRepository = appContext.studySessionRepository
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

        vm.browsePackageLessons(instId, "Persist Package")

        return vm to instId
    }

    @Test
    fun `importDraftMediaFile with image on existing item auto-persists immediately and updates canonical state`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val tempMediaDir = java.nio.file.Files.createTempDirectory("edit-media-test")
        val storage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempMediaDir)
        val (vm, _) = createViewModelWithPackageInContext(appContext, 2)
        // Set storage on vm
        val vmWithStorage = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = PackageContentBrowserFacade(
                queryService = appContext.packageBrowserQuery,
                editService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
                    contentRepository = appContext.contentRepository!!,
                    contentLibraryRepository = appContext.contentLibraryRepository,
                    installedPackageRepository = appContext.installedPackageRepository,
                    contentPackageRepository = appContext.contentPackageRepository,
                    transactionRunner = requireNotNull(appContext.transactionRunner)
                ),
                learningItemRepository = appContext.learningItemRepository
            ),
            contentMediaStorage = storage
        )
        val instId = vm.packageBrowserUiState!!.installedPackageId
        vmWithStorage.browsePackageLessons(instId, "Persist Package")
        vmWithStorage.attemptSelectRowAutoEdit("cnt-1")

        val imageFile = java.io.File.createTempFile("photo", ".png").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }

        vmWithStorage.importDraftMediaFile(imageFile, "image")

        val state = vmWithStorage.packageBrowserUiState!!
        assertFalse(state.isDirty, "Auto-saved image must clear dirty flag")
        assertNotNull(state.draftEdits?.imageRef)
        assertEquals(state.draftEdits?.imageRef, state.loadedBaselineDraft?.imageRef)
        val canonical = appContext.contentRepository!!.findById(ContentId("cnt-1"))
        assertEquals(state.draftEdits?.imageRef, canonical?.media?.image)
    }

    @Test
    fun `importDraftMediaFile failure retains draft in dirty state and exposes error`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val tempMediaDir = java.nio.file.Files.createTempDirectory("edit-media-fail")
        val storage = vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage(tempMediaDir)

        val baseRepo = appContext.contentRepository!!
        val throwingContentRepo = object : vn.loi.learning.application.port.ContentRepository by baseRepo {
            override fun save(content: vn.loi.learning.domain.content.model.Content) {
                throw RuntimeException("Disk full or simulated DB failure")
            }
        }

        val failingEditService = vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
            contentRepository = throwingContentRepo,
            contentLibraryRepository = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentPackageRepository = appContext.contentPackageRepository,
            transactionRunner = requireNotNull(appContext.transactionRunner)
        )

        val vm = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = PackageContentBrowserFacade(
                queryService = appContext.packageBrowserQuery,
                editService = failingEditService,
                learningItemRepository = appContext.learningItemRepository
            ),
            contentMediaStorage = storage
        )
        val (_, instId) = createViewModelWithPackageInContext(appContext, 2)
        vm.browsePackageLessons(instId, "Persist Package")
        vm.attemptSelectRowAutoEdit("cnt-1")

        val imageFile = java.io.File.createTempFile("photo_fail", ".jpg").apply {
            writeBytes(byteArrayOf(5, 6, 7))
            deleteOnExit()
        }

        vm.importDraftMediaFile(imageFile, "image")

        val state = vm.packageBrowserUiState!!
        assertTrue(state.isDirty, "Draft must remain dirty when persist fails")
        assertTrue(vm.uiState.importError?.contains("Save failed: Disk full") == true)
        assertNotNull(state.draftEdits?.imageRef)
    }

    @Test
    fun `applyGeneratedTtsAudio updates only the target field and preserves all other fields`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val (vm, instId) = createViewModelWithPackageInContext(appContext, contentCount = 2)
        vm.browsePackageLessons(instId, "Persist Package")
        vm.attemptSelectRowAutoEdit("cnt-1")

        // First assign question audio
        vm.applyGeneratedTtsAudio("cnt-1", vn.loi.learning.desktop.tts.TtsField.QUESTION, "pkg/q_audio.mp3")

        var state = vm.packageBrowserUiState!!
        assertEquals("pkg/q_audio.mp3", state.selectedItemAnywhere?.questionAudioRef)
        assertEquals("Question 1", state.selectedItemAnywhere?.questionText)
        assertEquals("Answer 1", state.selectedItemAnywhere?.answerText)
        assertNull(state.selectedItemAnywhere?.translationAudioRef)

        // Then assign translation audio
        vm.applyGeneratedTtsAudio("cnt-1", vn.loi.learning.desktop.tts.TtsField.TRANSLATION, "pkg/trans_audio.mp3")

        state = vm.packageBrowserUiState!!
        // Question audio must remain intact!
        assertEquals("pkg/q_audio.mp3", state.selectedItemAnywhere?.questionAudioRef)
        assertEquals("pkg/trans_audio.mp3", state.selectedItemAnywhere?.translationAudioRef)
        assertEquals("Question 1", state.selectedItemAnywhere?.questionText)
        assertEquals("Answer 1", state.selectedItemAnywhere?.answerText)
        assertNull(state.selectedItemAnywhere?.answerAudioRef)
        assertNull(state.selectedItemAnywhere?.exampleAudioRef)
    }
}
