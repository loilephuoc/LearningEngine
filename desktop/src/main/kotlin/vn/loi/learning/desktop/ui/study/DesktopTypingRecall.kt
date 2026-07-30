package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

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
        if (actionInProgress || state.evaluation?.isCorrect == true) return null
        val evaluation = evaluator.evaluate(prompt, state.input)
        return TypingRecallSubmissionOutcome(
            state = state.copy(evaluation = evaluation),
            shouldRevealAnswer = evaluation.isCorrect
        )
    }
}
