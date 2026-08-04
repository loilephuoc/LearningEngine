package vn.loi.learning.android.study

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.*

private val LocalLayoutPolicy = staticCompositionLocalOf { androidLayoutPolicy(360, 800) }

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

@Composable
fun HomeScreen(
    state: AndroidStudyState.Home,
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onEvent: (AndroidStudyEvent) -> Unit,
    onContentAction: (AndroidOperationKind) -> Unit = {},
    onContentDismiss: () -> Unit = {},
    onLibrary: () -> Unit = {}
) {
    Column(
        Modifier.widthIn(max = 840.dp).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .safeDrawingPadding().imePadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ready to learn?", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        when (contentState) {
            is AndroidContentOperationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = accessibilityStrings().loading })
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
        if (state.availability.canResume) {
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Continue learning", style = MaterialTheme.typography.titleLarge)
                Text("Your active session is ready where you left it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onEvent(AndroidStudyEvent.Resume) }, Modifier.fillMaxWidth()) { Text("Continue session") }
            } }
        }
        if (state.availability.canStartReview) EntryButton("Review due items", AndroidSessionEntry.REVIEW, true, onEvent)
        if (state.availability.canStartLatestSessionPractice) EntryButton("Practice latest session", AndroidSessionEntry.LATEST_SESSION, true, onEvent)
        if (state.availability.canStartDifficultPractice) EntryButton("Practice Again / Hard", AndroidSessionEntry.DIFFICULT, true, onEvent)
        if (state.availability.canStartLearnedReview) EntryButton("Review learned items", AndroidSessionEntry.LEARNED, true, onEvent)
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your library", style = MaterialTheme.typography.titleLarge)
            Text(if(state.installedPackageCount==0) "Import a package to begin." else "${state.installedPackageCount} installed package(s) ready to study.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick=onLibrary, modifier=Modifier.fillMaxWidth()) { Text("Open Library") }
            TextButton(onClick = { onContentAction(AndroidOperationKind.IMPORT) }, enabled = contentState !is AndroidContentOperationState.Running, modifier = Modifier.fillMaxWidth()) { Text("Import package") }
        } }
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
        val policy = androidLayoutPolicy(maxWidth.value.toInt(), maxHeight.value.toInt())
        CompositionLocalProvider(LocalLayoutPolicy provides policy) { Box(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .padding(horizontal = policy.horizontalPaddingDp.dp, vertical = policy.verticalPaddingDp.dp), contentAlignment = Alignment.Center
        ) { Box(Modifier.widthIn(max = policy.maxContentWidthDp.dp).fillMaxWidth()) {
            AnimatedContent(
                targetState = state,
                contentKey = ::studyPresentationKey,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "study destination"
            ) { target ->
                when (target) {
                    AndroidStudyState.Loading -> LoadingStudy()
                    is AndroidStudyState.Home -> HomeScreen(target, onEvent = onEvent)
                    is AndroidStudyState.Completion -> Completion(target, onEvent)
                    is AndroidStudyState.Failed -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(target.message, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive });Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={onEvent(AndroidStudyEvent.Retry)}){Text("Retry")};OutlinedButton(onClick={onEvent(AndroidStudyEvent.Home)}){Text("Back")}} }
                    is AndroidStudyState.Typing -> TypingRuntime(target, onEvent)
                    is AndroidStudyState.MultipleChoice -> MultipleChoiceRuntime(target, onEvent)
                    is AndroidStudyState.Listening -> ListeningRuntime(target, onEvent)
                    is AndroidStudyState.ImageRecall -> ImageRuntime(target, onEvent)
                    is AndroidStudyState.ExampleCompletion -> ExampleRuntime(target, onEvent)
                }
            }
        } } }
    }
}

private fun studyPresentationKey(state: AndroidStudyState): String = when (state) {
    AndroidStudyState.Loading -> "loading"
    is AndroidStudyState.Runtime -> "runtime-${state.plan.planId.value}"
    is AndroidStudyState.Completion -> "completion"
    is AndroidStudyState.Failed -> "failure"
    is AndroidStudyState.Home -> "home"
}

@Composable private fun LoadingStudy(){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){CircularProgressIndicator();Text("Preparing Study…",modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })}}

@Composable
private fun RuntimeColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalLayoutPolicy.current.runtimeSpacingDp.dp), content = content
    )
}

