package vn.loi.learning.desktop.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import vn.loi.learning.desktop.ui.study.StudyPresentationStagingState

@Composable
fun ContentHost(
    destination: NavigationDestination,
    dashboardUiState: DashboardUiState,
    statisticsUiState: StatisticsUiState,
    reviewHistoryUiState: ReviewHistoryUiState,
    studyUiState: StudyUiState,
    contentLibraryViewModel: ContentLibraryViewModel,
    libraryViewModel: vn.loi.learning.desktop.ui.library.LibraryViewModel? = null,
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
    onRefreshStudyHeaderStatistics: () -> Unit = {},
    onStartStudy: () -> Unit,
    onReplayLatestCompletedStudySession: () -> Unit = {},
    onStartLearnedItemsReview: () -> Unit = {},
    onEnableContinuousReview: () -> Unit = {},
    onDisableContinuousReview: () -> Unit = {},
    onStartLessonStudy: (vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection) -> Unit,
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit = onRevealAnswer,
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onCompleteAdaptiveSession: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onTypingCorrectCompleted: (vn.loi.learning.desktop.ui.study.TypingRecallSuccessRequest) -> Unit = {},
    onTypingReveal: (vn.loi.learning.desktop.ui.study.TypingRecallRevealRequest) -> Unit = {},
    onTypingForcedAgain: (vn.loi.learning.desktop.ui.study.TypingRecallRevealRequest) -> Unit = {},
    onEasy: () -> Unit,
    onRatingFeedbackConsumed: (Long) -> Unit,
    onSessionContinuityAdvanced: (Long) -> Unit,
    onUndo: () -> Unit,
    onPauseStudy: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToLesson: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)? = null,
    onBackToLibrary: (() -> Unit)? = null,
    onContinueLearning: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var presentationState by remember {
        mutableStateOf(
            StudyPresentationStagingState(
                itemId = studyUiState.currentLearningItemId,
                active = runtimeConfiguration.studyPresentation
            )
        )
    }
    val reconciledPresentationState =
        presentationState.reconcile(
            currentItemId = studyUiState.currentLearningItemId,
            persisted = runtimeConfiguration.studyPresentation
        )
    SideEffect {
        if (presentationState != reconciledPresentationState) {
            presentationState = reconciledPresentationState
        }
    }
    fun updatePresentation(configuration: DesktopRuntimeConfiguration) {
        if (configuration.studyPresentation != runtimeConfiguration.studyPresentation) {
            presentationState =
                reconciledPresentationState.stage(configuration.studyPresentation)
        }
        onRuntimeConfigurationChanged(configuration)
    }

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
                onRefreshHeaderStatistics = onRefreshStudyHeaderStatistics,
                onStartStudy = onStartStudy,
                onReplayLatestCompletedStudySession = onReplayLatestCompletedStudySession,
                onStartLearnedItemsReview = onStartLearnedItemsReview,
                onEnableContinuousReview = onEnableContinuousReview,
                onDisableContinuousReview = onDisableContinuousReview,
                onRevealAnswer = onRevealAnswer,
                onCompleteFlowStage = onCompleteFlowStage,
                onShowDecisionExplanation = onShowDecisionExplanation,
                onHideDecisionExplanation = onHideDecisionExplanation,
                onCompleteAdaptiveSession = onCompleteAdaptiveSession,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onTypingCorrectCompleted = onTypingCorrectCompleted,
                onTypingReveal = onTypingReveal,
                onTypingForcedAgain = onTypingForcedAgain,
                onEasy = onEasy,
                onRatingFeedbackConsumed = onRatingFeedbackConsumed,
                onSessionContinuityAdvanced = onSessionContinuityAdvanced,
                onUndo = onUndo,
                onPause = onPauseStudy,
                onBackToLesson = onBackToLesson,
                onBackToLibrary = onBackToLibrary,
                onContinueLearning = onContinueLearning,
                audioLoopDelaySeconds = runtimeConfiguration.audioLoopDelaySeconds,
                typographyPreferences = runtimeConfiguration.studyTypography,
                shortcutRegistry = runtimeConfiguration.studyShortcuts,
                presentationPreferences = reconciledPresentationState.active,
                presentationState = reconciledPresentationState,
                onPresentationPreferencesChanged = { preferences ->
                    updatePresentation(
                        runtimeConfiguration.copy(studyPresentation = preferences)
                    )
                },
                onOpenPresentationSettings = onOpenSettings,
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
            vn.loi.learning.desktop.ui.library.LibraryScreen(
                viewModel = libraryViewModel,
                contentLibraryViewModel = contentLibraryViewModel,
                contentMediaStorage = contentMediaStorage,
                onStartLessonStudy = onStartLessonStudy,
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )


        NavigationDestination.SETTINGS ->
            SettingsScreen(
                runtimeDiagnostics = runtimeDiagnostics,
                runtimeConfiguration = runtimeConfiguration,
                strings = strings,
                onRuntimeConfigurationChanged = ::updatePresentation,
                onExportDiagnostics = onExportDiagnostics,
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
                modifier = modifier.fillMaxSize()
            )
    }
}
