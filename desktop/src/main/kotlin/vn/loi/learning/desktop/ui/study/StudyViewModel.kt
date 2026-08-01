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
    private val ratingFeedbackTokens = RatingFeedbackTokenGenerator()

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
                    facade.refreshHeaderStatistics(
                        facade.projectContinuousReview(loaded).copy(
                            loadError = null, failureKind = null, actionInProgress = false
                        ),
                        previous.headerStatistics
                    )
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

    fun enterLearnEntry() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing learning choices"
    ) { facade.enterLearnEntry() }

    fun dismissCompletionPresentation() {
        uiState = flowCoordinator.synchronize(
            facade.dismissCompletionPresentation().copy(loadError = null, failureKind = null)
        )
    }

    fun refreshHeaderStatistics() {
        val previous = uiState.headerStatistics
        taskRunner.run(
            work = { facade.refreshHeaderStatistics(uiState, previous) },
            onSuccess = { uiState = it },
            onFailure = {
                uiState = uiState.copy(
                    headerStatistics = StudyHeaderStatisticsState.Unavailable(previous.lastKnownGoodForViewModel())
                )
            }
        )
    }

    fun startStudy() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing study session"
    ) { facade.continueGeneralStudyAfterCompletion() }

    fun continueLearningSelection() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing selected learning session"
    ) { facade.continueSelectedLearning() }

    fun enableContinuousReview() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Enabling Continuous Review"
    ) { facade.enableContinuousReview() }

    fun disableContinuousReview() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Disabling Continuous Review"
    ) { facade.disableContinuousReview() }

    fun replayCompletedStudySession() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing completed-session review"
    ) { facade.replayCompletedStudySession() }

    fun replayLatestCompletedStudySession() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing latest completed-session review"
    ) { facade.replayLatestCompletedStudySession() }

    fun startLearnedItemsReview() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing learned-items review"
    ) { facade.startLearnedItemsReview() }

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

    fun startLessonStudy(
        request: vn.loi.learning.application.session.StartPackageLessonStudyRequest,
        onComplete: () -> Unit = {}
    ) = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing package lesson study session",
        onSuccess = onComplete
    ) { facade.startLessonStudy(request) }


    fun revealAnswer() = updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }

    fun revealTypingRecall(request: TypingRecallRevealRequest) =
        updateSafely(StudyFailureKind.CONTENT) { facade.revealTypingRecall(request) }

    fun completeFlowStage() {
        if (uiState.contentIntroductionState == ContentIntroductionState.REQUIRED) {
            updateSafely(StudyFailureKind.CONTENT) {
                facade.completeContentIntroduction(
                    revealAnswer = shouldRevealAnswerAfterIntroduction(
                        primaryKind = uiState.learningFlowSelection?.selectedKind,
                        experienceCount =
                            uiState.learningFlowDefinition
                                ?.stages
                                ?.filterIsInstance<LearningFlowStage.Experience>()
                                ?.size
                                ?: 0
                    )
                )
            }
            return
        }
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

    fun consumeRatingActionFeedback(token: Long) {
        uiState = uiState.consumeRatingActionFeedback(token)
    }

    fun advanceSessionContinuity(token: Long) {
        uiState = uiState.advanceSessionContinuity(token)
    }

    fun completeCorrectTypingRecall(request: TypingRecallSuccessRequest) {
        if (uiState.experienceRotationContext != request.context) return
        updateSafely(
            StudyFailureKind.REVIEW_TRANSACTION,
            ratingFeedback = request.decision.rating,
            onSuccess = { onStudyDataChanged?.invoke() }
        ) {
            facade.completeCorrectTypingRecall(request) { revealed ->
                flowCoordinator.synchronize(revealed)
            }
        }
    }

    fun completeRevealedTypingRecallAsAgain(request: TypingRecallRevealRequest) {
        if (uiState.experienceRotationContext != request.context) return
        updateSafely(
            StudyFailureKind.REVIEW_TRANSACTION,
            ratingFeedback = ReviewRating.AGAIN,
            onSuccess = { onStudyDataChanged?.invoke() }
        ) {
            facade.completeRevealedTypingRecallAsAgain(request)
        }
    }

    fun undoLatestReview() {
        updateSafely(StudyFailureKind.UNDO, onSuccess = { onStudyDataChanged?.invoke() }) {
            facade.undoLatestReview()
        }
    }

    private fun review(rating: ReviewRating) {
        updateSafely(
            StudyFailureKind.REVIEW_TRANSACTION,
            ratingFeedback = rating,
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
        ratingFeedback: ReviewRating? = null,
        onSuccess: () -> Unit = {},
        operation: () -> StudyUiState
    ) {
        if (!actionInProgress) {
            actionInProgress = true
            val sourceItemId = uiState.currentLearningItemId
            val activation = ratingFeedback?.let(ratingFeedbackTokens::activate)
            uiState = uiState.copy(
                actionInProgress = true,
                message = preparingMessage ?: uiState.message,
                loadError = null,
                ratingActionFeedback = activation,
                sessionContinuityTransition = null
            )
            taskRunner.run(
                work = operation,
                onSuccess = { result ->
                    val stateToUse = if (result.loadError != null) {
                        result.copy(actionInProgress = false)
                    } else {
                        result.copy(loadError = null, failureKind = null, actionInProgress = false)
                    }
                    uiState = flowCoordinator.synchronize(
                        facade.refreshHeaderStatistics(
                            facade.projectContinuousReview(stateToUse), uiState.headerStatistics
                        )
                    ).copy(
                        ratingActionFeedback = activation?.let(::confirmRatingFeedback),
                        sessionContinuityTransition =
                            if (
                                activation != null &&
                                sourceItemId != null &&
                                stateToUse.loadError == null
                            ) {
                                createStudySessionContinuityTransition(
                                    activation,
                                    sourceItemId,
                                    stateToUse
                                )
                            } else {
                                null
                            }
                    )
                    actionInProgress = false
                    onSuccess()
                },
                onFailure = { exception ->
                val recoverableTypingState =
                    if (failureKind == StudyFailureKind.REVIEW_TRANSACTION) {
                        runCatching(facade::load).getOrNull()
                    } else {
                        null
                    }
                uiState =
                    if (
                        recoverableTypingState != null &&
                        recoverableTypingState.typingRatingMode in
                            setOf(
                                TypingRatingMode.AUTOMATIC_PENDING,
                                TypingRatingMode.FORCED_AGAIN
                            )
                    ) {
                        flowCoordinator.synchronize(
                            recoverableTypingState.copy(
                                loadError = null,
                                failureKind = failureKind,
                                message = "Rating was not saved. Continue to retry.",
                                workspaceState = ReviewWorkspaceState.AnswerRevealed,
                                actionInProgress = false,
                                ratingActionFeedback = null
                            )
                        )
                    } else {
                        uiState.copy(
                            loadError = exception.message ?: StudyFailureMessage.forStudyData(exception),
                            failureKind = failureKind,
                            message = "Study data needs attention.",
                            workspaceState = ReviewWorkspaceState.RecoverableFailure,
                            actionInProgress = false,
                            ratingActionFeedback = null
                        )
                    }
                actionInProgress = false
                }
            )
        }
    }
}

private fun StudyHeaderStatisticsState.lastKnownGoodForViewModel() =
    when (this) {
        is StudyHeaderStatisticsState.Available -> value
        is StudyHeaderStatisticsState.Unavailable -> lastKnownGood
        StudyHeaderStatisticsState.Loading -> null
    }
