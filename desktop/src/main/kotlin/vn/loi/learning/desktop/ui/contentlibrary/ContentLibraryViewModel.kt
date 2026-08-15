package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Path
import java.time.Clock
import vn.loi.learning.application.integrity.PackageIntegrityReport
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.desktop.ui.browser.toDraftEdits
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
    private val contentMediaStorage: vn.loi.learning.application.port.ContentMediaStorage? = null,
    private val onContentDataChanged:
    (() -> Unit)? = null,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner,
    private val searchDebouncer: DesktopDebouncer = ImmediateDesktopDebouncer,
    private val clock: Clock = Clock.systemUTC(),
    private val integrityCheck: (String, ContentMediaStorage?) -> PackageIntegrityReport = facade::checkPackageIntegrity,
    loadImmediately: Boolean = true
) {

    var uiState by mutableStateOf(
        ContentLibraryUiState()
    )
        private set

    var lessonBrowserUiState by mutableStateOf<LessonBrowserUiState?>(null)
        private set

    var packageBrowserUiState by mutableStateOf<vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState?>(null)
        private set

    var packageIntegrityDialogState by mutableStateOf(PackageIntegrityDialogState())
        private set

    var libraryHealthOverviewState by mutableStateOf(LibraryHealthOverviewState())
        private set

    fun checkPackageIntegrity(packageId: String) {
        if (packageIntegrityDialogState.scanning || libraryHealthOverviewState.scanning) return
        val item = uiState.packages.firstOrNull { it.id == packageId } ?: return
        packageIntegrityDialogState = PackageIntegrityDialogState(
            visible = true, packageId = item.id, packageName = item.name, scanning = true
        )
        taskRunner.run(
            work = { integrityCheck(item.id, contentMediaStorage) },
            onSuccess = { report ->
                packageIntegrityDialogState = packageIntegrityDialogState.copy(scanning = false, report = report)
            },
            onFailure = { exception ->
                packageIntegrityDialogState = packageIntegrityDialogState.copy(
                    scanning = false,
                    error = DesktopFailureMessage.forPersistedData(exception)
                )
            }
        )
    }

    fun checkLibraryHealth(
        targets: List<LibraryHealthPackageTarget> = uiState.packages.map {
            LibraryHealthPackageTarget(it.id, it.name)
        }
    ) {
        if (libraryHealthOverviewState.scanning || packageIntegrityDialogState.scanning) return
        val snapshot = targets.toList()
        libraryHealthOverviewState = libraryHealthOverviewState.copy(scanning = true, failure = null)
        taskRunner.run(
            work = {
                snapshot.map { (packageId, packageName) ->
                    try {
                        LibraryHealthPackageResult(
                            packageId = packageId,
                            packageName = packageName,
                            report = integrityCheck(packageId, contentMediaStorage)
                        )
                    } catch (exception: Exception) {
                        LibraryHealthPackageResult(
                            packageId = packageId,
                            packageName = packageName,
                            failure = DesktopFailureMessage.forPersistedData(exception)
                        )
                    }
                }
            },
            onSuccess = { results ->
                libraryHealthOverviewState = LibraryHealthOverviewState(
                    scanning = false,
                    scannedAt = clock.instant(),
                    results = results
                )
            },
            onFailure = { exception ->
                libraryHealthOverviewState = libraryHealthOverviewState.copy(
                    scanning = false,
                    failure = DesktopFailureMessage.forPersistedData(exception)
                )
            }
        )
    }

    fun showLibraryHealthReport(packageId: String) {
        if (libraryHealthOverviewState.scanning || packageIntegrityDialogState.scanning) return
        val result = libraryHealthOverviewState.results.firstOrNull { it.packageId == packageId }
            ?: return
        val report = result.report ?: return
        packageIntegrityDialogState = PackageIntegrityDialogState(
            visible = true,
            packageId = result.packageId,
            packageName = result.packageName,
            report = report
        )
    }

    fun dismissPackageIntegrityReport() {
        if (!packageIntegrityDialogState.scanning) packageIntegrityDialogState = PackageIntegrityDialogState()
    }

    private var deletedContentSnapshot: vn.loi.learning.application.contentpackaging.browser.DeletedContentSnapshot? = null

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

    private var hasLoadedSuccessfully = false
    private var isDirty = true
    private var loadInFlight = false

    init {
        if (loadImmediately) refresh()
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
        if (loadInFlight) return
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        loadInFlight = true
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
                markLoadedClean()
                loadInFlight = false
            },
            onFailure = { exception ->
                uiState = previousState.copy(
                    loadError = DesktopFailureMessage.forPersistedData(exception),
                    operation = ContentLibraryOperation.Idle
                )
                loadInFlight = false
            }
        )
    }

    fun ensureLoaded() {
        if (!hasLoadedSuccessfully || isDirty) refresh()
    }

    fun invalidate() {
        isDirty = true
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

        createCollection(
            libraryId = dialogState.libraryId,
            name = dialogState.collectionName,
            onSuccess = ::dismissCreateCollectionDialog
        )
    }

    fun createCollection(
        libraryId: String,
        name: String,
        onSuccess: () -> Unit = {}
    ) {
        clearOperationMessage()

        try {
            val normalizedName =
                normalizeCollectionName(
                    name
                )

            runContentLibraryMutation(
                title = normalizedName,
                phase = "Creating collection",
                successMessage = "Collection \"$normalizedName\" was created.",
                mutation = {
                    facade.createCollection(
                        libraryId = libraryId,
                        name = normalizedName
                    )
                },
                onSuccess = onSuccess,
                failureMessage = "Collection creation failed."
            )
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Collection creation failed."
            )
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

        renameCollection(
            collectionId = dialogState.collectionId,
            name = dialogState.collectionName,
            onSuccess = ::dismissRenameCollectionDialog
        )
    }

    fun renameCollection(
        collectionId: String,
        name: String,
        onSuccess: () -> Unit = {}
    ) {
        clearOperationMessage()

        try {
            val normalizedName =
                normalizeCollectionName(
                    name
                )

            runContentLibraryMutation(
                title = normalizedName,
                phase = "Renaming collection",
                successMessage = "Collection was renamed to \"$normalizedName\".",
                mutation = {
                    facade.renameCollection(
                        collectionId = collectionId,
                        name = normalizedName
                    )
                },
                onSuccess = onSuccess,
                failureMessage = "Collection rename failed."
            )
        } catch (exception: Exception) {
            showOperationError(
                exception = exception,
                fallbackMessage =
                    "Collection rename failed."
            )
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

        deleteCollection(
            collectionId = dialogState.collectionId,
            collectionName = dialogState.collectionName,
            onSuccess = ::dismissDeleteCollectionDialog
        )
    }

    fun uninstallPackage(
        packageId: String,
        packageName: String
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Loading(
                title = packageName,
                phase = "Removing topic and learning data"
            )
        )
        taskRunner.run(
            work = {
                facade.removeInstalledPackage(packageId)
                facade.load()
            },
            onSuccess = { refreshedState ->
                resetLibraryNavigationState()
                uiState = refreshedState.copy(
                    importMessage = "Topic \"$packageName\" was removed from Learning Engine.",
                    importError = null,
                    loadError = null,
                    operation = ContentLibraryOperation.Idle
                )
                markLoadedClean()
                onContentDataChanged?.invoke()
            },
            onFailure = { exception ->
                showOperationError(
                    exception = exception,
                    fallbackMessage = "Topic removal failed."
                )
                uiState = uiState.copy(operation = ContentLibraryOperation.Idle)
            }
        )
    }

    fun deleteCollection(
        collectionId: String,
        collectionName: String,
        onSuccess: () -> Unit = {}
    ) {
        clearOperationMessage()

        runContentLibraryMutation(
            title = collectionName,
            phase = "Deleting collection",
            successMessage = "Collection \"$collectionName\" was deleted.",
            mutation = {
                facade.deleteCollection(
                    collectionId = collectionId
                )
            },
            onSuccess = onSuccess,
            failureMessage = "Collection deletion failed."
        )
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

        attachPackageToCollection(
            collectionId = dialogState.collectionId,
            collectionName = dialogState.collectionName,
            packageId = selectedPackage.id,
            packageName = selectedPackage.name,
            onSuccess = ::dismissAttachPackageDialog
        )
    }

    fun attachPackageToCollection(
        collectionId: String,
        collectionName: String,
        packageId: String,
        packageName: String,
        onSuccess: () -> Unit = {}
    ) {
        clearOperationMessage()

        runContentLibraryMutation(
            title = packageName,
            phase = "Attaching package",
            successMessage = "Package \"$packageName\" was attached to collection \"$collectionName\".",
            mutation = {
                facade.attachPackageToCollection(
                    collectionId = collectionId,
                    packageId = packageId
                )
            },
            onSuccess = onSuccess,
            failureMessage = "Package attachment failed."
        )
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

        detachPackageFromCollection(
            collectionId = dialogState.collectionId,
            collectionName = dialogState.collectionName,
            packageId = dialogState.packageId,
            packageName = dialogState.packageName,
            onSuccess = ::dismissDetachPackageDialog
        )
    }

    fun detachPackageFromCollection(
        collectionId: String,
        collectionName: String,
        packageId: String,
        packageName: String,
        onSuccess: () -> Unit = {}
    ) {
        clearOperationMessage()

        runContentLibraryMutation(
            title = packageName,
            phase = "Detaching package",
            successMessage = "Package \"$packageName\" was detached from collection \"$collectionName\".",
            mutation = {
                facade.detachPackageFromCollection(
                    collectionId = collectionId,
                    packageId = packageId
                )
            },
            onSuccess = onSuccess,
            failureMessage = "Package detachment failed."
        )
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
        if (current != null && current.installedPackageId != installedPackageId) {
            deletedContentSnapshot = null
        }
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
            if (latest.query == query) {
                packageBrowserUiState = latest.copy(appliedQuery = query.trim())
            }
        }
    }

    fun clearPackageBrowserQuery() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            query = "",
            appliedQuery = "",
            centerSelectedRowRequest = current.centerSelectedRowRequest + 1L
        )
    }

    fun selectPackageBrowserSearchResult(query: String) {
        val current = packageBrowserUiState ?: return
        if (current.showUnsavedChangesDialog) return
        val submitted = current.copy(query = query, appliedQuery = query.trim())
        packageBrowserUiState = submitted
        val normalizedQuery = submitted.appliedQuery
        val exactMatches = submitted.filteredItems.filter {
            it.questionText.trim().equals(normalizedQuery, ignoreCase = true)
        }
        val target = when {
            exactMatches.size == 1 -> exactMatches.single()
            exactMatches.isEmpty() && submitted.filteredItems.size == 1 -> submitted.filteredItems.single()
            else -> null
        } ?: return
        attemptSelectRowAutoEdit(target.contentId.value)
    }

    fun togglePackageBrowserHighlight(contentId: String) {
        val current = packageBrowserUiState ?: return
        if (current.allItems.none { it.contentId.value == contentId }) return
        val updated = current.highlightedContentIds.toMutableSet().apply {
            if (!add(contentId)) remove(contentId)
        }
        packageBrowserUiState = current.copy(highlightedContentIds = updated)
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
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            selectedContentId = contentIdStr,
            editingContentId = null,
            loadedBaselineDraft = null,
            draftEdits = null
        )
    }

    /**
     * PLE-020 Remediation: Single-click auto-edit.
     * Selects row and loads its baseline draft.
     * `isDirty` remains false until an actual user mutation occurs.
     * If current item is dirty and switching to another row → unsaved protection dialog.
     */
    fun attemptSelectRowAutoEdit(contentIdStr: String) {
        val current = packageBrowserUiState ?: return

        // Guard: same row already selected — keep current draft
        if (current.selectedContentId == contentIdStr && current.editingContentId == contentIdStr) return

        // Guard: another row is dirty → unsaved protection
        if (current.isDirty && current.selectedContentId != contentIdStr) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DoubleClickRow(contentIdStr)
            )
            return
        }

        // Clean selection + baseline draft loading
        val item = current.allItems.firstOrNull { it.contentId.value == contentIdStr }
        val draft = item?.toDraftEdits()
        packageBrowserUiState = current.copy(
            selectedContentId = contentIdStr,
            editingContentId = contentIdStr,
            loadedBaselineDraft = draft,
            draftEdits = draft
        )
    }

    /**
     * PLE-020: Keyboard navigation in Explorer.
     * delta = +1 (down), -1 (up), Int.MIN_VALUE (home), Int.MAX_VALUE (end), ±10 (page).
     * If current item is dirty → unsaved protection before navigating.
     */
    fun navigateExplorerByDelta(delta: Int) {
        val current = packageBrowserUiState ?: return
        val items = current.filteredItems
        if (items.isEmpty()) return

        val currentIndex = items.indexOfFirst { it.contentId.value == current.selectedContentId }
        val targetIndex = when {
            delta == Int.MIN_VALUE -> 0
            delta == Int.MAX_VALUE -> items.size - 1
            currentIndex < 0 -> 0
            else -> (currentIndex + delta).coerceIn(0, items.size - 1)
        }
        if (targetIndex == currentIndex && currentIndex >= 0) return

        val targetId = items[targetIndex].contentId.value
        attemptSelectRowAutoEdit(targetId)
    }

    /**
     * PLE-020: Copy text to system clipboard.
     */
    fun copyToClipboard(text: String) {
        try {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val sel = java.awt.datatransfer.StringSelection(text)
            clipboard.setContents(sel, sel)
        } catch (_: Exception) {}
    }

    /**
     * PLE-020: Duplicate item (stub — shows informational message).
     */
    fun duplicateItem(contentIdStr: String) {
        uiState = uiState.copy(importError = "Duplicate is planned for a future release.")
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

        if (current.selectedContentId == contentIdStr && current.editingContentId == contentIdStr && current.draftEdits != null) {
            return
        }

        val item = current.allItems.firstOrNull { it.contentId.value == contentIdStr }
        val draft = item?.toDraftEdits()
        packageBrowserUiState = current.copy(
            selectedContentId = contentIdStr,
            editingContentId = contentIdStr,
            loadedBaselineDraft = draft,
            draftEdits = draft
        )
    }

    fun startNewItem() {
        val current = packageBrowserUiState ?: return
        if (current.isDirty) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.StartCreate
            )
            return
        }
        val emptyDraft = vn.loi.learning.desktop.ui.browser.ContentDraftEdits(
            contentId = "new_item_draft",
            questionText = "",
            answerText = "",
            pronunciation = "",
            partOfSpeech = "WORD",
            exampleText = "",
            exampleTranslation = ""
        )
        packageBrowserUiState = current.copy(
            isCreatingNewItem = true,
            editingContentId = "new_item_draft",
            loadedBaselineDraft = null,
            draftEdits = emptyDraft
        )
    }

    fun saveNewItem() {
        val current = packageBrowserUiState ?: return
        if (current.isCreateSubmitting) return
        val draft = current.draftEdits ?: return

        if (draft.questionText.isBlank() || draft.answerText.isBlank()) {
            uiState = uiState.copy(
                importError = "Question and Answer must not be blank."
            )
            return
        }

        packageBrowserUiState = current.copy(isCreateSubmitting = true)
        taskRunner.run(
            work = {
                packageBrowserFacade.createContent(
                    draft = draft,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { reloaded ->
                val selectedId = reloaded.selectedContentId
                val reloadedItem = reloaded.selectedItemInView ?: reloaded.selectedItemAnywhere
                val reloadedDraft = reloadedItem?.toDraftEdits()
                packageBrowserUiState = reloaded.copy(
                    isCreatingNewItem = false,
                    selectedContentId = selectedId,
                    editingContentId = selectedId,
                    loadedBaselineDraft = reloadedDraft,
                    draftEdits = reloadedDraft,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption,
                    highlightedContentIds = current.highlightedContentIds,
                    centerSelectedRowRequest = current.centerSelectedRowRequest,
                    isCreateSubmitting = false
                )
                onContentDataChanged?.invoke()
            },
            onFailure = { ex ->
                if (ex is vn.loi.learning.desktop.ui.browser.CanonicalMutationCommittedException) {
                    packageBrowserUiState = current.copy(isCreatingNewItem = false, editingContentId = null, loadedBaselineDraft = null, draftEdits = null, isCreateSubmitting = false)
                    uiState = uiState.copy(importMessage = ex.message, importError = null)
                    onContentDataChanged?.invoke()
                } else {
                    packageBrowserUiState = current.copy(isCreatingNewItem = true, editingContentId = current.editingContentId, draftEdits = current.draftEdits, isCreateSubmitting = false)
                    uiState = uiState.copy(importError = "Create item failed: ${ex.message}")
                }
            }
        )
    }

    fun cancelNewItem() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            isCreatingNewItem = false,
            editingContentId = null,
            loadedBaselineDraft = null,
            draftEdits = null
        )
    }

    /**
     * Bắt đầu edit content đang được chọn.
     */
    fun startEditContent() {
        val current = packageBrowserUiState ?: return
        val item = current.selectedItemAnywhere ?: return
        val draft = item.toDraftEdits()
        packageBrowserUiState = current.copy(
            editingContentId = item.contentId.value,
            loadedBaselineDraft = draft,
            draftEdits = draft
        )
    }

    fun updateDraftQuestion(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(questionText = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftAnswer(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(answerText = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftPronunciation(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(pronunciation = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftPartOfSpeech(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(partOfSpeech = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftExampleText(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(exampleText = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftExampleTranslation(value: String) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(exampleTranslation = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftImageRef(value: String?) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(imageRef = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftQuestionAudioRef(value: String?) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(questionAudioRef = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftAnswerAudioRef(value: String?) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(answerAudioRef = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftExampleAudioRef(value: String?) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(exampleAudioRef = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun updateDraftTranslationAudioRef(value: String?) {
        val current = packageBrowserUiState ?: return
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(translationAudioRef = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun importDraftMediaFile(file: java.io.File, slotName: String) {
        val current = packageBrowserUiState ?: return
        val mediaStorage = contentMediaStorage ?: return
        val normalizedSlot = slotName.lowercase()
        if (normalizedSlot !in setOf("image", "question", "answer", "example", "translation")) {
            uiState = uiState.copy(importError = "Unknown media slot: $slotName")
            return
        }
        val ref = try {
            packageBrowserFacade.importMediaAsset(current.packageName, file, mediaStorage)
        } catch (ex: Exception) {
            uiState = uiState.copy(importError = "Failed to import media file: ${ex.message}")
            return
        }
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = when (normalizedSlot) {
            "image" -> existingDraft.copy(imageRef = ref)
            "question" -> existingDraft.copy(questionAudioRef = ref)
            "answer" -> existingDraft.copy(answerAudioRef = ref)
            "example" -> existingDraft.copy(exampleAudioRef = ref)
            "translation" -> existingDraft.copy(translationAudioRef = ref)
            else -> error("Validated media slot became invalid: $normalizedSlot")
        }

        packageBrowserUiState = current.copy(
            editingContentId = if (current.isCreatingNewItem) current.editingContentId else (current.editingContentId ?: current.selectedContentId),
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    private fun createDraftFromSelectedItem(current: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState): vn.loi.learning.desktop.ui.browser.ContentDraftEdits {
        val item = current.selectedItemAnywhere
        return item?.toDraftEdits() ?: vn.loi.learning.desktop.ui.browser.ContentDraftEdits(contentId = "new_item_draft")
    }

    /**
     * Hủy bỏ edit: khôi phục view mode / clear draft.
     */
    fun discardEdits() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            isCreatingNewItem = false,
            editingContentId = null,
            loadedBaselineDraft = null,
            draftEdits = null
        )
    }

    /**
     * CP2: Save với persistence thật sự.
     * Gọi facade.persistEdit() → reload browser từ repository.
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
                val selectedId = current.selectedContentId
                val projected = reloaded.copy(query = current.query, appliedQuery = current.appliedQuery, selectedLessonFilter = current.selectedLessonFilter, mediaFilter = current.mediaFilter, sortOption = current.sortOption)
                val savedVisible = projected.filteredItems.firstOrNull { it.contentId.value == selectedId }
                val selection = savedVisible ?: projected.filteredItems.firstOrNull()
                val reloadedDraft = selection?.toDraftEdits()
                packageBrowserUiState = projected.copy(
                    selectedContentId = selection?.contentId?.value,
                    editingContentId = selection?.contentId?.value,
                    isCreatingNewItem = false,
                    loadedBaselineDraft = reloadedDraft,
                    draftEdits = reloadedDraft,
                    highlightedContentIds = current.highlightedContentIds,
                    centerSelectedRowRequest = current.centerSelectedRowRequest,
                    query = current.query
                )
                if (savedVisible == null) uiState = uiState.copy(importMessage = "Content saved; it is hidden by the current filter.", importError = null)
            },
            onFailure = { ex ->
                if (ex is vn.loi.learning.desktop.ui.browser.CanonicalMutationCommittedException) {
                    packageBrowserUiState = current.copy(loadedBaselineDraft = draft, draftEdits = draft)
                    uiState = uiState.copy(importMessage = ex.message, importError = null)
                } else uiState = uiState.copy(importError = "Save failed: ${ex.message}")
            }
        )
    }

    /**
     * CP1 fallback: Save in-memory (không persist).
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
                    exampleTranslation = draft.exampleTranslation.takeIf { it.isNotBlank() },
                    imageRef = draft.imageRef,
                    questionAudioRef = draft.questionAudioRef,
                    answerAudioRef = draft.answerAudioRef,
                    exampleAudioRef = draft.exampleAudioRef,
                    translationAudioRef = draft.translationAudioRef,
                    audioRef = draft.questionAudioRef ?: draft.answerAudioRef ?: draft.exampleAudioRef ?: draft.translationAudioRef,
                    hasImage = !draft.imageRef.isNullOrBlank(),
                    hasAudio = !draft.questionAudioRef.isNullOrBlank() || !draft.answerAudioRef.isNullOrBlank() || !draft.exampleAudioRef.isNullOrBlank() || !draft.translationAudioRef.isNullOrBlank()
                )
            } else item
        }

        packageBrowserUiState = current.copy(
            allItems = updatedItems,
            editingContentId = null,
            loadedBaselineDraft = null,
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
     */
    fun confirmDeleteContent() {
        val current = packageBrowserUiState ?: return
        val deleteId = current.selectedContentId ?: return
        if (current.isDirty) {
            dismissDeleteConfirmation()
            return
        }

        val currentFilteredIndex = current.filteredItems.indexOfFirst { it.contentId.value == deleteId }
        val candidateItems = current.filteredItems.filter { it.contentId.value != deleteId }
        val nextSelection = when {
            currentFilteredIndex >= 0 && currentFilteredIndex < candidateItems.size ->
                candidateItems[currentFilteredIndex].contentId.value
            candidateItems.isNotEmpty() ->
                candidateItems.last().contentId.value
            else -> null
        }

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
            onSuccess = { result ->
                val reloaded = result.state
                deletedContentSnapshot = result.snapshot
                val selectedItem = reloaded.allItems.firstOrNull { it.contentId.value == nextSelection } ?: reloaded.selectedItemInView
                val selectedDraft = selectedItem?.toDraftEdits()
                packageBrowserUiState = reloaded.copy(
                    selectedContentId = nextSelection,
                    editingContentId = nextSelection,
                    loadedBaselineDraft = selectedDraft,
                    draftEdits = selectedDraft,
                    showDeleteConfirm = false,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption,
                    canUndoDelete = true,
                    undoDeleteLabel = result.snapshot.displayLabel,
                    highlightedContentIds = current.highlightedContentIds - deleteId,
                    centerSelectedRowRequest = current.centerSelectedRowRequest
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

    fun undoDeleteContent() {
        val current = packageBrowserUiState ?: return
        val snapshot = deletedContentSnapshot ?: return
        if (current.isDirty || current.isCreatingNewItem) return
        taskRunner.run(
            work = {
                packageBrowserFacade.undoDelete(
                    snapshot = snapshot,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { reloaded ->
                val restoredId = snapshot.content.id.value
                val projected = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    sortOption = current.sortOption
                )
                val restored = projected.filteredItems.firstOrNull { it.contentId.value == restoredId }
                val retained = projected.filteredItems.firstOrNull { it.contentId.value == current.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val selection = restored ?: retained
                val draft = selection?.toDraftEdits()
                deletedContentSnapshot = null
                packageBrowserUiState = projected.copy(
                    selectedContentId = selection?.contentId?.value,
                    editingContentId = selection?.contentId?.value,
                    loadedBaselineDraft = draft,
                    draftEdits = draft,
                    canUndoDelete = false,
                    undoDeleteLabel = null,
                    highlightedContentIds = current.highlightedContentIds - restoredId,
                    centerSelectedRowRequest = current.centerSelectedRowRequest
                )
                uiState = uiState.copy(
                    importMessage = if (restored == null) "Undo Delete succeeded; the restored item is hidden by the current filter." else null,
                    importError = null
                )
                onContentDataChanged?.invoke()
            },
            onFailure = { ex ->
                packageBrowserUiState = current.copy(canUndoDelete = true, undoDeleteLabel = snapshot.displayLabel)
                uiState = uiState.copy(importError = "Undo Delete failed: ${ex.message}")
            }
        )
    }

    /**
     * CP3 in-memory stub.
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

        val selectedItem = newItems.firstOrNull { it.contentId.value == nextSelection }
        val selectedDraft = selectedItem?.toDraftEdits()

        packageBrowserUiState = current.copy(
            allItems = newItems,
            selectedContentId = nextSelection,
            editingContentId = nextSelection,
            loadedBaselineDraft = selectedDraft,
            draftEdits = selectedDraft,
            showDeleteConfirm = false,
            highlightedContentIds = current.highlightedContentIds - deleteId
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
        val selItem = current.selectedItemAnywhere
        val selDraft = selItem?.toDraftEdits()
        packageBrowserUiState = current.copy(
            isCreatingNewItem = false,
            editingContentId = selItem?.contentId?.value,
            loadedBaselineDraft = selDraft,
            draftEdits = selDraft,
            showUnsavedChangesDialog = false,
            pendingAction = null
        )
        executePendingAction(action)
    }

    /**
     * Người dùng chọn Save trong dialog → save (create hoặc persist) rồi thực hiện pending action.
     */
    fun confirmSaveAndProceed() {
        val current = packageBrowserUiState ?: return
        val draft = current.draftEdits ?: return
        val action = current.pendingAction

        if (current.isCreatingNewItem) {
            if (current.isCreateSubmitting) return
            if (draft.questionText.isBlank() || draft.answerText.isBlank()) {
                uiState = uiState.copy(
                    importError = "Question and Answer must not be blank."
                )
                return
            }

            packageBrowserUiState = current.copy(isCreateSubmitting = true)
            taskRunner.run(
                work = {
                    packageBrowserFacade.createContent(
                        draft = draft,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    )
                },
                onSuccess = { reloaded ->
                    val selectedItem = reloaded.selectedItemInView ?: reloaded.selectedItemAnywhere
                    val selectedDraft = selectedItem?.toDraftEdits()
                    packageBrowserUiState = reloaded.copy(
                        isCreatingNewItem = false,
                        editingContentId = selectedItem?.contentId?.value,
                        loadedBaselineDraft = selectedDraft,
                        draftEdits = selectedDraft,
                        showUnsavedChangesDialog = false,
                        pendingAction = null,
                        query = current.query,
                        appliedQuery = current.appliedQuery,
                        selectedLessonFilter = current.selectedLessonFilter,
                        mediaFilter = current.mediaFilter,
                        sortOption = current.sortOption,
                        highlightedContentIds = current.highlightedContentIds,
                        centerSelectedRowRequest = current.centerSelectedRowRequest,
                        isCreateSubmitting = false
                    )
                    onContentDataChanged?.invoke()
                    executePendingAction(action)
                },
                onFailure = { ex ->
                    if (ex is vn.loi.learning.desktop.ui.browser.CanonicalMutationCommittedException) {
                        packageBrowserUiState = current.copy(isCreatingNewItem = false, editingContentId = null, loadedBaselineDraft = null, draftEdits = null, showUnsavedChangesDialog = false, pendingAction = null, isCreateSubmitting = false)
                        uiState = uiState.copy(importMessage = ex.message, importError = null)
                        onContentDataChanged?.invoke()
                    } else {
                        packageBrowserUiState = current.copy(showUnsavedChangesDialog = true, isCreateSubmitting = false)
                        uiState = uiState.copy(importError = "Create item failed: ${ex.message}")
                    }
                }
            )
        } else {
            taskRunner.run(
                work = {
                    packageBrowserFacade.persistEdit(
                        draft = draft,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    )
                },
                onSuccess = { reloaded ->
                    val selectedItem = reloaded.allItems.firstOrNull { it.contentId.value == draft.contentId } ?: reloaded.selectedItemInView
                    val selectedDraft = selectedItem?.toDraftEdits()
                    packageBrowserUiState = reloaded.copy(
                        selectedContentId = selectedItem?.contentId?.value ?: current.selectedContentId,
                        editingContentId = selectedItem?.contentId?.value ?: current.selectedContentId,
                        loadedBaselineDraft = selectedDraft,
                        draftEdits = selectedDraft,
                        showUnsavedChangesDialog = false,
                        pendingAction = null,
                        query = current.query,
                        appliedQuery = current.appliedQuery,
                        selectedLessonFilter = current.selectedLessonFilter,
                        mediaFilter = current.mediaFilter,
                        sortOption = current.sortOption,
                        highlightedContentIds = current.highlightedContentIds,
                        centerSelectedRowRequest = current.centerSelectedRowRequest
                    )
                    onContentDataChanged?.invoke()
                    executePendingAction(action)
                },
                onFailure = { ex ->
                    if (ex is vn.loi.learning.desktop.ui.browser.CanonicalMutationCommittedException) {
                        packageBrowserUiState = current.copy(loadedBaselineDraft = draft, draftEdits = draft, showUnsavedChangesDialog = false, pendingAction = null)
                        uiState = uiState.copy(importMessage = ex.message, importError = null)
                    } else {
                        packageBrowserUiState = current.copy(showUnsavedChangesDialog = true)
                        uiState = uiState.copy(importError = "Save failed: ${ex.message}")
                    }
                }
            )
        }
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
                deletedContentSnapshot = null
                packageBrowserUiState = null
                lessonBrowserUiState = null
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BrowsePackage -> {
                deletedContentSnapshot = null
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
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.StartCreate -> startNewItem()
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
        deletedContentSnapshot = null
        packageBrowserUiState = null
        lessonBrowserUiState = null
    }

    fun resetLibraryNavigationState() {
        deletedContentSnapshot = null
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
                markLoadedClean()
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

    private fun runContentLibraryMutation(
        title: String,
        phase: String,
        successMessage: String,
        mutation: () -> Unit,
        onSuccess: () -> Unit,
        failureMessage: String
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return

        val previousLessonBrowserState = lessonBrowserUiState
        uiState = uiState.copy(
            importMessage = null,
            importError = null,
            operation = ContentLibraryOperation.Loading(title, phase)
        )

        taskRunner.run(
            work = {
                mutation()
                val refreshedLibraryState = facade.load()
                val refreshedBrowser = loadLessonBrowserSnapshot(
                    libraryState = refreshedLibraryState,
                    previousState = previousLessonBrowserState
                )
                ContentLibraryMutationResult(
                    libraryState = refreshedLibraryState,
                    lessonBrowserState = refreshedBrowser.state,
                    lessonBrowserLoadError = refreshedBrowser.loadError
                )
            },
            onSuccess = { result ->
                lessonBrowserUiState = result.lessonBrowserState
                uiState = result.libraryState.copy(
                    importMessage = successMessage,
                    importError = null,
                    loadError = result.lessonBrowserLoadError,
                    operation = ContentLibraryOperation.Idle
                )
                markLoadedClean()
                onContentDataChanged?.invoke()
                onSuccess()
            },
            onFailure = { exception ->
                showOperationError(exception, failureMessage)
                uiState = uiState.copy(operation = ContentLibraryOperation.Idle)
            }
        )
    }

    private fun loadLessonBrowserSnapshot(
        libraryState: ContentLibraryUiState,
        previousState: LessonBrowserUiState?
    ): LessonBrowserRefreshResult {
        if (previousState == null) return LessonBrowserRefreshResult(null, null)

        return try {
            val refreshedState =
                if (previousState.installedPackageId != null) {
                    lessonBrowserFacade.loadForPackage(
                        installedPackageId = previousState.installedPackageId,
                        packageName = previousState.libraryName
                    ).copy(
                        query = previousState.query,
                        appliedQuery = previousState.appliedQuery,
                        filter = previousState.filter,
                        sort = previousState.sort,
                        selectedLessonId = previousState.selectedLessonId
                    )
                } else {
                    libraryState.libraries
                        .firstOrNull { it.id == previousState.libraryId }
                        ?.let { selectedLibrary ->
                            lessonBrowserFacade.load(
                                libraryId = selectedLibrary.id,
                                libraryName = selectedLibrary.name
                            )
                        }
                }
            LessonBrowserRefreshResult(refreshedState, null)
        } catch (exception: Exception) {
            LessonBrowserRefreshResult(
                state = previousState,
                loadError = DesktopFailureMessage.forPersistedData(exception)
            )
        }
    }

    private data class ContentLibraryMutationResult(
        val libraryState: ContentLibraryUiState,
        val lessonBrowserState: LessonBrowserUiState?,
        val lessonBrowserLoadError: String?
    )

    private data class LessonBrowserRefreshResult(
        val state: LessonBrowserUiState?,
        val loadError: String?
    )

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

    private fun markLoadedClean() {
        hasLoadedSuccessfully = true
        isDirty = false
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
