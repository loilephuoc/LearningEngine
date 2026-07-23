package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionDecision
import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.ExperienceSelectionStrategy
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

enum class DesktopExperienceMode {
    DEFAULT,
    TYPING
}

object DesktopExperienceSelection {
    private const val COMPATIBILITY_ORDINAL = 0L

    fun isTypingAvailable(plan: LearningExperiencePlan?): Boolean =
        plan?.options?.orderedKinds?.contains(LearningExperienceKind.TYPING_RECALL) == true

    fun select(
        plan: LearningExperiencePlan?,
        mode: DesktopExperienceMode
    ): ExperienceSelectionResult? {
        plan ?: return null
        if (mode == DesktopExperienceMode.TYPING && isTypingAvailable(plan)) {
            val strategy = ExperienceSelectionStrategy {
                ExperienceSelectionDecision(
                    selectedIndex =
                        it.options.orderedKinds.indexOf(LearningExperienceKind.TYPING_RECALL),
                    reason = ExperienceSelectionReason.USER_CHOICE
                )
            }
            return ExperienceSelectionEngine(strategy).select(
                ExperienceSelectionRequest(plan.options, COMPATIBILITY_ORDINAL)
            )
        }
        return ExperienceSelectionEngine(RoundRobinExperienceStrategy()).select(
            ExperienceSelectionRequest(plan.options, COMPATIBILITY_ORDINAL)
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
