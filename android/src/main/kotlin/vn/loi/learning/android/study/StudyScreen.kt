package vn.loi.learning.android.study

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onLibrary: () -> Unit = {},
    onReview: () -> Unit = {}
) {
    val model = state.model
    LazyColumn(
        Modifier.widthIn(max = 840.dp).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(LearningSpacing.large),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
    ) {
        item("header") { HomeHeader() }
        item("content-operation") {
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
        }
        item("hero") { ContinueLearningCard(model, onEvent, onLibrary) }
        if (model.hasDueReview) item("due-review") { DueReviewCard(model, onReview) }
        if (model.reviewedToday > 0) item("today") { TodaySummary(model) }
        if (model.totalMemoryCount > 0) item("progress") { ProgressSummary(model) }
        item("navigation") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSecondaryButton("Library", onLibrary, Modifier.weight(1f))
                if (state.availability.canStartReview) LearningEngineSecondaryButton("Review", onReview, Modifier.weight(1f))
            }
        }
        if (!model.hasContent) item("empty") {
            LearningEngineEmptyState(
                title = "Your library is ready for content",
                detail = "Import a learning package, then return here to start.",
                actionLabel = "Import package",
                onAction = { onContentAction(AndroidOperationKind.IMPORT) }
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text("Learning Engine", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Ready for your next step?", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun ContinueLearningCard(model: AndroidHomeUiModel, onEvent: (AndroidStudyEvent) -> Unit, onLibrary: () -> Unit) {
    val (title, detail, actionLabel) = when (model.primaryAction) {
        is AndroidHomePrimaryAction.Resume -> Triple("Continue learning", "Resume exactly where you left off.", "Continue session")
        AndroidHomePrimaryAction.ReviewDue -> Triple("Review is ready", "Strengthen what is due today.", "Review now")
        AndroidHomePrimaryAction.StartLearning -> Triple("Start learning", "Begin the next canonical Study session.", "Start learning")
        AndroidHomePrimaryAction.OpenLibrary -> Triple("Choose what to learn", "Add or open content in your Library.", "Open Library")
    }
    LearningEngineCard(Modifier.fillMaxWidth().semantics { contentDescription = "$title. $detail" }) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            model.contextTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LearningEnginePrimaryButton(actionLabel, onClick = {
                when (val action = model.primaryAction) {
                    is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(action.sessionId))
                    AndroidHomePrimaryAction.ReviewDue -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.StartLearning -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DueReviewCard(model: AndroidHomeUiModel, onReview: () -> Unit) {
    val tone = if (model.overdueCount > 0) LearningStatusTone.OVERDUE else LearningStatusTone.DUE
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                Text("Due review", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                LearningEngineStatusBadge(if (model.overdueCount > 0) "${model.overdueCount} overdue" else "Due today", tone)
            }
            Text("${model.dueCount} item(s) are ready to review.")
            LearningEngineSecondaryButton("Review options", onReview)
        }
    }
}

@Composable
private fun TodaySummary(model: AndroidHomeUiModel) {
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineSectionHeader("Today")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                HomeMetric("Reviewed", model.reviewedToday.toString(), Modifier.weight(1f))
                model.accuracyPercent?.let { HomeMetric("Recall", "$it%", Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ProgressSummary(model: AndroidHomeUiModel) {
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineSectionHeader("Learning progress")
            LearningEngineProgress(model.learningProgress, "${model.activeMemoryCount} of ${model.totalMemoryCount} memories active")
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $value" }) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val policy = androidLayoutPolicy(maxWidth.value.toInt(), maxHeight.value.toInt())
        CompositionLocalProvider(LocalLayoutPolicy provides policy) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
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
                        is AndroidStudyState.Failed -> StudyFailureState(target, onEvent)
                        is AndroidStudyState.Runtime -> {
                            StudyRuntimeScreen(
                                state = target,
                                onEvent = onEvent,
                                onOpenFullscreenImage = { fullscreenImageUri = it }
                            )
                        }
                    }
                }

                fullscreenImageUri?.let { imagePath ->
                    FullscreenStudyImage(
                        imagePath = imagePath,
                        onDismiss = { fullscreenImageUri = null }
                    )
                }
            }
        }
    }
}

private fun studyPresentationKey(state: AndroidStudyState): String = when (state) {
    AndroidStudyState.Loading -> "loading"
    is AndroidStudyState.Runtime -> "runtime-${state.plan.planId.value}"
    is AndroidStudyState.Completion -> "completion"
    is AndroidStudyState.Failed -> "failure"
    is AndroidStudyState.Home -> "home"
}

@Composable
private fun LoadingStudy() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(LearningSpacing.medium))
        Text(
            "Preparing Study…",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val modeLabel = when (state) {
        is AndroidStudyState.Typing -> "Typing"
        is AndroidStudyState.MultipleChoice -> "Multiple Choice"
        is AndroidStudyState.Listening -> "Listening"
        is AndroidStudyState.ImageRecall -> "Image Recall"
        is AndroidStudyState.ExampleCompletion -> "Example Completion"
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = state.contextTitle ?: "Study",
                modeLabel = modeLabel,
                currentPosition = state.currentPosition,
                totalItems = state.totalItems,
                onBack = { onEvent(AndroidStudyEvent.Home) }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
            ) {
                // Main Learning Card
                StudyMainCard(
                    state = state,
                    onEvent = onEvent,
                    onOpenFullscreenImage = onOpenFullscreenImage
                )

                // Revealed Answer & Feedback Section
                StudyRevealAndFeedbackSection(
                    state = state,
                    onEvent = onEvent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyTopBar(
    title: String,
    modeLabel: String,
    currentPosition: Int?,
    totalItems: Int?,
    onBack: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LearningEngineStatusBadge(
                        label = modeLabel,
                        tone = LearningStatusTone.INFO
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit study session"
                    )
                }
            },
            actions = {
                if (currentPosition != null && totalItems != null && totalItems > 0) {
                    Text(
                        "$currentPosition / $totalItems",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = LearningSpacing.medium)
                            .semantics {
                                contentDescription = "Item $currentPosition of $totalItems"
                            }
                    )
                }
            }
        )
        if (currentPosition != null && totalItems != null && totalItems > 0) {
            val progress = (currentPosition.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = LearningEngineThemeTokens.semanticColors.progressTrack
            )
        }
    }
}

