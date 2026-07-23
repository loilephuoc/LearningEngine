package vn.loi.learning.desktop.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryScreen
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.dashboard.DashboardScreen
import vn.loi.learning.desktop.ui.dashboard.DashboardUiState
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryScreen
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryUiState
import vn.loi.learning.desktop.ui.settings.SettingsScreen
import vn.loi.learning.desktop.ui.statistics.StatisticsScreen
import vn.loi.learning.desktop.ui.statistics.StatisticsUiState
import vn.loi.learning.desktop.ui.study.StudyScreen
import vn.loi.learning.desktop.ui.study.StudyUiState
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.desktop.ui.localization.DesktopStrings
import vn.loi.learning.desktop.ui.study.LearningContentPresenter

@Composable
fun ContentHost(
    destination: NavigationDestination,
    dashboardUiState: DashboardUiState,
    statisticsUiState: StatisticsUiState,
    reviewHistoryUiState: ReviewHistoryUiState,
    studyUiState: StudyUiState,
    contentLibraryViewModel: ContentLibraryViewModel,
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    strings: DesktopStrings,
    learningContentPresenter: LearningContentPresenter,
    contentMediaStorage: vn.loi.learning.application.port.ContentMediaStorage,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit,
    onExportDiagnostics: () -> String?,
    onCreateBackup: () -> String?,
    onRestoreBackup: () -> String?,
    onRefreshDashboard: () -> Unit,
    onRefreshStatistics: () -> Unit,
    onRefreshReviewHistory: () -> Unit,
    onReviewHistoryQueryChanged: (String) -> Unit,
    onClearReviewHistoryQuery: () -> Unit,
    onReviewHistoryFilterChanged: (vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryFilter) -> Unit,
    onReviewHistorySortChanged: (vn.loi.learning.desktop.ui.reviewhistory.ReviewHistorySort) -> Unit,
    onRefreshStudy: () -> Unit,
    onStartStudy: () -> Unit,
    onStartLessonStudy: (String) -> Unit,
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit = onRevealAnswer,
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    onUndo: () -> Unit,
    onPauseStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (destination) {
        NavigationDestination.DASHBOARD ->
            DashboardScreen(
                uiState = dashboardUiState,
                onRetry = onRefreshDashboard,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.STUDY ->
            StudyScreen(
                uiState = studyUiState,
                contentPresenter = learningContentPresenter,
                contentStrings = strings.learningContent,
                workspaceStrings = strings.studyWorkspace,
                onRefresh = onRefreshStudy,
                onStartStudy = onStartStudy,
                onRevealAnswer = onRevealAnswer,
                onCompleteFlowStage = onCompleteFlowStage,
                onShowDecisionExplanation = onShowDecisionExplanation,
                onHideDecisionExplanation = onHideDecisionExplanation,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onEasy = onEasy,
                onUndo = onUndo,
                onPause = onPauseStudy,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.STATISTICS ->
            StatisticsScreen(
                uiState = statisticsUiState,
                onRetry = onRefreshStatistics,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.REVIEW_HISTORY ->
            ReviewHistoryScreen(
                uiState = reviewHistoryUiState,
                onRetry = onRefreshReviewHistory,
                onQueryChanged = onReviewHistoryQueryChanged,
                onClearQuery = onClearReviewHistoryQuery,
                onFilterChanged = onReviewHistoryFilterChanged,
                onSortChanged = onReviewHistorySortChanged,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.CONTENT_LIBRARY ->
            ContentLibraryScreen(
                viewModel = contentLibraryViewModel,
                contentMediaStorage = contentMediaStorage,
                onStartLessonStudy =
                    onStartLessonStudy,
                modifier = modifier.fillMaxSize()
            )

        NavigationDestination.SETTINGS ->
            SettingsScreen(
                runtimeDiagnostics = runtimeDiagnostics,
                runtimeConfiguration = runtimeConfiguration,
                strings = strings,
                onRuntimeConfigurationChanged = onRuntimeConfigurationChanged,
                onExportDiagnostics = onExportDiagnostics,
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
                modifier = modifier.fillMaxSize()
            )
    }
}
