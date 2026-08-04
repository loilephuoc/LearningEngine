package vn.loi.learning.android.study

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.android.platform.*

@Composable
fun HomeScreen(
    state: AndroidStudyState.Home,
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onEvent: (AndroidStudyEvent) -> Unit,
    onContentAction: (AndroidOperationKind) -> Unit = {},
    onContentDismiss: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Learning Engine", style = MaterialTheme.typography.headlineMedium)
        when (contentState) {
            is AndroidContentOperationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
            is AndroidContentOperationState.Succeeded -> Text(contentState.detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            is AndroidContentOperationState.Failed -> {
                Text(contentState.failure.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onContentAction(contentState.kind) }) { Text("Retry") }
                    TextButton(onClick = onContentDismiss) { Text("Dismiss") }
                }
            }
            AndroidContentOperationState.Idle -> Unit
        }
        Button(onClick = { onContentAction(AndroidOperationKind.IMPORT) }, enabled = contentState !is AndroidContentOperationState.Running, modifier = Modifier.fillMaxWidth()) { Text("Import package") }
        if (state.availability.canResume) {
            Button(onClick = { onEvent(AndroidStudyEvent.Resume) }, Modifier.fillMaxWidth()) { Text("Resume") }
        }
        EntryButton("Review", AndroidSessionEntry.REVIEW, state.availability.canStartReview, onEvent)
        EntryButton("Latest session Practice", AndroidSessionEntry.LATEST_SESSION, state.availability.canStartLatestSessionPractice, onEvent)
        EntryButton("Again / Hard Practice", AndroidSessionEntry.DIFFICULT, state.availability.canStartDifficultPractice, onEvent)
        EntryButton("Learned items", AndroidSessionEntry.LEARNED, state.availability.canStartLearnedReview, onEvent)
        OutlinedButton(onClick = { onContentAction(AndroidOperationKind.BACKUP) }, enabled = contentState !is AndroidContentOperationState.Running, modifier = Modifier.fillMaxWidth()) { Text("Create backup") }
        OutlinedButton(onClick = { onContentAction(AndroidOperationKind.RESTORE) }, enabled = contentState !is AndroidContentOperationState.Running, modifier = Modifier.fillMaxWidth()) { Text("Restore backup") }
    }
}

@Composable
private fun EntryButton(label: String, entry: AndroidSessionEntry, enabled: Boolean, onEvent: (AndroidStudyEvent) -> Unit) {
    OutlinedButton(
        onClick = { onEvent(AndroidStudyEvent.Start(entry)) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) { Text(label) }
}

@Composable
fun StudyScreen(state: AndroidStudyState, onEvent: (AndroidStudyEvent) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val horizontal = if (maxWidth >= 600.dp) 64.dp else 20.dp
        Box(
            Modifier.fillMaxSize().padding(horizontal = horizontal, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is AndroidStudyState.Home -> HomeScreen(state, onEvent = onEvent)
                is AndroidStudyState.Completion -> Completion(state, onEvent)
                is AndroidStudyState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is AndroidStudyState.Typing -> TypingRuntime(state, onEvent)
                is AndroidStudyState.MultipleChoice -> MultipleChoiceRuntime(state, onEvent)
                is AndroidStudyState.Listening -> ListeningRuntime(state, onEvent)
                is AndroidStudyState.ImageRecall -> ImageRuntime(state, onEvent)
                is AndroidStudyState.ExampleCompletion -> ExampleRuntime(state, onEvent)
            }
        }
    }
}

@Composable
private fun RuntimeColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp), content = content
    )
}

@Composable
private fun TypingRuntime(state: AndroidStudyState.Typing, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Typing", state.prompt)
    AnswerField(state.answer, !state.completed && !state.revealed,
        state.evaluation == TypingAnswerEvaluationStatus.INCORRECT, onEvent)
    if (state.evaluation == TypingAnswerEvaluationStatus.INCORRECT && !state.completed) {
        Text("Keep trying", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { onEvent(AndroidStudyEvent.Retry) }) { Text("Retry") }
    }
    RuntimeFooter(state, state.answer, state.revealed, onEvent)
}

@Composable
private fun MultipleChoiceRuntime(state: AndroidStudyState.MultipleChoice, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Multiple choice", state.question)
    state.choices.forEachIndexed { index, choice ->
        OutlinedButton(
            onClick = { onEvent(AndroidStudyEvent.Choose(choice.id)) }, enabled = !state.completed,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Option ${index + 1}: ${choice.text}" }
        ) { Text("${index + 1}. ${choice.text}") }
    }
    RuntimeFooter(state, state.selectedChoiceId.orEmpty(), false, onEvent, allowReveal = false)
}

