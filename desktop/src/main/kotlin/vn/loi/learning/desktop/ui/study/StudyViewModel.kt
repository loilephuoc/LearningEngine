package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.application.learningflow.LearningFlowStage

class StudyViewModel(
    private val facade: StudyFacade,
    private val onStudyDataChanged: (() -> Unit)? = null,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {
    private val flowCoordinator = DesktopLearningFlowCoordinator()
    private var actionInProgress = false

    var uiState by mutableStateOf(StudyUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        if (actionInProgress) return
        val previous = uiState
        actionInProgress = true
        uiState = previous.copy(actionInProgress = true, message = "Loading study session")
        taskRunner.run(
            work = facade::load,
            onSuccess = { loaded ->
                uiState = flowCoordinator.synchronize(
                    loaded.copy(loadError = null, failureKind = null, actionInProgress = false)
                )
                actionInProgress = false
            },
            onFailure = { exception ->
                uiState = previous.copy(
                    loadError = StudyFailureMessage.forStudyData(exception),
                    failureKind = StudyFailureKind.SESSION_RECOVERY,
                    message = "Study data needs attention.",
                    workspaceState = ReviewWorkspaceState.RecoverableFailure,
                    actionInProgress = false
                )
                actionInProgress = false
            }
        )
    }

    fun startStudy() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing study session"
    ) { facade.startStudy() }

    fun bootstrapSessionOverview(topicId: String) =
        updateSafely(
            failureKind = StudyFailureKind.PREPARATION,
            preparingMessage = "Bootstrapping session overview"
        ) { facade.bootstrapSessionOverview(topicId) }

    fun startFirstScene(promptText: String, expectedAnswer: String) =
        updateSafely(
            failureKind = StudyFailureKind.PREPARATION,
            preparingMessage = "Executing first scene"
        ) { facade.startFirstScene(promptText, expectedAnswer) }

    fun submitSceneAttempt(userAttempt: String, latencyMs: Long = 1000L) =
        updateSafely(
            failureKind = StudyFailureKind.CONTENT
        ) { facade.submitSceneAttempt(userAttempt, latencyMs) }

    fun completeAdaptiveSession() =
        updateSafely(
            failureKind = StudyFailureKind.REVIEW_TRANSACTION
        ) { facade.completeAdaptiveSession() }

    fun toggleDecisionExplanationVisibility() =
        updateSafely(
            failureKind = StudyFailureKind.CONTENT
        ) { facade.toggleDecisionExplanationVisibility() }

    fun showDecisionExplanation() =
        updateSafely(
            failureKind = StudyFailureKind.CONTENT
        ) { facade.showDecisionExplanation() }

    fun hideDecisionExplanation() =
        updateSafely(
            failureKind = StudyFailureKind.CONTENT
        ) { facade.hideDecisionExplanation() }



    fun startLessonStudy(contentId: String, onComplete: () -> Unit = {}) =
        updateSafely(
            failureKind = StudyFailureKind.PREPARATION,
            preparingMessage = "Preparing lesson study session",
            onSuccess = onComplete
        ) { facade.startLessonStudy(contentId) }


    fun revealAnswer() = updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }

    fun completeFlowStage() {
        if (uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal) {
            updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }
            return
        }
        val (advanced, revealRequested) = flowCoordinator.completeCurrent(uiState)
        uiState = advanced
        if (revealRequested) {
            updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }
        }
    }

    fun reviewAgain() = review(ReviewRating.AGAIN)
    fun reviewHard() = review(ReviewRating.HARD)
    fun reviewGood() = review(ReviewRating.GOOD)
    fun reviewEasy() = review(ReviewRating.EASY)

    fun undoLatestReview() {
        updateSafely(StudyFailureKind.UNDO, onSuccess = { onStudyDataChanged?.invoke() }) {
            facade.undoLatestReview()
        }
    }

    private fun review(rating: ReviewRating) {
        updateSafely(
            StudyFailureKind.REVIEW_TRANSACTION,
            onSuccess = { onStudyDataChanged?.invoke() }
        ) {
            facade.review(rating)
        }
    }

    private fun loadSafely(
        previousState: StudyUiState = StudyUiState()
    ): StudyUiState =
        try {
            facade.load().copy(loadError = null, failureKind = null)
        } catch (exception: Exception) {
            previousState.copy(
                loadError = StudyFailureMessage.forStudyData(exception),
                failureKind = StudyFailureKind.SESSION_RECOVERY,
                message = "Study data needs attention.",
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

    private fun updateSafely(
        failureKind: StudyFailureKind,
        preparingMessage: String? = null,
        onSuccess: () -> Unit = {},
        operation: () -> StudyUiState
    ) {
        if (!actionInProgress) {
            actionInProgress = true
            uiState = uiState.copy(
                actionInProgress = true,
                message = preparingMessage ?: uiState.message,
                loadError = null
            )
            taskRunner.run(
                work = operation,
                onSuccess = { result ->
                    uiState = flowCoordinator.synchronize(
                        result.copy(loadError = null, failureKind = null, actionInProgress = false)
                    )
                    actionInProgress = false
                    onSuccess()
                },
                onFailure = { exception ->
                uiState = uiState.copy(
                    loadError = StudyFailureMessage.forStudyData(exception),
                    failureKind = failureKind,
                    message = "Study data needs attention.",
                    workspaceState = ReviewWorkspaceState.RecoverableFailure,
                    actionInProgress = false
                )
                actionInProgress = false
                }
            )
        }
    }
}
