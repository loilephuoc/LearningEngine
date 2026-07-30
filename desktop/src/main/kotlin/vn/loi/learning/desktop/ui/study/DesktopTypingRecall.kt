package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningexperience.ExperienceRotationContext

data class TypingRecallSuccessRequest(
    val context: ExperienceRotationContext,
    val inputRevision: Long
)

data class TypingRecallUiState(
    val itemId: String? = null,
    val input: String = "",
    val selection: TextRange = TextRange(input.length),
    val composition: TextRange? = null,
    val liveEvaluation: TypingAnswerEvaluation? = null,
    val revealEvaluation: TypingAnswerEvaluation? = null,
    val explicitIncorrectFeedback: Boolean = false,
    val automaticSuccessRequested: Boolean = false,
    val successInProgress: Boolean = false,
    val inputRevision: Long = 0
) {
    val textFieldValue: TextFieldValue
        get() = TextFieldValue(input, selection, composition)
}

data class TypingRecallSubmissionOutcome(
    val state: TypingRecallUiState,
    val shouldRevealAnswer: Boolean
)

data class TypingInputPresentation(
    val minimumHeightDp: Int,
    val maximumHeightDp: Int,
    val fontSizeSp: Int,
    val revealWidthFraction: Float
)

data class TypingSuccessOverlayPresentation(
    val answerFontSizeSp: Int,
    val answerLineHeightSp: Int,
    val horizontalMarginDp: Int
)

internal object TypingPresentationResolver {
    fun input(viewportClass: StudyViewportClass): TypingInputPresentation =
        when (viewportClass) {
            StudyViewportClass.WIDE -> TypingInputPresentation(88, 176, 24, 0.70f)
            StudyViewportClass.STANDARD -> TypingInputPresentation(84, 168, 22, 0.82f)
            StudyViewportClass.COMPACT -> TypingInputPresentation(80, 160, 20, 1f)
        }

    fun successOverlay(
        viewportClass: StudyViewportClass,
        canonicalAnswer: String
    ): TypingSuccessOverlayPresentation {
        val longAnswer = canonicalAnswer.length > 24
        return when (viewportClass) {
            StudyViewportClass.WIDE ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 30 else 44,
                    if (longAnswer) 38 else 52,
                    48
                )
            StudyViewportClass.STANDARD ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 27 else 38,
                    if (longAnswer) 35 else 46,
                    32
                )
            StudyViewportClass.COMPACT ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 24 else 30,
                    if (longAnswer) 32 else 38,
                    16
                )
        }
    }
}

internal fun shouldRequestTypingInputFocus(
    enabled: Boolean,
    successInProgress: Boolean
): Boolean = enabled && !successInProgress

object TypingRecallInteraction {
    fun initial(itemId: String?): TypingRecallUiState =
        TypingRecallUiState(itemId = itemId)

    fun updateInput(
        state: TypingRecallUiState,
        value: TextFieldValue,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator
    ): TypingRecallUiState {
        val rawChanged = value.text != state.input
        val compositionChanged = value.composition != state.composition
        if (!rawChanged && !compositionChanged) {
            return state.copy(selection = value.selection)
        }
        val evaluation = evaluator.evaluate(prompt, value.text)
        return state.copy(
            input = value.text,
            selection = value.selection,
            composition = value.composition,
            liveEvaluation = evaluation,
            revealEvaluation = null,
            explicitIncorrectFeedback = false,
            automaticSuccessRequested =
                evaluation.status == TypingAnswerEvaluationStatus.CORRECT &&
                    value.composition == null,
            successInProgress = false,
            inputRevision = state.inputRevision + 1
        )
    }

    fun updateInput(
        state: TypingRecallUiState,
        input: String,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator
    ): TypingRecallUiState =
        updateInput(
            state,
            TextFieldValue(input, selection = TextRange(input.length)),
            prompt,
            evaluator
        )

    fun submit(
        state: TypingRecallUiState,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        actionInProgress: Boolean = false
    ): TypingRecallSubmissionOutcome? {
        if (actionInProgress || state.successInProgress) return null
        val evaluation = evaluator.evaluate(prompt, state.input)
        return TypingRecallSubmissionOutcome(
            state =
                state.copy(
                    liveEvaluation = evaluation,
                    explicitIncorrectFeedback =
                        evaluation.status == TypingAnswerEvaluationStatus.INCORRECT,
                    automaticSuccessRequested =
                        evaluation.status == TypingAnswerEvaluationStatus.CORRECT &&
                            state.composition == null,
                    inputRevision =
                        if (state.automaticSuccessRequested) state.inputRevision
                        else state.inputRevision + 1
                ),
            shouldRevealAnswer = false
        )
    }

    fun confirmRealtimeSuccess(
        state: TypingRecallUiState,
        expectedRevision: Long
    ): TypingRecallUiState =
        if (
            state.inputRevision == expectedRevision &&
            state.automaticSuccessRequested &&
            state.composition == null &&
            state.liveEvaluation?.status == TypingAnswerEvaluationStatus.CORRECT
        ) {
            state.copy(successInProgress = true)
        } else {
            state
        }

    fun cancelAutomaticSuccess(state: TypingRecallUiState): TypingRecallUiState =
        state.copy(
            automaticSuccessRequested = false,
            successInProgress = false,
            inputRevision = state.inputRevision + 1
        )

    fun evaluateForReveal(
        state: TypingRecallUiState,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator
    ): TypingRecallUiState {
        val evaluation = evaluator.evaluate(prompt, state.input)
        return state.copy(
            liveEvaluation = evaluation,
            revealEvaluation = evaluation,
            explicitIncorrectFeedback = false,
            automaticSuccessRequested = false,
            successInProgress = false,
            inputRevision = state.inputRevision + 1
        )
    }
}
