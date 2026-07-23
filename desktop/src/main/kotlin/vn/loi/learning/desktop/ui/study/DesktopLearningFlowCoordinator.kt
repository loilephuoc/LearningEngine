package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperiencePolicy
import vn.loi.learning.application.learningflow.LearningFlowController
import vn.loi.learning.application.learningflow.LearningFlowDefinition
import vn.loi.learning.application.learningflow.LearningFlowPlanner
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningflow.LearningFlowStageId
import vn.loi.learning.application.learningflow.LearningFlowState
import vn.loi.learning.application.learningflow.LearningFlowTransition

class DesktopLearningFlowCoordinator(
    private val planner: LearningFlowPlanner = LearningFlowPlanner(),
    private val controller: LearningFlowController = LearningFlowController(),
    private val experiencePolicy: LearningExperiencePolicy = LearningExperiencePolicy()
) {
    private var definition: LearningFlowDefinition? = null
    private var state: LearningFlowState? = null

    fun synchronize(uiState: StudyUiState): StudyUiState {
        val rotation = uiState.experienceRotationContext
        val content = uiState.learningContent
        if (!uiState.hasActiveSession || rotation == null || content == null) {
            definition = null
            state = null
            return uiState.withoutFlow()
        }
        val plan =
            requireNotNull(
                experiencePolicy.plan(
                    content,
                    LearningExperienceContext(answerRevealed = uiState.canReview)
                )
            )
        if (definition?.context != rotation) {
            definition = planner.plan(plan, rotation)
            state =
                if (uiState.canReview) {
                    controller.initializeRevealed(requireNotNull(definition))
                } else {
                    controller.initialize(requireNotNull(definition))
                }
        } else if (uiState.canReview) {
            val currentDefinition = requireNotNull(definition)
            val currentState = requireNotNull(state)
            state =
                when (
                    val transition =
                        controller.confirmAnswerRevealed(currentDefinition, currentState)
                ) {
                    is LearningFlowTransition.RatingReady -> transition.state
                    is LearningFlowTransition.Rejected -> currentState
                    else -> transition.state
                }
        }
        return project(uiState, plan)
    }

    fun completeCurrent(uiState: StudyUiState): Pair<StudyUiState, Boolean> {
        if (uiState.actionInProgress) return uiState to false
        val currentDefinition = definition ?: return uiState to false
        val currentState = state ?: return uiState to false
        val currentStage = controller.current(currentDefinition, currentState)
        val transition =
            controller.completeCurrent(
                currentDefinition,
                currentState,
                currentStage.id
            )
        state = transition.state
        return synchronizeProjection(uiState) to
            (transition is LearningFlowTransition.AnswerRevealRequested)
    }

    private fun synchronizeProjection(uiState: StudyUiState): StudyUiState {
        val plan =
            experiencePolicy.plan(
                uiState.learningContent,
                LearningExperienceContext(answerRevealed = uiState.canReview)
            ) ?: return uiState
        return project(uiState, plan)
    }

    private fun project(
        uiState: StudyUiState,
        plan: vn.loi.learning.application.learningexperience.LearningExperiencePlan
    ): StudyUiState {
        val currentDefinition = requireNotNull(definition)
        val currentState = requireNotNull(state)
        val current = controller.current(currentDefinition, currentState)
        val visibleSelection =
            (current as? LearningFlowStage.Experience)?.selection
                ?: currentDefinition.stages
                    .filterIsInstance<LearningFlowStage.Experience>()
                    .first()
                    .selection
        return uiState.copy(
            learningFlowDefinition = currentDefinition,
            learningFlowState = currentState,
            learningFlowProgress = controller.progress(currentDefinition, currentState),
            learningFlowCurrentStage = current,
            learningFlowSelection = visibleSelection,
            learningExperiencePlan = plan
        )
    }

    private fun StudyUiState.withoutFlow() =
        copy(
            learningFlowDefinition = null,
            learningFlowState = null,
            learningFlowProgress = null,
            learningFlowCurrentStage = null,
            learningFlowSelection = null,
            learningExperiencePlan = null
        )
}
