package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.domain.study.recall.StudyMode

class StudyViewModel(
    private val facade: StudyFacade,
    private val onStudyDataChanged: (() -> Unit)? = null,
    private val onContentEdited: (() -> Unit)? = null,
    private val taskRunner: DesktopTaskRunner = ImmediateDesktopTaskRunner
) {
    private var pendingContinuityDestination: StudyUiState? = null
    private val flowCoordinator = DesktopLearningFlowCoordinator()
    private var actionInProgress = false
    private val ratingFeedbackTokens = RatingFeedbackTokenGenerator()
    private var learnEntryHydrationToken: Long = 0L

    private data class PreparedTypingCompletion(
        val request: TypingRecallSuccessRequest,
        val sourceState: StudyUiState,
        val sourceItemId: String?,
        val activation: RatingActionFeedback?,
        var result: StudyUiState? = null,
        var failure: Exception? = null,
        var released: Boolean = false
    )

    private var preparedTypingCompletion: PreparedTypingCompletion? = null

    var uiState by mutableStateOf(StudyUiState())
        private set

    // Study is intentionally lazy. Loading it during ViewModel construction used to
    // make application startup pay for package-wide memory/history queries before F2
    // had even been opened.

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

    fun quickEditCurrentItem(
        draft: vn.loi.learning.desktop.ui.browser.ContentDraftEdits,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (actionInProgress) return
        val previous = uiState
        actionInProgress = true
        uiState = previous.copy(actionInProgress = true)
        taskRunner.run(
            work = { facade.quickEditCurrentItem(draft) },
            onSuccess = { refreshed ->
                uiState = flowCoordinator.synchronize(
                    refreshed.copy(actionInProgress = false, loadError = null, failureKind = null)
                )
                actionInProgress = false
                onContentEdited?.invoke()
                onSuccess()
            },
            onFailure = { exception ->
                uiState = if (exception is StudyQuickEditProjectionException) {
                    previous.copy(
                        canRevealAnswer = false,
                        canReview = false,
                        actionInProgress = false,
                        loadError = exception.message,
                        failureKind = StudyFailureKind.CONTENT,
                        workspaceState = ReviewWorkspaceState.RecoverableFailure
                    )
                } else {
                    previous.copy(actionInProgress = false)
                }
                actionInProgress = false
                onFailure(exception.message ?: "Không thể lưu thay đổi.")
            }
        )
    }

    fun enterLearnEntry() {
        // First paint is deliberately synchronous but lightweight: package scope +
        // active queue summary only. Do not block navigation on review history, rating
        // inventory, session recovery, or item hydration.
        val token = ++learnEntryHydrationToken
        val shell = facade.enterLearnEntryQuick(uiState)
        uiState = flowCoordinator.synchronize(shell)
        actionInProgress = false

        // Hydrate the expensive chooser projections on Dispatchers.IO. The token and
        // visibility guard prevent a late result from overwriting a Study session that
        // the user already started while hydration was running.
        taskRunner.run(
            work = { facade.hydrateLearnEntryDetails(shell) },
            onSuccess = { hydrated ->
                if (token == learnEntryHydrationToken && uiState.learnEntryChooserVisible) {
                    uiState = flowCoordinator.synchronize(
                        hydrated.copy(loadError = null, failureKind = null, actionInProgress = false)
                    )
                }
            },
            onFailure = { exception ->
                if (token == learnEntryHydrationToken && uiState.learnEntryChooserVisible) {
                    uiState = uiState.copy(
                        loadError = exception.message ?: StudyFailureMessage.forStudyData(exception),
                        failureKind = StudyFailureKind.PREPARATION,
                        message = "Không tải được đầy đủ lựa chọn ôn tập.",
                        actionInProgress = false
                    )
                }
            }
        )
    }

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

    fun confirmStartNewConfiguredSession() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing new study session"
    ) { facade.startNewConfiguredSession() }

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

    fun startLatestCompletedNewItemsReview() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing latest-session New review"
    ) { facade.startLatestCompletedNewItemsReview() }

    fun startAgainHardItemsReview() = updateSafely(
        failureKind = StudyFailureKind.PREPARATION,
        preparingMessage = "Preparing Again/Hard review"
    ) { facade.startAgainHardItemsReview() }

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
                    revealAnswer =
                        uiState.studyMode == StudyMode.LEARN_NEW ||
                            shouldRevealAnswerAfterIntroduction(
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
        if (uiState.studyMode == StudyMode.LEARN_NEW && uiState.canRevealAnswer) {
            updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }
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
        val transition = uiState.sessionContinuityTransition?.takeIf { it.token == token } ?: return
        when (transition.phase) {
            StudySessionTransitionPhase.RESULT_SHOWN -> {
                uiState = uiState.copy(
                    sessionContinuityTransition =
                        transition.copy(
                            phase = requireNotNull(nextStudySessionTransitionPhase(transition.phase))
                        )
                )
            }
            StudySessionTransitionPhase.EXITING_CURRENT -> {
                val destination = pendingContinuityDestination ?: return
                pendingContinuityDestination = null
                uiState = destination.copy(
                    actionInProgress = true,
                    ratingActionFeedback = uiState.ratingActionFeedback,
                    sessionContinuityTransition =
                        transition.copy(
                            phase = requireNotNull(nextStudySessionTransitionPhase(transition.phase))
                        )
                )
            }
            StudySessionTransitionPhase.ENTERING_NEXT -> {
                uiState = uiState.copy(
                    actionInProgress = false,
                    sessionContinuityTransition = null,
                    schedulerFeedback =
                        if (transition.destination == StudySessionTransitionDestination.NEXT_ITEM) null
                        else uiState.schedulerFeedback
                )
                actionInProgress = false
            }
        }
    }

    fun completeCorrectTypingRecall(request: TypingRecallSuccessRequest) {
        // Legacy/immediate path used by explicit retry UI after a failed automatic
        // rating. Normal successful Typing uses prepare + release below so backend
        // work overlaps with answer audio.
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

    fun prepareCorrectTypingRecall(request: TypingRecallSuccessRequest) {
        if (uiState.experienceRotationContext != request.context) return

        val existing = preparedTypingCompletion
        if (existing?.request == request) return
        if (actionInProgress) return

        val sourceState = uiState
        val sourceItemId = sourceState.currentLearningItemId
        val activation = ratingFeedbackTokens.activate(request.decision.rating)
        val prepared =
            PreparedTypingCompletion(
                request = request,
                sourceState = sourceState,
                sourceItemId = sourceItemId,
                activation = activation
            )

        preparedTypingCompletion = prepared
        actionInProgress = true
        uiState =
            sourceState.copy(
                actionInProgress = true,
                loadError = null,
                ratingActionFeedback = activation,
                sessionContinuityTransition = null
            )

        taskRunner.run(
            work = {
                val result = facade.completeCorrectTypingRecall(request)
                facade.projectContinuousReview(
                    if (result.loadError != null) {
                        result.copy(actionInProgress = false)
                    } else {
                        result.copy(
                            loadError = null,
                            failureKind = null,
                            actionInProgress = false
                        )
                    }
                )
            },
            onSuccess = { result ->
                if (preparedTypingCompletion === prepared) {
                    prepared.result = flowCoordinator.synchronize(result)
                    if (prepared.released) {
                        publishPreparedCorrectTypingRecall(prepared)
                    }
                }
            },
            onFailure = { exception ->
                if (preparedTypingCompletion === prepared) {
                    prepared.failure = exception
                    if (prepared.released) {
                        publishPreparedCorrectTypingRecall(prepared)
                    }
                }
            }
        )
    }

    fun releasePreparedCorrectTypingRecall(request: TypingRecallSuccessRequest) {
        val prepared = preparedTypingCompletion
        if (prepared == null || prepared.request != request) {
            // Defensive fallback: if preparation could not start, preserve the old
            // behavior rather than swallowing a valid completed attempt.
            if (!actionInProgress && uiState.experienceRotationContext == request.context) {
                completeCorrectTypingRecall(request)
            }
            return
        }

        prepared.released = true
        if (prepared.result != null || prepared.failure != null) {
            publishPreparedCorrectTypingRecall(prepared)
        }
    }

    private fun publishPreparedCorrectTypingRecall(prepared: PreparedTypingCompletion) {
        if (preparedTypingCompletion !== prepared) return

        val failure = prepared.failure
        if (failure != null) {
            preparedTypingCompletion = null
            pendingContinuityDestination = null
            actionInProgress = false
            uiState =
                prepared.sourceState.copy(
                    loadError = failure.message ?: StudyFailureMessage.forStudyData(failure),
                    failureKind = StudyFailureKind.REVIEW_TRANSACTION,
                    message = "Rating was not saved. Continue to retry.",
                    workspaceState = ReviewWorkspaceState.RecoverableFailure,
                    actionInProgress = false,
                    ratingActionFeedback = null,
                    sessionContinuityTransition = null
                )
            return
        }

        val committedState = prepared.result ?: return
        val transition =
            if (
                prepared.activation != null &&
                prepared.sourceItemId != null &&
                committedState.loadError == null &&
                !committedState.sessionCompleted &&
                committedState.currentLearningItemId != prepared.sourceItemId
            ) {
                createStudySessionContinuityTransition(
                    prepared.activation,
                    prepared.sourceItemId,
                    committedState
                )
            } else {
                null
            }

        preparedTypingCompletion = null
        uiState =
            if (transition == null) {
                committedState.copy(
                    actionInProgress = false,
                    ratingActionFeedback = prepared.activation?.let(::confirmRatingFeedback),
                    sessionContinuityTransition = null
                )
            } else {
                pendingContinuityDestination = committedState
                prepared.sourceState.copy(
                    actionInProgress = true,
                    ratingActionFeedback = confirmRatingFeedback(requireNotNull(prepared.activation)),
                    sessionContinuityTransition = transition
                )
            }

        actionInProgress = transition != null
        onStudyDataChanged?.invoke()
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

    fun submitMultipleChoice(optionId: String) =
        updateSafely(
            StudyFailureKind.REVIEW_TRANSACTION,
            onSuccess = { onStudyDataChanged?.invoke() }
        ) { facade.submitMultipleChoice(optionId) }

    fun submitListening(rawInput: String) =
        updateSafely(
            failureKind = StudyFailureKind.REVIEW_TRANSACTION,
            preparingMessage = "Submitting Listening recall"
        ) { facade.submitListening(rawInput) }

    fun submitImageRecall(rawInput: String) =
        updateSafely(
            failureKind = StudyFailureKind.REVIEW_TRANSACTION,
            preparingMessage = "Submitting Image recall"
        ) { facade.submitImageRecall(rawInput) }

    fun submitExampleCompletion(rawInput: String) =
        updateSafely(
            failureKind = StudyFailureKind.REVIEW_TRANSACTION,
            preparingMessage = "Submitting Example Completion"
        ) { facade.submitExampleCompletion(rawInput) }

    fun undoLatestReview() {
        updateSafely(StudyFailureKind.UNDO, onSuccess = { onStudyDataChanged?.invoke() }) {
            facade.undoLatestReview()
        }
    }

    fun overrideCurrentPracticeRating(rating: ReviewRating) {
        updateSafely(StudyFailureKind.REVIEW_TRANSACTION, onSuccess = { onStudyDataChanged?.invoke() }) {
            facade.overrideCurrentPracticeRating(rating)
        }
    }

    fun leavePractice() {
        updateSafely(StudyFailureKind.SESSION_RECOVERY) { facade.leavePractice() }
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
        if (uiState.learnEntryChooserVisible) {
            learnEntryHydrationToken++
        }
        if (actionInProgress && pendingContinuityDestination != null && ratingFeedback == null) {
            uiState = requireNotNull(pendingContinuityDestination).copy(
                actionInProgress = false,
                sessionContinuityTransition = null
            )
            pendingContinuityDestination = null
            actionInProgress = false
        }
        if (!actionInProgress) {
            actionInProgress = true
            pendingContinuityDestination = null
            val sourceState = uiState
            val sourceItemId = sourceState.currentLearningItemId
            val activation = ratingFeedback
                ?.let(ratingFeedbackTokens::activate)
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
                    val committedState = flowCoordinator.synchronize(
                        facade.projectContinuousReview(stateToUse)
                    )
                    val transition =
                        if (
                            activation != null &&
                            sourceItemId != null &&
                            stateToUse.loadError == null &&
                            !stateToUse.sessionCompleted &&
                            stateToUse.currentLearningItemId != sourceItemId
                        ) {
                            createStudySessionContinuityTransition(
                                activation,
                                sourceItemId,
                                stateToUse
                            )
                        } else {
                            null
                        }
                    uiState = if (transition == null) {
                        committedState.copy(
                            ratingActionFeedback = activation?.let(::confirmRatingFeedback),
                            sessionContinuityTransition = null
                        )
                    } else {
                        pendingContinuityDestination = committedState
                        sourceState.copy(
                            actionInProgress = true,
                            ratingActionFeedback = confirmRatingFeedback(requireNotNull(activation)),
                            sessionContinuityTransition = transition
                        )
                    }
                    actionInProgress = transition != null
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
                pendingContinuityDestination = null
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
