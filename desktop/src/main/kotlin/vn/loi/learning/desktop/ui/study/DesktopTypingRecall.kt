package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.ExperienceSelectionProfile
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningexperience.UserChoiceExperienceStrategy

enum class DesktopExperienceMode {
    DEFAULT,
    TYPING
}

object DesktopExperienceSelection {
    fun isTypingAvailable(plan: LearningExperiencePlan?): Boolean =
        plan?.options?.orderedKinds?.contains(LearningExperienceKind.TYPING_RECALL) == true

    fun select(
        plan: LearningExperiencePlan?,
        mode: DesktopExperienceMode,
        rotationContext: ExperienceRotationContext
    ): ExperienceSelectionResult? {
        plan ?: return null
        if (mode == DesktopExperienceMode.TYPING && isTypingAvailable(plan)) {
            val options =
                ExperienceSelectionProfile.USER_SELECTABLE.project(plan.options)
            return ExperienceSelectionEngine(
                UserChoiceExperienceStrategy(LearningExperienceKind.TYPING_RECALL)
            ).select(
                ExperienceSelectionRequest(options, rotationContext.ordinal)
            )
        }
        val options =
            ExperienceSelectionProfile.AUTOMATIC.project(plan.options)
        return ExperienceSelectionEngine(RoundRobinExperienceStrategy()).select(
            ExperienceSelectionRequest(options, rotationContext.ordinal)
        )
    }
}

data class TypingRecallUiState(
    val itemId: String? = null,
    val input: String = "",
    val evaluation: TypingAnswerEvaluation? = null
)

data class TypingRecallSubmissionOutcome(
    val state: TypingRecallUiState,
    val shouldRevealAnswer: Boolean
)

object TypingRecallInteraction {
    fun initial(itemId: String?): TypingRecallUiState =
        TypingRecallUiState(itemId = itemId)

    fun updateInput(
        state: TypingRecallUiState,
        input: String
    ): TypingRecallUiState =
        state.copy(input = input, evaluation = null)

    fun submit(
        state: TypingRecallUiState,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        actionInProgress: Boolean = false
    ): TypingRecallSubmissionOutcome? {
        if (actionInProgress || state.evaluation?.isCompletedAttempt == true) return null
        val evaluation = evaluator.evaluate(prompt, state.input)
        return TypingRecallSubmissionOutcome(
            state = state.copy(evaluation = evaluation),
            shouldRevealAnswer = evaluation.isCompletedAttempt
        )
    }
}
