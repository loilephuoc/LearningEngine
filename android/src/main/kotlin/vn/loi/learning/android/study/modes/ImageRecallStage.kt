package vn.loi.learning.android.study.modes

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import vn.loi.learning.android.study.*
import vn.loi.learning.android.study.components.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.android.ui.isReducedMotionEnabled

@Composable
internal fun ImageRecallStudyStage(
    state: AndroidStudyState.ImageRecall,
    baseDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    feedbackContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentInput by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
    LaunchedEffect(state.answer) { currentInput = state.answer }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val feedbackVisible = state.completed
    val density = resolveImageRecallDensity(baseDensity, imeVisible, 14, feedbackVisible)
    val feedback = imageRecallFeedback(state.outcome)
    val reducedMotion = isReducedMotionEnabled()
    val imageScale by animateFloatAsState(
        if (state.outcome == vn.loi.learning.domain.study.recall.RecallOutcome.CORRECT) 1.01f else 1f,
        tween(imageRecallMediaMotionMillis(reducedMotion)),
        label = "image recall confirmation"
    )
    StudyStageCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StudySpacing.section)
        ) {
            StudyPrompt("Name this item", null, false, {})
            Box(
                Modifier.fillMaxWidth().animateContentSize(tween(imageRecallMediaMotionMillis(reducedMotion)))
                    .graphicsLayer { scaleX = imageScale; scaleY = imageScale }
            ) {
                StudyMedia(
                    state.resolvedImage,
                    imageRecallMediaRole(imeVisible, feedbackVisible),
                    density,
                    availableMediaHeightDp,
                    onOpenFullscreenImage
                )
            }
            if (state.imageUnavailable) Text("Image unavailable")
            StudyAnswerInput(
                state.plan.planId.value,
                state.answer,
                !state.completed && !state.imageUnavailable,
                state.outcome == vn.loi.learning.domain.study.recall.RecallOutcome.INCORRECT,
                label = "Name this item",
                feedback = feedback,
                onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
                onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
            )
            if (!state.completed) {
                Button(
                    onClick = { onEvent(AndroidStudyEvent.Submit(currentInput)) },
                    enabled = currentInput.isNotBlank() && !state.imageUnavailable,
                    modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Check") }
            }
            feedbackContent()
        }
    }
}
