package vn.loi.learning.android.study.modes

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import vn.loi.learning.android.study.*
import vn.loi.learning.android.study.components.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.isReducedMotionEnabled
import vn.loi.learning.android.study.partOfSpeechPresentation
import vn.loi.learning.android.study.components.PartOfSpeechBadge
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.recall.RecallOutcome

@Composable
internal fun ImageRecallStudyStage(
    state: AndroidStudyState.ImageRecall,
    activeRole: AudioRole?,
    baseDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    feedbackContent: @Composable () -> Unit,
    onCompactSuccessRendered: () -> Unit = {},
    onStageTap: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentInput by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
    var inputFocusGeneration by remember(state.plan.planId.value) { mutableIntStateOf(0) }
    LaunchedEffect(state.answer) { currentInput = state.answer }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val feedbackVisible = state.completed || state.revealed
    val liveEvaluation = remember(currentInput, state.plan.answerContract.canonicalAnswer) {
        TypingAnswerEvaluator().evaluate(
            TypingRecallPrompt(state.plan.answerContract.canonicalAnswer),
            currentInput
        )
    }
    val density = resolveImageRecallDensity(baseDensity, imeVisible, 14, feedbackVisible)
    val inputState = resolveStudyInputVisualState(
        enabled = !feedbackVisible && !state.imageUnavailable,
        focused = !feedbackVisible,
        typingEvaluation = liveEvaluation.status,
        outcome = state.outcome
    )
    LaunchedEffect(inputFocusGeneration) {
        if (inputFocusGeneration > 0) {
            withFrameNanos { }
            keyboardController?.show()
        }
    }

    val submitImageRecall: (String) -> Unit = { answer ->
        onEvent(AndroidStudyEvent.Submit(answer))
    }

    if (state.completionPending && state.outcome == RecallOutcome.CORRECT && !state.revealed) {
        TypedAnswerStageFrame(
            modifier = modifier,
            feedback = StudyFeedbackVisualState.CORRECT,
            density = density,
            fillViewport = true
        ) {
            TypedCompactSuccess(
                requireNotNull(typedResultPresentation(state)), density,
                availableMediaHeightDp, onOpenFullscreenImage,
                onRendered = onCompactSuccessRendered
            )
        }
        return
    }

    if (state.completed || state.revealed) {
        TypedAnswerStageFrame(
            modifier = modifier,
            feedback = StudyFeedbackVisualState.INCORRECT,
            density = StudyContentDensity.DENSE,
            fillViewport = true,
            onStageTap = onStageTap,
            onSwipeNext = onSwipeNext
        ) {
            feedbackContent()
        }
        return
    }

    val reducedMotion = isReducedMotionEnabled()
    val imageScale by animateFloatAsState(
        1f,
        tween(imageRecallMediaMotionMillis(reducedMotion)),
        label = "image recall confirmation"
    )
    StudyStageCard(modifier = modifier.fillMaxWidth(), feedback = inputState.feedbackVisual()) {
        Column(
            Modifier.fillMaxWidth().padding(
                if (imeVisible || density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (imeVisible) StudySpacing.micro
                else if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section
            )
        ) {
            partOfSpeechPresentation(state.partOfSpeech)?.let { PartOfSpeechBadge(it) }
            Box(
                Modifier.fillMaxWidth().animateContentSize(tween(imageRecallMediaMotionMillis(reducedMotion)))
                    .graphicsLayer { scaleX = imageScale; scaleY = imageScale }
            ) {
                StudyMedia(
                    state.resolvedImage,
                    imageRecallMediaRole(imeVisible, false),
                    density,
                    availableMediaHeightDp,
                    onOpenFullscreenImage
                )
            }
            if (state.imageUnavailable) StudyUnavailableNotice("Recall image unavailable")
            StudyAnswerInput(
                "${state.plan.planId.value}:$inputFocusGeneration",
                state.answer,
                !state.imageUnavailable,
                liveEvaluation.status == TypingAnswerEvaluationStatus.INCORRECT,
                label = "Type your answer",
                accessibilityLabel = "Nhập từ tiếng Anh được gợi nhớ bởi hình ảnh",
                feedback = inputState.feedbackVisual(),
                onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
                onSubmit = submitImageRecall
            )
            TypingInputActions(
                answer = currentInput,
                visualState = inputState,
                showRetry = liveEvaluation.status == TypingAnswerEvaluationStatus.INCORRECT,
                onSubmit = { submitImageRecall(currentInput) },
                onRetry = {
                    inputFocusGeneration++
                    onEvent(AndroidStudyEvent.Retry)
                },
                onReveal = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onEvent(AndroidStudyEvent.Reveal(currentInput))
                },
                showIncorrectStatus = false
            )
        }
    }
}

internal fun shouldDismissImageRecallImeOnSubmit(status: TypingAnswerEvaluationStatus): Boolean =
    status == TypingAnswerEvaluationStatus.INCORRECT
