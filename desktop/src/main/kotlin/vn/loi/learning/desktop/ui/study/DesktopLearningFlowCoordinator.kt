package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningflow.LearningFlowController
import vn.loi.learning.application.learningflow.LearningFlowDefinition
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningflow.LearningFlowState
import vn.loi.learning.application.learningflow.LearningFlowTransition
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.domain.study.recall.StudyMode

class DesktopLearningFlowCoordinator(
    private val productBrainPlanner: ProductBrainPlanner = ProductBrainPlanner(),
    private val controller: LearningFlowController = LearningFlowController()
) {
    private var definition: LearningFlowDefinition? = null
    private var state: LearningFlowState? = null

    fun synchronize(uiState: StudyUiState): StudyUiState {
        if (uiState.studyMode == StudyMode.LEARN_NEW) {
            definition = null
            state = null
            return uiState.withoutFlow()
        }
        val rotation = uiState.experienceRotationContext
        val content = uiState.learningContent
        if (!uiState.hasActiveSession || rotation == null || content == null) {
            definition = null
            state = null
            return uiState.withoutFlow()
        }
        val intent = requireNotNull(
            DesktopRecallStudyModeResolver.resolve(
                productBrainPlanner,
                content,
                LearningExperienceContext(
                    answerRevealed = uiState.canReview,
                    stage = uiState.learningStage
                ),
                rotation
            )
        )
        val plan = intent.experiencePlan
        if (definition?.context != rotation) {
            definition = intent.definition
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
                when (controller.current(currentDefinition, currentState)) {
                    is LearningFlowStage.RatingReady -> currentState
                    is LearningFlowStage.AnswerReveal ->
                        controller.confirmAnswerRevealed(currentDefinition, currentState).state
                    is LearningFlowStage.Experience ->
                        controller.initializeRevealed(currentDefinition)
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
        val content = uiState.learningContent ?: return uiState
        val rotation = uiState.experienceRotationContext ?: return uiState
        val intent = DesktopRecallStudyModeResolver.resolve(
            productBrainPlanner,
            content,
            LearningExperienceContext(
                answerRevealed = uiState.canReview,
                stage = uiState.learningStage
            ),
            rotation
        ) ?: return uiState
        return project(uiState, intent.experiencePlan)
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
