package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningflow.LearningFlowStage

internal enum class StudyActionDockMode {
    INTRODUCTION,
    ANSWER_ACTIONS,
    FRONT_CONTEXT,
    REVIEW_CONTEXT,
    IDLE,
    HIDDEN
}

internal fun shouldPresentNewItemDiscoveryFront(uiState: StudyUiState): Boolean =
    uiState.hasActiveSession &&
        !uiState.canReview &&
        uiState.canRevealAnswer &&
        uiState.currentItemReviewContext?.origin ==
            vn.loi.learning.domain.study.session.model.SessionItemOrigin.NEW &&
        uiState.learningFlowSelection?.selectedKind != LearningExperienceKind.TYPING_RECALL

internal fun resolveStudyActionDockMode(uiState: StudyUiState): StudyActionDockMode =
    when {
        uiState.sessionCompleted || uiState.loadError != null -> StudyActionDockMode.HIDDEN
        uiState.contentIntroductionState == ContentIntroductionState.REQUIRED ->
            StudyActionDockMode.INTRODUCTION
        shouldPresentNewItemDiscoveryFront(uiState) ->
            StudyActionDockMode.FRONT_CONTEXT
        uiState.evaluativeRatingAvailability ==
            vn.loi.learning.application.session.EvaluativeRatingAvailability.AVAILABLE ->
            StudyActionDockMode.ANSWER_ACTIONS
        uiState.canReview && uiState.learningFlowProgress?.isRatingReady == true ->
            StudyActionDockMode.ANSWER_ACTIONS
        uiState.canRevealAnswer &&
            (
                uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal ||
                    (
                        uiState.learningFlowCurrentStage is LearningFlowStage.Experience &&
                            uiState.learningFlowCurrentStage.selection.selectedKind !=
                            LearningExperienceKind.TYPING_RECALL
                    )
            ) -> StudyActionDockMode.FRONT_CONTEXT
        !uiState.canReview &&
            uiState.currentItemReviewContext?.origin ==
                vn.loi.learning.domain.study.session.model.SessionItemOrigin.REVIEW ->
            StudyActionDockMode.REVIEW_CONTEXT
        !uiState.hasActiveSession && resolveStudyIdlePresentation(uiState) != null ->
            StudyActionDockMode.IDLE
        else -> StudyActionDockMode.HIDDEN
    }