@Composable
private fun ListeningRuntime(state: AndroidStudyState.Listening, onEvent: (AndroidStudyEvent) -> Unit) {
    val controller = remember { AndroidAudioController() }
    var audioState by remember(state.plan.planId) {
        mutableStateOf<AndroidAudioState>(if (state.audioUnavailable) AndroidAudioState.Unavailable else AndroidAudioState.Idle)
    }
    DisposableEffect(controller) { onDispose(controller::close) }
    RuntimeColumn {
        RuntimeHeading("Listening", "Listen and type the answer")
        Button(
            onClick = { audioState = controller.replay(state.audioPath) { audioState = it } },
            enabled = !state.audioUnavailable,
            modifier = Modifier.semantics { contentDescription = "Replay audio" }
        ) { Text("Replay") }
        if (audioState == AndroidAudioState.Unavailable || audioState == AndroidAudioState.Failed) {
            Text("Audio unavailable", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        AnswerField(state.answer, !state.completed && !state.audioUnavailable, false, onEvent)
        if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank()) { Text("Submit") }
        RuntimeFooter(state, state.answer, false, onEvent, allowReveal = false)
    }
}

@Composable
private fun ImageRuntime(state: AndroidStudyState.ImageRecall, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Image recall", "Name the item shown")
    val bitmap by produceState<android.graphics.Bitmap?>(null, state.imagePath) {
        value = withContext(Dispatchers.IO) { state.imagePath?.let { decodeBoundedImage(it, 1600, 1600) } }
    }
    when {
        state.imageUnavailable -> Text("Image unavailable", color = MaterialTheme.colorScheme.error)
        bitmap == null -> Text("Image could not be decoded", color = MaterialTheme.colorScheme.error)
        else -> Image(
            bitmap = requireNotNull(bitmap).asImageBitmap(), contentDescription = "Recall prompt image",
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
        )
    }
    AnswerField(state.answer, !state.completed && bitmap != null, false, onEvent)
    if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank() && bitmap != null) { Text("Submit") }
    RuntimeFooter(state, state.answer, false, onEvent, allowReveal = false)
}

private fun decodeBoundedImage(path: String, maxWidth: Int, maxHeight: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

@Composable
private fun ExampleRuntime(state: AndroidStudyState.ExampleCompletion, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Example completion", buildAnnotatedString {
        append(state.prefix)
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            append(if (state.revealed) state.blank else " ".repeat(state.blank.length.coerceAtLeast(3)))
        }
        append(state.suffix)
    })
    AnswerField(state.answer, !state.completed && !state.revealed, false, onEvent)
    if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank()) { Text("Submit") }
    RuntimeFooter(state, state.answer, state.revealed, onEvent)
}

@Composable
private fun RuntimeHeading(label: String, prompt: Any) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    when (prompt) {
        is String -> Text(prompt, style = MaterialTheme.typography.headlineMedium)
        is androidx.compose.ui.text.AnnotatedString -> Text(prompt, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun AnswerField(answer: String, enabled: Boolean, error: Boolean, onEvent: (AndroidStudyEvent) -> Unit) {
    OutlinedTextField(
        value = answer, onValueChange = { onEvent(AndroidStudyEvent.AnswerChanged(it)) },
        enabled = enabled, singleLine = true, isError = error,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(AndroidStudyEvent.Submit) }),
        label = { Text("Answer") }, modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RuntimeFooter(
    state: AndroidStudyState.Runtime,
    answer: String,
    revealed: Boolean,
    onEvent: (AndroidStudyEvent) -> Unit,
    allowReveal: Boolean = true
) {
    if (state.completed) {
        Column(
            Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (revealed) "Answer revealed" else if (state.outcome == RecallOutcome.CORRECT) "Correct" else "Answer recorded")
            Text("Your answer: ${answer.ifBlank { "—" }}")
            Text("Expected: ${state.plan.answerContract.canonicalAnswer}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEvent(AndroidStudyEvent.Next) }) { Text("Continue") }
            TextButton(onClick = { onEvent(AndroidStudyEvent.Undo) }) { Text("Undo") }
        }
    } else if (allowReveal) {
        TextButton(onClick = { onEvent(AndroidStudyEvent.Reveal) }) { Text("Reveal answer") }
    }
    if (!state.completed && state.plan.provenance == RecallProvenance.PRACTICE) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(ReviewRating.AGAIN, ReviewRating.HARD, ReviewRating.GOOD, ReviewRating.EASY).forEach { rating ->
                TextButton(onClick = { onEvent(AndroidStudyEvent.OverrideRating(rating)) }) { Text(rating.name) }
            }
        }
    }
}

@Composable
private fun Completion(state: AndroidStudyState.Completion, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    Text("Session complete", style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    if (state.canUndo) TextButton(onClick = { onEvent(AndroidStudyEvent.Undo) }) { Text("Undo latest") }
    Button(onClick = { onEvent(AndroidStudyEvent.Home) }) { Text("Back to Home") }
}
