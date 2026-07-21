package vn.loi.learning.desktop.ui.shell

import androidx.compose.foundation.layout.Column
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

@Composable
fun LearningShell(
    applicationContext:
    LearningApplicationContext,
    engineName: String,
    dashboardName: String
) {
    val navigationState =
        remember {
            NavigationState()
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

    Surface(
        modifier =
            Modifier.fillMaxSize(),
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
                    onDestinationSelected = {
                            destination ->

                        navigationState.navigateTo(
                            destination
                        )

                        dashboardViewModel.refresh()
                        statisticsViewModel.refresh()
                        reviewHistoryViewModel.refresh()

                        if (
                            destination ==
                            NavigationDestination
                                .CONTENT_LIBRARY
                        ) {
                            contentLibraryViewModel
                                .refresh()
                        }

                        if (
                            destination ==
                            NavigationDestination
                                .STUDY
                        ) {
                            studyViewModel.refresh()
                        }
                    }
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
                    onRefreshStudy =
                        studyViewModel::refresh,
                    onStartStudy =
                        studyViewModel::startStudy,
                    onStartLessonStudy = {
                            contentId ->

                        studyViewModel
                            .startLessonStudy(
                                contentId
                            )

                        navigationState.navigateTo(
                            NavigationDestination.STUDY
                        )
                    },
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