package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Path
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.state.DesktopDebouncer
import vn.loi.learning.desktop.ui.state.ImmediateDesktopDebouncer

/**
 * Quản lý Presentation State của Content Library.
 */
class ContentLibraryViewModel(
    private val facade: ContentLibraryFacade,
    private val lessonBrowserFacade: LessonBrowserFacade,
    private val packageBrowserFacade: vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade = vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade(),
    private val onContentDataChanged:
    (() -> Unit)? = null,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner,
    private val searchDebouncer: DesktopDebouncer = ImmediateDesktopDebouncer
) {

    var uiState by mutableStateOf(
        ContentLibraryUiState()
    )
        private set

    var lessonBrowserUiState by mutableStateOf<LessonBrowserUiState?>(null)
        private set

    var packageBrowserUiState by mutableStateOf<vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState?>(null)
        private set

    var createCollectionDialogState by mutableStateOf(
        CreateCollectionDialogState()
    )
        private set

    var renameCollectionDialogState by mutableStateOf(
        RenameCollectionDialogState()
    )
        private set

    var deleteCollectionDialogState by mutableStateOf(
        DeleteCollectionDialogState()
    )
        private set

    var attachPackageDialogState by mutableStateOf(
        AttachPackageDialogState()
    )
        private set

    var detachPackageDialogState by mutableStateOf(
        DetachPackageDialogState()
    )
        private set

    init {
        refresh()
    }

    fun updateLessonQuery(query: String) {
        lessonBrowserUiState = lessonBrowserUiState?.copy(query = query)
        searchDebouncer.submit {
            val current = lessonBrowserUiState ?: return@submit
            lessonBrowserUiState = applyDebouncedLessonQuery(current, query)
        }
    }
    fun clearLessonQuery() { updateLessonQuery("") }
    fun updateLessonFilter(filter: LessonBrowserFilter) { lessonBrowserUiState = lessonBrowserUiState?.copy(filter = filter) }
    fun updateLessonSort(sort: LessonBrowserSort) { lessonBrowserUiState = lessonBrowserUiState?.copy(sort = sort) }

    fun refresh() {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        val previousState = uiState
        uiState = previousState.copy(
            operation = ContentLibraryOperation.Loading("Content Library", "Refreshing libraries")
        )
        taskRunner.run(
            work = facade::load,
            onSuccess = { refreshedState ->
                uiState = refreshedState.copy(
                    importMessage = previousState.importMessage,
                    importError = previousState.importError,
                    operation = ContentLibraryOperation.Idle
                )
                refreshLessonBrowser()
            },
            onFailure = { exception ->
                uiState = previousState.copy(
                    loadError = DesktopFailureMessage.forPersistedData(exception),
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    fun showCreateCollectionDialog(
        libraryId: String
    ) {
        val library =
            uiState.libraries.firstOrNull { item ->
                item.id == libraryId
            } ?: return

        createCollectionDialogState =
            CreateCollectionDialogState(
                visible = true,
                libraryId = library.id,
                libraryName = library.name
            )
    }

    fun updateCreateCollectionName(
        name: String
    ) {
        createCollectionDialogState =
            createCollectionDialogState.copy(
                collectionName = name
            )
    }

    fun dismissCreateCollectionDialog() {
        createCollectionDialogState =
            CreateCollectionDialogState()
    }

    fun confirmCreateCollection() {
        val dialogState =
            createCollectionDialogState

        if (!dialogState.visible) {
            return
        }

        val created =
            createCollection(
                libraryId = dialogState.libraryId,
                name = dialogState.collectionName
            )

        if (created) {
            dismissCreateCollectionDialog()
        }
    }

    fun createCollection(
        libraryId: String,
        name: String
    ): Boolean {
        clearOperationMessage()

        return try {
            val normalizedName =
                normalizeCollectionName(
                    name
                )

            facade.createCollection(
                libraryId = libraryId,
                name = normalizedName
            )

            reloadWithSuccessMessage(
                message =
                    "Collection \"$normalizedName\" was created."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Collection creation failed."
            )

            false
        }
    }

    fun showRenameCollectionDialog(
        collectionId: String
    ) {
        val collection =
            findCollection(
                collectionId
            ) ?: return

        renameCollectionDialogState =
            RenameCollectionDialogState(
                visible = true,
                collectionId = collection.id,
                currentName = collection.name,
                collectionName = collection.name
            )
    }

    fun updateRenameCollectionName(
        name: String
    ) {
        renameCollectionDialogState =
            renameCollectionDialogState.copy(
                collectionName = name
            )
    }

    fun dismissRenameCollectionDialog() {
        renameCollectionDialogState =
            RenameCollectionDialogState()
    }

    fun confirmRenameCollection() {
        val dialogState =
            renameCollectionDialogState

        if (!dialogState.visible) {
            return
        }

        val renamed =
            renameCollection(
                collectionId =
                    dialogState.collectionId,
                name =
                    dialogState.collectionName
            )

        if (renamed) {
            dismissRenameCollectionDialog()
        }
    }

    fun renameCollection(
        collectionId: String,
        name: String
    ): Boolean {
        clearOperationMessage()

        return try {
            val normalizedName =
                normalizeCollectionName(
                    name
                )

            facade.renameCollection(
                collectionId = collectionId,
                name = normalizedName
            )

            reloadWithSuccessMessage(
                message =
                    "Collection was renamed to \"$normalizedName\"."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Collection rename failed."
            )

            false
        }
    }

    fun showDeleteCollectionDialog(
        collectionId: String
    ) {
        val collection =
            findCollection(
                collectionId
            ) ?: return

        deleteCollectionDialogState =
            DeleteCollectionDialogState(
                visible = true,
                collectionId = collection.id,
                collectionName = collection.name
            )
    }

    fun dismissDeleteCollectionDialog() {
        deleteCollectionDialogState =
            DeleteCollectionDialogState()
    }

    fun confirmDeleteCollection() {
        val dialogState =
            deleteCollectionDialogState

        if (!dialogState.visible) {
            return
        }

        val deleted =
            deleteCollection(
                collectionId =
                    dialogState.collectionId,
                collectionName =
                    dialogState.collectionName
            )

        if (deleted) {
            dismissDeleteCollectionDialog()
        }
    }

    fun uninstallPackage(
        packageId: String,
        packageName: String
    ): Boolean {
        clearOperationMessage()

        return try {
            facade.removeInstalledPackage(packageId)

            resetLibraryNavigationState()

            reloadWithSuccessMessage(
                message = "Topic \"$packageName\" was removed from Learning Engine."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage = "Topic removal failed."
            )

            false
        }
    }

    fun deleteCollection(
        collectionId: String,
        collectionName: String
    ): Boolean {
        clearOperationMessage()

        return try {
            facade.deleteCollection(
                collectionId = collectionId
            )

            reloadWithSuccessMessage(
                message =
                    "Collection \"$collectionName\" was deleted."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Collection deletion failed."
            )

            false
        }
    }

    fun showAttachPackageDialog(
        collectionId: String
    ) {
        val collection =
            findCollection(
                collectionId
            ) ?: return

        val attachedPackageIds =
            collection.attachedPackages
                .map { attachedPackage ->
                    attachedPackage.id
                }
                .toSet()

        val packages =
            uiState.packages
                .filterNot { packageItem ->
                    packageItem.id in attachedPackageIds
                }

        attachPackageDialogState =
            AttachPackageDialogState(
                visible = true,
                collectionId = collection.id,
                collectionName = collection.name,
                selectedPackageId =
                    packages.firstOrNull()?.id
                        .orEmpty(),
                availablePackages = packages
            )
    }

    fun selectPackageForAttachment(
        packageId: String
    ) {
        val packageExists =
            attachPackageDialogState
                .availablePackages
                .any { packageItem ->
                    packageItem.id == packageId
                }

        if (!packageExists) {
            return
        }

        attachPackageDialogState =
            attachPackageDialogState.copy(
                selectedPackageId = packageId
            )
    }

    fun dismissAttachPackageDialog() {
        attachPackageDialogState =
            AttachPackageDialogState()
    }

    fun confirmAttachPackage() {
        val dialogState =
            attachPackageDialogState

        if (
            !dialogState.visible ||
            dialogState.selectedPackageId.isBlank()
        ) {
            return
        }

        val selectedPackage =
            dialogState.availablePackages
                .firstOrNull { packageItem ->
                    packageItem.id ==
                            dialogState.selectedPackageId
                } ?: return

        val attached =
            attachPackageToCollection(
                collectionId =
                    dialogState.collectionId,
                collectionName =
                    dialogState.collectionName,
                packageId =
                    selectedPackage.id,
                packageName =
                    selectedPackage.name
            )

        if (attached) {
            dismissAttachPackageDialog()
        }
    }

    fun attachPackageToCollection(
        collectionId: String,
        collectionName: String,
        packageId: String,
        packageName: String
    ): Boolean {
        clearOperationMessage()

        return try {
            facade.attachPackageToCollection(
                collectionId = collectionId,
                packageId = packageId
            )

            reloadWithSuccessMessage(
                message =
                    "Package \"$packageName\" was attached to collection \"$collectionName\"."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Package attachment failed."
            )

            false
        }
    }

    fun showDetachPackageDialog(
        collectionId: String,
        packageId: String
    ) {
        val collection =
            findCollection(
                collectionId
            ) ?: return

        val attachedPackage =
            collection.attachedPackages
                .firstOrNull { packageItem ->
                    packageItem.id == packageId
                } ?: return

        detachPackageDialogState =
            DetachPackageDialogState(
                visible = true,
                collectionId = collection.id,
                collectionName = collection.name,
                packageId = attachedPackage.id,
                packageName = attachedPackage.name
            )
    }

    fun dismissDetachPackageDialog() {
        detachPackageDialogState =
            DetachPackageDialogState()
    }

    fun confirmDetachPackage() {
        val dialogState =
            detachPackageDialogState

        if (
            !dialogState.visible ||
            dialogState.collectionId.isBlank() ||
            dialogState.packageId.isBlank()
        ) {
            return
        }

        val detached =
            detachPackageFromCollection(
                collectionId =
                    dialogState.collectionId,
                collectionName =
                    dialogState.collectionName,
                packageId =
                    dialogState.packageId,
                packageName =
                    dialogState.packageName
            )

        if (detached) {
            dismissDetachPackageDialog()
        }
    }

    fun detachPackageFromCollection(
        collectionId: String,
        collectionName: String,
        packageId: String,
        packageName: String
    ): Boolean {
        clearOperationMessage()

        return try {
            facade.detachPackageFromCollection(
                collectionId = collectionId,
                packageId = packageId
            )

            reloadWithSuccessMessage(
                message =
                    "Package \"$packageName\" was detached from collection \"$collectionName\"."
            )

            true
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Package detachment failed."
            )

            false
        }
    }

    fun openLibrary(
        libraryId: String
    ) {
        val library =
            uiState.libraries.firstOrNull { item ->
                item.id == libraryId
            }
        if (library == null) {
            uiState = uiState.copy(
                loadError = "Library with id '$libraryId' not found.",
                operation = ContentLibraryOperation.Idle
            )
            return
        }

        if (uiState.operation !is ContentLibraryOperation.Idle) return
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Loading(library.name, "Loading library contents")
        )
        taskRunner.run(
            work = {
                lessonBrowserFacade.load(
                    libraryId = library.id,
                    libraryName = library.name
                )
            },
            onSuccess = { loaded ->
                lessonBrowserUiState = loaded
                uiState = uiState.copy(loadError = null, operation = ContentLibraryOperation.Idle)
            },
            onFailure = { exception ->
                lessonBrowserUiState = null
                uiState = uiState.copy(
                    loadError = DesktopFailureMessage.forPersistedData(exception),
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    var learningWorkspaceUiState by mutableStateOf<LearningWorkspaceUiState?>(null)
        private set

    var isStartingSession by mutableStateOf(false)
        private set

    fun browsePackageLessons(
        installedPackageId: vn.loi.learning.domain.library.model.InstalledPackageId,
        packageName: String
    ) {
        val current = packageBrowserUiState
        if (current != null && current.isDirty) {
            if (current.installedPackageId == installedPackageId) return
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BrowsePackage(installedPackageId, packageName)
            )
            return
        }
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        learningWorkspaceUiState = null
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Loading(packageName, "Loading package browser")
        )
        taskRunner.run(
            work = {
                val loadedBrowser = try {
                    packageBrowserFacade.loadForPackage(
                        installedPackageId = installedPackageId,
                        packageName = packageName
                    )
                } catch (ex: Exception) {
                    null
                }
                val loadedLessonBrowser = try {
                    lessonBrowserFacade.loadForPackage(
                        installedPackageId = installedPackageId,
                        packageName = packageName
                    )
                } catch (ex: Exception) {
                    null
                }
                if (loadedBrowser == null && loadedLessonBrowser == null) {
                    throw IllegalArgumentException("Package with id '${installedPackageId.value}' is not available in library.")
                }
                loadedBrowser to loadedLessonBrowser
            },
            onSuccess = { (loadedBrowser, loadedLessonBrowser) ->
                packageBrowserUiState = loadedBrowser
                lessonBrowserUiState = loadedLessonBrowser
                uiState = uiState.copy(loadError = null, operation = ContentLibraryOperation.Idle)
            },
            onFailure = { exception ->
                packageBrowserUiState = null
                lessonBrowserUiState = null
                uiState = uiState.copy(
                    loadError = exception.message ?: "Failed to load package browser.",
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    private fun isEditedRowStillVisible(
        current: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState,
        candidateQuery: String = current.appliedQuery,
        candidateLessonFilter: String = current.selectedLessonFilter,
        candidateMediaFilter: vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter = current.mediaFilter,
        candidateSortOption: vn.loi.learning.application.contentpackaging.browser.BrowserSortOption = current.sortOption
    ): Boolean {
        val editingId = current.editingContentId ?: return true
        val candidateFiltered = vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserProjectionPolicy.filterAndSort(
            items = current.allItems,
            query = candidateQuery,
            lessonFilter = candidateLessonFilter,
            mediaFilter = candidateMediaFilter,
            sortOption = candidateSortOption
        )
        return candidateFiltered.any { it.contentId.value == editingId }
    }

    fun updatePackageBrowserQuery(query: String) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateQuery = query.trim())) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyQuery(query)
            )
            return
        }
        packageBrowserUiState = current.copy(query = query)
        searchDebouncer.submit {
            val latest = packageBrowserUiState ?: return@submit
            packageBrowserUiState = latest.copy(appliedQuery = query.trim())
        }
    }

    fun clearPackageBrowserQuery() {
        updatePackageBrowserQuery("")
    }

    fun updatePackageBrowserLessonFilter(lesson: String) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateLessonFilter = lesson)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyLessonFilter(lesson)
            )
            return
        }
        packageBrowserUiState = current.copy(selectedLessonFilter = lesson)
    }

    fun updatePackageBrowserMediaFilter(filter: vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateMediaFilter = filter)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyMediaFilter(filter)
            )
            return
        }
        packageBrowserUiState = current.copy(mediaFilter = filter)
    }

    fun updatePackageBrowserSort(sort: vn.loi.learning.application.contentpackaging.browser.BrowserSortOption) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateSortOption = sort)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplySort(sort)
            )
            return
        }
        packageBrowserUiState = current.copy(sortOption = sort)
    }

    fun resetPackageBrowserFilters() {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateQuery = "", candidateLessonFilter = "ALL", candidateMediaFilter = vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL, candidateSortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ResetFilters
            )
            return
        }
        packageBrowserUiState = current.copy(
            query = "",
            appliedQuery = "",
            selectedLessonFilter = "ALL",
            mediaFilter = vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL,
            sortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER
        )
    }

    fun selectPackageBrowserRow(contentIdStr: String) {
        packageBrowserUiState = packageBrowserUiState?.copy(selectedContentId = contentIdStr)
    }

    /**
     * Chuyển sang row mới. Nếu đang dirty → hiện dialog unsaved changes.
     */
    fun attemptSelectRow(contentIdStr: String) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && current.selectedContentId != contentIdStr) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.SelectRow(contentIdStr)
            )
        } else {
            selectPackageBrowserRow(contentIdStr)
        }
    }

    /**
     * Double click vào một row: chọn row và chuyển ngay sang chế độ edit.
     * Nếu row khác đang dirty, áp dụng unsaved protection để không làm mất draft.
     * Nếu chính row đó đang ở chế độ edit, giữ nguyên draft hiện tại.
     */
    fun doubleClickPackageBrowserRow(contentIdStr: String) {
        val current = packageBrowserUiState ?: return

        if (current.isDirty && current.selectedContentId != contentIdStr) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DoubleClickRow(contentIdStr)
            )
            return
        }

        val stateWithSelection = current.copy(selectedContentId = contentIdStr)

        if (stateWithSelection.editingContentId == contentIdStr && stateWithSelection.draftEdits != null) {
            packageBrowserUiState = stateWithSelection
            return
        }

        // Ngược lại, bắt đầu edit mode cho row này
        val item = stateWithSelection.selectedItemAnywhere ?: return
        val draft = vn.loi.learning.desktop.ui.browser.ContentDraftEdits(
            contentId = item.contentId.value,
            questionText = item.questionText,
            answerText = item.answerText,
            pronunciation = item.pronunciation,
            partOfSpeech = item.partOfSpeech,
            exampleText = item.exampleText.orEmpty(),
            exampleTranslation = item.exampleTranslation.orEmpty()
        )
        packageBrowserUiState = stateWithSelection.copy(
            editingContentId = item.contentId.value,
            draftEdits = draft
        )
    }


    /**
     * Bắt đầu edit content đang được chọn.
     * Tạo ContentDraftEdits từ dữ liệu hiện tại của item.
     */
    fun startEditContent() {
        val current = packageBrowserUiState ?: return
        val item = current.selectedItemAnywhere ?: return
        val draft = vn.loi.learning.desktop.ui.browser.ContentDraftEdits(
            contentId = item.contentId.value,
            questionText = item.questionText,
            answerText = item.answerText,
            pronunciation = item.pronunciation,
            partOfSpeech = item.partOfSpeech,
            exampleText = item.exampleText.orEmpty(),
            exampleTranslation = item.exampleTranslation.orEmpty()
        )
        packageBrowserUiState = current.copy(
            editingContentId = item.contentId.value,
            draftEdits = draft
        )
    }

    fun updateDraftQuestion(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(questionText = value)
        )
    }

    fun updateDraftAnswer(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(answerText = value)
        )
    }

    fun updateDraftPronunciation(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(pronunciation = value)
        )
    }

    fun updateDraftPartOfSpeech(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(partOfSpeech = value)
        )
    }

    fun updateDraftExampleText(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(exampleText = value)
        )
    }

    fun updateDraftExampleTranslation(value: String) {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            draftEdits = current.draftEdits?.copy(exampleTranslation = value)
        )
    }

    /**
     * Hủy bỏ edit: khôi phục view mode, giữ nguyên selection.
     */
    fun discardEdits() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            editingContentId = null,
            draftEdits = null
        )
    }

    /**
     * CP2: Save với persistence thật sự.
     * Gọi facade.persistEdit() → reload browser từ repository.
     * Nếu facade không có editService → fallback về in-memory save.
     */
    fun saveEdit() {
        val current = packageBrowserUiState ?: return
        val draft = current.draftEdits ?: return

        taskRunner.run(
            work = {
                packageBrowserFacade.persistEdit(
                    draft = draft,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { reloaded ->
                packageBrowserUiState = reloaded.copy(
                    selectedContentId = current.selectedContentId,
                    editingContentId = null,
                    draftEdits = null,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption
                )
            },
            onFailure = { ex ->
                uiState = uiState.copy(
                    importError = "Save failed: ${ex.message}"
                )
            }
        )
    }

    /**
     * CP1 fallback: Save in-memory (không persist).
     * Được giữ lại để các delegate của dialog vẫn hoạt động.
     */
    fun saveEditLocal() {
        val current = packageBrowserUiState ?: return
        val draft = current.draftEdits ?: return
        val editingId = current.editingContentId ?: return

        val updatedItems = current.allItems.map { item ->
            if (item.contentId.value == editingId) {
                item.copy(
                    questionText = draft.questionText,
                    answerText = draft.answerText,
                    pronunciation = draft.pronunciation,
                    partOfSpeech = draft.partOfSpeech,
                    exampleText = draft.exampleText.takeIf { it.isNotBlank() },
                    exampleTranslation = draft.exampleTranslation.takeIf { it.isNotBlank() }
                )
            } else item
        }

        packageBrowserUiState = current.copy(
            allItems = updatedItems,
            editingContentId = null,
            draftEdits = null
        )
    }

    /** Hiển thị dialog xác nhận xóa Content. */
    fun showDeleteConfirmation() {
        val current = packageBrowserUiState ?: return
        val selId = current.selectedContentId ?: return
        if (current.isDirty) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DeleteContent(selId)
            )
            return
        }
        packageBrowserUiState = current.copy(showDeleteConfirm = true)
    }

    /** Ẩn dialog xác nhận xóa. */
    fun dismissDeleteConfirmation() {
        packageBrowserUiState = packageBrowserUiState?.copy(showDeleteConfirm = false)
    }

    /**
     * CP3: Xóa Content với persistence thật sự.
     * Gọi facade.deleteContent() → reload browser từ repository.
     * Advance selection sang row kế tiếp trong filteredItems trước khi reload.
     */
    fun confirmDeleteContent() {
        val current = packageBrowserUiState ?: return
        val deleteId = current.selectedContentId ?: return
        if (current.isDirty) {
            dismissDeleteConfirmation()
            return
        }

        // Tính next selection từ filtered view hiện tại
        val currentFilteredIndex = current.filteredItems.indexOfFirst { it.contentId.value == deleteId }
        val candidateItems = current.filteredItems.filter { it.contentId.value != deleteId }
        val nextSelection = when {
            currentFilteredIndex >= 0 && currentFilteredIndex < candidateItems.size ->
                candidateItems[currentFilteredIndex].contentId.value
            candidateItems.isNotEmpty() ->
                candidateItems.last().contentId.value
            else -> null
        }

        // Ẩn dialog ngay
        packageBrowserUiState = current.copy(
            showDeleteConfirm = false
        )

        taskRunner.run(
            work = {
                packageBrowserFacade.deleteContent(
                    contentId = vn.loi.learning.domain.content.model.ContentId(deleteId),
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { reloaded ->
                packageBrowserUiState = reloaded.copy(
                    selectedContentId = nextSelection,
                    editingContentId = null,
                    draftEdits = null,
                    showDeleteConfirm = false,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption
                )
                onContentDataChanged?.invoke()
            },
            onFailure = { ex ->
                packageBrowserUiState = current.copy(
                    showDeleteConfirm = false
                )
                uiState = uiState.copy(
                    importError = "Delete failed: ${ex.message}"
                )
            }
        )
    }

    /**
     * CP3 in-memory stub – giữ lại để backward-compat với dialog callbacks.
     */
    fun confirmDeleteContentLocal() {
        val current = packageBrowserUiState ?: return
        val deleteId = current.selectedContentId ?: return

        val newItems = current.allItems.filter { it.contentId.value != deleteId }
        val currentFilteredIndex = current.filteredItems.indexOfFirst { it.contentId.value == deleteId }
        val nextSelection = when {
            currentFilteredIndex >= 0 && currentFilteredIndex < current.filteredItems.size - 1 ->
                current.filteredItems[currentFilteredIndex + 1].contentId.value
            currentFilteredIndex > 0 ->
                current.filteredItems[currentFilteredIndex - 1].contentId.value
            else -> newItems.firstOrNull()?.contentId?.value
        }

        packageBrowserUiState = current.copy(
            allItems = newItems,
            selectedContentId = nextSelection,
            showDeleteConfirm = false,
            editingContentId = null,
            draftEdits = null
        )
    }

    /** Ẩn unsaved changes dialog, giữ nguyên state. */
    fun cancelUnsavedChangesDialog() {
        packageBrowserUiState = packageBrowserUiState?.copy(
            showUnsavedChangesDialog = false,
            pendingAction = null
        )
    }

    /**
     * Người dùng chọn Discard trong dialog → clear draft rồi thực hiện pending action.
     */
    fun confirmDiscardAndProceed() {
        val current = packageBrowserUiState ?: return
        val action = current.pendingAction
        packageBrowserUiState = current.copy(
            editingContentId = null,
            draftEdits = null,
            showUnsavedChangesDialog = false,
            pendingAction = null
        )
        executePendingAction(action)
    }

    /**
     * Người dùng chọn Save trong dialog → save (persist) rồi thực hiện pending action.
     */
    fun confirmSaveAndProceed() {
        val current = packageBrowserUiState ?: return
        val draft = current.draftEdits ?: return
        val action = current.pendingAction

        taskRunner.run(
            work = {
                packageBrowserFacade.persistEdit(
                    draft = draft,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { reloaded ->
                packageBrowserUiState = reloaded.copy(
                    selectedContentId = current.selectedContentId,
                    editingContentId = null,
                    draftEdits = null,
                    showUnsavedChangesDialog = false,
                    pendingAction = null,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption
                )
                onContentDataChanged?.invoke()
                executePendingAction(action)
            },
            onFailure = { ex ->
                packageBrowserUiState = current.copy(
                    showUnsavedChangesDialog = true
                )
                uiState = uiState.copy(
                    importError = "Save failed: ${ex.message}"
                )
            }
        )
    }

    private fun executePendingAction(action: vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction?) {
        when (action) {
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.SelectRow -> {
                selectPackageBrowserRow(action.contentId)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DoubleClickRow -> {
                doubleClickPackageBrowserRow(action.contentId)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.FocusImage -> {
                selectPackageBrowserRow(action.contentId)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.PlayQuestionAudio -> {
                selectPackageBrowserRow(action.contentId)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.CloseBrowser,
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BackToLibrary -> {
                packageBrowserUiState = null
                lessonBrowserUiState = null
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BrowsePackage -> {
                packageBrowserUiState = null
                browsePackageLessons(action.installedPackageId, action.packageName)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DeleteContent -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(
                    selectedContentId = action.contentId,
                    showDeleteConfirm = true
                )
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyQuery -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(
                    query = action.query,
                    appliedQuery = action.query.trim()
                )
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyLessonFilter -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(selectedLessonFilter = action.lessonFilter)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyMediaFilter -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(mediaFilter = action.mediaFilter)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplySort -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(sortOption = action.sortOption)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ResetFilters -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(
                    query = "",
                    appliedQuery = "",
                    selectedLessonFilter = "ALL",
                    mediaFilter = vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL,
                    sortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER
                )
            }
            null -> {}
        }
    }

    fun playBrowserAudio(audioRef: String) {
        packageBrowserUiState = packageBrowserUiState?.copy(activePlayingAudioRef = audioRef)
    }

    fun stopBrowserAudio() {
        packageBrowserUiState = packageBrowserUiState?.copy(activePlayingAudioRef = null)
    }

    fun closePackageBrowser() {
        val current = packageBrowserUiState
        if (current != null && current.isDirty) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.CloseBrowser
            )
            return
        }
        packageBrowserUiState = null
        lessonBrowserUiState = null
    }

    fun resetLibraryNavigationState() {
        learningWorkspaceUiState = null
        lessonBrowserUiState = null
        packageBrowserUiState = null
    }

    fun closeLibrary() {
        resetLibraryNavigationState()
    }

    fun openWorkspaceForSelection(selection: PackageLessonSelection) {
        val browserState = lessonBrowserUiState ?: return
        val selectedItem = browserState.lessons.firstOrNull { it.id == selection.lessonId } ?: return
        val exploreItems = lessonBrowserFacade.getExploreItemsForLesson(selection.lessonId)
        val workspace = LearningWorkspaceProjectionPolicy.create(browserState, selectedItem, exploreItems) ?: return
        if (workspace.canStart) {
            learningWorkspaceUiState = workspace
        }
    }

    fun openWorkspaceForSelectedLesson() {
        val browserState = lessonBrowserUiState ?: return
        val selectedItem = browserState.selectedLessonInView ?: return
        openWorkspaceForSelection(
            PackageLessonSelection(
                installedPackageId = browserState.installedPackageId,
                lessonId = selectedItem.id,
                packageName = browserState.libraryName,
                lessonTitle = selectedItem.title
            )
        )
    }

    fun closeWorkspace() {
        learningWorkspaceUiState = null
    }

    fun navigateToPrepareMode() {
        learningWorkspaceUiState = learningWorkspaceUiState?.copy(mode = WorkspaceMode.PREPARE)
    }

    fun navigateToExploreMode() {
        learningWorkspaceUiState = learningWorkspaceUiState?.copy(mode = WorkspaceMode.EXPLORE)
    }

    fun nextExploreItem() {
        val workspace = learningWorkspaceUiState ?: return
        if (workspace.canNavigateNext) {
            learningWorkspaceUiState = workspace.copy(exploreIndex = workspace.exploreIndex + 1)
        }
    }

    fun previousExploreItem() {
        val workspace = learningWorkspaceUiState ?: return
        if (workspace.canNavigatePrevious) {
            learningWorkspaceUiState = workspace.copy(exploreIndex = workspace.exploreIndex - 1)
        }
    }

    fun handleWorkspaceBack() {
        val workspace = learningWorkspaceUiState ?: return
        when (workspace.mode) {
            WorkspaceMode.PREPARE -> {
                learningWorkspaceUiState = workspace.copy(mode = WorkspaceMode.EXPLORE)
            }
            WorkspaceMode.EXPLORE -> {
                closeWorkspace()
            }
        }
    }

    fun startStudyFromWorkspace(onStartLessonStudy: (PackageLessonSelection) -> Unit) {
        val workspace = learningWorkspaceUiState ?: return
        if (workspace.mode != WorkspaceMode.PREPARE) return
        if (!workspace.canStart || isStartingSession) return
        isStartingSession = true
        try {
            onStartLessonStudy(
                PackageLessonSelection(
                    installedPackageId = workspace.installedPackageId,
                    lessonId = workspace.contentId.value,
                    packageName = workspace.packageName,
                    lessonTitle = workspace.lessonTitle
                )
            )
        } finally {
            isStartingSession = false
        }
    }

    fun selectLesson(
        lessonId: String
    ) {
        val currentState =
            lessonBrowserUiState
                ?: return

        lessonBrowserUiState =
            currentState.select(
                lessonId
            )
    }

    fun clearLessonSelection() {
        val currentState =
            lessonBrowserUiState
                ?: return

        lessonBrowserUiState =
            currentState.clearSelection()
    }

    private var activeCancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal? = null

    fun cancelImport() {
        activeCancellationSignal?.cancel()
    }

    fun importFromFiles(
        files: List<Path>
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()

        val source = try {
            vn.loi.learning.application.contentpackaging.PackageImportSourceResolver.resolve(files)
        } catch (ex: vn.loi.learning.application.contentpackaging.InvalidImportSelectionException) {
            uiState = uiState.copy(
                importError = ex.message,
                importMessage = null,
                operation = ContentLibraryOperation.Idle
            )
            return
        }

        val targetPath = when (source) {
            is vn.loi.learning.application.contentpackaging.PackageImportSource.Opd3File -> source.file
            is vn.loi.learning.application.contentpackaging.PackageImportSource.LegacyPair -> source.jsonFile
        }

        importFromDirectory(targetPath)
    }

    fun importFromDirectory(
        directory: Path
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()
        val cancellationSignal = vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal()
        activeCancellationSignal = cancellationSignal

        uiState = uiState.copy(
            operation = ContentLibraryOperation.Importing(
                phase = "Resolving selected package files",
                cancellationSignal = cancellationSignal
            )
        )
        taskRunner.run(
            work = {
                val result = facade.importFromDirectory(
                    directory = directory,
                    progressListener = { event ->
                        taskRunner.dispatch { updateImportProgress(event, cancellationSignal) }
                    },
                    cancellationSignal = cancellationSignal
                )
                result to facade.load()
            },
            onSuccess = { (result, refreshedState) ->
                activeCancellationSignal = null
                uiState = refreshedState.copy(
                    importMessage =
                        buildImportMessage(
                            result
                        ),
                    importError =
                        buildImportError(
                            result
                        ),
                    loadError = null,
                    operation = ContentLibraryOperation.Idle
                )
                lessonBrowserUiState = null
                try {
                    onContentDataChanged?.invoke()
                } catch (ex: Throwable) {
                    uiState = uiState.copy(
                        importMessage = (uiState.importMessage ?: "") + " (Projection refresh pending retry: ${ex.message})",
                        operation = ContentLibraryOperation.Idle
                    )
                }
            },
            onFailure = { exception ->
                activeCancellationSignal = null
                if (exception is vn.loi.learning.application.contentpackaging.PackageImportCancelledException) {
                    uiState = uiState.copy(
                        importMessage = "Package import was cancelled.",
                        importError = null,
                        operation = ContentLibraryOperation.Idle
                    )
                } else {
                    showOperationError(exception, "Package import failed.")
                    uiState = uiState.copy(operation = ContentLibraryOperation.Idle)
                }
            }
        )
    }

    fun exportPackage(
        installedPackageId: String,
        packageName: String,
        destinationPath: Path
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()

        uiState = uiState.copy(
            operation = ContentLibraryOperation.Exporting(
                packageName = packageName,
                phase = "Preparing export...",
                processed = 0,
                total = 100
            )
        )

        taskRunner.run(
            work = {
                facade.exportPackage(
                    installedPackageId = installedPackageId,
                    destinationPath = destinationPath,
                    progressListener = { stage, message, processed, total ->
                        if (uiState.operation is ContentLibraryOperation.Exporting) {
                            uiState = uiState.copy(
                                operation = ContentLibraryOperation.Exporting(
                                    packageName = packageName,
                                    phase = message,
                                    processed = processed,
                                    total = total
                                )
                            )
                        }
                    }
                )
            },
            onSuccess = { result ->
                when (result) {
                    is vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult.Success -> {
                        uiState = uiState.copy(
                            importMessage = "Package '$packageName' exported successfully to '${result.outputPath.fileName}' (${result.contentCount} contents, ${result.learningItemCount} items, ${result.mediaAssetCount} media files).",
                            importError = null,
                            operation = ContentLibraryOperation.Idle
                        )
                    }
                    is vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult.Failure -> {
                        uiState = uiState.copy(
                            importMessage = null,
                            importError = result.message,
                            operation = ContentLibraryOperation.Idle
                        )
                    }
                }
            },
            onFailure = { exception ->
                uiState = uiState.copy(
                    importMessage = null,
                    importError = exception.message ?: "Failed to export package.",
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    private fun updateImportProgress(
        event: PackageImportProgressEvent,
        cancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal
    ) {
        if (uiState.operation !is ContentLibraryOperation.Importing) return
        val phase = event.message ?: when (event.stage) {
            PackageImportProgressStage.RESOLVING -> "Resolving selected package files"
            PackageImportProgressStage.SCANNING -> "Scanning selected package"
            PackageImportProgressStage.READING_METADATA -> "Reading package metadata"
            PackageImportProgressStage.OPENING_MEDIA -> "Opening legacy media package"
            PackageImportProgressStage.INDEXING_MEDIA -> "Indexing media entries"
            PackageImportProgressStage.EXTRACTING_MEDIA -> "Extracting media files (${event.processed} / ${event.total})"
            PackageImportProgressStage.IMPORTING_CONTENT -> "Importing content (${event.processed} items)"
            PackageImportProgressStage.VALIDATING_MEDIA -> "Validating media references"
            PackageImportProgressStage.PACKAGE_INSTALLED -> "Package descriptor validated"
            PackageImportProgressStage.CONTENT_IMPORTED -> "Preparing imported content"
            PackageImportProgressStage.VALIDATING_TOPIC_STRUCTURE -> "Validating topic structure"
            PackageImportProgressStage.PREPARING_CONTENT_RECORDS -> "Preparing content records"
            PackageImportProgressStage.SAVING_CONTENT -> if (event.total > 0) "Persisting content (${event.processed}/${event.total})" else "Persisting content records"
            PackageImportProgressStage.SAVING_LEARNING_ITEMS -> if (event.total > 0) "Persisting learning items (${event.processed}/${event.total})" else "Persisting learning items"
            PackageImportProgressStage.REGISTERING_PACKAGE -> "Registering package"
            PackageImportProgressStage.UPDATING_LIBRARY -> "Updating Library"
            PackageImportProgressStage.REFRESHING_PROJECTIONS -> "Refreshing projections"
            PackageImportProgressStage.COMPLETED -> "Refreshing Content Library"
        }
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Importing(
                phase = phase,
                processed = event.processed,
                total = event.total,
                committed = event.stage == PackageImportProgressStage.COMPLETED,
                cancellationSignal = cancellationSignal
            )
        )
    }

    private fun buildImportMessage(
        result: ContentLibraryImportResult
    ): String? =
        when {
            result.discoveredPackageCount == 0 ->
                "No .opd3 or .pkg files were found in the selected directory."

            result.importedPackageCount == 0 ->
                null

            else ->
                buildString {
                    append("Imported ")
                    append(result.importedPackageCount)
                    append(" package")

                    if (result.importedPackageCount != 1) {
                        append("s")
                    }

                    append(", ")
                    append(result.importedLibraryCount)
                    append(" libraries, ")
                    append(result.importedContentCount)
                    append(" contents and ")
                    append(result.importedLearningItemCount)
                    append(" learning items.")

                    if (result.failedPackageCount > 0) {
                        append(" ")
                        append(result.failedPackageCount)
                        append(" incompatible package")

                        if (result.failedPackageCount != 1) {
                            append("s were")
                        } else {
                            append(" was")
                        }

                        append(" skipped; see the error details below.")
                    }
                }
        }

    private fun buildImportError(
        result: ContentLibraryImportResult
    ): String? {
        if (result.failures.isEmpty()) {
            return null
        }

        return buildString {
            append("Package import issues:")

            result.failures.forEach { failure ->
                append(System.lineSeparator())
                append("• ")
                append(failure.source)
                append(": ")
                append(sanitizeFailureMessage(failure.message, failure.source))
            }
        }
    }

    private fun sanitizeFailureMessage(message: String, source: String = ""): String {
        val lines = message.lines().filter { it.isNotBlank() }
        val conflictLines = lines.filter {
            "CONTENT_ID_ALREADY_INSTALLED" in it ||
            "CONTENT_ALREADY_EXISTS" in it ||
            "LEARNING_ITEM_ID_ALREADY_INSTALLED" in it ||
            "LEARNING_ITEM_ALREADY_INSTALLED" in it
        }

        if (conflictLines.isNotEmpty()) {
            val count = conflictLines.size
            val sourceName = source.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")
            val sourceNorm = sourceName.replace(" ", "").lowercase()
            val matchingPkg = uiState.packages.firstOrNull { pkg ->
                val pkgNorm = pkg.name.replace(" ", "").lowercase()
                pkgNorm == sourceNorm || pkgNorm.contains(sourceNorm) || sourceNorm.contains(pkgNorm)
            }

            val pkgName = matchingPkg?.name ?: (if (sourceName.isNotBlank()) sourceName else "Selected package")
            val realState = facade.getPackageLifecycleState(pkgName) ?: facade.getPackageLifecycleState(sourceName)
            val stateText = when (realState) {
                "ARCHIVED" -> " (State: ARCHIVED)"
                "ACTIVE" -> " (State: ACTIVE)"
                else -> ""
            }
            val countText = if (count > 1) "$count content conflict(s) detected: CONTENT_ID_ALREADY_INSTALLED" else "CONTENT_ID_ALREADY_INSTALLED"
            val actionText = if (realState == "ARCHIVED" || realState == "ACTIVE") "Open Library to Restore or Remove it before re-importing." else "Open Library to manage existing packages before re-importing."
            return "Topic '$pkgName' is already installed$stateText ($countText). $actionText"
        } else if (message.contains("conflict", ignoreCase = true)) {
            val sourceName = source.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")
            val sourceNorm = sourceName.replace(" ", "").lowercase()
            val matchingPkg = uiState.packages.firstOrNull { pkg ->
                val pkgNorm = pkg.name.replace(" ", "").lowercase()
                pkgNorm == sourceNorm || pkgNorm.contains(sourceNorm) || sourceNorm.contains(pkgNorm)
            }

            val pkgName = matchingPkg?.name ?: (if (sourceName.isNotBlank()) sourceName else "Selected package")
            val realState = facade.getPackageLifecycleState(pkgName) ?: facade.getPackageLifecycleState(sourceName)
            val stateText = when (realState) {
                "ARCHIVED" -> " (State: ARCHIVED)"
                "ACTIVE" -> " (State: ACTIVE)"
                else -> ""
            }
            val actionText = if (realState == "ARCHIVED" || realState == "ACTIVE") "Open Library to Restore or Remove it before re-importing." else "Open Library to manage existing packages before re-importing."
            return "Topic '$pkgName' is already installed$stateText. $actionText"
        }

        if (lines.size <= 3) return message
        return lines.take(3).joinToString(separator = "\n") + "\n... (${lines.size - 3} more issues)"
    }

    private fun normalizeCollectionName(
        name: String
    ): String {
        val normalizedName =
            name.trim()

        require(
            normalizedName.isNotEmpty()
        ) {
            "Collection name must not be blank."
        }

        return normalizedName
    }

    private fun findCollection(
        collectionId: String
    ): ContentLibraryCollectionItem? =
        uiState.libraries
            .asSequence()
            .flatMap { library ->
                library.collections.asSequence()
            }
            .firstOrNull { collection ->
                collection.id == collectionId
            }

    fun clearOperationMessage() {
        uiState =
            uiState.copy(
                importMessage = null,
                importError = null
            )
    }

    private fun reloadWithSuccessMessage(
        message: String
    ) {
        val refreshedState =
            facade.load()

        uiState =
            refreshedState.copy(
                importMessage = message,
                importError = null,
                loadError = null
            )

        refreshLessonBrowser()

        onContentDataChanged?.invoke()
    }

    private fun showOperationError(
        exception: Exception,
        fallbackMessage: String
    ) {
        uiState =
            uiState.copy(
                importMessage = null,
                importError = "$fallbackMessage Check the selected data and try again."
            )
    }

    private fun refreshLessonBrowser() {
        val currentLessonBrowserState =
            lessonBrowserUiState
                ?: return

        lessonBrowserUiState =
            try {
                if (currentLessonBrowserState.installedPackageId != null) {
                    lessonBrowserFacade.loadForPackage(
                        installedPackageId = currentLessonBrowserState.installedPackageId,
                        packageName = currentLessonBrowserState.libraryName
                    ).copy(
                        query = currentLessonBrowserState.query,
                        appliedQuery = currentLessonBrowserState.appliedQuery,
                        filter = currentLessonBrowserState.filter,
                        sort = currentLessonBrowserState.sort,
                        selectedLessonId = currentLessonBrowserState.selectedLessonId
                    )
                } else {
                    val selectedLibrary =
                        uiState.libraries.firstOrNull { library ->
                            library.id ==
                                    currentLessonBrowserState.libraryId
                        }
                    if (selectedLibrary == null) {
                        null
                    } else {
                        lessonBrowserFacade.load(
                            libraryId = selectedLibrary.id,
                            libraryName = selectedLibrary.name
                        )
                    }
                }
            } catch (exception: Exception) {
                uiState =
                    uiState.copy(
                        loadError =
                            DesktopFailureMessage.forPersistedData(
                                exception
                            )
                    )
                currentLessonBrowserState
            }
    }

    private fun loadContentLibrary(
        previousState: ContentLibraryUiState =
            ContentLibraryUiState()
    ): ContentLibraryUiState =
        try {
            facade.load()
        } catch (exception: Exception) {
            previousState.copy(
                loadError =
                    DesktopFailureMessage.forPersistedData(
                        exception
                    )
            )
        }
}

internal fun applyDebouncedLessonQuery(
    current: LessonBrowserUiState,
    requestedQuery: String
): LessonBrowserUiState =
    if (current.query == requestedQuery) current.copy(appliedQuery = requestedQuery) else current
