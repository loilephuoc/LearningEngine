package vn.loi.learning.desktop.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.component.AppHeader
import vn.loi.learning.desktop.ui.component.ContentHost
import vn.loi.learning.desktop.ui.component.Sidebar
import vn.loi.learning.desktop.ui.component.StatusBar
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.dashboard.DashboardFacade
import vn.loi.learning.desktop.ui.dashboard.DashboardViewModel
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryFacade
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryViewModel
import vn.loi.learning.desktop.ui.statistics.StatisticsFacade
import vn.loi.learning.desktop.ui.statistics.StatisticsViewModel
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.runtime.toSessionPolicy
import vn.loi.learning.desktop.ui.localization.DesktopLocalization
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.study.LearningContentPresenter
import vn.loi.learning.desktop.ui.study.usesFocusedStudyShell
import vn.loi.learning.desktop.ui.state.CoroutineDesktopTaskRunner
import vn.loi.learning.desktop.ui.state.CoroutineDesktopDebouncer

@Composable
fun LearningShell(
    applicationContext:
    LearningApplicationContext,
    contentMediaStorage: ContentMediaStorage,
    engineName: String,
    dashboardName: String,
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit,
    onExportDiagnostics: () -> String?,
    onCreateBackup: () -> String?,
    onRestoreBackup: (Boolean) -> String?
) {
    val strings = DesktopLocalization.strings(runtimeConfiguration.locale)
    val taskScope = rememberCoroutineScope()
    val taskRunner = remember(taskScope) { CoroutineDesktopTaskRunner(taskScope) }
    val searchDebouncer = remember(taskScope) { CoroutineDesktopDebouncer(taskScope) }
    val learningContentPresenter = remember(contentMediaStorage, strings.learningContent) {
        LearningContentPresenter(contentMediaStorage, strings.learningContent)
    }
    val navigationState =
        remember {
            NavigationState()
        }

    val shellFocusRequester =
        remember {
            FocusRequester()
        }
    val navigationFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var focusedRegion by remember { mutableStateOf(ShellFocusRegion.NAVIGATION) }

    fun focusRegion(region: ShellFocusRegion) {
        focusedRegion = region
        when (region) {
            ShellFocusRegion.NAVIGATION -> navigationFocusRequester.requestFocus()
            ShellFocusRegion.CONTENT -> contentFocusRequester.requestFocus()
        }
    }

    val dashboardViewModel =
        remember(applicationContext) {
            DashboardViewModel(
                DashboardFacade(
                    applicationContext
                ),
                taskRunner
            )
        }

    val statisticsViewModel =
        remember(applicationContext) {
            StatisticsViewModel(
                StatisticsFacade(
                    applicationContext
                ),
                taskRunner
            )
        }

    val reviewHistoryViewModel =
        remember(applicationContext) {
            ReviewHistoryViewModel(
                ReviewHistoryFacade(
                    applicationContext
                ),
                taskRunner
            )
        }

    var onStudyDataChangedRef: (() -> Unit)? = remember { null }
    val studyViewModel =
        remember(applicationContext) {
            StudyViewModel(
                facade =
                    StudyFacade(
                        applicationContext,
                        sessionPolicyProvider = { runtimeConfiguration.toSessionPolicy() }
                    ),
                onStudyDataChanged = {
                    dashboardViewModel.refresh()
                    statisticsViewModel.refresh()
                    reviewHistoryViewModel.refresh()
                    onStudyDataChangedRef?.invoke()
                },
                taskRunner = taskRunner
            )
        }
    val focusedStudy = usesFocusedStudyShell(
        navigationState.currentDestination,
        studyViewModel.uiState.hasActiveSession
    )

    val lessonStudyNavigationCoordinator =
        remember(
            studyViewModel,
            navigationState
        ) {
            LessonStudyNavigationCoordinator(
                studyViewModel = studyViewModel,
                navigationState = navigationState
            )
        }

    var onContentDataChangedRef: (() -> Unit)? = remember { null }

    val contentLibraryViewModel =
        remember(applicationContext) {
            ContentLibraryViewModel(
                facade =
                    ContentLibraryFacade(
                        applicationContext
                    ),
                lessonBrowserFacade =
                    LessonBrowserFacade(
                        applicationContext
                    ),
                packageBrowserFacade =
                    vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade(
                        queryService = applicationContext.packageBrowserQuery,
                        editService = applicationContext.contentRepository?.let { contentRepo ->
                            vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService(
                                contentRepository = contentRepo,
                                contentLibraryRepository = applicationContext.contentLibraryRepository,
                                installedPackageRepository = applicationContext.installedPackageRepository,
                                contentPackageRepository = applicationContext.contentPackageRepository
                            )
                        },
                        learningItemRepository = applicationContext.learningItemRepository
                    ),
                contentMediaStorage = contentMediaStorage,
                onContentDataChanged = {
                    dashboardViewModel.refresh()
                    statisticsViewModel.refresh()
                    reviewHistoryViewModel.refresh()
                    studyViewModel.refresh()
                    onContentDataChangedRef?.invoke()
                },
                taskRunner = taskRunner,
                searchDebouncer = searchDebouncer
            )
        }

    val libraryViewModel =
        remember(applicationContext) {
            val facade = createCanonicalLibraryFacade(applicationContext)
            vn.loi.learning.desktop.ui.library.LibraryViewModel(
                facade = facade,
                taskRunner = taskRunner,
                onLibraryDataChanged = {
                    dashboardViewModel.refresh()
                    statisticsViewModel.refresh()
                    reviewHistoryViewModel.refresh()
                    studyViewModel.refresh()
                    contentLibraryViewModel.refresh()
                }
            )
        }

    onContentDataChangedRef = {
        libraryViewModel.refresh()
    }
    onStudyDataChangedRef = {
        libraryViewModel.refresh()
    }


    fun refreshDestination(
        destination: NavigationDestination
    ) {
        when (destination) {
            NavigationDestination.DASHBOARD ->
                dashboardViewModel.refresh()

            NavigationDestination.STUDY ->
                studyViewModel.refresh()

            NavigationDestination.STATISTICS ->
                statisticsViewModel.refresh()

            NavigationDestination.REVIEW_HISTORY ->
                reviewHistoryViewModel.refresh()

            NavigationDestination.CONTENT_LIBRARY -> {
                contentLibraryViewModel.resetLibraryNavigationState()
                studyViewModel.dismissCompletionPresentation()
                libraryViewModel.refresh()
                contentLibraryViewModel.refresh()
            }

            NavigationDestination.SETTINGS ->
                Unit
        }
    }


    fun navigateTo(
        destination: NavigationDestination
    ) {
        navigationState.navigateTo(
            destination
        )

        refreshDestination(
            destination
        )
    }

    LaunchedEffect(Unit) {
        shellFocusRequester.requestFocus()
    }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(
                    shellFocusRequester
                )
                .focusable()
                .semantics {
                    contentDescription =
                        shellKeyboardHint()
                }
                .onPreviewKeyEvent { event ->
                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {
                        return@onPreviewKeyEvent false
                    }

                    val key =
                        when (event.key) {
                            Key.F1 ->
                                ShellKeyboardKey.F1

                            Key.F2 ->
                                ShellKeyboardKey.F2

                            Key.F3 ->
                                ShellKeyboardKey.F3

                            Key.F4 ->
                                ShellKeyboardKey.F4

                            Key.F5 ->
                                ShellKeyboardKey.F5

                            Key.F6 ->
                                ShellKeyboardKey.F6

                            Key.PageUp ->
                                ShellKeyboardKey.PAGE_UP

                            Key.PageDown ->
                                ShellKeyboardKey.PAGE_DOWN

                            Key.R ->
                                ShellKeyboardKey.R

                            else -> null
                        } ?: return@onPreviewKeyEvent false

                    when (
                        val action =
                            resolveShellKeyboardAction(
                                key = key,
                                controlPressed =
                                    event.isCtrlPressed,
                                shiftPressed =
                                    event.isShiftPressed
                            )
                    ) {
                        is ShellKeyboardAction.Navigate -> {
                            navigateTo(
                                action.destination
                            )
                            true
                        }

                        ShellKeyboardAction.NavigatePrevious -> {
                            navigationState
                                .navigatePrevious()

                            refreshDestination(
                                navigationState
                                    .currentDestination
                            )
                            true
                        }

                        ShellKeyboardAction.NavigateNext -> {
                            navigationState
                                .navigateNext()

                            refreshDestination(
                                navigationState
                                    .currentDestination
                            )
                            true
                        }

                        ShellKeyboardAction.RefreshCurrent -> {
                            refreshDestination(
                                navigationState
                                    .currentDestination
                            )
                            true
                        }

                        ShellKeyboardAction.FocusNextRegion -> {
                            focusRegion(focusedRegion.next())
                            true
                        }

                        ShellKeyboardAction.FocusPreviousRegion -> {
                            focusRegion(focusedRegion.previous())
                            true
                        }

                        null -> false
                    }
                },
        color =
            MaterialTheme
                .colorScheme
                .background
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {
            if (!focusedStudy) {
                AppHeader()
                HorizontalDivider()
            }

            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {
                if (!focusedStudy) Sidebar(
                    currentDestination =
                        navigationState
                            .currentDestination,
                    destinationLabel = strings::destination,
                    onDestinationSelected =
                        ::navigateTo,
                    modifier =
                        Modifier
                            .focusRequester(navigationFocusRequester)
                            .focusGroup()
                )

                if (!focusedStudy) VerticalDivider(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                )

                ContentHost(
                    destination =
                        navigationState
                            .currentDestination,
                    dashboardUiState =
                        dashboardViewModel
                            .uiState,
                    statisticsUiState =
                        statisticsViewModel
                            .uiState,
                    reviewHistoryUiState =
                        reviewHistoryViewModel
                            .uiState,
                    studyUiState =
                        studyViewModel
                            .uiState,
                    contentLibraryViewModel =
                        contentLibraryViewModel,
                    libraryViewModel = libraryViewModel,
                    runtimeDiagnostics = runtimeDiagnostics,

                    runtimeConfiguration = runtimeConfiguration,
                    strings = strings,
                    learningContentPresenter = learningContentPresenter,
                    contentMediaStorage = contentMediaStorage,
                    onRuntimeConfigurationChanged = onRuntimeConfigurationChanged,
                    onExportDiagnostics = onExportDiagnostics,
                    onCreateBackup = onCreateBackup,
                    onRestoreBackup = { onRestoreBackup(studyViewModel.uiState.hasActiveSession) },
                    onRefreshDashboard =
                        dashboardViewModel::refresh,
                    onRefreshStatistics =
                        statisticsViewModel::refresh,
                    onRefreshReviewHistory =
                        reviewHistoryViewModel::refresh,
                    onReviewHistoryQueryChanged = reviewHistoryViewModel::updateQuery,
                    onClearReviewHistoryQuery = reviewHistoryViewModel::clearQuery,
                    onReviewHistoryFilterChanged = reviewHistoryViewModel::updateFilter,
                    onReviewHistorySortChanged = reviewHistoryViewModel::updateSort,
                    onRefreshStudy =
                        studyViewModel::refresh,
                    onStartStudy =
                        studyViewModel::startStudy,
                    onStartLessonStudy =
                        lessonStudyNavigationCoordinator
                        ::startLessonStudy,
                    onRevealAnswer =
                        studyViewModel
                        ::revealAnswer,
                    onCompleteFlowStage =
                        studyViewModel
                        ::completeFlowStage,
                    onShowDecisionExplanation =
                        studyViewModel
                        ::showDecisionExplanation,
                    onHideDecisionExplanation =
                        studyViewModel
                        ::hideDecisionExplanation,
                    onCompleteAdaptiveSession =
                        studyViewModel
                        ::completeAdaptiveSession,
                    onAgain =
                        studyViewModel
                        ::reviewAgain,
                    onHard =
                        studyViewModel
                        ::reviewHard,
                    onGood =
                        studyViewModel
                        ::reviewGood,
                    onEasy =
                        studyViewModel
                        ::reviewEasy,
                    onUndo =
                        studyViewModel
                        ::undoLatestReview,
                    onPauseStudy = {
                        navigateTo(NavigationDestination.DASHBOARD)
                    },
                    onBackToLesson = { pkgId, contentId ->
                        val pkgName = studyViewModel.uiState.activeInstalledPackageId?.value ?: "Package"
                        navigateTo(NavigationDestination.CONTENT_LIBRARY)
                        contentLibraryViewModel.browsePackageLessons(pkgId, pkgName)
                        contentLibraryViewModel.selectLesson(contentId.value)
                    },
                    onBackToLibrary = {
                        navigateTo(NavigationDestination.CONTENT_LIBRARY)
                    },
                    onContinueLearning = { pkgId, contentId ->
                        val pkgName = studyViewModel.uiState.activeInstalledPackageId?.value ?: "Package"
                        navigateTo(NavigationDestination.CONTENT_LIBRARY)
                        contentLibraryViewModel.browsePackageLessons(pkgId, pkgName)
                        contentLibraryViewModel.selectLesson(contentId.value)
                        contentLibraryViewModel.openWorkspaceForSelectedLesson()
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(contentFocusRequester)
                            .focusGroup()
                )
            }

            if (!focusedStudy) {
                HorizontalDivider()
                StatusBar(
                    engineName = engineName,
                    dashboardName = dashboardName
                )
            }
        }
    }
}

internal fun createCanonicalLibraryFacade(
    applicationContext: vn.loi.learning.infrastructure.LearningApplicationContext
): vn.loi.learning.desktop.ui.library.LibraryFacade? {
    val libraryId = applicationContext.defaultLibraryId ?: return null
    return vn.loi.learning.desktop.ui.library.LibraryFacade(
        queryService = applicationContext.libraryQuery,
        commandService = applicationContext.libraryCommand,
        libraryId = libraryId,
        packageProgressQueryService = applicationContext.packageProgress
    )
}