@Composable
private fun TypingRuntime(state: AndroidStudyState.Typing, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Typing", state.prompt)
    AnswerField(state.plan.planId.value, state.answer, !state.completed && !state.revealed,
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
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).semantics {
                selected = state.selectedChoiceId == choice.id
                stateDescription = accessibilityStrings().option(index + 1, state.choices.size, state.selectedChoiceId == choice.id)
            }
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
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics {
                contentDescription = accessibilityStrings().replay
                stateDescription = audioState::class.simpleName.orEmpty()
            }
        ) {
            if (audioState == AndroidAudioState.Preparing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (audioState == AndroidAudioState.Playing) "Playing" else "Replay")
        }
        if (audioState == AndroidAudioState.Unavailable || audioState == AndroidAudioState.Failed) {
            Text("Audio unavailable", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        AnswerField(state.plan.planId.value, state.answer, !state.completed && !state.audioUnavailable, false, onEvent)
        if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank()) { Text("Submit") }
        RuntimeFooter(state, state.answer, false, onEvent, allowReveal = false)
    }
}

@Composable
private fun ImageRuntime(state: AndroidStudyState.ImageRecall, onEvent: (AndroidStudyEvent) -> Unit) = RuntimeColumn {
    RuntimeHeading("Image recall", "Name the item shown")
    val imageState by produceState<ImagePresentationState>(ImagePresentationState.Loading, state.imagePath) {
        value = withContext(Dispatchers.IO) {
            state.imagePath?.let { decodeBoundedImage(it, 1600, 1600) }
                ?.let(ImagePresentationState::Ready) ?: ImagePresentationState.Failed
        }
    }
    Crossfade(
        targetState = if (state.imageUnavailable) ImagePresentationState.Unavailable else imageState,
        label = "image loading"
    ) { presentation ->
        when (presentation) {
            ImagePresentationState.Loading -> CircularProgressIndicator(
                Modifier.semantics { contentDescription = accessibilityStrings().loading }
            )
            ImagePresentationState.Unavailable -> Text("Image unavailable", color = MaterialTheme.colorScheme.error)
            ImagePresentationState.Failed -> Text("Image could not be decoded", color = MaterialTheme.colorScheme.error)
            is ImagePresentationState.Ready -> Image(
                bitmap = presentation.bitmap.asImageBitmap(), contentDescription = accessibilityStrings().imagePrompt,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()
                    .heightIn(max = LocalLayoutPolicy.current.maxMediaHeightDp.dp)
            )
        }
    }
    val imageReady = imageState is ImagePresentationState.Ready && !state.imageUnavailable
    AnswerField(state.plan.planId.value, state.answer, !state.completed && imageReady, false, onEvent)
    if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank() && imageReady) { Text("Submit") }
    RuntimeFooter(state, state.answer, false, onEvent, allowReveal = false)
}

private sealed interface ImagePresentationState {
    data object Loading : ImagePresentationState
    data object Unavailable : ImagePresentationState
    data object Failed : ImagePresentationState
    data class Ready(val bitmap: android.graphics.Bitmap) : ImagePresentationState
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
    }, if (state.revealed) null else "${state.prefix} ${accessibilityStrings().blank} ${state.suffix}")
    AnswerField(state.plan.planId.value, state.answer, !state.completed && !state.revealed, false, onEvent)
    if (!state.completed) Button(onClick = { onEvent(AndroidStudyEvent.Submit) }, enabled = state.answer.isNotBlank()) { Text("Submit") }
    RuntimeFooter(state, state.answer, state.revealed, onEvent)
}

@Composable
private fun RuntimeHeading(label: String, prompt: Any, accessiblePrompt: String? = null) {
    Card(
        Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            when (prompt) {
                is String -> Text(prompt, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                is androidx.compose.ui.text.AnnotatedString -> Text(prompt, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading(); accessiblePrompt?.let { contentDescription = it } })
            }
        }
    }
}

@Composable
private fun AnswerField(planId: String, answer: String, enabled: Boolean, error: Boolean, onEvent: (AndroidStudyEvent) -> Unit) {
    val focusRequester = remember(planId) { FocusRequester() }
    val bringIntoView = remember(planId) { BringIntoViewRequester() }
    var focusedForPlan by rememberSaveable(planId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(planId, enabled) {
        if (enabled && !focusedForPlan) { focusedForPlan = true; focusRequester.requestFocus(); bringIntoView.bringIntoView() }
        if (!enabled) keyboard?.hide()
    }
    OutlinedTextField(
        value = answer, onValueChange = { onEvent(AndroidStudyEvent.AnswerChanged(it)) },
        enabled = enabled, singleLine = false, minLines = 1, maxLines = 4, isError = error,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(AndroidStudyEvent.Submit) }),
        label = { Text(accessibilityStrings().answer) }, modifier = Modifier.fillMaxWidth()
            .focusRequester(focusRequester).bringIntoViewRequester(bringIntoView)
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