@Composable
private fun StudyMainCard(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = LearningEngineShapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
    ) {
        Column(
            Modifier.padding(LearningSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
        ) {
            // Prompt Header (Word / Prompt Text / Listening / Image / Example)
            StudyPromptHeader(state = state)

            // Audio Action Button (if available)
            val audioUri = state.resolvedAudio
            if (audioUri != null) {
                StudyAudioButton(audioPath = audioUri)
            }

            // Image Content (if available)
            val imageUri = state.resolvedImage
            if (imageUri != null) {
                StudyImage(
                    imagePath = imageUri,
                    imageUnavailable = false,
                    onOpenFullscreen = onOpenFullscreenImage
                )
            }

            // Mode-specific Input & Options
            StudyModeInputArea(
                state = state,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun StudyPromptHeader(state: AndroidStudyState.Runtime) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        when (state) {
            is AndroidStudyState.Typing -> {
                Text(
                    state.prompt,
                    style = LearningContentTypography.vocabulary,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.MultipleChoice -> {
                Text(
                    state.question,
                    style = LearningContentTypography.sectionTitle,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.Listening -> {
                Text(
                    "Listen and type the answer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.ImageRecall -> {
                Text(
                    "Name the item shown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.ExampleCompletion -> {
                val isRevealed = state.revealed || state.completed
                Text(
                    buildAnnotatedString {
                        append(state.prefix)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                            append(if (isRevealed) state.blank else " ".repeat(state.blank.length.coerceAtLeast(3)))
                        }
                        append(state.suffix)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics {
                        heading()
                        contentDescription = if (isRevealed) "${state.prefix} ${state.blank} ${state.suffix}"
                        else "${state.prefix} ${accessibilityStrings().blank} ${state.suffix}"
                    }
                )
            }
        }

        // Pronunciation display if available
        state.pronunciation?.takeIf { it.isNotBlank() }?.let { pron ->
            Text(
                pron,
                style = LearningContentTypography.pronunciation,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyAudioButton(audioPath: String?) {
    val controller = remember { AndroidAudioController() }
    var audioState by remember(audioPath) {
        mutableStateOf<AndroidAudioState>(if (audioPath == null) AndroidAudioState.Unavailable else AndroidAudioState.Idle)
    }
    DisposableEffect(controller) { onDispose(controller::close) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
    ) {
        FilledTonalButton(
            onClick = {
                if (audioPath != null && audioState != AndroidAudioState.Preparing) {
                    audioState = controller.replay(audioPath) { newState -> audioState = newState }
                }
            },
            enabled = audioPath != null && audioState != AndroidAudioState.Unavailable,
            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
                contentDescription = accessibilityStrings().replay
                stateDescription = audioState::class.simpleName.orEmpty()
            }
        ) {
            if (audioState == AndroidAudioState.Preparing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(LearningSpacing.small))
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(LearningSpacing.small))
            }
            Text(
                when (audioState) {
                    AndroidAudioState.Preparing -> "Loading audio"
                    AndroidAudioState.Playing -> "Playing"
                    AndroidAudioState.Failed -> "Audio error"
                    AndroidAudioState.Unavailable -> "Audio unavailable"
                    else -> "Listen audio"
                }
            )
        }

        if (audioState == AndroidAudioState.Failed || audioState == AndroidAudioState.Unavailable) {
            Text(
                "Audio unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
fun StudyImage(
    imagePath: String?,
    imageUnavailable: Boolean = imagePath == null,
    onOpenFullscreen: (String) -> Unit = {}
) {
    if (imagePath == null && !imageUnavailable) return // Natural rebalance, no placeholder box

    if (imageUnavailable) {
        Text(
            "Image unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
        )
        return
    }

    val imageState by produceState<ImagePresentationState>(ImagePresentationState.Loading, imagePath) {
        value = withContext(Dispatchers.IO) {
            imagePath?.let { decodeBoundedImage(it, 1600, 1600) }
                ?.let(ImagePresentationState::Ready) ?: ImagePresentationState.Failed
        }
    }

    when (val presentation = imageState) {
        ImagePresentationState.Loading -> {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .semantics { contentDescription = accessibilityStrings().loading },
                    strokeWidth = 3.dp
                )
            }
        }
        ImagePresentationState.Failed -> {
            Text(
                "Image could not be loaded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        ImagePresentationState.Unavailable -> {
            Text(
                "Image unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        is ImagePresentationState.Ready -> {
            Card(
                shape = LearningEngineShapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { imagePath?.let(onOpenFullscreen) }
                    .semantics {
                        role = Role.Button
                        contentDescription = "View full size image"
                    }
            ) {
                Image(
                    bitmap = presentation.bitmap.asImageBitmap(),
                    contentDescription = accessibilityStrings().imagePrompt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = LocalLayoutPolicy.current.maxMediaHeightDp.dp)
                        .padding(LearningSpacing.small)
                )
            }
        }
    }
}

@Composable
fun FullscreenStudyImage(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val imageState by produceState<ImagePresentationState>(ImagePresentationState.Loading, imagePath) {
        value = withContext(Dispatchers.IO) {
            decodeBoundedImage(imagePath, 2400, 2400)?.let(ImagePresentationState::Ready)
                ?: ImagePresentationState.Failed
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)
        ) {
            Box(Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LearningSpacing.medium)
                        .defaultMinSize(minWidth = LearningSpacing.touchTarget, minHeight = LearningSpacing.touchTarget)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close full size image",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LearningSpacing.large)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    when (val presentation = imageState) {
                        ImagePresentationState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        is ImagePresentationState.Ready -> {
                            Image(
                                bitmap = presentation.bitmap.asImageBitmap(),
                                contentDescription = "Full size image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> Text("Image unavailable", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
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
private fun StudyModeInputArea(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    when (state) {
        is AndroidStudyState.Typing -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
                    onEvent = onEvent
                )
                if (state.evaluation == TypingAnswerEvaluationStatus.INCORRECT && !state.completed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Keep trying",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        TextButton(
                            onClick = { onEvent(AndroidStudyEvent.Retry) },
                            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                        ) { Text("Retry") }
                    }
                }
            }
        }
        is AndroidStudyState.MultipleChoice -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                state.choices.forEachIndexed { index, choice ->
                    val isSelected = state.selectedChoiceId == choice.id
                    OutlinedButton(
                        onClick = { onEvent(AndroidStudyEvent.Choose(choice.id)) },
                        enabled = !state.completed,
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            .semantics {
                                selected = isSelected
                                stateDescription = accessibilityStrings().option(index + 1, state.choices.size, isSelected)
                            }
                    ) {
                        Text(
                            "${index + 1}. ${choice.text}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        is AndroidStudyState.Listening -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.audioUnavailable,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ImageRecall -> {
            val imageReady = !state.imageUnavailable
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && imageReady,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank() && imageReady,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ExampleCompletion -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
private fun StudyRevealAndFeedbackSection(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    val isRevealed = when (state) {
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }

    val allowReveal = when (state) {
        is AndroidStudyState.Typing, is AndroidStudyState.ExampleCompletion -> true
        else -> false
    }

    AnimatedVisibility(
        visible = state.completed || isRevealed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = LearningEngineShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                Modifier.padding(LearningSpacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
            ) {
                // Feedback badge
                val tone = when (state.outcome) {
                    RecallOutcome.CORRECT -> LearningStatusTone.SUCCESS
                    RecallOutcome.INCORRECT -> LearningStatusTone.ERROR
                    RecallOutcome.REVEALED -> LearningStatusTone.WARNING
                    else -> LearningStatusTone.INFO
                }
                val badgeText = when {
                    isRevealed -> "Answer revealed"
                    state.outcome == RecallOutcome.CORRECT -> "Correct"
                    state.outcome == RecallOutcome.INCORRECT -> "Incorrect"
                    else -> "Answer recorded"
                }
                LearningEngineStatusBadge(label = badgeText, tone = tone)

                // Canonical Answer
                Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                    Text(
                        "Expected Answer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.plan.answerContract.canonicalAnswer,
                        style = LearningContentTypography.meaning,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Meaning / Target translation if present
                state.meaning?.takeIf { it.isNotBlank() }?.let { m ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        Text(
                            "Meaning",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(m, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Example & Translation if present
                state.example?.takeIf { it.isNotBlank() }?.let { ex ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        Text(
                            "Example",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(ex, style = LearningContentTypography.example)
                        state.translation?.takeIf { it.isNotBlank() }?.let { tr ->
                            Text(tr, style = LearningContentTypography.translation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Response Actions: Continue, Undo, Rating override
                Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                    ) {
                        LearningEnginePrimaryButton(
                            label = "Continue",
                            onClick = { onEvent(AndroidStudyEvent.Next) },
                            modifier = Modifier.weight(1f)
                        )
                        LearningEngineSecondaryButton(
                            label = "Undo",
                            onClick = { onEvent(AndroidStudyEvent.Undo) },
                            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                        )
                    }

                    // Practice Rating Overrides (if canonical practice mode)
                    if (state.plan.provenance == RecallProvenance.PRACTICE) {
                        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                            Text(
                                "Rate your recall:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(ReviewRating.AGAIN, ReviewRating.HARD, ReviewRating.GOOD, ReviewRating.EASY).forEach { rating ->
                                    val ratingTone = when (rating) {
                                        ReviewRating.AGAIN, ReviewRating.HARD -> LearningDifficultyTone.DIFFICULT
                                        ReviewRating.GOOD -> LearningDifficultyTone.MEDIUM
                                        ReviewRating.EASY -> LearningDifficultyTone.EASY
                                    }
                                    LearningEngineFeedbackBadge(
                                        tone = ratingTone,
                                        modifier = Modifier.clickable { onEvent(AndroidStudyEvent.OverrideRating(rating)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (!state.completed && !isRevealed && allowReveal) {
        TextButton(
            onClick = { onEvent(AndroidStudyEvent.Reveal) },
            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
        ) {
            Text("Reveal answer")
        }
    }
}

@Composable
private fun Completion(state: AndroidStudyState.Completion, onEvent: (AndroidStudyEvent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = LearningEngineShapes.large
        ) {
            Column(
                Modifier.padding(LearningSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
            ) {
                LearningEngineStatusBadge(label = "Complete", tone = LearningStatusTone.COMPLETED)
                Text(
                    "Session complete",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Text(
                    "Great work! You have completed all items in this study session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.canUndo) {
                        LearningEngineSecondaryButton(
                            label = "Undo latest",
                            onClick = { onEvent(AndroidStudyEvent.Undo) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LearningEnginePrimaryButton(
                        label = "Back to Home",
                        onClick = { onEvent(AndroidStudyEvent.Home) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyFailureState(
    state: AndroidStudyState.Failed,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen),
        contentAlignment = Alignment.Center
    ) {
        LearningEngineErrorState(
            title = "Study session unavailable",
            message = state.message,
            onRetry = if (state.retryable) { { onEvent(AndroidStudyEvent.Retry) } } else null
        )
    }
}
