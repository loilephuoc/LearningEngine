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

@Composable
fun ContentHost(
    destination: NavigationDestination,
    dashboardUiState: DashboardUiState,
    statisticsUiState: StatisticsUiState,
    reviewHistoryUiState: ReviewHistoryUiState,
    studyUiState: StudyUiState,
    contentLibraryViewModel: ContentLibraryViewModel,
    onRefreshStudy: () -> Unit,
    onStartStudy: () -> Unit,
    onStartLessonStudy: (String) -> Unit,
    onRevealAnswer: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (destination) {
        NavigationDestination.DASHBOARD ->
            DashboardScreen(
                uiState = dashboardUiState,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.STUDY ->
            StudyScreen(
                uiState = studyUiState,
                onRefresh = onRefreshStudy,
                onStartStudy = onStartStudy,
                onRevealAnswer = onRevealAnswer,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onEasy = onEasy,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.STATISTICS ->
            StatisticsScreen(
                uiState = statisticsUiState,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.REVIEW_HISTORY ->
            ReviewHistoryScreen(
                uiState = reviewHistoryUiState,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(24.dp)
            )

        NavigationDestination.CONTENT_LIBRARY ->
            ContentLibraryScreen(
                viewModel = contentLibraryViewModel,
                onStartLessonStudy =
                    onStartLessonStudy,
                modifier = modifier.fillMaxSize()
            )

        NavigationDestination.SETTINGS ->
            SettingsScreen(
                modifier = modifier.fillMaxSize()
            )
    }
}