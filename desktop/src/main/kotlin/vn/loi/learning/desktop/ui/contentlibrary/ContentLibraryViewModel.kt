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

            if (lessonBrowserUiState?.libraryId == packageId) {
                lessonBrowserUiState = null
            }

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
            } ?: return

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
                uiState = uiState.copy(
                    loadError = DesktopFailureMessage.forPersistedData(exception),
                    operation = ContentLibraryOperation.Idle
                )
            }
        )
    }

    fun closeLibrary() {
        lessonBrowserUiState = null
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

    fun importFromDirectory(
        directory: Path
    ) {
        if (uiState.operation !is ContentLibraryOperation.Idle) return
        clearOperationMessage()
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Importing("Scanning selected directory")
        )
        taskRunner.run(
            work = {
                val result = facade.importFromDirectory(directory) { event ->
                    taskRunner.dispatch { updateImportProgress(event) }
                }
                result to facade.load()
            },
            onSuccess = { (result, refreshedState) ->
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
                onContentDataChanged?.invoke()
            },
            onFailure = { exception ->
                showOperationError(exception, "Package import failed.")
                uiState = uiState.copy(operation = ContentLibraryOperation.Idle)
            }
        )
    }

    private fun updateImportProgress(event: PackageImportProgressEvent) {
        if (uiState.operation !is ContentLibraryOperation.Importing) return
        val phase = when (event.stage) {
            PackageImportProgressStage.SCANNING -> "Scanning selected directory"
            PackageImportProgressStage.PACKAGE_INSTALLED -> "Reading JSON and validating OPD3 media"
            PackageImportProgressStage.CONTENT_IMPORTED -> "Preparing imported content"
            PackageImportProgressStage.SAVING_CONTENT -> "Committing contents"
            PackageImportProgressStage.SAVING_LEARNING_ITEMS -> "Committing learning items"
            PackageImportProgressStage.REGISTERING_PACKAGE -> "Registering package"
            PackageImportProgressStage.COMPLETED -> "Refreshing Content Library"
        }
        uiState = uiState.copy(
            operation = ContentLibraryOperation.Importing(
                phase = phase,
                processed = event.processed,
                total = event.total,
                committed = event.stage == PackageImportProgressStage.COMPLETED
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
                append(failure.message)
            }
        }
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

        val selectedLibrary =
            uiState.libraries.firstOrNull { library ->
                library.id ==
                        currentLessonBrowserState.libraryId
            }

        lessonBrowserUiState =
            if (selectedLibrary == null) {
                null
            } else {
                try {
                    lessonBrowserFacade.load(
                        libraryId = selectedLibrary.id,
                        libraryName = selectedLibrary.name
                    )
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
