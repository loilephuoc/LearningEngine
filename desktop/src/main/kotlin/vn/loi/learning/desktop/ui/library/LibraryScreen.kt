package vn.loi.learning.desktop.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.theme.LEColors
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryOperation
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserCard
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.PackageExportDialog
import vn.loi.learning.desktop.ui.contentlibrary.PackageIntegrityDialog
import vn.loi.learning.desktop.ui.contentlibrary.LibraryHealthOverviewState
import vn.loi.learning.desktop.ui.contentlibrary.LibraryHealthPackageTarget
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel?,
    contentLibraryViewModel: ContentLibraryViewModel,
    contentMediaStorage: ContentMediaStorage,
    onStartLessonStudy: (vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection) -> Unit,
    modifier: Modifier = Modifier,
    packageChooser: () -> List<Path> = ::choosePackageFiles
) {
    if (viewModel == null) {
        LibraryErrorView(
            message = LibraryFailureMessage.forCategory(LibraryFailureCategory.MISCONFIGURED_SERVICE),
            onRetry = {},
            modifier = modifier
        )
        return
    }

    val contentLibraryUiState = contentLibraryViewModel.uiState
    val isImporting = contentLibraryUiState.operation is ContentLibraryOperation.Importing
    val importPhase = (contentLibraryUiState.operation as? ContentLibraryOperation.Importing)?.phase
    var packagePendingRemoval by remember { mutableStateOf<Pair<String, String>?>(null) }

    val handleImport = {
        if (!isImporting) {
            val selectedFiles = packageChooser()
            if (selectedFiles.isNotEmpty()) {
                contentLibraryViewModel.importFromFiles(selectedFiles)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isImporting && importPhase != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Importing package: $importPhase...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { contentLibraryViewModel.cancelImport() }
                        ) {
                            Text("Cancel Import", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            (contentLibraryUiState.operation as? ContentLibraryOperation.Loading)?.takeIf { it.phase != "Loading package browser" }?.let { loadingOp ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Loading ${loadingOp.title}: ${loadingOp.phase}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            contentLibraryUiState.importMessage?.let { importMsg ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = importMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = contentLibraryViewModel::clearOperationMessage) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            contentLibraryUiState.importError?.let { importErr ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = importErr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = contentLibraryViewModel::clearOperationMessage) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            val workspaceUiState = contentLibraryViewModel.learningWorkspaceUiState
            val packageBrowserUiState = contentLibraryViewModel.packageBrowserUiState
            if (workspaceUiState != null) {
                vn.loi.learning.desktop.ui.contentlibrary.LearningWorkspaceCard(
                    uiState = workspaceUiState,
                    onBack = { contentLibraryViewModel.handleWorkspaceBack() },
                    onNavigateToPrepare = contentLibraryViewModel::navigateToPrepareMode,
                    onNavigateToExplore = contentLibraryViewModel::navigateToExploreMode,
                    onPreviousExploreItem = contentLibraryViewModel::previousExploreItem,
                    onNextExploreItem = contentLibraryViewModel::nextExploreItem,
                    onStartLearning = {
                        contentLibraryViewModel.startStudyFromWorkspace(onStartLessonStudy)
                    }
                )
            } else if (packageBrowserUiState != null) {
                vn.loi.learning.desktop.ui.studio.ContentStudioScreen(
                    uiState = packageBrowserUiState,
                    onClose = contentLibraryViewModel::closePackageBrowser,
                    // PLE-020: single-click auto-edit
                    onSelectRow = contentLibraryViewModel::attemptSelectRowAutoEdit,
                    onSubmitSearch = contentLibraryViewModel::selectPackageBrowserSearchResult,
                    onToggleHighlight = contentLibraryViewModel::togglePackageBrowserHighlight,
                    onToggleMultiSelection = contentLibraryViewModel::togglePackageBrowserMultiSelection,
                    onSelectMultiRange = contentLibraryViewModel::selectPackageBrowserVisibleRange,
                    onSelectAllVisible = contentLibraryViewModel::selectAllVisiblePackageBrowserItems,
                    onClearMultiSelection = contentLibraryViewModel::clearPackageBrowserMultiSelection,
                    onHighlightSelected = contentLibraryViewModel::highlightSelectedPackageBrowserItems,
                    onRemoveHighlightSelected = contentLibraryViewModel::removeHighlightFromSelectedPackageBrowserItems,
                    onCheckSelectedMedia = contentLibraryViewModel::checkSelectedPackageBrowserMedia,
                    onConfirmBatchDelete = contentLibraryViewModel::confirmBatchDelete,
                    onDismissBatchDelete = contentLibraryViewModel::dismissBatchDeleteConfirmation,
                    onDismissBatchDeleteBlocker = contentLibraryViewModel::dismissBatchDeleteBlocker,
                    onDismissSelectedMediaCheck = contentLibraryViewModel::dismissSelectedPackageBrowserMediaCheck,
                    onConfirmBatchPartOfSpeech = contentLibraryViewModel::confirmBatchPartOfSpeech,
                    onCancelBatchPartOfSpeech = contentLibraryViewModel::cancelBatchPartOfSpeech,
                    onQueryChanged = contentLibraryViewModel::updatePackageBrowserQuery,
                    onClearQuery = contentLibraryViewModel::clearPackageBrowserQuery,
                    onLessonFilterChanged = contentLibraryViewModel::updatePackageBrowserLessonFilter,
                    onMediaFilterChanged = contentLibraryViewModel::updatePackageBrowserMediaFilter,
                    onImageStatusFilterChanged = contentLibraryViewModel::updatePackageBrowserImageStatusFilter,
                    onSortChanged = contentLibraryViewModel::updatePackageBrowserSort,
                    onProblemFilterChanged = contentLibraryViewModel::updatePackageBrowserProblemFilter,
                    onPreviousProblem = { contentLibraryViewModel.navigatePackageBrowserProblem(-1) },
                    onNextProblem = { contentLibraryViewModel.navigatePackageBrowserProblem(1) },
                    onResetFilters = contentLibraryViewModel::resetPackageBrowserFilters,
                    onPlayAudio = contentLibraryViewModel::playBrowserAudio,
                    onStopAudio = contentLibraryViewModel::stopBrowserAudio,
                    thumbnailLoader = remember(contentMediaStorage) {
                        LessonThumbnailLoader(contentMediaStorage)
                    },
                    contentMediaStorage = contentMediaStorage,
                    // Edit & Create callbacks
                    onStartNewItem = contentLibraryViewModel::startNewItem,
                    onEditContent = contentLibraryViewModel::startEditContent,
                    onSaveEdit = contentLibraryViewModel::saveEdit,
                    onDiscardEdit = contentLibraryViewModel::discardEdits,
                    onSaveNewItem = contentLibraryViewModel::saveNewItem,
                    onCancelNewItem = contentLibraryViewModel::cancelNewItem,
                    onUpdateDraftQuestion = contentLibraryViewModel::updateDraftQuestion,
                    onUpdateDraftAnswer = contentLibraryViewModel::updateDraftAnswer,
                    onUpdateDraftPronunciation = contentLibraryViewModel::updateDraftPronunciation,
                    onUpdateDraftPartOfSpeech = contentLibraryViewModel::updateDraftPartOfSpeech,
                    onUpdateDraftExampleText = contentLibraryViewModel::updateDraftExampleText,
                    onUpdateDraftExampleTranslation = contentLibraryViewModel::updateDraftExampleTranslation,
                    onUpdateDraftImageRef = contentLibraryViewModel::updateDraftImageRef,
                    onUpdateDraftQuestionAudioRef = contentLibraryViewModel::updateDraftQuestionAudioRef,
                    onUpdateDraftAnswerAudioRef = contentLibraryViewModel::updateDraftAnswerAudioRef,
                    onUpdateDraftExampleAudioRef = contentLibraryViewModel::updateDraftExampleAudioRef,
                    onUpdateDraftTranslationAudioRef = contentLibraryViewModel::updateDraftTranslationAudioRef,
                    onImportMediaFile = contentLibraryViewModel::importDraftMediaFile,
                    onImageAcquisitionError = contentLibraryViewModel::reportMediaAcquisitionError,
                    onDoubleClickRow = contentLibraryViewModel::doubleClickPackageBrowserRow,
                    // Delete callbacks
                    onRequestDelete = contentLibraryViewModel::showDeleteConfirmation,
                    onConfirmDelete = contentLibraryViewModel::confirmDeleteContent,
                    onDismissDelete = contentLibraryViewModel::dismissDeleteConfirmation,
                    onUndoDelete = contentLibraryViewModel::undoDeleteContent,
                    // Unsaved changes dialog callbacks
                    onConfirmSaveAndProceed = contentLibraryViewModel::confirmSaveAndProceed,
                    onConfirmDiscardAndProceed = contentLibraryViewModel::confirmDiscardAndProceed,
                    onCancelUnsavedDialog = contentLibraryViewModel::cancelUnsavedChangesDialog,
                    // PLE-020: keyboard navigation
                    onNavigateUp = { contentLibraryViewModel.navigateExplorerByDelta(-1) },
                    onNavigateDown = { contentLibraryViewModel.navigateExplorerByDelta(1) },
                    onNavigateHome = { contentLibraryViewModel.navigateExplorerByDelta(Int.MIN_VALUE) },
                    onNavigateEnd = { contentLibraryViewModel.navigateExplorerByDelta(Int.MAX_VALUE) },
                    onNavigatePageUp = { contentLibraryViewModel.navigateExplorerByDelta(-10) },
                    onNavigatePageDown = { contentLibraryViewModel.navigateExplorerByDelta(10) },
                    // PLE-020: context menu
                    onDuplicateItem = contentLibraryViewModel::duplicateItem,
                    onCopyQuestion = { text -> contentLibraryViewModel.copyToClipboard(text) },
                    onCopyAnswer = { text -> contentLibraryViewModel.copyToClipboard(text) },
                    onOpenImageReuseReview = {
                        contentLibraryViewModel.openImageReuseReview(
                            packageBrowserUiState.installedPackageId,
                            packageBrowserUiState.packageName
                        )
                    },
                    // POS Review callbacks
                    onOpenPosReview = { contentLibraryViewModel.openPosReview() },
                    onSelectPosReviewScope = contentLibraryViewModel::setPosReviewScope,
                    onTogglePosReviewRowSelection = contentLibraryViewModel::togglePosReviewRowSelection,
                    onTogglePosReviewAllFiltered = contentLibraryViewModel::togglePosReviewAllFiltered,
                    onClearPosReviewSelection = contentLibraryViewModel::clearPosReviewSelection,
                    onUpdatePosReviewRowNewPos = contentLibraryViewModel::updatePosReviewRowNewPos,
                    onAnalyzePosReview = contentLibraryViewModel::analyzePosReview,
                    onResetPosReviewDrafts = contentLibraryViewModel::resetPosReviewDrafts,
                    onBatchSetPosReviewSelectedPos = contentLibraryViewModel::batchSetPosReviewSelectedPos,
                    onBatchSetPosReviewFilteredPos = contentLibraryViewModel::batchSetPosReviewFilteredPos,
                    onPosReviewSearchQueryChanged = contentLibraryViewModel::setPosReviewSearchQuery,
                    onPosReviewStatusFilterChanged = contentLibraryViewModel::setPosReviewStatusFilter,
                    onRequestUnlockPosReviewSelected = contentLibraryViewModel::requestUnlockPosReviewSelected,
                    onCancelUnlockPosReviewConfirmation = contentLibraryViewModel::cancelUnlockPosReviewConfirmation,
                    onConfirmUnlockPosReviewSelected = contentLibraryViewModel::confirmUnlockPosReviewSelected,
                    onRequestApplyPosReview = contentLibraryViewModel::requestApplyPosReview,
                    onCancelApplyPosReview = contentLibraryViewModel::cancelApplyPosReview,
                    onConfirmApplyPosReview = contentLibraryViewModel::confirmApplyPosReview,
                    onClosePosReview = contentLibraryViewModel::closePosReview,
                    // Content Maintenance Export callbacks
                    onOpenContentMaintenanceExport = { contentLibraryViewModel.openContentMaintenanceExport() },
                    onSelectContentMaintenanceExportScope = contentLibraryViewModel::setContentMaintenanceExportScope,
                    onTargetExportDirectoryChanged = contentLibraryViewModel::setExportTargetDirectory,
                    onTargetExportFileNameChanged = contentLibraryViewModel::setExportFileName,
                    onExecuteContentMaintenanceExport = contentLibraryViewModel::executeContentMaintenanceExport,
                    onCloseContentMaintenanceExport = contentLibraryViewModel::closeContentMaintenanceExport,
                    onApplyTtsAudio = contentLibraryViewModel::applyGeneratedTtsAudio,
                    onApplyBatchTtsAudio = contentLibraryViewModel::applyBatchTtsAudio,
                    onUndoBatchTts = contentLibraryViewModel::undoLastBatchTts,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            } else {
                contentLibraryViewModel.lessonBrowserUiState?.let { browserUiState ->
                    LessonBrowserCard(
                        uiState = browserUiState,
                        onClose = contentLibraryViewModel::closeLibrary,
                        onSelectLesson = contentLibraryViewModel::selectLesson,
                        onClearLessonSelection = contentLibraryViewModel::clearLessonSelection,
                        onQueryChanged = contentLibraryViewModel::updateLessonQuery,
                        onClearQuery = contentLibraryViewModel::clearLessonQuery,
                        onFilterChanged = contentLibraryViewModel::updateLessonFilter,
                        onSortChanged = contentLibraryViewModel::updateLessonSort,
                        thumbnailLoader = remember(contentMediaStorage) {
                            LessonThumbnailLoader(contentMediaStorage)
                        },
                        onStartStudy = contentLibraryViewModel::openWorkspaceForSelection
                    )
                }
            }

            if (workspaceUiState == null && packageBrowserUiState == null) {
                LibraryScreenContent(
                    uiState = viewModel.uiState,
                    feedbackMessage = viewModel.feedbackMessage,
                    onClearFeedback = viewModel::clearFeedback,
                    onSelectSection = viewModel::selectSection,
                    onRefresh = {
                        viewModel.refresh()
                        contentLibraryViewModel.refresh()
                    },
                    onImport = handleImport,
                    isImporting = isImporting,
                    isExporting = contentLibraryViewModel.packageExportDialogState.exporting,
                    onOpenLibrary = { pkgId, pkgName ->
                        contentLibraryViewModel.browsePackageLessons(pkgId, pkgName)
                    },
                    onExportPackage = { pkgId, pkgName, destPath ->
                        contentLibraryViewModel.exportPackage(pkgId.value, pkgName, destPath)
                    },
                    onRemovePackage = { packageId, packageName ->
                        packagePendingRemoval = packageId to packageName
                    },
                    onCheckPackageIntegrity = contentLibraryViewModel::checkPackageIntegrity,
                    integrityScanningPackageId = contentLibraryViewModel.packageIntegrityDialogState
                        .takeIf { it.scanning }?.packageId,
                    integrityScanBusy = contentLibraryViewModel.libraryHealthOverviewState.scanning ||
                        contentLibraryViewModel.packageIntegrityDialogState.scanning,
                    libraryHealthOverviewState = contentLibraryViewModel.libraryHealthOverviewState,
                    onCheckLibraryHealth = {
                        val packages = (viewModel.uiState as? LibraryUiState.Content)
                            ?.installedPackages.orEmpty()
                            .map { LibraryHealthPackageTarget(it.packageId.value, it.name) }
                        contentLibraryViewModel.checkLibraryHealth(packages)
                    },
                    onOpenLibraryHealthReport = contentLibraryViewModel::showLibraryHealthReport,
                    onCreateCollection = viewModel::openCreateCollectionDialog,
                    onRenameCollection = viewModel::openRenameCollectionDialog,
                    onDeleteCollection = viewModel::openDeleteCollectionDialog,
                    onAssignPackage = viewModel::openAssignPackageDialog,
                    onRemoveAssignment = viewModel::openRemoveAssignmentDialog,
                    onArchivePackage = viewModel::openArchivePackageDialog,
                    onRestorePackage = viewModel::openRestorePackageDialog,
                    onSetActivePackage = viewModel::setActivePackage,
                    onMoveUpPackage = viewModel::movePackageUp,
                    onMoveDownPackage = viewModel::movePackageDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (contentLibraryUiState.operation is ContentLibraryOperation.Loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        packagePendingRemoval?.let { (pkgId, pkgName) ->
            AlertDialog(
                onDismissRequest = { packagePendingRemoval = null },
                title = { Text("Xóa chủ đề và toàn bộ tiến độ?") },
                text = {
                    Text("Chủ đề, nội dung đã cài đặt, lịch sử học và lịch ôn của chủ đề này sẽ bị xóa vĩnh viễn. Nếu nhập lại package sau này, bạn sẽ học lại từ đầu. Thao tác này không thể hoàn tác.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            packagePendingRemoval = null
                            contentLibraryViewModel.uninstallPackage(pkgId, pkgName)
                        }
                    ) {
                        Text("Xóa chủ đề", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { packagePendingRemoval = null }) {
                        Text("Hủy")
                    }
                }
            )
        }

        if (contentLibraryViewModel.packageIntegrityDialogState.visible) {
            PackageIntegrityDialog(
                contentLibraryViewModel.packageIntegrityDialogState,
                contentLibraryViewModel::dismissPackageIntegrityReport
            )
        }

        if (contentLibraryViewModel.packageExportDialogState.visible) {
            PackageExportDialog(
                contentLibraryViewModel.packageExportDialogState,
                contentLibraryViewModel::dismissPackageExportDialog
            )
        }

        val imageReuseState = contentLibraryViewModel.imageReuseDialogState
        if (imageReuseState.visible) {
            vn.loi.learning.desktop.ui.browser.imagereuse.ImageReuseReviewDialog(
                state = imageReuseState,
                thumbnailLoader = remember(contentMediaStorage) {
                    vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader(contentMediaStorage)
                },
                contentMediaStorage = contentMediaStorage,
                onToggleSourcePackage = contentLibraryViewModel::toggleImageReuseSourcePackage,
                onSelectAllSourcePackages = contentLibraryViewModel::selectAllImageReuseSourcePackages,
                onClearAllSourcePackages = contentLibraryViewModel::clearAllImageReuseSourcePackages,
                onScopeChanged = contentLibraryViewModel::updateImageReuseScope,
                onStartScan = contentLibraryViewModel::startImageReuseScan,
                onPreviousItem = contentLibraryViewModel::previousImageReuseItem,
                onUndoLastUse = contentLibraryViewModel::undoLastImageReuse,
                onSkipCandidate = contentLibraryViewModel::skipImageReuseCandidate,
                onSkipItem = contentLibraryViewModel::skipImageReuseItem,
                onApplyAndNext = contentLibraryViewModel::applyImageReuseAndNext,
                onReplaceTargetImage = contentLibraryViewModel::replaceImageReuseTargetImage,
                onRemoveTargetImage = contentLibraryViewModel::removeImageReuseTargetImage,
                onClose = contentLibraryViewModel::closeImageReuseReview
            )
        }

        LibraryDialogHost(
            dialogState = viewModel.activeDialog,
            isBusy = viewModel.isBusy,
            onClose = viewModel::closeDialog,
            onSubmitCreateCollection = viewModel::submitCreateCollection,
            onSubmitRenameCollection = { newName ->
                (viewModel.activeDialog as? LibraryDialogState.RenameCollection)?.let {
                    viewModel.submitRenameCollection(it.collectionId, newName)
                }
            },
            onSubmitDeleteCollection = {
                (viewModel.activeDialog as? LibraryDialogState.DeleteCollectionConfirm)?.let {
                    viewModel.submitDeleteCollection(it.collectionId)
                }
            },
            onSubmitAssignPackage = { installedPackageId ->
                (viewModel.activeDialog as? LibraryDialogState.AssignPackage)?.let {
                    viewModel.submitAssignPackage(it.collectionId, installedPackageId)
                }
            },
            onSubmitRemoveAssignment = {
                (viewModel.activeDialog as? LibraryDialogState.RemoveAssignmentConfirm)?.let {
                    viewModel.submitRemoveAssignment(it.collectionId, it.installedPackageId)
                }
            },
            onSubmitArchivePackage = {
                (viewModel.activeDialog as? LibraryDialogState.ArchivePackageConfirm)?.let {
                    viewModel.submitArchivePackage(it.installedPackageId)
                }
            },
            onSubmitRestorePackage = {
                (viewModel.activeDialog as? LibraryDialogState.RestorePackageConfirm)?.let {
                    viewModel.submitRestorePackage(it.installedPackageId)
                }
            }
        )
    }
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LibraryScreenContent(
            uiState = viewModel.uiState,
            feedbackMessage = viewModel.feedbackMessage,
            onClearFeedback = viewModel::clearFeedback,
            onSelectSection = viewModel::selectSection,
            onRefresh = viewModel::refresh,
            onCreateCollection = viewModel::openCreateCollectionDialog,
            onRenameCollection = viewModel::openRenameCollectionDialog,
            onDeleteCollection = viewModel::openDeleteCollectionDialog,
            onAssignPackage = viewModel::openAssignPackageDialog,
            onRemoveAssignment = viewModel::openRemoveAssignmentDialog,
            onArchivePackage = viewModel::openArchivePackageDialog,
            onRestorePackage = viewModel::openRestorePackageDialog,
            onSetActivePackage = viewModel::setActivePackage,
            onMoveUpPackage = viewModel::movePackageUp,
            onMoveDownPackage = viewModel::movePackageDown,
            modifier = Modifier.fillMaxSize()
        )

        LibraryDialogHost(
            dialogState = viewModel.activeDialog,
            isBusy = viewModel.isBusy,
            onClose = viewModel::closeDialog,
            onSubmitCreateCollection = viewModel::submitCreateCollection,
            onSubmitRenameCollection = { newName ->
                (viewModel.activeDialog as? LibraryDialogState.RenameCollection)?.let {
                    viewModel.submitRenameCollection(it.collectionId, newName)
                }
            },
            onSubmitDeleteCollection = {
                (viewModel.activeDialog as? LibraryDialogState.DeleteCollectionConfirm)?.let {
                    viewModel.submitDeleteCollection(it.collectionId)
                }
            },
            onSubmitAssignPackage = { installedPackageId ->
                (viewModel.activeDialog as? LibraryDialogState.AssignPackage)?.let {
                    viewModel.submitAssignPackage(it.collectionId, installedPackageId)
                }
            },
            onSubmitRemoveAssignment = {
                (viewModel.activeDialog as? LibraryDialogState.RemoveAssignmentConfirm)?.let {
                    viewModel.submitRemoveAssignment(it.collectionId, it.installedPackageId)
                }
            },
            onSubmitArchivePackage = {
                (viewModel.activeDialog as? LibraryDialogState.ArchivePackageConfirm)?.let {
                    viewModel.submitArchivePackage(it.installedPackageId)
                }
            },
            onSubmitRestorePackage = {
                (viewModel.activeDialog as? LibraryDialogState.RestorePackageConfirm)?.let {
                    viewModel.submitRestorePackage(it.installedPackageId)
                }
            }
        )
    }
}

@Composable
fun LibraryScreenContent(
    uiState: LibraryUiState,
    feedbackMessage: String? = null,
    onClearFeedback: () -> Unit = {},
    onSelectSection: (LibrarySection) -> Unit = {},
    onRefresh: () -> Unit = {},
    onImport: () -> Unit = {},
    isImporting: Boolean = false,
    isExporting: Boolean = false,
    onOpenLibrary: ((InstalledPackageId, String) -> Unit)? = null,
    onExportPackage: ((InstalledPackageId, String, Path) -> Unit)? = null,
    onRemovePackage: ((String, String) -> Unit)? = null,
    onCheckPackageIntegrity: ((String) -> Unit)? = null,
    integrityScanningPackageId: String? = null,
    integrityScanBusy: Boolean = false,
    libraryHealthOverviewState: LibraryHealthOverviewState = LibraryHealthOverviewState(),
    onCheckLibraryHealth: (() -> Unit)? = null,
    onOpenLibraryHealthReport: (String) -> Unit = {},
    onCreateCollection: () -> Unit = {},
    onRenameCollection: (CollectionId, String) -> Unit = { _, _ -> },
    onDeleteCollection: (CollectionId, String) -> Unit = { _, _ -> },
    onAssignPackage: (CollectionId, String) -> Unit = { _, _ -> },
    onRemoveAssignment: (CollectionId, String, InstalledPackageId, String) -> Unit = { _, _, _, _ -> },
    onArchivePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onRestorePackage: (InstalledPackageId, String) -> Unit = { _, _ -> },
    onSetActivePackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveUpPackage: ((InstalledPackageId) -> Unit)? = null,
    onMoveDownPackage: ((InstalledPackageId) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LibraryUiState.Loading -> {
            LibraryLoadingView(modifier = modifier)
        }
        is LibraryUiState.Empty -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                onCheckLibraryHealth?.let { checkLibraryHealth ->
                    LibraryHealthOverview(
                        state = libraryHealthOverviewState,
                        onCheck = checkLibraryHealth,
                        onOpenReport = onOpenLibraryHealthReport
                    )
                }
                LibraryEmptyView(
                    message = uiState.message,
                    onRefresh = onRefresh,
                    onImport = onImport,
                    isImporting = isImporting,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is LibraryUiState.Error -> {
            LibraryErrorView(
                message = uiState.message,
                onRetry = onRefresh,
                modifier = modifier
            )
        }
        is LibraryUiState.Content -> {
            val scrollState = rememberScrollState()
            val sectionCounts = mapOf(
                LibrarySection.OVERVIEW to uiState.installedPackages.size + uiState.collections.size,
                LibrarySection.INSTALLED to uiState.installedPackages.size,
                LibrarySection.ACTIVE to uiState.activePackages.size,
                LibrarySection.ARCHIVED to uiState.archivedPackages.size,
                LibrarySection.COLLECTIONS to uiState.collections.size,
                LibrarySection.DELETED to uiState.deletedCollections.size
            )

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (feedbackMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feedbackMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearFeedback) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                LibraryHeader(
                    libraryName = uiState.tree.libraryName,
                    statistics = uiState.statistics,
                    onImport = onImport,
                    onCreateCollection = onCreateCollection,
                    isImporting = isImporting
                )

                onCheckLibraryHealth?.let { checkLibraryHealth ->
                    LibraryHealthOverview(
                        state = libraryHealthOverviewState,
                        onCheck = checkLibraryHealth,
                        onOpenReport = onOpenLibraryHealthReport
                    )
                }

                LibrarySectionTabs(
                    selectedSection = uiState.selectedSection,
                    onSelectSection = onSelectSection,
                    counts = sectionCounts
                )

                when (uiState.selectedSection) {
                    LibrarySection.OVERVIEW ->
                        LibraryOverviewSection(
                            uiState = uiState,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage,
                            onSetActivePackage = onSetActivePackage,
                            onMoveUpPackage = onMoveUpPackage,
                            onMoveDownPackage = onMoveDownPackage,
                            onOpenLibrary = onOpenLibrary,
                            onExportPackage = onExportPackage,
                            onRemovePackage = onRemovePackage,
                            onCheckPackageIntegrity = onCheckPackageIntegrity,
                            integrityScanningPackageId = integrityScanningPackageId,
                            integrityScanBusy = integrityScanBusy,
                            exportBusy = isExporting
                        )

                    LibrarySection.INSTALLED ->
                        PackageListSection(
                            title = "Installed Packages (${uiState.installedPackages.size})",
                            packages = uiState.installedPackages,
                            packageProgress = uiState.packageProgress,
                            activePackageId = uiState.activePackageId,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage,
                            onSetActivePackage = onSetActivePackage,
                            onMoveUpPackage = onMoveUpPackage,
                            onMoveDownPackage = onMoveDownPackage,
                            onOpenLibrary = onOpenLibrary,
                            onExportPackage = onExportPackage,
                            onRemovePackage = onRemovePackage,
                            onCheckPackageIntegrity = onCheckPackageIntegrity,
                            integrityScanningPackageId = integrityScanningPackageId,
                            integrityScanBusy = integrityScanBusy,
                            exportBusy = isExporting
                        )

                    LibrarySection.ACTIVE ->
                        PackageListSection(
                            title = "Active Packages (${uiState.activePackages.size})",
                            packages = uiState.activePackages,
                            packageProgress = uiState.packageProgress,
                            activePackageId = uiState.activePackageId,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage,
                            onSetActivePackage = onSetActivePackage,
                            onMoveUpPackage = onMoveUpPackage,
                            onMoveDownPackage = onMoveDownPackage,
                            onOpenLibrary = onOpenLibrary,
                            onExportPackage = onExportPackage,
                            onRemovePackage = onRemovePackage,
                            onCheckPackageIntegrity = onCheckPackageIntegrity,
                            integrityScanningPackageId = integrityScanningPackageId,
                            integrityScanBusy = integrityScanBusy,
                            exportBusy = isExporting
                        )

                    LibrarySection.ARCHIVED ->
                        PackageListSection(
                            title = "Archived Packages (${uiState.archivedPackages.size})",
                            packages = uiState.archivedPackages,
                            packageProgress = uiState.packageProgress,
                            activePackageId = uiState.activePackageId,
                            onArchivePackage = onArchivePackage,
                            onRestorePackage = onRestorePackage,
                            onSetActivePackage = onSetActivePackage,
                            onMoveUpPackage = onMoveUpPackage,
                            onMoveDownPackage = onMoveDownPackage,
                            onOpenLibrary = onOpenLibrary,
                            onExportPackage = onExportPackage,
                            onRemovePackage = onRemovePackage,
                            onCheckPackageIntegrity = onCheckPackageIntegrity,
                            integrityScanningPackageId = integrityScanningPackageId,
                            integrityScanBusy = integrityScanBusy,
                            exportBusy = isExporting
                        )

                    LibrarySection.COLLECTIONS ->
                        ActiveCollectionListSection(
                            collections = uiState.collections,
                            onCreateCollection = onCreateCollection,
                            onRenameCollection = onRenameCollection,
                            onDeleteCollection = onDeleteCollection,
                            onAssignPackage = onAssignPackage,
                            onRemoveAssignment = onRemoveAssignment
                        )

                    LibrarySection.DELETED ->
                        DeletedCollectionListSection(
                            collections = uiState.deletedCollections
                        )
                }
            }
        }
    }
}
