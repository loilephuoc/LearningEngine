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
import vn.loi.learning.desktop.ui.browser.summarize
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

    var packageExportDialogState by mutableStateOf(PackageExportDialogState())
        private set

    var libraryHealthOverviewState by mutableStateOf(LibraryHealthOverviewState())
        private set

    var imageReuseDialogState by mutableStateOf(vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewDialogState())
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
    private var batchDeletedContentSnapshot: vn.loi.learning.application.contentpackaging.browser.BatchDeletedContentSnapshot? = null

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
            batchDeletedContentSnapshot = null
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
                    withProblemProjection(packageBrowserFacade.loadForPackage(
                        installedPackageId = installedPackageId,
                        packageName = packageName
                    ))
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
                val prev = packageBrowserUiState
                val configuredBrowser = if (prev != null && prev.installedPackageId == loadedBrowser?.installedPackageId) {
                    loadedBrowser.copy(
                        query = prev.query,
                        appliedQuery = prev.appliedQuery,
                        selectedLessonFilter = prev.selectedLessonFilter,
                        mediaFilter = prev.mediaFilter,
                        imageStatusFilter = prev.imageStatusFilter,
                        sortOption = prev.sortOption,
                        problemFilter = prev.problemFilter
                    )
                } else {
                    loadedBrowser
                }
                packageBrowserUiState = configuredBrowser
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
        candidateImageStatusFilter: vn.loi.learning.desktop.ui.browser.ImageStatusFilter = current.imageStatusFilter,
        candidateSortOption: vn.loi.learning.application.contentpackaging.browser.BrowserSortOption = current.sortOption,
        candidateProblemFilter: vn.loi.learning.desktop.ui.browser.ContentProblemFilter = current.problemFilter
    ): Boolean {
        val editingId = current.editingContentId ?: return true
        val baseFiltered = vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserProjectionPolicy.filterAndSort(
            items = current.allItems,
            query = candidateQuery,
            lessonFilter = candidateLessonFilter,
            mediaFilter = candidateMediaFilter,
            sortOption = candidateSortOption
        ).filter { current.problemProjection.matches(it.contentId.value, candidateProblemFilter) }
        val candidateFiltered = vn.loi.learning.desktop.ui.browser.ImageStatusProjectionPolicy.filter(
            items = baseFiltered,
            filter = candidateImageStatusFilter,
            duplicateImageKeys = current.duplicateImageKeys,
            duplicateQuestionKeys = current.duplicateQuestionKeys
        )
        return candidateFiltered.any { it.contentId.value == editingId }
    }

    fun updatePackageBrowserQuery(query: String) {
        val current = packageBrowserUiState ?: return
        val directNumber = vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserSearchEnterPolicy
            .parseDirectItemNumber(query)

        if (directNumber != null) {
            // Numeric lookup candidate: display typed digits without debounced text filtering
            packageBrowserUiState = current.copy(query = query)
            return
        }

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

        val directNumber = vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserSearchEnterPolicy
            .parseDirectItemNumber(query)

        if (directNumber != null) {
            val existsInPackage = current.allItems.any { it.index == directNumber }
            if (!existsInPackage) {
                uiState = uiState.copy(
                    importError = "Item #$directNumber does not exist in package ${current.packageName}.",
                    importMessage = null
                )
                return
            }

            val target = current.filteredItems.firstOrNull { it.index == directNumber }
            if (target == null) {
                val filterName = when {
                    current.imageStatusFilter == vn.loi.learning.desktop.ui.browser.ContentItemFilter.MISSING_IMAGE -> "Missing Image"
                    current.imageStatusFilter == vn.loi.learning.desktop.ui.browser.ContentItemFilter.HAS_IMAGE -> "Has Image"
                    current.imageStatusFilter == vn.loi.learning.desktop.ui.browser.ContentItemFilter.DUPLICATE_IMAGE -> "Duplicate Image"
                    current.imageStatusFilter == vn.loi.learning.desktop.ui.browser.ContentItemFilter.DUPLICATE_QUESTION -> "Duplicate Question"
                    current.selectedLessonFilter != "ALL" -> "Lesson '${current.selectedLessonFilter}'"
                    current.mediaFilter != vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL -> current.mediaFilter.name
                    current.problemFilter != vn.loi.learning.desktop.ui.browser.ContentProblemFilter.NONE -> "Problem"
                    current.appliedQuery.isNotBlank() -> "current search"
                    else -> "current"
                }
                uiState = uiState.copy(
                    importMessage = "Item #$directNumber is not in the current $filterName filter.",
                    importError = null
                )
                return
            }

            attemptSelectRowAutoEdit(target.contentId.value)
            packageBrowserUiState = packageBrowserUiState?.copy(
                centerSelectedRowRequest = (packageBrowserUiState?.centerSelectedRowRequest ?: 0L) + 1L
            )
            return
        }

        val submitted = current.copy(query = query, appliedQuery = query.trim())
        packageBrowserUiState = submitted
        val target = vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserSearchEnterPolicy
            .resolveTarget(submitted.filteredItems, submitted.appliedQuery) ?: return
        attemptSelectRowAutoEdit(target.contentId.value)
        packageBrowserUiState = packageBrowserUiState?.copy(
            centerSelectedRowRequest = (packageBrowserUiState?.centerSelectedRowRequest ?: 0L) + 1L
        )
    }

    fun togglePackageBrowserHighlight(contentId: String) {
        val current = packageBrowserUiState ?: return
        if (current.allItems.none { it.contentId.value == contentId }) return
        val updated = current.highlightedContentIds.toMutableSet().apply {
            if (!add(contentId)) remove(contentId)
        }
        packageBrowserUiState = current.copy(highlightedContentIds = updated)
    }

    fun togglePackageBrowserMultiSelection(contentId: String) {
        val current = packageBrowserUiState ?: return
        if (current.allItems.none { it.contentId.value == contentId }) return
        val selected = current.selectedContentIds.toMutableSet().apply {
            if (!add(contentId)) remove(contentId)
        }
        packageBrowserUiState = current.copy(
            selectedContentIds = selected,
            selectionAnchorContentId = contentId,
            selectedMediaCheck = null
        )
    }

    fun selectPackageBrowserVisibleRange(contentId: String) {
        val current = packageBrowserUiState ?: return
        val visibleIds = current.filteredItems.map { it.contentId.value }
        val targetIndex = visibleIds.indexOf(contentId)
        if (targetIndex < 0) return
        val anchorIndex = current.selectionAnchorContentId?.let(visibleIds::indexOf)?.takeIf { it >= 0 }
            ?: current.selectedContentId?.let(visibleIds::indexOf)?.takeIf { it >= 0 }
        val range = if (anchorIndex == null) setOf(contentId) else {
            val bounds = minOf(anchorIndex, targetIndex)..maxOf(anchorIndex, targetIndex)
            bounds.mapTo(linkedSetOf()) { visibleIds[it] }
        }
        packageBrowserUiState = current.copy(
            selectedContentIds = current.selectedContentIds + range,
            selectionAnchorContentId = contentId,
            selectedMediaCheck = null
        )
    }

    fun selectAllVisiblePackageBrowserItems() {
        val current = packageBrowserUiState ?: return
        val visible = current.filteredItems.mapTo(linkedSetOf()) { it.contentId.value }
        packageBrowserUiState = current.copy(
            selectedContentIds = current.selectedContentIds + visible,
            selectionAnchorContentId = visible.lastOrNull() ?: current.selectionAnchorContentId,
            selectedMediaCheck = null
        )
    }

    fun clearPackageBrowserMultiSelection() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            selectedContentIds = emptySet(),
            selectionAnchorContentId = null,
            selectedMediaCheck = null
        )
    }

    fun highlightSelectedPackageBrowserItems() {
        val current = packageBrowserUiState ?: return
        val existingIds = current.allItems.mapTo(hashSetOf()) { it.contentId.value }
        packageBrowserUiState = current.copy(
            highlightedContentIds = current.highlightedContentIds + (current.selectedContentIds intersect existingIds)
        )
    }

    fun removeHighlightFromSelectedPackageBrowserItems() {
        val current = packageBrowserUiState ?: return
        packageBrowserUiState = current.copy(
            highlightedContentIds = current.highlightedContentIds - current.selectedContentIds
        )
    }

    fun checkSelectedPackageBrowserMedia() {
        val current = packageBrowserUiState ?: return
        val existingIds = current.allItems.mapTo(hashSetOf()) { it.contentId.value }
        val sanitized = current.selectedContentIds intersect existingIds
        packageBrowserUiState = current.copy(
            selectedContentIds = sanitized,
            selectionAnchorContentId = current.selectionAnchorContentId?.takeIf { it in existingIds },
            selectedMediaCheck = current.problemProjection.summarize(sanitized)
        )
    }

    fun dismissSelectedPackageBrowserMediaCheck() {
        packageBrowserUiState = packageBrowserUiState?.copy(selectedMediaCheck = null)
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

    fun updatePackageBrowserImageStatusFilter(filter: vn.loi.learning.desktop.ui.browser.ImageStatusFilter) {
        val current = packageBrowserUiState ?: return
        if (current.imageStatusFilter == filter) return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateImageStatusFilter = filter)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyImageStatusFilter(filter)
            )
            return
        }
        packageBrowserUiState = current.copy(imageStatusFilter = filter)
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

    fun updatePackageBrowserProblemFilter(filter: vn.loi.learning.desktop.ui.browser.ContentProblemFilter) {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateProblemFilter = filter)) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyProblemFilter(filter)
            )
            return
        }
        val projected = current.copy(problemFilter = filter)
        val selection = projected.filteredItems.firstOrNull { it.contentId.value == current.selectedContentId }
            ?: projected.filteredItems.firstOrNull()
        val draft = selection?.toDraftEdits()
        packageBrowserUiState = projected.copy(
            selectedContentId = selection?.contentId?.value,
            editingContentId = selection?.contentId?.value,
            loadedBaselineDraft = draft,
            draftEdits = draft
        )
    }

    fun navigatePackageBrowserProblem(delta: Int) {
        val current = packageBrowserUiState ?: return
        val items = current.filteredItems
        val currentIndex = items.indexOfFirst { it.contentId.value == current.selectedContentId }
        val targetIndex = currentIndex + delta
        if (currentIndex < 0 || targetIndex !in items.indices) return
        attemptSelectRowAutoEdit(items[targetIndex].contentId.value)
    }

    fun resetPackageBrowserFilters() {
        val current = packageBrowserUiState ?: return
        if (current.isDirty && !isEditedRowStillVisible(current, candidateQuery = "", candidateLessonFilter = "ALL", candidateMediaFilter = vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL, candidateImageStatusFilter = vn.loi.learning.desktop.ui.browser.ImageStatusFilter.ALL, candidateSortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER)) {
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
            imageStatusFilter = vn.loi.learning.desktop.ui.browser.ImageStatusFilter.ALL,
            sortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER,
            problemFilter = vn.loi.learning.desktop.ui.browser.ContentProblemFilter.NONE
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
                withProblemProjection(packageBrowserFacade.createContent(
                    draft = draft,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                ))
            },
            onSuccess = { reloaded ->
                val projected = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter
                )
                val selection = projected.filteredItems.firstOrNull { it.contentId.value == reloaded.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val reloadedDraft = selection?.toDraftEdits()
                packageBrowserUiState = projected.copy(
                    isCreatingNewItem = false,
                    selectedContentId = selection?.contentId?.value,
                    editingContentId = selection?.contentId?.value,
                    loadedBaselineDraft = reloadedDraft,
                    draftEdits = reloadedDraft,
                    highlightedContentIds = current.highlightedContentIds,
                    selectedContentIds = current.selectedContentIds,
                    selectionAnchorContentId = current.selectionAnchorContentId,
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
        val existingIds = current.allItems.mapTo(hashSetOf()) { it.contentId.value }
        val selectedIds = current.selectedContentIds intersect existingIds
        if (selectedIds.size > 1) {
            if (current.isDirty) {
                uiState = uiState.copy(
                    importError = "Save or discard the current draft before changing POS for selected items."
                )
                return
            }
            packageBrowserUiState = current.copy(
                selectedContentIds = selectedIds,
                pendingBatchPartOfSpeech = value.trim(),
                batchPartOfSpeechResult = null
            )
            return
        }
        val baseline = current.loadedBaselineDraft ?: current.selectedItemAnywhere?.toDraftEdits()
        val existingDraft = current.draftEdits ?: baseline ?: return
        val updatedDraft = existingDraft.copy(partOfSpeech = value)
        packageBrowserUiState = current.copy(
            editingContentId = current.editingContentId ?: current.selectedContentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
    }

    fun cancelBatchPartOfSpeech() {
        packageBrowserUiState = packageBrowserUiState?.copy(pendingBatchPartOfSpeech = null)
    }

    fun confirmBatchPartOfSpeech() {
        val current = packageBrowserUiState ?: return
        val target = current.pendingBatchPartOfSpeech ?: return
        if (current.isBatchPartOfSpeechSubmitting || current.isDirty) return
        val existingIds = current.allItems.mapTo(hashSetOf()) { it.contentId.value }
        val selectedIds = current.selectedContentIds intersect existingIds
        if (selectedIds.size <= 1) {
            packageBrowserUiState = current.copy(pendingBatchPartOfSpeech = null)
            return
        }
        packageBrowserUiState = current.copy(isBatchPartOfSpeechSubmitting = true)
        taskRunner.run(
            work = {
                packageBrowserFacade.updatePartOfSpeechBatch(
                    contentIds = selectedIds,
                    partOfSpeech = target,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { (result, reloaded) ->
                val projected = withProblemProjection(reloaded).copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    selectedContentIds = result.contentIds.mapTo(linkedSetOf()) { it.value },
                    selectionAnchorContentId = current.selectionAnchorContentId?.takeIf { it in selectedIds },
                    highlightedContentIds = current.highlightedContentIds,
                    centerSelectedRowRequest = current.centerSelectedRowRequest
                )
                val primary = projected.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val draft = primary?.toDraftEdits()
                packageBrowserUiState = projected.copy(
                    selectedContentId = primary?.contentId?.value,
                    editingContentId = primary?.contentId?.value,
                    loadedBaselineDraft = draft,
                    draftEdits = draft,
                    pendingBatchPartOfSpeech = null,
                    isBatchPartOfSpeechSubmitting = false,
                    batchPartOfSpeechResult = "${result.selectedCount} selected · ${result.changedCount} changed · ${result.unchangedCount} already $target"
                )
                uiState = uiState.copy(importError = null)
                onContentDataChanged?.invoke()
            },
            onFailure = { failure ->
                packageBrowserUiState = current.copy(
                    isBatchPartOfSpeechSubmitting = false,
                    pendingBatchPartOfSpeech = target
                )
                uiState = uiState.copy(importError = "Batch POS failed: ${failure.message}")
            }
        )
    }

    fun openPosReview(initialScope: vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope? = null) {
        val current = packageBrowserUiState ?: return
        val resolvedScope = initialScope ?: when {
            current.selectedContentIds.size > 1 -> vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.SELECTED_ITEMS
            current.problemFilter != vn.loi.learning.desktop.ui.browser.ContentProblemFilter.NONE -> vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_FILTER_RESULTS
            current.appliedQuery.isNotBlank() -> vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_SEARCH_RESULTS
            else -> vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.ALL_ITEMS
        }
        val items = resolveItemsForPosReviewScope(current, resolvedScope)
        val rows = buildPosReviewRows(items)
        packageBrowserUiState = current.copy(
            posReviewState = vn.loi.learning.desktop.ui.browser.posreview.PosBatchReviewState(
                scope = resolvedScope,
                currentRows = rows
            )
        )
    }

    fun closePosReview() {
        packageBrowserUiState = packageBrowserUiState?.copy(posReviewState = null)
    }

    fun setPosReviewScope(scope: vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        val items = resolveItemsForPosReviewScope(current, scope)
        val rows = buildPosReviewRows(items)
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(
                scope = scope,
                currentRows = rows,
                selectedRowIds = emptySet(),
                searchQuery = "",
                statusFilter = vn.loi.learning.desktop.ui.browser.posreview.PosReviewRowStatus.ALL,
                showConfirmApply = false,
                errorMessage = null
            )
        )
    }

    fun togglePosReviewRowSelection(contentId: String) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.toggleRowSelection(contentId)
        )
    }

    fun togglePosReviewAllFiltered() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.toggleAllFiltered()
        )
    }

    fun clearPosReviewSelection() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.clearSelection()
        )
    }

    fun updatePosReviewRowNewPos(contentId: String, newPos: String) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.updateRowNewPos(contentId, newPos)
        )
    }

    fun analyzePosReview() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.analyzeRows()
        )
    }

    fun resetPosReviewDrafts() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.resetDrafts()
        )
    }

    fun batchSetPosReviewSelectedPos(newPos: String) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.batchSetSelectedPos(newPos)
        )
    }

    fun batchSetPosReviewFilteredPos(newPos: String) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.batchSetFilteredPos(newPos)
        )
    }

    fun setPosReviewSearchQuery(query: String) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(searchQuery = query)
        )
    }

    fun setPosReviewStatusFilter(filter: vn.loi.learning.desktop.ui.browser.posreview.PosReviewRowStatus) {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(statusFilter = filter)
        )
    }

    fun requestApplyPosReview() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        if (reviewState.changedCount > 0 && !reviewState.isSubmitting) {
            packageBrowserUiState = current.copy(
                posReviewState = reviewState.copy(showConfirmApply = true)
            )
        }
    }

    fun cancelApplyPosReview() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(showConfirmApply = false)
        )
    }

    fun confirmApplyPosReview() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        if (reviewState.isSubmitting || reviewState.changedCount == 0) return

        val updates = reviewState.currentRows
            .filter { it.isChanged }
            .associate { it.contentId to it.newPos }

        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(isSubmitting = true, showConfirmApply = false)
        )

        taskRunner.run(
            work = {
                packageBrowserFacade.updatePartOfSpeechMultiBatch(
                    updates = updates,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { (result, reloaded) ->
                val projected = withProblemProjection(reloaded).copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    selectedContentIds = current.selectedContentIds,
                    selectionAnchorContentId = current.selectionAnchorContentId,
                    highlightedContentIds = current.highlightedContentIds,
                    centerSelectedRowRequest = current.centerSelectedRowRequest,
                    posReviewState = null,
                    batchPartOfSpeechResult = "POS Review: ${result.changedCount} items updated (${result.unchangedCount} unchanged)"
                )
                val primary = projected.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val draft = primary?.toDraftEdits()
                packageBrowserUiState = projected.copy(
                    selectedContentId = primary?.contentId?.value,
                    editingContentId = primary?.contentId?.value,
                    loadedBaselineDraft = draft,
                    draftEdits = draft
                )
                uiState = uiState.copy(importError = null)
                onContentDataChanged?.invoke()
            },
            onFailure = { failure ->
                val latest = packageBrowserUiState
                packageBrowserUiState = latest?.copy(
                    posReviewState = latest.posReviewState?.copy(
                        isSubmitting = false,
                        errorMessage = "Batch POS apply failed: ${failure.message}"
                    )
                )
                uiState = uiState.copy(importError = "Batch POS apply failed: ${failure.message}")
            }
        )
    }

    fun requestUnlockPosReviewSelected() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        if (reviewState.canUnlockSelected) {
            packageBrowserUiState = current.copy(
                posReviewState = reviewState.copy(showConfirmUnlock = true)
            )
        }
    }

    fun cancelUnlockPosReviewConfirmation() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(showConfirmUnlock = false)
        )
    }

    fun confirmUnlockPosReviewSelected() {
        val current = packageBrowserUiState ?: return
        val reviewState = current.posReviewState ?: return
        val confirmedSelectedIds = reviewState.selectedRowIds
            .filter { id -> reviewState.currentRows.any { it.contentId == id && it.isConfirmed } }
            .toSet()
        if (confirmedSelectedIds.isEmpty() || reviewState.isSubmitting) return

        packageBrowserUiState = current.copy(
            posReviewState = reviewState.copy(isSubmitting = true, showConfirmUnlock = false)
        )

        taskRunner.run(
            work = {
                packageBrowserFacade.unlockPartOfSpeechReviewBatch(
                    contentIds = confirmedSelectedIds,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                )
            },
            onSuccess = { (result, reloaded) ->
                val projected = withProblemProjection(reloaded).copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    selectedContentIds = current.selectedContentIds,
                    selectionAnchorContentId = current.selectionAnchorContentId,
                    highlightedContentIds = current.highlightedContentIds,
                    centerSelectedRowRequest = current.centerSelectedRowRequest
                )
                val primary = projected.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val draft = primary?.toDraftEdits()
                val reloadedItems = resolveItemsForPosReviewScope(projected, reviewState.scope)
                val newRows = buildPosReviewRows(reloadedItems)
                packageBrowserUiState = projected.copy(
                    selectedContentId = primary?.contentId?.value,
                    editingContentId = primary?.contentId?.value,
                    loadedBaselineDraft = draft,
                    draftEdits = draft,
                    posReviewState = reviewState.copy(
                        currentRows = newRows,
                        isSubmitting = false,
                        showConfirmUnlock = false,
                        selectedRowIds = emptySet()
                    ),
                    batchPartOfSpeechResult = "Unlocked ${result.changedCount} POS review item(s)"
                )
                uiState = uiState.copy(importError = null)
                onContentDataChanged?.invoke()
            },
            onFailure = { failure ->
                val latest = packageBrowserUiState
                packageBrowserUiState = latest?.copy(
                    posReviewState = latest.posReviewState?.copy(
                        isSubmitting = false,
                        showConfirmUnlock = false,
                        errorMessage = "Unlock POS failed: ${failure.message}"
                    )
                )
                uiState = uiState.copy(importError = "Unlock POS failed: ${failure.message}")
            }
        )
    }

    private fun resolveItemsForPosReviewScope(
        state: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState,
        scope: vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope
    ): List<vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem> =
        when (scope) {
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.ALL_ITEMS ->
                state.allItems
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.SELECTED_ITEMS ->
                state.allItems.filter { it.contentId.value in state.selectedContentIds }
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_SEARCH_RESULTS ->
                if (state.appliedQuery.isBlank()) state.allItems
                else state.allItems.filter { it.searchableText.contains(state.appliedQuery.lowercase()) }
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewScope.CURRENT_FILTER_RESULTS ->
                state.filteredItems
        }

    private fun buildPosReviewRows(
        items: List<vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem>
    ): List<vn.loi.learning.desktop.ui.browser.posreview.PosReviewRowItem> =
        items.map { item ->
            val pos = item.partOfSpeech.orEmpty()
            val canonical = vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer.canonicalize(pos)
            val authority = if (item.partOfSpeechReviewStatus?.equals("USER_CONFIRMED", ignoreCase = true) == true) {
                vn.loi.learning.desktop.ui.browser.posreview.PosReviewAuthority.USER_CONFIRMED
            } else {
                vn.loi.learning.desktop.ui.browser.posreview.PosReviewAuthority.UNREVIEWED
            }
            vn.loi.learning.desktop.ui.browser.posreview.PosReviewRowItem(
                contentId = item.contentId.value,
                question = item.questionText,
                answer = item.answerText,
                translation = item.exampleTranslation.orEmpty(),
                originalPos = pos,
                newPos = pos,
                isCustomOrUnknown = pos.isNotBlank() && (canonical == null || !canonical.known),
                exampleText = item.exampleText,
                pronunciation = item.pronunciation.orEmpty(),
                reviewAuthority = authority
            )
        }

    fun openContentMaintenanceExport(initialScope: vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope? = null) {
        val current = packageBrowserUiState ?: return
        val resolvedScope = initialScope ?: when {
            current.problemFilter != vn.loi.learning.desktop.ui.browser.ContentProblemFilter.NONE ->
                vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_FILTER_RESULTS
            current.selectedContentIds.size > 1 ->
                vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.SELECTED_ITEMS
            current.appliedQuery.isNotBlank() ->
                vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_SEARCH_RESULTS
            else ->
                vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.ALL_ITEMS
        }
        val defaultDir = System.getProperty("user.home") ?: "."
        val fileName = vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExporter.generateSuggestedFileName(
            packageName = current.packageName,
            filter = current.problemFilter,
            scope = resolvedScope
        )
        packageBrowserUiState = current.copy(
            contentMaintenanceExportState = vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportState(
                selectedScope = resolvedScope,
                targetDirectory = defaultDir,
                targetFileName = fileName
            )
        )
    }

    fun closeContentMaintenanceExport() {
        packageBrowserUiState = packageBrowserUiState?.copy(contentMaintenanceExportState = null)
    }

    fun setContentMaintenanceExportScope(scope: vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope) {
        val current = packageBrowserUiState ?: return
        val exportState = current.contentMaintenanceExportState ?: return
        val fileName = vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExporter.generateSuggestedFileName(
            packageName = current.packageName,
            filter = current.problemFilter,
            scope = scope
        )
        packageBrowserUiState = current.copy(
            contentMaintenanceExportState = exportState.copy(
                selectedScope = scope,
                targetFileName = fileName,
                exportSuccessMessage = null,
                errorMessage = null
            )
        )
    }

    fun setExportTargetDirectory(dir: String) {
        val current = packageBrowserUiState ?: return
        val exportState = current.contentMaintenanceExportState ?: return
        packageBrowserUiState = current.copy(
            contentMaintenanceExportState = exportState.copy(targetDirectory = dir)
        )
    }

    fun setExportFileName(fileName: String) {
        val current = packageBrowserUiState ?: return
        val exportState = current.contentMaintenanceExportState ?: return
        packageBrowserUiState = current.copy(
            contentMaintenanceExportState = exportState.copy(targetFileName = fileName)
        )
    }

    fun executeContentMaintenanceExport() {
        val current = packageBrowserUiState ?: return
        val exportState = current.contentMaintenanceExportState ?: return
        val items = resolveItemsForExportScope(current, exportState.selectedScope)
        if (items.isEmpty()) {
            packageBrowserUiState = current.copy(
                contentMaintenanceExportState = exportState.copy(errorMessage = "No items in selected scope to export.")
            )
            return
        }

        packageBrowserUiState = current.copy(
            contentMaintenanceExportState = exportState.copy(isExporting = true, errorMessage = null)
        )

        taskRunner.run(
            work = {
                val targetFile = java.io.File(exportState.targetDirectory, exportState.targetFileName)
                vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExporter.exportToFile(
                    targetFile = targetFile,
                    packageId = current.installedPackageId.value,
                    packageName = current.packageName,
                    scope = exportState.selectedScope,
                    filter = current.problemFilter,
                    items = items
                )
            },
            onSuccess = { file ->
                val latest = packageBrowserUiState
                packageBrowserUiState = latest?.copy(
                    contentMaintenanceExportState = latest.contentMaintenanceExportState?.copy(
                        isExporting = false,
                        exportSuccessMessage = "Exported ${items.size} items to ${file.name}",
                        exportedFilePath = file.absolutePath
                    )
                )
            },
            onFailure = { failure ->
                val latest = packageBrowserUiState
                packageBrowserUiState = latest?.copy(
                    contentMaintenanceExportState = latest.contentMaintenanceExportState?.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${failure.message}"
                    )
                )
            }
        )
    }

    private fun resolveItemsForExportScope(
        state: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState,
        scope: vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope
    ): List<vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem> =
        when (scope) {
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.ALL_ITEMS ->
                state.allItems
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.SELECTED_ITEMS ->
                state.allItems.filter { it.contentId.value in state.selectedContentIds }
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_SEARCH_RESULTS ->
                if (state.appliedQuery.isBlank()) state.allItems
                else state.allItems.filter { it.searchableText.contains(state.appliedQuery.lowercase()) }
            vn.loi.learning.desktop.ui.browser.export.ContentMaintenanceExportScope.CURRENT_FILTER_RESULTS ->
                state.filteredItems
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

        val updatedState = current.copy(
            editingContentId = if (current.isCreatingNewItem) current.editingContentId else (current.editingContentId ?: current.selectedContentId),
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
        packageBrowserUiState = updatedState

        if (normalizedSlot == "image" && !current.isCreatingNewItem) {
            persistDraft(updatedState, updatedDraft)
        }
    }

    /**
     * Applies generated TTS audio reference to the specified field and persists it.
     */
    fun applyGeneratedTtsAudio(contentId: String, field: vn.loi.learning.desktop.tts.TtsField, audioRef: String) {
        val current = packageBrowserUiState ?: return
        val item = current.allItems.firstOrNull { it.contentId.value == contentId }
            ?: current.selectedItemAnywhere ?: return
        val baseline = item.toDraftEdits()
        val existingDraft = if (current.editingContentId == contentId && current.draftEdits != null) {
            current.draftEdits
        } else {
            baseline
        }

        val updatedDraft = when (field) {
            vn.loi.learning.desktop.tts.TtsField.QUESTION -> existingDraft.copy(questionAudioRef = audioRef)
            vn.loi.learning.desktop.tts.TtsField.ANSWER -> existingDraft.copy(answerAudioRef = audioRef)
            vn.loi.learning.desktop.tts.TtsField.EXAMPLE -> existingDraft.copy(exampleAudioRef = audioRef)
            vn.loi.learning.desktop.tts.TtsField.TRANSLATION -> existingDraft.copy(translationAudioRef = audioRef)
        }

        val updatedState = current.copy(
            editingContentId = contentId,
            selectedContentId = contentId,
            loadedBaselineDraft = baseline,
            draftEdits = updatedDraft
        )
        packageBrowserUiState = updatedState
        persistDraft(updatedState, updatedDraft)
    }

    /**
     * Applies a batch of generated TTS audio references atomically to content items and captures an undo snapshot.
     */
    fun applyBatchTtsAudio(
        results: List<vn.loi.learning.desktop.tts.batch.BatchTtsJobResult>,
        onProgress: ((appliedCount: Int, totalCount: Int, failedCount: Int) -> Unit)? = null,
        onComplete: ((appliedCount: Int, failedCount: Int) -> Unit)? = null
    ) {
        val current = packageBrowserUiState ?: run {
            onComplete?.invoke(0, 0)
            return
        }
        val validResults = results.filter { it.status == vn.loi.learning.desktop.tts.batch.BatchTtsJobStatus.SUCCESS && it.assetRelativePath != null }
        if (validResults.isEmpty()) {
            onComplete?.invoke(0, 0)
            return
        }

        val totalTargets = validResults.size
        var appliedCount = 0
        var failedCount = 0

        val undoEntries = mutableListOf<vn.loi.learning.desktop.tts.batch.BatchTtsUndoEntry>()
        val newlyCreatedPaths = validResults.mapNotNull { it.assetRelativePath }.toSet()

        val byContentId = validResults.groupBy { it.job.contentId }

        taskRunner.run(
            work = {
                val reloadedBefore = packageBrowserFacade.loadForPackage(current.installedPackageId, current.packageName)

                for ((contentId, jobs) in byContentId) {
                    try {
                        val item = reloadedBefore.allItems.firstOrNull { it.contentId.value == contentId }
                        if (item == null) {
                            failedCount += jobs.size
                            onProgress?.invoke(appliedCount, totalTargets, failedCount)
                            continue
                        }
                        var draft = item.toDraftEdits()

                        for (res in jobs) {
                            val path = res.assetRelativePath ?: continue
                            val previousRef = when (res.job.field) {
                                vn.loi.learning.desktop.tts.TtsField.QUESTION -> item.questionAudioRef
                                vn.loi.learning.desktop.tts.TtsField.ANSWER -> item.answerAudioRef
                                vn.loi.learning.desktop.tts.TtsField.EXAMPLE -> item.exampleAudioRef
                                vn.loi.learning.desktop.tts.TtsField.TRANSLATION -> item.translationAudioRef
                            }
                            undoEntries.add(
                                vn.loi.learning.desktop.tts.batch.BatchTtsUndoEntry(
                                    contentId = contentId,
                                    field = res.job.field,
                                    previousAudioRef = previousRef,
                                    appliedAudioRef = path
                                )
                            )
                            draft = when (res.job.field) {
                                vn.loi.learning.desktop.tts.TtsField.QUESTION -> draft.copy(questionAudioRef = path)
                                vn.loi.learning.desktop.tts.TtsField.ANSWER -> draft.copy(answerAudioRef = path)
                                vn.loi.learning.desktop.tts.TtsField.EXAMPLE -> draft.copy(exampleAudioRef = path)
                                vn.loi.learning.desktop.tts.TtsField.TRANSLATION -> draft.copy(translationAudioRef = path)
                            }
                            appliedCount++
                        }

                        packageBrowserFacade.persistEdit(
                            draft = draft,
                            installedPackageId = current.installedPackageId,
                            packageName = current.packageName
                        )
                    } catch (_: Exception) {
                        failedCount += jobs.size
                    }
                    onProgress?.invoke(appliedCount, totalTargets, failedCount)
                }

                val snapshot = vn.loi.learning.desktop.tts.batch.BatchTtsUndoSnapshot(
                    packageName = current.packageName,
                    entries = undoEntries,
                    newlyCreatedAssetPaths = newlyCreatedPaths
                )

                val reloaded = packageBrowserFacade.loadForPackage(current.installedPackageId, current.packageName)
                withProblemProjection(reloaded) to snapshot
            },
            onSuccess = { (reloaded, snapshot) ->
                packageBrowserUiState = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    canUndoBatchTts = true,
                    undoBatchTtsLabel = "Undo TTS (${snapshot.totalApplied})",
                    lastBatchTtsUndoSnapshot = snapshot
                )
                uiState = uiState.copy(importMessage = "Applied ${snapshot.totalApplied} audio targets successfully.")
                onComplete?.invoke(snapshot.totalApplied, failedCount)
            },
            onFailure = { ex ->
                uiState = uiState.copy(importError = "Failed to apply batch TTS: ${ex.message}")
                onComplete?.invoke(appliedCount, totalTargets - appliedCount)
            }
        )
    }

    /**
     * Reverts the last applied TTS batch, restoring previous audio references and safely cleaning unreferenced assets.
     */
    fun undoLastBatchTts() {
        val current = packageBrowserUiState ?: return
        val snapshot = current.lastBatchTtsUndoSnapshot ?: return

        taskRunner.run(
            work = {
                val byContentId = snapshot.entries.groupBy { it.contentId }
                val reloadedBeforeUndo = packageBrowserFacade.loadForPackage(current.installedPackageId, current.packageName)

                for ((contentId, entries) in byContentId) {
                    val item = reloadedBeforeUndo.allItems.firstOrNull { it.contentId.value == contentId } ?: continue
                    var draft = item.toDraftEdits()

                    for (entry in entries) {
                        draft = when (entry.field) {
                            vn.loi.learning.desktop.tts.TtsField.QUESTION -> draft.copy(questionAudioRef = entry.previousAudioRef)
                            vn.loi.learning.desktop.tts.TtsField.ANSWER -> draft.copy(answerAudioRef = entry.previousAudioRef)
                            vn.loi.learning.desktop.tts.TtsField.EXAMPLE -> draft.copy(exampleAudioRef = entry.previousAudioRef)
                            vn.loi.learning.desktop.tts.TtsField.TRANSLATION -> draft.copy(translationAudioRef = entry.previousAudioRef)
                        }
                    }

                    packageBrowserFacade.persistEdit(
                        draft = draft,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    )
                }

                // Safe cleanup: only delete newly created files that are no longer referenced in the package
                val reloadedAfterUndo = packageBrowserFacade.loadForPackage(current.installedPackageId, current.packageName)
                val allActiveAudioRefs = reloadedAfterUndo.allItems.flatMap {
                    listOfNotNull(it.questionAudioRef, it.answerAudioRef, it.exampleAudioRef, it.translationAudioRef, it.audioRef)
                }.toSet()

                val storage = contentMediaStorage
                if (storage != null) {
                    for (path in snapshot.newlyCreatedAssetPaths) {
                        if (path !in allActiveAudioRefs) {
                            try {
                                storage.resolve(path)?.toFile()?.delete()
                            } catch (_: Exception) {}
                        }
                    }
                }

                withProblemProjection(reloadedAfterUndo)
            },
            onSuccess = { reloaded ->
                packageBrowserUiState = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    canUndoBatchTts = false,
                    undoBatchTtsLabel = null,
                    lastBatchTtsUndoSnapshot = null
                )
                uiState = uiState.copy(importMessage = "TTS batch undone: ${snapshot.totalApplied} audio references restored.")
            },
            onFailure = { ex ->
                uiState = uiState.copy(importError = "Failed to undo batch TTS: ${ex.message}")
            }
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
        persistDraft(current, draft)
    }

    private fun persistDraft(
        current: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState,
        draft: vn.loi.learning.desktop.ui.browser.ContentDraftEdits
    ) {
        taskRunner.run(
            work = {
                withProblemProjection(packageBrowserFacade.persistEdit(
                    draft = draft,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                ))
            },
            onSuccess = { reloaded ->
                val selectedId = current.selectedContentId
                val projected = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter
                )
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
                    selectedContentIds = current.selectedContentIds,
                    selectionAnchorContentId = current.selectionAnchorContentId,
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

        packageBrowserUiState = withProblemProjection(current.copy(
            allItems = updatedItems,
            editingContentId = null,
            loadedBaselineDraft = null,
            draftEdits = null
        ))
    }

    /** Hiển thị dialog xác nhận xóa Content. */
    fun showBatchDeleteConfirmation() {
        val current = packageBrowserUiState ?: return
        if (current.selectedContentIds.size < 2) return
        val preflight = try {
            packageBrowserFacade.preflightBatchDelete(current.selectedContentIds, current.installedPackageId)
        } catch (failure: Throwable) {
            uiState = uiState.copy(importError = "Batch Delete preflight failed: ${failure.message}")
            return
        }
        val resolvable = preflight.resolvableContentIds.mapTo(linkedSetOf()) { it.value }
        val sanitizedState = current.copy(
            selectedContentIds = current.selectedContentIds intersect resolvable,
            selectionAnchorContentId = current.selectionAnchorContentId?.takeIf { it in resolvable }
        )
        if (preflight.blockers.isNotEmpty()) {
            packageBrowserUiState = sanitizedState.copy(
                pendingBatchDeleteContentIds = emptySet(),
                batchDeleteBlockerMessage = "Cannot delete selected items: ${preflight.blockers.joinToString { it.reason }}"
            )
            return
        }
        if (resolvable.size < 2) {
            packageBrowserUiState = sanitizedState
            return
        }
        if (current.isDirty && current.selectedContentId in resolvable) {
            packageBrowserUiState = sanitizedState.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BatchDelete(resolvable)
            )
            return
        }
        packageBrowserUiState = sanitizedState.copy(
            pendingBatchDeleteContentIds = resolvable,
            batchDeleteBlockerMessage = null
        )
    }

    fun dismissBatchDeleteConfirmation() {
        packageBrowserUiState = packageBrowserUiState?.copy(pendingBatchDeleteContentIds = emptySet())
    }

    fun dismissBatchDeleteBlocker() {
        packageBrowserUiState = packageBrowserUiState?.copy(batchDeleteBlockerMessage = null)
    }

    fun confirmBatchDelete() {
        val current = packageBrowserUiState ?: return
        val deleteIds = current.pendingBatchDeleteContentIds
        if (deleteIds.size < 2 || current.isBatchDeleteSubmitting) return
        if (current.isDirty && current.selectedContentId in deleteIds) return
        val primaryDeleted = current.selectedContentId in deleteIds
        val currentIndex = current.filteredItems.indexOfFirst { it.contentId.value == current.selectedContentId }
        val remainingVisible = current.filteredItems.filterNot { it.contentId.value in deleteIds }
        val replacement = if (primaryDeleted) {
            remainingVisible.getOrNull(currentIndex.coerceAtLeast(0)) ?: remainingVisible.lastOrNull()
        } else null
        val preflight = try {
            packageBrowserFacade.preflightBatchDelete(deleteIds, current.installedPackageId)
        } catch (failure: Throwable) {
            packageBrowserUiState = current.copy(pendingBatchDeleteContentIds = emptySet())
            uiState = uiState.copy(importError = "Batch Delete preflight failed: ${failure.message}")
            return
        }
        if (preflight.blockers.isNotEmpty() || preflight.resolvableContentIds.size != deleteIds.size) {
            packageBrowserUiState = current.copy(
                pendingBatchDeleteContentIds = emptySet(),
                batchDeleteBlockerMessage = "Cannot delete selected items because their canonical state changed."
            )
            return
        }
        packageBrowserUiState = current.copy(isBatchDeleteSubmitting = true)
        taskRunner.run(
            work = {
                packageBrowserFacade.deleteContents(preflight, current.installedPackageId, current.packageName)
                    .let { it.copy(state = withProblemProjection(it.state)) }
            },
            onSuccess = { result ->
                batchDeletedContentSnapshot = result.snapshot
                deletedContentSnapshot = null
                val projected = result.state.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter
                )
                val retainedPrimary = projected.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                val selected = if (primaryDeleted) {
                    replacement?.contentId?.value?.let { id -> projected.allItems.firstOrNull { it.contentId.value == id } }
                } else retainedPrimary
                val draft = if (!primaryDeleted && current.isDirty) current.draftEdits else selected?.toDraftEdits()
                val baseline = if (!primaryDeleted && current.isDirty) current.loadedBaselineDraft else selected?.toDraftEdits()
                packageBrowserUiState = projected.copy(
                    selectedContentId = selected?.contentId?.value,
                    editingContentId = selected?.contentId?.value,
                    draftEdits = draft,
                    loadedBaselineDraft = baseline,
                    selectedContentIds = current.selectedContentIds - deleteIds,
                    highlightedContentIds = current.highlightedContentIds - deleteIds,
                    selectionAnchorContentId = current.selectionAnchorContentId?.takeUnless { it in deleteIds },
                    pendingBatchDeleteContentIds = emptySet(),
                    isBatchDeleteSubmitting = false,
                    canUndoDelete = true,
                    undoDeleteLabel = result.snapshot.displayLabel,
                    centerSelectedRowRequest = current.centerSelectedRowRequest
                )
                onContentDataChanged?.invoke()
            },
            onFailure = { failure ->
                packageBrowserUiState = current.copy(pendingBatchDeleteContentIds = emptySet(), isBatchDeleteSubmitting = false)
                uiState = uiState.copy(importError = "Batch Delete failed: ${failure.message}")
            }
        )
    }

    fun showDeleteConfirmation() {
        val current = packageBrowserUiState ?: return
        if (current.selectedContentIds.size >= 2) {
            showBatchDeleteConfirmation()
            return
        }
        val targetId = current.selectedContentIds.singleOrNull() ?: current.selectedContentId ?: return
        if (current.isDirty && current.selectedContentId == targetId) {
            packageBrowserUiState = current.copy(
                showUnsavedChangesDialog = true,
                pendingAction = vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DeleteContent(targetId)
            )
            return
        }
        packageBrowserUiState = current.copy(showDeleteConfirm = true, deleteTargetContentId = targetId)
    }

    /** Ẩn dialog xác nhận xóa. */
    fun dismissDeleteConfirmation() {
        packageBrowserUiState = packageBrowserUiState?.copy(showDeleteConfirm = false, deleteTargetContentId = null)
    }

    /**
     * CP3: Xóa Content với persistence thật sự.
     */
    fun confirmDeleteContent() {
        val current = packageBrowserUiState ?: return
        val deleteId = current.deleteTargetContentId ?: current.selectedContentId ?: return
        val primaryDeleted = current.selectedContentId == deleteId
        if (current.isDirty && primaryDeleted) {
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
            showDeleteConfirm = false,
            deleteTargetContentId = null
        )

        taskRunner.run(
            work = {
                packageBrowserFacade.deleteContent(
                    contentId = vn.loi.learning.domain.content.model.ContentId(deleteId),
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                ).let { it.copy(state = withProblemProjection(it.state)) }
            },
            onSuccess = { result ->
                val reloaded = result.state
                deletedContentSnapshot = result.snapshot
                batchDeletedContentSnapshot = null
                val selectedItem = if (primaryDeleted) {
                    reloaded.allItems.firstOrNull { it.contentId.value == nextSelection } ?: reloaded.selectedItemInView
                } else {
                    reloaded.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                }
                val selectedDraft = if (!primaryDeleted && current.isDirty) current.draftEdits else selectedItem?.toDraftEdits()
                val selectedBaseline = if (!primaryDeleted && current.isDirty) current.loadedBaselineDraft else selectedItem?.toDraftEdits()
                packageBrowserUiState = reloaded.copy(
                    selectedContentId = selectedItem?.contentId?.value,
                    editingContentId = selectedItem?.contentId?.value,
                    loadedBaselineDraft = selectedBaseline,
                    draftEdits = selectedDraft,
                    showDeleteConfirm = false,
                    deleteTargetContentId = null,
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter,
                    canUndoDelete = true,
                    undoDeleteLabel = result.snapshot.displayLabel,
                    highlightedContentIds = current.highlightedContentIds - deleteId,
                    selectedContentIds = current.selectedContentIds - deleteId,
                    selectionAnchorContentId = current.selectionAnchorContentId?.takeUnless { it == deleteId },
                    centerSelectedRowRequest = current.centerSelectedRowRequest
                )
                onContentDataChanged?.invoke()
            },
            onFailure = { ex ->
                packageBrowserUiState = current.copy(
                    showDeleteConfirm = false,
                    deleteTargetContentId = null
                )
                uiState = uiState.copy(
                    importError = "Delete failed: ${ex.message}"
                )
            }
        )
    }

    fun undoDeleteContent() {
        val current = packageBrowserUiState ?: return
        val batchSnapshot = batchDeletedContentSnapshot
        if (batchSnapshot != null) {
            if (current.isDirty || current.isCreatingNewItem) return
            taskRunner.run(
                work = {
                    withProblemProjection(packageBrowserFacade.undoBatchDelete(
                        snapshot = batchSnapshot,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    ))
                },
                onSuccess = { reloaded ->
                    val projected = reloaded.copy(
                        query = current.query,
                        appliedQuery = current.appliedQuery,
                        selectedLessonFilter = current.selectedLessonFilter,
                        mediaFilter = current.mediaFilter,
                        imageStatusFilter = current.imageStatusFilter,
                        sortOption = current.sortOption,
                        problemFilter = current.problemFilter
                    )
                    val retained = projected.allItems.firstOrNull { it.contentId.value == current.selectedContentId }
                        ?: projected.filteredItems.firstOrNull()
                    val draft = retained?.toDraftEdits()
                    batchDeletedContentSnapshot = null
                    packageBrowserUiState = projected.copy(
                        selectedContentId = retained?.contentId?.value,
                        editingContentId = retained?.contentId?.value,
                        loadedBaselineDraft = draft,
                        draftEdits = draft,
                        canUndoDelete = false,
                        undoDeleteLabel = null,
                        selectedContentIds = current.selectedContentIds - batchSnapshot.contentIds.map { it.value }.toSet(),
                        highlightedContentIds = current.highlightedContentIds - batchSnapshot.contentIds.map { it.value }.toSet(),
                        centerSelectedRowRequest = current.centerSelectedRowRequest
                    )
                    uiState = uiState.copy(importMessage = "Restored ${batchSnapshot.contents.size} deleted items.", importError = null)
                    onContentDataChanged?.invoke()
                },
                onFailure = { failure ->
                    packageBrowserUiState = current.copy(canUndoDelete = true, undoDeleteLabel = batchSnapshot.displayLabel)
                    uiState = uiState.copy(importError = "Undo Delete failed: ${failure.message}")
                }
            )
            return
        }
        val snapshot = deletedContentSnapshot ?: return
        if (current.isDirty || current.isCreatingNewItem) return
        taskRunner.run(
            work = {
                withProblemProjection(packageBrowserFacade.undoDelete(
                    snapshot = snapshot,
                    installedPackageId = current.installedPackageId,
                    packageName = current.packageName
                ))
            },
            onSuccess = { reloaded ->
                val restoredId = snapshot.content.id.value
                val projected = reloaded.copy(
                    query = current.query,
                    appliedQuery = current.appliedQuery,
                    selectedLessonFilter = current.selectedLessonFilter,
                    mediaFilter = current.mediaFilter,
                    imageStatusFilter = current.imageStatusFilter,
                    sortOption = current.sortOption,
                    problemFilter = current.problemFilter
                )
                val restored = projected.filteredItems.firstOrNull { it.contentId.value == restoredId }
                val retained = projected.filteredItems.firstOrNull { it.contentId.value == current.selectedContentId }
                    ?: projected.filteredItems.firstOrNull()
                val selection = restored ?: retained
                val draft = selection?.toDraftEdits()
                deletedContentSnapshot = null
                batchDeletedContentSnapshot = null
                packageBrowserUiState = projected.copy(
                    selectedContentId = selection?.contentId?.value,
                    editingContentId = selection?.contentId?.value,
                    loadedBaselineDraft = draft,
                    draftEdits = draft,
                    canUndoDelete = false,
                    undoDeleteLabel = null,
                    highlightedContentIds = current.highlightedContentIds - restoredId,
                    selectedContentIds = current.selectedContentIds - restoredId,
                    selectionAnchorContentId = current.selectionAnchorContentId?.takeUnless { it == restoredId },
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
            problemProjection = current.problemProjection.copy(
                byContentId = current.problemProjection.byContentId - deleteId
            ),
            selectedContentId = nextSelection,
            editingContentId = nextSelection,
            loadedBaselineDraft = selectedDraft,
            draftEdits = selectedDraft,
            showDeleteConfirm = false,
            highlightedContentIds = current.highlightedContentIds - deleteId,
            selectedContentIds = current.selectedContentIds - deleteId,
            selectionAnchorContentId = current.selectionAnchorContentId?.takeUnless { it == deleteId }
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
                    withProblemProjection(packageBrowserFacade.createContent(
                        draft = draft,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    ))
                },
                onSuccess = { reloaded ->
                    val projected = reloaded.copy(
                        query = current.query,
                        appliedQuery = current.appliedQuery,
                        selectedLessonFilter = current.selectedLessonFilter,
                        mediaFilter = current.mediaFilter,
                        imageStatusFilter = current.imageStatusFilter,
                        sortOption = current.sortOption,
                        problemFilter = current.problemFilter
                    )
                    val selectedItem = projected.filteredItems.firstOrNull { it.contentId.value == reloaded.selectedContentId }
                        ?: projected.filteredItems.firstOrNull()
                    val selectedDraft = selectedItem?.toDraftEdits()
                    packageBrowserUiState = projected.copy(
                        isCreatingNewItem = false,
                        selectedContentId = selectedItem?.contentId?.value,
                        editingContentId = selectedItem?.contentId?.value,
                        loadedBaselineDraft = selectedDraft,
                        draftEdits = selectedDraft,
                        showUnsavedChangesDialog = false,
                        pendingAction = null,
                        highlightedContentIds = current.highlightedContentIds,
                        selectedContentIds = current.selectedContentIds,
                        selectionAnchorContentId = current.selectionAnchorContentId,
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
                    withProblemProjection(packageBrowserFacade.persistEdit(
                        draft = draft,
                        installedPackageId = current.installedPackageId,
                        packageName = current.packageName
                    ))
                },
                onSuccess = { reloaded ->
                    val projected = reloaded.copy(
                        query = current.query,
                        appliedQuery = current.appliedQuery,
                        selectedLessonFilter = current.selectedLessonFilter,
                        mediaFilter = current.mediaFilter,
                        imageStatusFilter = current.imageStatusFilter,
                        sortOption = current.sortOption,
                        problemFilter = current.problemFilter
                    )
                    val selectedItem = projected.filteredItems.firstOrNull { it.contentId.value == draft.contentId }
                        ?: projected.filteredItems.firstOrNull()
                    val selectedDraft = selectedItem?.toDraftEdits()
                    packageBrowserUiState = projected.copy(
                        selectedContentId = selectedItem?.contentId?.value,
                        editingContentId = selectedItem?.contentId?.value,
                        loadedBaselineDraft = selectedDraft,
                        draftEdits = selectedDraft,
                        showUnsavedChangesDialog = false,
                        pendingAction = null,
                        highlightedContentIds = current.highlightedContentIds,
                        selectedContentIds = current.selectedContentIds,
                        selectionAnchorContentId = current.selectionAnchorContentId,
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
                batchDeletedContentSnapshot = null
                packageBrowserUiState = null
                lessonBrowserUiState = null
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BrowsePackage -> {
                deletedContentSnapshot = null
                batchDeletedContentSnapshot = null
                packageBrowserUiState = null
                browsePackageLessons(action.installedPackageId, action.packageName)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.DeleteContent -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(
                    showDeleteConfirm = true,
                    deleteTargetContentId = action.contentId
                )
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.BatchDelete -> {
                packageBrowserUiState = packageBrowserUiState?.copy(selectedContentIds = action.contentIds)
                showBatchDeleteConfirmation()
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
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyImageStatusFilter -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(imageStatusFilter = action.imageStatusFilter)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplySort -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(sortOption = action.sortOption)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ApplyProblemFilter -> {
                updatePackageBrowserProblemFilter(action.problemFilter)
            }
            is vn.loi.learning.desktop.ui.browser.PackageBrowserPendingAction.ResetFilters -> {
                val current = packageBrowserUiState ?: return
                packageBrowserUiState = current.copy(
                    query = "",
                    appliedQuery = "",
                    selectedLessonFilter = "ALL",
                    mediaFilter = vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter.ALL,
                    imageStatusFilter = vn.loi.learning.desktop.ui.browser.ImageStatusFilter.ALL,
                    sortOption = vn.loi.learning.application.contentpackaging.browser.BrowserSortOption.ORIGINAL_ORDER,
                    problemFilter = vn.loi.learning.desktop.ui.browser.ContentProblemFilter.NONE
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

    private fun withProblemProjection(
        state: vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
    ): vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState = state.copy(
        problemProjection = vn.loi.learning.desktop.ui.browser.projectContentProblems(state.allItems, contentMediaStorage)
    )

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
        batchDeletedContentSnapshot = null
        packageBrowserUiState = null
        lessonBrowserUiState = null
    }

    fun resetLibraryNavigationState() {
        deletedContentSnapshot = null
        batchDeletedContentSnapshot = null
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
        if (packageExportDialogState.exporting || uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()

        packageExportDialogState = PackageExportDialogState(
            visible = true,
            exporting = true,
            packageId = installedPackageId,
            packageName = packageName,
            stage = vn.loi.learning.application.contentpackaging.export.ExportProgressStage.RESOLVING_PACKAGE,
            phase = "Preparing export...",
            processed = 0,
            total = 100,
            outputPath = destinationPath
        )

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
                        taskRunner.dispatch {
                            if (packageExportDialogState.exporting) {
                                packageExportDialogState = packageExportDialogState.copy(
                                    stage = stage,
                                    phase = message,
                                    processed = processed,
                                    total = total
                                )
                            }
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
                    }
                )
            },
            onSuccess = { result ->
                when (result) {
                    is vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult.Success -> {
                        packageExportDialogState = packageExportDialogState.copy(
                            exporting = false,
                            stage = vn.loi.learning.application.contentpackaging.export.ExportProgressStage.COMPLETED,
                            phase = "OPD3 package export complete.",
                            processed = 100,
                            total = 100,
                            result = result,
                            error = null
                        )
                        uiState = uiState.copy(
                            importMessage = "Package '$packageName' exported successfully to '${result.outputPath.fileName}' (${result.contentCount} contents, ${result.learningItemCount} items, ${result.mediaAssetCount} media files).",
                            importError = null,
                            operation = ContentLibraryOperation.Idle
                        )
                    }
                    is vn.loi.learning.application.contentpackaging.export.ExportContentPackageResult.Failure -> {
                        packageExportDialogState = packageExportDialogState.copy(
                            exporting = false,
                            phase = "Export failed",
                            error = result.message,
                            result = null
                        )
                        uiState = uiState.copy(
                            importMessage = null,
                            importError = result.message,
                            operation = ContentLibraryOperation.Idle
                        )
                    }
                }
            },
            onFailure = { exception ->
                packageExportDialogState = packageExportDialogState.copy(
                    exporting = false,
                    phase = "Export failed",
                    error = exception.message ?: "Failed to export package.",
                    result = null
                )
                uiState = uiState.copy(
                    importMessage = null,
                    importError = exception.message ?: "Failed to export package.",
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    fun dismissPackageExportDialog() {
        if (!packageExportDialogState.exporting) {
            packageExportDialogState = PackageExportDialogState()
        }
    }

    fun openImageReuseReview(
        installedPackageId: vn.loi.learning.domain.library.model.InstalledPackageId,
        packageName: String
    ) {
        val availableSources = uiState.packages
            .filter {
                it.id != installedPackageId.value &&
                    !it.name.equals(packageName, ignoreCase = true)
            }
            .map {
                vn.loi.learning.desktop.ui.browser.imagereuse.ImageReusePackageOption(
                    id = it.id,
                    name = it.name,
                    version = it.version
                )
            }
        val initialSelectedIds = availableSources.map { it.id }.toSet()
        imageReuseDialogState = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewDialogState(
            visible = true,
            targetPackageId = installedPackageId.value,
            targetPackageName = packageName,
            stage = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup(
                availableSourcePackages = availableSources,
                selectedSourcePackageIds = initialSelectedIds,
                scope = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseScope.MISSING_IMAGES_ONLY
            )
        )
    }

    fun toggleImageReuseSourcePackage(packageId: String) {
        val currentStage = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup ?: return
        val updated = if (packageId in currentStage.selectedSourcePackageIds) {
            currentStage.selectedSourcePackageIds - packageId
        } else {
            currentStage.selectedSourcePackageIds + packageId
        }
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = currentStage.copy(selectedSourcePackageIds = updated, scanError = null)
        )
    }

    fun selectAllImageReuseSourcePackages() {
        val currentStage = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup ?: return
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = currentStage.copy(
                selectedSourcePackageIds = currentStage.availableSourcePackages.map { it.id }.toSet(),
                scanError = null
            )
        )
    }

    fun clearAllImageReuseSourcePackages() {
        val currentStage = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup ?: return
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = currentStage.copy(
                selectedSourcePackageIds = emptySet(),
                scanError = null
            )
        )
    }

    fun updateImageReuseScope(scope: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseScope) {
        val currentStage = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup ?: return
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = currentStage.copy(scope = scope, scanError = null)
        )
    }

    fun startImageReuseScan() {
        val currentStage = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Setup ?: return
        if (currentStage.selectedSourcePackageIds.isEmpty()) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = currentStage.copy(scanError = "Please select at least one source package to scan.")
            )
            return
        }
        val targetPackageId = imageReuseDialogState.targetPackageId
        val targetPackageName = imageReuseDialogState.targetPackageName
        val selectedSources = currentStage.availableSourcePackages
            .filter { it.id in currentStage.selectedSourcePackageIds }
            .filter { it.id != targetPackageId && !it.name.equals(targetPackageName, ignoreCase = true) }
        val allowedSourcePackageIds = selectedSources.map { it.id }.toSet()
        val scope = currentStage.scope

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = currentStage.copy(isScanning = true, scanError = null)
        )

        taskRunner.run(
            work = {
                val targetUi = packageBrowserFacade.loadForPackage(
                    installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(targetPackageId),
                    packageName = targetPackageName
                )
                val sourcePackagesWithItems = selectedSources.associateWith { sourcePkg ->
                    packageBrowserFacade.loadForPackage(
                        installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(sourcePkg.id),
                        packageName = sourcePkg.name
                    ).allItems
                }
                vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseDiscoveryEngine.discoverCandidates(
                    targetItems = targetUi.allItems,
                    sourcePackagesWithItems = sourcePackagesWithItems,
                    scope = scope,
                    mediaStorage = contentMediaStorage
                )
            },
            onSuccess = { candidates ->
                if (candidates.isEmpty()) {
                    imageReuseDialogState = imageReuseDialogState.copy(
                        stage = currentStage.copy(
                            isScanning = false,
                            scanError = "No candidate image matches found for the selected scope."
                        )
                    )
                } else {
                    imageReuseDialogState = imageReuseDialogState.copy(
                        stage = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review(
                            targetItems = candidates,
                            currentTargetIndex = 0,
                            currentCandidateIndex = 0,
                            isApplying = false,
                            appliedCount = 0,
                            allowedSourcePackageIds = allowedSourcePackageIds
                        )
                    )
                }
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = currentStage.copy(
                        isScanning = false,
                        scanError = ex.message ?: "Failed to scan packages for candidate images."
                    )
                )
            }
        )
    }

    fun updateImageReuseTargetQuestion(question: String) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(question = question)
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun updateImageReuseTargetAnswer(answer: String) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(answer = answer)
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun updateImageReuseTargetTranslation(translation: String) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(translation = translation)
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun updateImageReuseTargetExample(example: String) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(exampleText = example)
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun updateImageReuseTargetPartOfSpeech(pos: String) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(partOfSpeech = pos)
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun replaceImageReuseTargetImage(file: java.io.File) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val mediaStorage = contentMediaStorage ?: run {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(applyError = "Media storage is unavailable.")
            )
            return
        }
        val targetPackageName = imageReuseDialogState.targetPackageName
        try {
            val relativePath = packageBrowserFacade.importMediaAsset(targetPackageName, file, mediaStorage)
            val currentDraft = review.effectiveTargetDraft
            val updatedDraft = currentDraft.copy(
                imageIntent = vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace(relativePath)
            )
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(targetDraft = updatedDraft, applyError = null)
            )
        } catch (ex: Exception) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(applyError = ex.message ?: "Failed to import replacement image.")
            )
        }
    }

    fun removeImageReuseTargetImage() {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        val currentDraft = review.effectiveTargetDraft
        val updatedDraft = currentDraft.copy(
            imageIntent = vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove
        )
        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(targetDraft = updatedDraft, applyError = null)
        )
    }

    fun previousImageReuseItem(explicitDraft: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseTargetDraft? = null) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        if (review.isApplying || !review.canGoPrevious) return
        val currentTarget = review.currentTarget ?: return
        val draft = explicitDraft ?: review.effectiveTargetDraft

        if (!draft.isDirty(currentTarget)) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(
                    currentTargetIndex = review.currentTargetIndex - 1,
                    currentCandidateIndex = 0,
                    targetDraft = null,
                    applyError = null
                )
            )
            return
        }

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(isApplying = true, applyError = null)
        )

        taskRunner.run(
            work = {
                val resolvedImageRef = when (val intent = draft.imageIntent) {
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove -> null
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.ReuseSource -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Unchanged -> currentTarget.currentImageRef
                }

                packageBrowserFacade.persistTargetItem(
                    targetContentId = currentTarget.targetContentId,
                    draft = draft,
                    resolvedImageRef = resolvedImageRef
                )

                resolvedImageRef
            },
            onSuccess = { resolvedImageRef ->
                val updatedTargetItems = review.targetItems.mapIndexed { idx, item ->
                    if (idx == review.currentTargetIndex && item.targetContentId == currentTarget.targetContentId) {
                        item.copy(
                            question = draft.question,
                            answer = draft.answer,
                            translation = draft.translation,
                            exampleText = draft.exampleText,
                            partOfSpeech = draft.partOfSpeech,
                            currentImageRef = resolvedImageRef
                        )
                    } else {
                        item
                    }
                }
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        targetItems = updatedTargetItems,
                        currentTargetIndex = review.currentTargetIndex - 1,
                        currentCandidateIndex = 0,
                        isApplying = false,
                        targetDraft = null,
                        applyError = null
                    )
                )
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        isApplying = false,
                        applyError = ex.message ?: "Failed to auto-save target changes."
                    )
                )
            }
        )
    }

    fun undoLastImageReuse() {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        if (review.isApplying || !review.canUndo) return
        val lastUndo = review.undoStack.lastOrNull() ?: return

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(isApplying = true, applyError = null)
        )

        taskRunner.run(
            work = {
                packageBrowserFacade.undoImageReuse(
                    targetContentId = lastUndo.targetContentId,
                    expectedCurrentImageRef = lastUndo.appliedImageRef,
                    restoreImageRef = lastUndo.beforeImageRef
                )
            },
            onSuccess = { _ ->
                val nextAppliedCount = maxOf(0, review.appliedCount - 1)
                val updatedTargetItems = review.targetItems.mapIndexed { idx, item ->
                    if (idx == lastUndo.targetIndex && item.targetContentId == lastUndo.targetContentId) {
                        item.copy(currentImageRef = lastUndo.beforeImageRef)
                    } else {
                        item
                    }
                }
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        targetItems = updatedTargetItems,
                        currentTargetIndex = lastUndo.targetIndex,
                        currentCandidateIndex = lastUndo.candidateIndex,
                        isApplying = false,
                        appliedCount = nextAppliedCount,
                        undoStack = review.undoStack.dropLast(1),
                        targetDraft = null,
                        applyError = null
                    )
                )
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        isApplying = false,
                        applyError = ex.message ?: "Failed to undo last image reuse."
                    )
                )
            }
        )
    }

    fun skipImageReuseCandidate(explicitDraft: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseTargetDraft? = null) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        if (review.isApplying) return
        val currentTarget = review.currentTarget ?: return
        val draft = explicitDraft ?: review.effectiveTargetDraft

        if (!draft.isDirty(currentTarget)) {
            advanceCandidateOrTarget(review)
            return
        }

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(isApplying = true, applyError = null)
        )

        taskRunner.run(
            work = {
                val resolvedImageRef = when (val intent = draft.imageIntent) {
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove -> null
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.ReuseSource -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Unchanged -> currentTarget.currentImageRef
                }

                packageBrowserFacade.persistTargetItem(
                    targetContentId = currentTarget.targetContentId,
                    draft = draft,
                    resolvedImageRef = resolvedImageRef
                )

                resolvedImageRef
            },
            onSuccess = { resolvedImageRef ->
                val updatedTargetItems = review.targetItems.mapIndexed { idx, item ->
                    if (idx == review.currentTargetIndex && item.targetContentId == currentTarget.targetContentId) {
                        item.copy(
                            question = draft.question,
                            answer = draft.answer,
                            translation = draft.translation,
                            exampleText = draft.exampleText,
                            partOfSpeech = draft.partOfSpeech,
                            currentImageRef = resolvedImageRef
                        )
                    } else {
                        item
                    }
                }
                val updatedReview = review.copy(
                    targetItems = updatedTargetItems,
                    isApplying = false,
                    targetDraft = null,
                    applyError = null
                )
                advanceCandidateOrTarget(updatedReview)
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        isApplying = false,
                        applyError = ex.message ?: "Failed to auto-save target changes."
                    )
                )
            }
        )
    }

    private fun advanceCandidateOrTarget(review: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review) {
        val currentTarget = review.currentTarget ?: return
        if (review.currentCandidateIndex + 1 < currentTarget.candidates.size) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(
                    currentCandidateIndex = review.currentCandidateIndex + 1,
                    targetDraft = null,
                    applyError = null
                )
            )
        } else if (review.currentTargetIndex + 1 < review.targetItems.size) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(
                    currentTargetIndex = review.currentTargetIndex + 1,
                    currentCandidateIndex = 0,
                    targetDraft = null,
                    applyError = null
                )
            )
        } else {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Complete(
                    totalReviewedTargets = review.targetItems.size,
                    totalAppliedCount = review.appliedCount
                )
            )
        }
    }

    fun skipImageReuseItem(explicitDraft: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseTargetDraft? = null) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        if (review.isApplying) return
        val currentTarget = review.currentTarget ?: return
        val draft = explicitDraft ?: review.effectiveTargetDraft

        if (!draft.isDirty(currentTarget)) {
            advanceTargetItem(review)
            return
        }

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(isApplying = true, applyError = null)
        )

        taskRunner.run(
            work = {
                val resolvedImageRef = when (val intent = draft.imageIntent) {
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove -> null
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.ReuseSource -> intent.imageRef
                    is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Unchanged -> currentTarget.currentImageRef
                }

                packageBrowserFacade.persistTargetItem(
                    targetContentId = currentTarget.targetContentId,
                    draft = draft,
                    resolvedImageRef = resolvedImageRef
                )

                resolvedImageRef
            },
            onSuccess = { resolvedImageRef ->
                val updatedTargetItems = review.targetItems.mapIndexed { idx, item ->
                    if (idx == review.currentTargetIndex && item.targetContentId == currentTarget.targetContentId) {
                        item.copy(
                            question = draft.question,
                            answer = draft.answer,
                            translation = draft.translation,
                            exampleText = draft.exampleText,
                            partOfSpeech = draft.partOfSpeech,
                            currentImageRef = resolvedImageRef
                        )
                    } else {
                        item
                    }
                }
                val updatedReview = review.copy(
                    targetItems = updatedTargetItems,
                    isApplying = false,
                    targetDraft = null,
                    applyError = null
                )
                advanceTargetItem(updatedReview)
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        isApplying = false,
                        applyError = ex.message ?: "Failed to auto-save target changes."
                    )
                )
            }
        )
    }

    private fun advanceTargetItem(review: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review) {
        if (review.currentTargetIndex + 1 < review.targetItems.size) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(
                    currentTargetIndex = review.currentTargetIndex + 1,
                    currentCandidateIndex = 0,
                    targetDraft = null,
                    applyError = null
                )
            )
        } else {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Complete(
                    totalReviewedTargets = review.targetItems.size,
                    totalAppliedCount = review.appliedCount
                )
            )
        }
    }

    fun applyImageReuseAndNext(explicitDraft: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseTargetDraft? = null) {
        val review = imageReuseDialogState.stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review ?: return
        if (review.isApplying) return
        val currentTarget = review.currentTarget ?: return
        val currentCandidate = review.currentCandidate ?: return
        val mediaStorage = contentMediaStorage ?: run {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = review.copy(applyError = "Media storage is unavailable.")
            )
            return
        }
        val targetPackageName = imageReuseDialogState.targetPackageName
        val draft = explicitDraft ?: review.effectiveTargetDraft
        val isDirty = draft.isDirty(currentTarget)

        imageReuseDialogState = imageReuseDialogState.copy(
            stage = review.copy(isApplying = true, applyError = null)
        )

        taskRunner.run(
            work = {
                val resolvedImageRef = if (isDirty) {
                    // Dirty target -> resolve image strictly according to explicit intent (Unchanged = keep original Target image!)
                    when (val intent = draft.imageIntent) {
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace -> intent.imageRef
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove -> null
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.ReuseSource -> intent.imageRef
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Unchanged -> currentTarget.currentImageRef
                    }
                } else {
                    // Pristine target -> User explicitly clicked "Use Source Image & Next"
                    vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseDiscoveryEngine.copySourceImageToTargetPackage(
                        targetPackageName = targetPackageName,
                        sourceCandidate = currentCandidate,
                        mediaStorage = mediaStorage
                    )
                }

                packageBrowserFacade.persistTargetItem(
                    targetContentId = currentTarget.targetContentId,
                    draft = draft,
                    resolvedImageRef = resolvedImageRef
                )

                resolvedImageRef
            },
            onSuccess = { resolvedImageRef ->
                val nextAppliedCount = if (!isDirty) review.appliedCount + 1 else review.appliedCount
                val undoEntry = if (!isDirty) {
                    vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseUndoEntry(
                        targetContentId = currentTarget.targetContentId,
                        targetIndex = review.currentTargetIndex,
                        candidateIndex = review.currentCandidateIndex,
                        beforeImageRef = currentTarget.currentImageRef,
                        appliedImageRef = resolvedImageRef ?: ""
                    )
                } else null

                val updatedTargetItems = review.targetItems.mapIndexed { idx, item ->
                    if (idx == review.currentTargetIndex && item.targetContentId == currentTarget.targetContentId) {
                        item.copy(
                            question = draft.question,
                            answer = draft.answer,
                            translation = draft.translation,
                            exampleText = draft.exampleText,
                            partOfSpeech = draft.partOfSpeech,
                            currentImageRef = resolvedImageRef
                        )
                    } else {
                        item
                    }
                }
                val updatedUndoStack = if (undoEntry != null) review.undoStack + undoEntry else review.undoStack

                if (review.currentTargetIndex + 1 < review.targetItems.size) {
                    imageReuseDialogState = imageReuseDialogState.copy(
                        stage = review.copy(
                            targetItems = updatedTargetItems,
                            currentTargetIndex = review.currentTargetIndex + 1,
                            currentCandidateIndex = 0,
                            isApplying = false,
                            appliedCount = nextAppliedCount,
                            undoStack = updatedUndoStack,
                            targetDraft = null,
                            applyError = null
                        )
                    )
                } else {
                    imageReuseDialogState = imageReuseDialogState.copy(
                        stage = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Complete(
                            totalReviewedTargets = review.targetItems.size,
                            totalAppliedCount = nextAppliedCount
                        )
                    )
                }
            },
            onFailure = { ex ->
                imageReuseDialogState = imageReuseDialogState.copy(
                    stage = review.copy(
                        isApplying = false,
                        applyError = ex.message ?: "Failed to save target item."
                    )
                )
            }
        )
    }

    fun closeImageReuseReview(explicitDraft: vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseTargetDraft? = null) {
        val stage = imageReuseDialogState.stage
        val currentTarget = (stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review)?.currentTarget
        val draft = explicitDraft ?: (stage as? vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review)?.effectiveTargetDraft

        if (stage is vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review && currentTarget != null && draft != null && draft.isDirty(currentTarget)) {
            imageReuseDialogState = imageReuseDialogState.copy(
                stage = stage.copy(isApplying = true, applyError = null)
            )
            taskRunner.run(
                work = {
                    val resolvedImageRef = when (val intent = draft.imageIntent) {
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Replace -> intent.imageRef
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Remove -> null
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.ReuseSource -> intent.imageRef
                        is vn.loi.learning.desktop.ui.browser.imagereuse.TargetImageIntent.Unchanged -> currentTarget.currentImageRef
                    }

                    packageBrowserFacade.persistTargetItem(
                        targetContentId = currentTarget.targetContentId,
                        draft = draft,
                        resolvedImageRef = resolvedImageRef
                    )
                },
                onSuccess = {
                    performCloseAndRefresh()
                },
                onFailure = { ex ->
                    imageReuseDialogState = imageReuseDialogState.copy(
                        stage = stage.copy(
                            isApplying = false,
                            applyError = ex.message ?: "Failed to save changes before closing."
                        )
                    )
                }
            )
            return
        }
        performCloseAndRefresh()
    }

    private fun performCloseAndRefresh() {
        val stage = imageReuseDialogState.stage
        val appliedCount = when (stage) {
            is vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Review -> stage.appliedCount
            is vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewStage.Complete -> stage.totalAppliedCount
            else -> 0
        }
        val targetPackageId = imageReuseDialogState.targetPackageId
        val targetPackageName = imageReuseDialogState.targetPackageName

        imageReuseDialogState = vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewDialogState()

        if (targetPackageId.isNotBlank() && targetPackageName.isNotBlank()) {
            browsePackageLessons(
                installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(targetPackageId),
                packageName = targetPackageName
            )
        }
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

    fun reportMediaAcquisitionError(message: String) {
        uiState = uiState.copy(importError = message, importMessage = null)
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
