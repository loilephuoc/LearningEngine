package vn.loi.learning.application.learningflow

sealed interface LearningFlowTransition {
    val state: LearningFlowState

    data class StageAdvanced(
        override val state: LearningFlowState
    ) : LearningFlowTransition

    data class AnswerRevealRequested(
        override val state: LearningFlowState
    ) : LearningFlowTransition

    data class RatingReady(
        override val state: LearningFlowState
    ) : LearningFlowTransition

    data class Rejected(
        override val state: LearningFlowState,
        val reason: String
    ) : LearningFlowTransition
}

class LearningFlowController {
    fun initialize(definition: LearningFlowDefinition): LearningFlowState =
        LearningFlowState(
            flowId = definition.id,
            sessionId = definition.context.sessionId,
            learningItemId = definition.context.learningItemId,
            currentStageIndex = 0,
            completedStageIds = emptySet()
        )

    fun initializeRevealed(definition: LearningFlowDefinition): LearningFlowState {
        val ratingIndex = definition.stages.indexOfFirst { it is LearningFlowStage.RatingReady }
        return LearningFlowState(
            definition.id,
            definition.context.sessionId,
            definition.context.learningItemId,
            ratingIndex,
            definition.stages.take(ratingIndex).mapTo(linkedSetOf()) { it.id }
        )
    }

    fun current(
        definition: LearningFlowDefinition,
        state: LearningFlowState
    ): LearningFlowStage {
        validate(definition, state)
        return definition.stages[state.currentStageIndex]
    }

    fun completeCurrent(
        definition: LearningFlowDefinition,
        state: LearningFlowState,
        expectedStageId: LearningFlowStageId
    ): LearningFlowTransition {
        val current = current(definition, state)
        if (current.id != expectedStageId) {
            return LearningFlowTransition.Rejected(state, "Stale flow stage.")
        }
        if (current !is LearningFlowStage.Experience) {
            return LearningFlowTransition.Rejected(state, "Current stage is not completable.")
        }
        val advanced =
            state.copy(
                currentStageIndex = state.currentStageIndex + 1,
                completedStageIds = state.completedStageIds + current.id
            )
        return if (current(definition, advanced) is LearningFlowStage.AnswerReveal) {
            LearningFlowTransition.AnswerRevealRequested(advanced)
        } else {
            LearningFlowTransition.StageAdvanced(advanced)
        }
    }

    fun confirmAnswerRevealed(
        definition: LearningFlowDefinition,
        state: LearningFlowState
    ): LearningFlowTransition {
        val current = current(definition, state)
        if (current is LearningFlowStage.RatingReady) {
            return LearningFlowTransition.RatingReady(state)
        }
        if (current !is LearningFlowStage.AnswerReveal) {
            return LearningFlowTransition.Rejected(state, "Answer reveal was not requested.")
        }
        val advanced =
            state.copy(
                currentStageIndex = state.currentStageIndex + 1,
                completedStageIds = state.completedStageIds + current.id
            )
        return LearningFlowTransition.RatingReady(advanced)
    }

    fun progress(
        definition: LearningFlowDefinition,
        state: LearningFlowState
    ): LearningFlowProgress {
        val current = current(definition, state)
        val experiences = definition.stages.filterIsInstance<LearningFlowStage.Experience>()
        val completed =
            experiences.count { it.id in state.completedStageIds }
        val number =
            (current as? LearningFlowStage.Experience)
                ?.let { experiences.indexOf(it) + 1 }
        return LearningFlowProgress(
            completedExperienceCount = completed,
            currentExperienceNumber = number,
            totalExperienceCount = experiences.size,
            isRevealPending = current is LearningFlowStage.AnswerReveal,
            isRatingReady = current is LearningFlowStage.RatingReady
        )
    }

    private fun validate(
        definition: LearningFlowDefinition,
        state: LearningFlowState
    ) {
        require(state.flowId == definition.id)
        require(state.sessionId == definition.context.sessionId)
        require(state.learningItemId == definition.context.learningItemId)
        require(state.currentStageIndex in definition.stages.indices)
        require(state.completedStageIds.all { id -> definition.stages.any { it.id == id } })
        require(
            state.completedStageIds ==
                definition.stages.take(state.currentStageIndex).mapTo(linkedSetOf()) { it.id }
        ) {
            "Completed flow stages must form the ordered prefix."
        }
    }
}
