package vn.loi.learning.desktop.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.focusable
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

@Composable
fun LearningShell(
    applicationContext:
    LearningApplicationContext,
    engineName: String,
    dashboardName: String,
    runtimeDiagnostics: DesktopRuntimeDiagnostics
) {
    val navigationState =
        remember {
            NavigationState()
        }

    val shellFocusRequester =
        remember {
            FocusRequester()
        }

    val dashboardViewModel =
        remember(applicationContext) {
            DashboardViewModel(
                DashboardFacade(
                    applicationContext
                )
            )
        }

    val statisticsViewModel =
        remember(applicationContext) {
            StatisticsViewModel(
                StatisticsFacade(
                    applicationContext
                )
            )
        }

    val reviewHistoryViewModel =
        remember(applicationContext) {
            ReviewHistoryViewModel(
                ReviewHistoryFacade(
                    applicationContext
                )
            )
        }

    val studyViewModel =
        remember(applicationContext) {
            StudyViewModel(
                facade =
                    StudyFacade(
                        applicationContext
                    ),
                onStudyDataChanged = {
                    dashboardViewModel.refresh()
                    statisticsViewModel.refresh()
                    reviewHistoryViewModel.refresh()
                }
            )
        }

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
                onContentDataChanged = {
                    dashboardViewModel.refresh()
                    statisticsViewModel.refresh()
                    reviewHistoryViewModel.refresh()
                    studyViewModel.refresh()
                }
            )
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

            NavigationDestination.CONTENT_LIBRARY ->
                contentLibraryViewModel.refresh()

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
            AppHeader()

            HorizontalDivider()

            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {
                Sidebar(
                    currentDestination =
                        navigationState
                            .currentDestination,
                    onDestinationSelected =
                        ::navigateTo
                )

                VerticalDivider(
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
                    runtimeDiagnostics = runtimeDiagnostics,
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
                    modifier =
                        Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            StatusBar(
                engineName = engineName,
                dashboardName = dashboardName
            )
        }
    }
}
