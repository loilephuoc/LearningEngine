package vn.loi.learning.android.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.media.AndroidAudioPlaybackEvent
import vn.loi.learning.android.media.LearningEngineAudioPolicy
import vn.loi.learning.android.packageexperience.AndroidPackageQuickEditDraft
import vn.loi.learning.android.R
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.*
import vn.loi.learning.android.study.components.StudyActionDock
import vn.loi.learning.android.study.components.StudyQuickEditDialog
import vn.loi.learning.android.study.components.StudyRatingBar
import vn.loi.learning.android.study.components.ReviewImageNavigationOverlay
import vn.loi.learning.android.study.components.reviewNavigationGestures
import vn.loi.learning.android.study.components.PartOfSpeechBadge
import vn.loi.learning.android.study.components.StudyAnswerSection
import vn.loi.learning.android.study.components.StudyAudioTextTarget
import vn.loi.learning.android.study.components.StudyTextInteraction
import vn.loi.learning.android.study.components.StudyRuntimeShell
import vn.loi.learning.android.study.components.StudyStageCard
import vn.loi.learning.android.study.components.StudyMedia
import vn.loi.learning.android.study.components.StudyAnswerInput
import vn.loi.learning.android.study.components.StudyPrompt
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.study.modes.ListeningStudyStage
import vn.loi.learning.android.study.modes.MultipleChoiceStudyStage
import vn.loi.learning.android.study.modes.ExampleCompletionStudyStage
import vn.loi.learning.android.study.modes.ImageRecallStudyStage
import vn.loi.learning.android.study.modes.TypingStudyStage
import vn.loi.learning.android.study.modes.TypingDifferenceComparison

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

internal data class AndroidLearningLandingPresentation(
    val primaryAction: AndroidHomePrimaryAction,
    val hasActiveSession: Boolean,
    val contextTitle: String?,
    val hasContent: Boolean,
    val dueCount: Int,
    val reviewedToday: Int,
    val accuracyPercent: Int?,
    val activeMemoryCount: Int,
    val totalMemoryCount: Int,
    val learningProgress: Float,
    val dailyBudget: vn.loi.learning.application.study.DailyStudyBudgetSnapshot?,
    val forecastInsights: vn.loi.learning.android.dashboard.AndroidForecastInsightsUiModel? = null
)

internal fun resolveLearningLandingPresentation(state: AndroidStudyState.Home) =
    AndroidLearningLandingPresentation(
        primaryAction = state.model.primaryAction,
        hasActiveSession = state.availability.canResume,
        contextTitle = state.model.contextTitle,
        hasContent = state.model.hasContent,
        dueCount = state.model.dueCount,
        reviewedToday = state.model.reviewedToday,
        accuracyPercent = state.model.accuracyPercent,
        activeMemoryCount = state.model.activeMemoryCount,
        totalMemoryCount = state.model.totalMemoryCount,
        learningProgress = state.model.learningProgress,
        dailyBudget = state.model.dailyBudget,
        forecastInsights = state.model.forecastInsights
    )

@Composable
fun HomeScreen(
    state: AndroidStudyState.Home,
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onEvent: (AndroidStudyEvent) -> Unit,
    onContentAction: (AndroidOperationKind) -> Unit = {},
    onContentDismiss: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onReview: () -> Unit = {},
    onStudyLauncher: () -> Unit = {},
    onAutoPlay: () -> Unit = {}
) {
    val model = state.model
    val presentation = resolveLearningLandingPresentation(state)
    LazyColumn(
        Modifier.widthIn(max = 840.dp).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
    ) {
        item("header") { HomeHeader() }
        item("active-package") { ActiveLearningPackageCard(model, onLibrary) }
        item("content-operation") {
            when (contentState) {
                is AndroidContentOperationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = accessibilityStrings().loading })
                is AndroidContentOperationState.Succeeded -> Text(contentState.detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                is AndroidContentOperationState.Failed -> {
                    Text(contentState.failure.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onContentAction(contentState.kind) }) { Text("Thử lại") }
                        TextButton(onClick = onContentDismiss) { Text("Đóng") }
                    }
                }
                AndroidContentOperationState.Idle -> Unit
            }
        }
        item("hero") { ContinueLearningCard(model, presentation, onEvent, onLibrary, onReview, onStudyLauncher) }
        item("stats") { HomeDashboardStats(presentation) }
        if (state.availability.canStartReview && presentation.dueCount > 0) {
            item("due-review") { DueReviewCard(model, onReview) }
        }
        if (model.hasContent) {
            item("autoplay") { AutoPlayCard(onAutoPlay) }
        }
        if (presentation.totalMemoryCount > 0) item("progress") { HomeLearningProgress(presentation) }
        presentation.forecastInsights?.let { insights ->
            if (presentation.totalMemoryCount > 0 || insights.forecast7Days.any { it.count > 0 } || insights.availableScopes.size > 1) {
                if (insights.availableScopes.size > 1) {
                    item("insights-scope") {
                        vn.loi.learning.android.dashboard.InsightsScopeSelector(
                            currentScope = insights.scope,
                            availableScopes = insights.availableScopes,
                            onScopeChange = { onEvent(AndroidStudyEvent.ChangeInsightsScope(it)) }
                        )
                    }
                }
                item("forecast") { vn.loi.learning.android.dashboard.ReviewForecastCard(insights.forecast7Days) }
                item("memory-retention") { vn.loi.learning.android.dashboard.MemoryRetentionCard(insights.memoryDistribution) }
            }
            if (insights.todayRatings.totalCount > 0 || presentation.reviewedToday > 0) {
                item("today-ratings") { vn.loi.learning.android.dashboard.TodayRatingsCard(insights.todayRatings) }
            }
        }
        if (!model.hasContent) item("empty") {
            LearningEngineEmptyState(
                title = stringResource(R.string.home_empty_title),
                detail = stringResource(R.string.home_empty_detail),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = { onContentAction(AndroidOperationKind.IMPORT) }
            )
        }
    }
}

@Composable
private fun ActiveLearningPackageCard(model: AndroidHomeUiModel, onLibrary: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLibrary).semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = model.activePackageName?.let { "Gói đang học $it. Nhấn để đổi gói." }
                ?: "Chưa chọn gói học. Nhấn để chọn gói."
        },
        shape = LearningEngineShapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("GÓI ĐANG HỌC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(model.activePackageName ?: "Chưa chọn gói học", style = MaterialTheme.typography.titleSmall)
                model.activePackageContentCount?.let { Text("$it nội dung", style = MaterialTheme.typography.bodySmall) }
            }
            Text(if (model.activePackageName == null) "Chọn gói" else "Đổi gói", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AutoPlayCard(onAutoPlay: () -> Unit) {
    LearningEngineActionCard(
        icon = Icons.Default.PlayCircleOutline,
        title = stringResource(R.string.home_autoplay_title),
        detail = stringResource(R.string.home_autoplay_desc),
        actionLabel = stringResource(R.string.home_open_autoplay),
        onAction = onAutoPlay,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text(stringResource(R.string.home_header_brand), style = LearningTextRole.brand, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_header_title), style = LearningTextRole.screenTitle,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun ContinueLearningCard(
    model: AndroidHomeUiModel,
    presentation: AndroidLearningLandingPresentation,
    onEvent: (AndroidStudyEvent) -> Unit,
    onLibrary: () -> Unit,
    onReview: () -> Unit,
    onStudyLauncher: () -> Unit
) {
    val (title, detail, actionLabel) = when (model.primaryAction) {
        is AndroidHomePrimaryAction.Resume -> Triple(stringResource(R.string.home_continue_session), stringResource(R.string.study_resume_detail), stringResource(R.string.home_continue_session))
        AndroidHomePrimaryAction.ReviewDue -> Triple(stringResource(R.string.home_adaptive_study), stringResource(R.string.study_review_due_detail), stringResource(R.string.home_adaptive_study))
        AndroidHomePrimaryAction.StartLearning -> Triple(stringResource(R.string.home_learn_new), stringResource(R.string.study_start_learning_detail), stringResource(R.string.home_learn_new))
        AndroidHomePrimaryAction.DailyComplete -> Triple(stringResource(R.string.home_open_library), stringResource(R.string.study_daily_complete_detail), stringResource(R.string.home_open_library))
        AndroidHomePrimaryAction.OpenLibrary -> Triple(stringResource(R.string.home_choose_content), stringResource(R.string.study_open_library_detail), stringResource(R.string.home_open_library))
    }
    LearningEngineHeroCard(
        icon = if (presentation.hasActiveSession) Icons.Default.PlayArrow else Icons.Default.School,
        eyebrow = if (presentation.hasActiveSession) stringResource(R.string.home_active_learning) else stringResource(R.string.home_ready_to_study),
        title = model.contextTitle ?: title,
        detail = detail,
        actionLabel = actionLabel,
        onAction = {
            when (val action = model.primaryAction) {
                is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(action.sessionId))
                AndroidHomePrimaryAction.ReviewDue,
                AndroidHomePrimaryAction.StartLearning -> onStudyLauncher()
                AndroidHomePrimaryAction.DailyComplete -> onLibrary()
                AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HomeDashboardStats(presentation: AndroidLearningLandingPresentation) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        LearningEngineStatTile(stringResource(R.string.home_due_label), presentation.dueCount.toString(), Modifier.weight(1f))
        LearningEngineStatTile(stringResource(R.string.home_active_memories), presentation.activeMemoryCount.toString(), Modifier.weight(1f))
        LearningEngineStatTile(stringResource(R.string.home_reviewed_today), presentation.reviewedToday.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun DueReviewCard(model: AndroidHomeUiModel, onReview: () -> Unit) {
    val detail = stringResource(R.string.home_due_review_desc, model.dueCount)
    LearningEngineActionCard(Icons.Default.AutoStories, stringResource(R.string.home_due_label), detail, stringResource(R.string.home_due_review_action), onReview, Modifier.fillMaxWidth())
}

@Composable
private fun HomeLearningProgress(presentation: AndroidLearningLandingPresentation) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        LearningEngineSectionHeader(stringResource(R.string.home_progress_title))
        LearningEngineProgress(
            presentation.learningProgress,
            stringResource(
                R.string.home_active_memory_count,
                presentation.activeMemoryCount,
                presentation.totalMemoryCount
            )
        )
        presentation.dailyBudget?.let { daily ->
            Text(
                stringResource(
                    R.string.home_daily_new_progress,
                    daily.newCompletedToday,
                    daily.limits.newPerDay,
                    daily.reviewCompletedToday,
                    daily.limits.reviewPerDay
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier,
    onAutoPlay: (() -> Unit)? = null,
    isDifficult: (String) -> Boolean = { false },
    onToggleDifficult: ((String) -> Boolean)? = null,
    onSaveQuickEdit: ((AndroidPackageQuickEditDraft, (Result<Unit>) -> Unit) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val feedbackAudioController = remember(context) { AndroidAudioController(context) }
    val audioOwnership = remember { StudyAudioOwnership() }
    val isMuted by LearningEngineAudioPolicy.isMuted.collectAsState()
    var editingDraft by remember { mutableStateOf<AndroidPackageQuickEditDraft?>(null) }
    var difficultToggleCount by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner, state is AndroidStudyState.Typing, audioOwnership, feedbackAudioController) {
        vn.loi.learning.android.controller.StudyControllerBridge.onStudySurfaceChanged(true)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                audioOwnership.stopForForegroundLoss()
                feedbackAudioController.stop()
                if (state is AndroidStudyState.Typing) {
                    vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                        vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                    )
                }
                vn.loi.learning.android.controller.StudyControllerBridge.onActivityForegroundChanged(false)
            }
            if (state is AndroidStudyState.Typing) when (event) {
                Lifecycle.Event.ON_STOP -> onEvent(AndroidStudyEvent.PauseTyping)
                Lifecycle.Event.ON_START -> onEvent(AndroidStudyEvent.ResumeTyping)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioOwnership.stopForForegroundLoss()
            feedbackAudioController.stop()
            if (state is AndroidStudyState.Typing) {
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
            }
            vn.loi.learning.android.controller.StudyControllerBridge.onStudySurfaceChanged(false)
        }
    }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var outgoingFeedback by remember { mutableStateOf<OutgoingStudyFeedback?>(null) }
    var frozenIntroduction by remember { mutableStateOf<AndroidStudyState.Introduction?>(null) }
    var pendingIntroductionHudRating by remember { mutableStateOf<PendingIntroductionHudRating?>(null) }
    val reducedMotion = isReducedMotionEnabled()
    audioOwnership.update(
        authoritativeItemKey = (state as? AndroidStudyState.Runtime)?.let(::studyRuntimeItemKey),
        feedbackActive = outgoingFeedback != null
    )

    DisposableEffect(feedbackAudioController) {
        onDispose { feedbackAudioController.close() }
    }

    LaunchedEffect(outgoingFeedback?.feedbackId) {
        val feedback = outgoingFeedback ?: return@LaunchedEffect
        val audioFinished = CompletableDeferred<Unit>()
        val initialState = feedbackAudioController.replay(feedback.audio.path, isLooping = false) { audioState ->
            if (audioState is AndroidAudioState.Idle || audioState is AndroidAudioState.Failed) {
                audioFinished.complete(Unit)
            }
        }
        if (initialState is AndroidAudioState.Unavailable || initialState is AndroidAudioState.Failed) {
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(StudyRatingFeedbackPolicy.timeoutMillis) { audioFinished.await() }
        feedbackAudioController.stop()
        if (outgoingFeedback?.feedbackId == feedback.feedbackId) {
            outgoingFeedback = null
            frozenIntroduction = null
        }
    }

    LaunchedEffect(state, outgoingFeedback?.feedbackId, pendingIntroductionHudRating) {
        val pending = pendingIntroductionHudRating ?: return@LaunchedEffect
        val canonicalHud = (state as? AndroidStudyState.Runtime)?.hud
        if (outgoingFeedback == null && canonicalHud?.let(pending::isAcknowledgedBy) == true) {
            pendingIntroductionHudRating = null
        }
    }

    BackHandler(enabled = fullscreenImageUri != null) {
        fullscreenImageUri = null
    }
    BackHandler(enabled = editingDraft != null) { editingDraft = null }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val policy = androidLayoutPolicy(maxWidth.value.toInt(), maxHeight.value.toInt())
        CompositionLocalProvider(LocalLayoutPolicy provides policy) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val transitionSpec: AnimatedContentTransitionScope<AndroidStudyState>.() -> ContentTransform = {
                    if (initialState is AndroidStudyState.Runtime && targetState is AndroidStudyState.Runtime) {
                        val enterDuration = studyMotionDurationMillis(StudyMotionRole.CARD_ENTER, reducedMotion)
                        val exitDuration = studyMotionDurationMillis(StudyMotionRole.CARD_EXIT, reducedMotion)
                        (fadeIn(tween(enterDuration)) + slideInVertically(tween(enterDuration)) { it / 14 }) togetherWith
                                (fadeOut(tween(exitDuration)) + slideOutVertically(tween(exitDuration)) { -it / 14 })
                    } else if (reducedMotion) {
                        fadeIn(animationSpec = tween(durationMillis = 50)) togetherWith fadeOut(animationSpec = tween(durationMillis = 50))
                    } else {
                        val duration = studyMotionDurationMillis(StudyMotionRole.FEEDBACK, reducedMotion)
                        (fadeIn(animationSpec = tween(durationMillis = duration)) + slideInHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> fullWidth / 12 })
                            .togetherWith(fadeOut(animationSpec = tween(durationMillis = duration)) + slideOutHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> -fullWidth / 12 })
                    }
                }

                val presentedState = frozenIntroduction ?: state
                AnimatedContent(
                    targetState = presentedState,
                    contentKey = ::studyPresentationKey,
                    transitionSpec = transitionSpec,
                    label = "study destination"
                ) { target ->
                    when (target) {
                        AndroidStudyState.Loading -> LoadingStudy()
                        is AndroidStudyState.PreparingMode -> PreparingStudyMode(target)
                        is AndroidStudyState.Home -> HomeScreen(target, onEvent = onEvent)
                        is AndroidStudyState.Completion -> Completion(target, onEvent)
                        is AndroidStudyState.Failed -> StudyFailureState(target, onEvent)
                        is AndroidStudyState.Runtime -> {
                            val currentContentId = (target as? AndroidStudyState.Introduction)?.contentId
                                ?: target.plan?.contentId?.value
                            val difficult = currentContentId?.let { difficultToggleCount; isDifficult(it) } ?: false
                            StudyRuntimeScreen(
                                state = target,
                                onEvent = onEvent,
                                audioOwnership = audioOwnership,
                                audioOwnerToken = audioOwnership.tokenFor(studyRuntimeItemKey(target)),
                                autoplayGateOpen = outgoingFeedback == null,
                                feedbackRating = outgoingFeedback
                                    ?.takeIf { it.learningItemId == (target as? AndroidStudyState.Introduction)?.learningItemId }
                                    ?.selectedRating,
                                feedbackOrigin = outgoingFeedback?.origin,
                                pendingIntroductionHudRating = pendingIntroductionHudRating,
                                onIntroductionRatingWithFeedback = { introduction, rating, focus, origin ->
                                    frozenIntroduction = introduction.copy(
                                        revealedStage = true,
                                        compactRatingExit = !introduction.revealed
                                    )
                                    val feedback = outgoingStudyFeedback(introduction, rating, focus, origin)
                                    outgoingFeedback = feedback
                                    introduction.hud?.let { hud ->
                                        pendingIntroductionHudRating = optimisticIntroductionRating(hud, feedback)
                                    }
                                    onEvent(AndroidStudyEvent.RateIntroduction(rating))
                                },
                                onOpenFullscreenImage = { fullscreenImageUri = it },
                                onAutoPlay = onAutoPlay,
                                isMuted = isMuted,
                                onToggleMute = LearningEngineAudioPolicy::toggleMuted,
                                isDifficult = difficult,
                                onToggleDifficult = if (currentContentId != null && onToggleDifficult != null) ({
                                    onToggleDifficult(currentContentId)
                                    difficultToggleCount++
                                }) else null,
                                onEditItem = if (currentContentId != null && onSaveQuickEdit != null) ({
                                    editingDraft = AndroidPackageQuickEditDraft(
                                        currentContentId,
                                        target.meaning.orEmpty(),
                                        (target as? AndroidStudyState.Introduction)?.answerText
                                            ?: target.plan?.answerContract?.canonicalAnswer.orEmpty(),
                                        target.pronunciation.orEmpty(), target.partOfSpeech.orEmpty(),
                                        target.example.orEmpty(), target.translation.orEmpty()
                                    )
                                }) else null
                            )
                        }
                    }
                }

                editingDraft?.let { draft ->
                    StudyQuickEditDialog(draft, onDismiss = { editingDraft = null }) { updated, callback ->
                        onSaveQuickEdit?.invoke(updated) { result ->
                            callback(result)
                            if (result.isSuccess) editingDraft = null
                        } ?: callback(Result.failure(IllegalStateException("Editor unavailable")))
                    }
                }

                fullscreenImageUri?.let { imagePath ->
                    FullscreenLearningImage(
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
    is AndroidStudyState.PreparingMode -> "preparing-${state.mode.name}"
    is AndroidStudyState.Introduction -> "runtime-intro-${state.learningItemId}"
    is AndroidStudyState.Runtime -> "runtime-${state.requireRecallPlan().planId.value}"
    is AndroidStudyState.Completion -> "completion"
    is AndroidStudyState.Failed -> "failure"
    is AndroidStudyState.Home -> "home"
}

private fun studyRuntimeItemKey(state: AndroidStudyState.Runtime): String = when (state) {
    is AndroidStudyState.Introduction -> state.presentationVisitId ?: state.learningItemId
    else -> state.requireRecallPlan().planId.value
}

private fun AndroidStudyState.Runtime.requireRecallPlan() =
    requireNotNull(plan) { "Recall runtime state must provide a RecallPlan" }

@Composable
private fun LoadingStudy() {
    LearningEngineLoadingState(label = "Preparing Study…")
}

@Composable
private fun PreparingStudyMode(state: AndroidStudyState.PreparingMode) {
    val label = when (state.mode) {
        StudyMode.LEARN_NEW -> "Preparing Learn new…"
        StudyMode.ADAPTIVE -> "Preparing Adaptive study…"
        StudyMode.TYPING -> "Preparing Typing practice…"
    }
    LearningEngineLoadingState(label = label)
}

internal enum class AudioRole { PROMPT, EXPECTED_ANSWER, MEANING, EXAMPLE_ENGLISH, EXAMPLE_VIETNAMESE }

internal data class MultipleChoiceRevealAudioRoute(
    val role: AudioRole,
    val path: String
)

private fun looksVietnameseText(text: String?): Boolean {
    val value = text?.trim().orEmpty()
    if (value.isEmpty()) return false
    val vietnameseCharacters =
        "ăâđêôơưĂÂĐÊÔƠƯ" +
                "àáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệ" +
                "ìíỉĩịòóỏõọồốổỗộờớởỡợ" +
                "ùúủũụừứửữựỳýỷỹỵ" +
                "ÀÁẢÃẠẰẮẲẴẶẦẤẨẪẬÈÉẺẼẸỀẾỂỄỆ" +
                "ÌÍỈĨỊÒÓỎÕỌỒỐỔỖỘỜỚỞỠỢ" +
                "ÙÚỦŨỤỪỨỬỮỰỲÝỶỸỴ"
    return value.any { it in vietnameseCharacters }
}

/**
 * Resolves which side of an MCQ should be spoken when a WRONG answer opens
 * the full-answer reveal:
 *
 * EN question -> VI choices -> speak the Vietnamese correct answer.
 * VI question -> EN choices -> speak the English correct answer.
 *
 * Prefer the authoritative audio-path relationship.  If prompt audio is a
 * dedicated asset instead of reusing one side's primary audio, use the
 * selected-choice language only as a fallback because every choice in one MCQ
 * is generated on the same answer side.
 */
internal fun multipleChoiceWrongRevealAudioRoute(
    promptAudio: String?,
    englishAnswerAudio: String?,
    vietnameseAnswerAudio: String?,
    selectedChoiceText: String?
): MultipleChoiceRevealAudioRoute? {
    val prompt = promptAudio?.takeIf { it.isNotBlank() }
    val english = englishAnswerAudio?.takeIf { it.isNotBlank() }
    val vietnamese = vietnameseAnswerAudio?.takeIf { it.isNotBlank() }

    return when {
        prompt != null && english != null && prompt == english && vietnamese != null ->
            MultipleChoiceRevealAudioRoute(AudioRole.MEANING, vietnamese)

        prompt != null && vietnamese != null && prompt == vietnamese && english != null ->
            MultipleChoiceRevealAudioRoute(AudioRole.EXPECTED_ANSWER, english)

        looksVietnameseText(selectedChoiceText) && vietnamese != null ->
            MultipleChoiceRevealAudioRoute(AudioRole.MEANING, vietnamese)

        !selectedChoiceText.isNullOrBlank() && english != null ->
            MultipleChoiceRevealAudioRoute(AudioRole.EXPECTED_ANSWER, english)

        vietnamese != null && english == null ->
            MultipleChoiceRevealAudioRoute(AudioRole.MEANING, vietnamese)

        english != null && vietnamese == null ->
            MultipleChoiceRevealAudioRoute(AudioRole.EXPECTED_ANSWER, english)

        else -> null
    }
}

internal data class RevealExamplePair(
    val english: String?,
    val vietnamese: String?
)

/**
 * Normal source data already stores Example and Translation separately.
 * Some legacy/preview states can still carry both lines in `example` while
 * `translation` is blank.  Split only that fallback shape, so normal content
 * is never rewritten.
 */
internal fun resolveRevealExamplePair(
    englishExample: String?,
    vietnameseExample: String?
): RevealExamplePair {
    val english = englishExample?.trim()?.takeIf { it.isNotBlank() }
    val vietnamese = vietnameseExample?.trim()?.takeIf { it.isNotBlank() }

    if (english == null || vietnamese != null) {
        return RevealExamplePair(english, vietnamese)
    }

    val nonBlankLines = english
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()

    if (nonBlankLines.size < 2) {
        return RevealExamplePair(english, null)
    }

    val firstVietnameseLine = nonBlankLines.indexOfFirst(::looksVietnameseText)
    if (firstVietnameseLine <= 0) {
        return RevealExamplePair(english, null)
    }

    return RevealExamplePair(
        english = nonBlankLines.take(firstVietnameseLine).joinToString("\n"),
        vietnamese = nonBlankLines.drop(firstVietnameseLine).joinToString("\n")
    )
}

internal data class IntroductionExampleAudioRoute(
    val role: AudioRole,
    val path: String?,
    val isLooping: Boolean
)

internal fun introductionExampleAudioRoute(
    vietnamese: Boolean,
    englishPath: String?,
    vietnamesePath: String?
): IntroductionExampleAudioRoute = if (vietnamese) {
    IntroductionExampleAudioRoute(AudioRole.EXAMPLE_VIETNAMESE, vietnamesePath, false)
} else {
    IntroductionExampleAudioRoute(AudioRole.EXAMPLE_ENGLISH, englishPath, true)
}

private fun Modifier.introductionStageGestures(
    itemKey: String,
    alreadySubmitted: Boolean,
    ratingEnabled: Boolean,
    navigationEnabled: Boolean,
    gatedUpwardNavigation: Boolean,
    revealed: Boolean,
    historyPreview: Boolean,
    onDragOffset: (Float) -> Unit,
    onHorizontalDragOffset: (Float) -> Unit,
    onGestureEnd: (IntroductionStageGesture) -> Unit,
    onSwipeGood: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) = pointerInput(
    itemKey,
    alreadySubmitted,
    ratingEnabled,
    navigationEnabled,
    gatedUpwardNavigation,
    revealed,
    historyPreview
) {
    val swipeThresholdPx = 44.dp.toPx()
    val tapSlopPx = 12.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val gestureStartedAtMillis = android.os.SystemClock.uptimeMillis()
        var end = down.position
        var gestureDurationMillis = 0L
        var childConsumed = down.isConsumed
        var pressed = true
        var ownsUpwardDrag = false
        var ownsHorizontalDrag = false
        while (pressed) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            gestureDurationMillis = android.os.SystemClock.uptimeMillis() - gestureStartedAtMillis
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            end = change.position
            childConsumed = childConsumed || change.isConsumed
            val deltaX = end.x - down.position.x
            val deltaY = end.y - down.position.y
            val absX = kotlin.math.abs(deltaX)
            val absY = kotlin.math.abs(deltaY)
            if (navigationEnabled && !ownsUpwardDrag && absX > tapSlopPx && absX > absY * 1.25f) {
                ownsHorizontalDrag = true
            }
            if (ownsHorizontalDrag) {
                change.consume()
                onHorizontalDragOffset(deltaX)
            } else if ((ratingEnabled || gatedUpwardNavigation || (historyPreview && navigationEnabled) || navigationEnabled) &&
                !ownsHorizontalDrag && deltaY < -tapSlopPx && absY > absX * 1.25f
            ) {
                ownsUpwardDrag = true
                change.consume()
                onDragOffset(deltaY.coerceAtMost(0f))
            }
            pressed = change.pressed
        }
        val gesture = resolveIntroductionStageGesture(
            deltaX = end.x - down.position.x,
            deltaY = end.y - down.position.y,
            swipeThresholdPx = swipeThresholdPx,
            tapSlopPx = tapSlopPx,
            scrollRequired = false,
            childConsumed = childConsumed && !ownsUpwardDrag && !ownsHorizontalDrag,
            alreadySubmitted = alreadySubmitted,
            ratingEnabled = ratingEnabled,
            navigationEnabled = navigationEnabled,
            gatedUpwardNavigation = gatedUpwardNavigation,
            gestureDurationMillis = gestureDurationMillis
        )
        when (gesture) {
            IntroductionStageGesture.TAP -> {
                onGestureEnd(gesture)
            }
            IntroductionStageGesture.SWIPE_GOOD -> {
                onSwipeGood()
                onGestureEnd(gesture)
            }
            IntroductionStageGesture.NONE -> onGestureEnd(gesture)
            IntroductionStageGesture.PREVIOUS -> { onGestureEnd(gesture); onPrevious() }
            IntroductionStageGesture.NEXT -> { onGestureEnd(gesture); onNext() }
        }
    }
}

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    audioOwnership: StudyAudioOwnership,
    audioOwnerToken: StudyAudioOwnerToken,
    autoplayGateOpen: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    pendingIntroductionHudRating: PendingIntroductionHudRating?,
    onIntroductionRatingWithFeedback: (
        AndroidStudyState.Introduction,
        ReviewRating,
        IntroductionPlaybackFocus,
        IntroductionRatingFeedbackOrigin
    ) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    onAutoPlay: (() -> Unit)? = null,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    isDifficult: Boolean = false,
    onToggleDifficult: (() -> Unit)? = null,
    onEditItem: (() -> Unit)? = null
) {
    var limitEditor by rememberSaveable { mutableStateOf<String?>(null) }
    val quickReview = state is AndroidStudyState.Introduction && state.focusedPracticeKind ==
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW
    val focusedSkimUx = state is AndroidStudyState.Introduction && usesFocusedSkimUx(state.focusedPracticeKind)
    val recallModeLabel = when (state) {
        is AndroidStudyState.Introduction -> when {
            quickReview -> "Quick Review"
            state.focusedPracticeKind ==
                    vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT -> "Again / Hard"
            else -> "NEW · PACKAGE"
        }
        is AndroidStudyState.Typing -> "Typing"
        is AndroidStudyState.MultipleChoice -> "MCQ"
        is AndroidStudyState.Listening -> "Listening"
        is AndroidStudyState.ImageRecall -> "Image"
        is AndroidStudyState.ExampleCompletion -> "Cloze"
    }
    val modeLabel = androidStudyRuntimeModeLabel(state, recallModeLabel)

    val context = LocalContext.current
    val audioController = remember(context) { AndroidAudioController(context) }
    val itemKey = studyRuntimeItemKey(state)
    var activeRole by remember(itemKey) { mutableStateOf<AudioRole?>(null) }
    var introductionPlaybackFocus by remember(itemKey) { mutableStateOf(IntroductionPlaybackFocus.WORD) }
    var typingLoopFocus by remember(itemKey) { mutableStateOf<IntroductionPlaybackFocus?>(null) }
    var listeningLoopFocus by remember(itemKey) { mutableStateOf<IntroductionPlaybackFocus?>(null) }
    var imageRecallLoopFocus by remember(itemKey) { mutableStateOf<IntroductionPlaybackFocus?>(null) }
    var introductionImageExpanded by remember(itemKey) { mutableStateOf(false) }
    var swipeRatingSubmitted by remember(itemKey) { mutableStateOf(false) }
    var revealAudioStarted by rememberSaveable(itemKey) { mutableStateOf(false) }
    var quickReviewTransitionPending by remember(itemKey) { mutableStateOf(false) }
    var quickReviewQuestionPlaying by remember(itemKey) { mutableStateOf(false) }
    var multipleChoiceWrongRevealReady by remember(itemKey) { mutableStateOf(false) }
    var listeningCompactSuccessRendered by remember(itemKey) { mutableStateOf(false) }
    var imageRecallCompactSuccessRendered by remember(itemKey) { mutableStateOf(false) }
    var quickReviewTransitionGeneration by remember { mutableLongStateOf(0L) }
    val foregroundAudioOwner = remember(audioController, itemKey) { Any() }

    LaunchedEffect(itemKey) {
        AndroidTypingSuccessTrace.activePlanId()?.takeIf { it != itemKey }?.let { completedPlanId ->
            AndroidTypingSuccessTrace.event(
                "nextVisible",
                completedPlanId,
                true,
                true,
                activeRole,
                "nextItem=$itemKey"
            )
        }
    }

    DisposableEffect(audioController, itemKey) {
        audioOwnership.registerForegroundStop(foregroundAudioOwner) {
            quickReviewTransitionGeneration++
            quickReviewTransitionPending = false
            quickReviewQuestionPlaying = false
            activeRole = null
            audioController.stop()
        }
        onDispose {
            audioOwnership.unregisterForegroundStop(foregroundAudioOwner)
            quickReviewTransitionGeneration++
            quickReviewTransitionPending = false
            quickReviewQuestionPlaying = false
            audioController.stop()
        }
    }

    val playAudio: (AudioRole, String?, Boolean) -> Unit = { role, path, isLooping ->
        if (!quickReviewTransitionPending && audioOwnership.permitsManualPlayback(audioOwnerToken) && !path.isNullOrBlank()) {
            if (state is AndroidStudyState.ImageRecall && state.revealed) {
                imageRecallLoopFocus = when (role) {
                    AudioRole.EXPECTED_ANSWER -> IntroductionPlaybackFocus.WORD.takeIf { isLooping }
                    AudioRole.EXAMPLE_ENGLISH -> IntroductionPlaybackFocus.EXAMPLE.takeIf { isLooping }
                    AudioRole.MEANING, AudioRole.EXAMPLE_VIETNAMESE, AudioRole.PROMPT -> null
                }
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
            }
            if (state is AndroidStudyState.Listening && state.revealed) {
                listeningLoopFocus = when (role) {
                    AudioRole.EXPECTED_ANSWER -> IntroductionPlaybackFocus.WORD.takeIf { isLooping }
                    AudioRole.EXAMPLE_ENGLISH -> IntroductionPlaybackFocus.EXAMPLE.takeIf { isLooping }
                    AudioRole.MEANING, AudioRole.EXAMPLE_VIETNAMESE, AudioRole.PROMPT -> null
                }
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
            }
            if (state is AndroidStudyState.Typing && state.revealed) {
                typingLoopFocus = when (role) {
                    AudioRole.EXPECTED_ANSWER -> IntroductionPlaybackFocus.WORD.takeIf { isLooping }
                    AudioRole.EXAMPLE_ENGLISH -> IntroductionPlaybackFocus.EXAMPLE.takeIf { isLooping }
                    AudioRole.MEANING, AudioRole.EXAMPLE_VIETNAMESE, AudioRole.PROMPT -> null
                }
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
            }
            if (activeRole == role) {
                audioController.stop()
                activeRole = null
            } else {
                audioController.stop()
                activeRole = role
                when (role) {
                    AudioRole.EXPECTED_ANSWER -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.WORD
                    }
                    AudioRole.EXAMPLE_ENGLISH -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                    }
                    else -> Unit
                }
                audioController.replay(path, isLooping = isLooping) { state ->
                    if (state is AndroidAudioState.Idle || state is AndroidAudioState.Failed) {
                        if (audioOwnership.isCurrent(audioOwnerToken) && activeRole == role) activeRole = null
                    }
                }
            }
        }
    }

    val restartAudio: (AudioRole, String?, Boolean) -> Unit = { role, path, isLooping ->
        if (!quickReviewTransitionPending && audioOwnership.permitsManualPlayback(audioOwnerToken) && !path.isNullOrBlank()) {
            activeRole = role
            when (role) {
                AudioRole.EXPECTED_ANSWER -> {
                    introductionPlaybackFocus = IntroductionPlaybackFocus.WORD
                }
                AudioRole.EXAMPLE_ENGLISH -> {
                    introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                }
                else -> Unit
            }
            vn.loi.learning.android.controller.StudyControllerBridge.playAudio(
                itemKey = itemKey,
                path = path,
                role = role.name,
                isLooping = isLooping,
                reason = if (isLooping) vn.loi.learning.android.controller.StudyAudioReason.MANUAL_LOOP
                else vn.loi.learning.android.controller.StudyAudioReason.MANUAL_PLAY
            )
        }
    }

    val toggleIntroductionEnglishLoop: () -> Unit = {
        val introduction = state as? AndroidStudyState.Introduction
        if (introduction != null && introduction.revealed && !quickReviewTransitionPending) {
            nextIntroductionPlaybackFocus(
                introductionPlaybackFocus,
                hasWordAudio = !introduction.resolvedExpectedAnswerAudio.isNullOrBlank() ||
                        !introduction.resolvedPromptAudio.isNullOrBlank(),
                hasExampleAudio = !introduction.resolvedExampleEnglishAudio.isNullOrBlank()
            )?.let { nextFocus ->
                if (nextFocus == IntroductionPlaybackFocus.WORD) {
                    restartAudio(
                        AudioRole.EXPECTED_ANSWER,
                        introduction.resolvedExpectedAnswerAudio ?: introduction.resolvedPromptAudio,
                        true
                    )
                } else {
                    restartAudio(AudioRole.EXAMPLE_ENGLISH, introduction.resolvedExampleEnglishAudio, true)
                }
            }
        }
    }

    val toggleTypingRevealedAudioLoop: () -> Unit = {
        val typing = state as? AndroidStudyState.Typing
        if (typing != null && typing.revealed && !typing.completionPending) {
            val nextFocus = when (typingLoopFocus) {
                null, IntroductionPlaybackFocus.EXAMPLE -> IntroductionPlaybackFocus.WORD
                IntroductionPlaybackFocus.WORD -> IntroductionPlaybackFocus.EXAMPLE
            }
            val path = when (nextFocus) {
                IntroductionPlaybackFocus.WORD -> typing.resolvedExpectedAnswerAudio
                IntroductionPlaybackFocus.EXAMPLE -> typing.resolvedExampleEnglishAudio
            }
            if (!path.isNullOrBlank()) {
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
                audioController.stop()
                activeRole = null
                typingLoopFocus = nextFocus
                if (nextFocus == IntroductionPlaybackFocus.WORD) {
                    restartAudio(AudioRole.EXPECTED_ANSWER, path, true)
                } else {
                    restartAudio(AudioRole.EXAMPLE_ENGLISH, path, true)
                }
            }
        }
    }

    val toggleListeningRevealedAudioLoop: () -> Unit = {
        val listening = state as? AndroidStudyState.Listening
        if (listening != null && listening.revealed && !listening.completionPending) {
            val nextFocus = when (listeningLoopFocus) {
                null, IntroductionPlaybackFocus.EXAMPLE -> IntroductionPlaybackFocus.WORD
                IntroductionPlaybackFocus.WORD -> IntroductionPlaybackFocus.EXAMPLE
            }
            val path = when (nextFocus) {
                IntroductionPlaybackFocus.WORD -> listening.resolvedExpectedAnswerAudio
                IntroductionPlaybackFocus.EXAMPLE -> listening.resolvedExampleEnglishAudio
            }
            if (!path.isNullOrBlank()) {
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
                audioController.stop()
                activeRole = null
                listeningLoopFocus = nextFocus
                if (nextFocus == IntroductionPlaybackFocus.WORD) {
                    restartAudio(
                        AudioRole.EXPECTED_ANSWER,
                        path,
                        true
                    )
                } else {
                    restartAudio(AudioRole.EXAMPLE_ENGLISH, path, true)
                }
            }
        }
    }

    val toggleImageRecallRevealedAudioLoop: () -> Unit = {
        val imageRecall = state as? AndroidStudyState.ImageRecall
        if (imageRecall != null && imageRecall.revealed && !imageRecall.completionPending) {
            val nextFocus = when (imageRecallLoopFocus) {
                null, IntroductionPlaybackFocus.EXAMPLE -> IntroductionPlaybackFocus.WORD
                IntroductionPlaybackFocus.WORD -> IntroductionPlaybackFocus.EXAMPLE
            }
            val path = when (nextFocus) {
                IntroductionPlaybackFocus.WORD -> imageRecall.resolvedExpectedAnswerAudio
                IntroductionPlaybackFocus.EXAMPLE -> imageRecall.resolvedExampleEnglishAudio
            }
            if (!path.isNullOrBlank()) {
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
                audioController.stop()
                activeRole = null
                imageRecallLoopFocus = nextFocus
                if (nextFocus == IntroductionPlaybackFocus.WORD) {
                    restartAudio(
                        AudioRole.EXPECTED_ANSWER,
                        path,
                        true
                    )
                } else {
                    restartAudio(AudioRole.EXAMPLE_ENGLISH, path, true)
                }
            }
        }
    }

    val stopAudioAndDispatch: (AndroidStudyEvent) -> Unit = { event ->
        vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
            vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
        )
        activeRole = null
        onEvent(event)
    }
    val submitIntroductionRating: (ReviewRating, IntroductionRatingFeedbackOrigin) -> Unit = { rating, origin ->
        if (state is AndroidStudyState.Introduction && (!state.historyPreview || state.navigation.canCorrectRating) &&
            !swipeRatingSubmitted && !quickReviewTransitionPending
        ) {
            swipeRatingSubmitted = true
            audioController.stop()
            vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
            )
            activeRole = null
            onIntroductionRatingWithFeedback(state, rating, introductionPlaybackFocus, origin)
        }
    }
    val startFocusedPracticeQuestionGate: (AndroidStudyEvent) -> Unit = { terminalEvent ->
        val introduction = state as? AndroidStudyState.Introduction
        if (introduction != null && focusedSkimUx && introduction.revealed && !introduction.historyPreview &&
            !quickReviewTransitionPending
        ) {
            quickReviewTransitionPending = true
            quickReviewQuestionPlaying = false
            audioController.stop()
            vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
            )
            activeRole = null
            val acceptedItemKey = itemKey
            val generation = ++quickReviewTransitionGeneration
            var finished = false
            val finish: () -> Unit = finish@{
                if (finished || generation != quickReviewTransitionGeneration || itemKey != acceptedItemKey) return@finish
                finished = true
                quickReviewTransitionPending = false
                quickReviewQuestionPlaying = false
                onEvent(terminalEvent)
            }
            val initial = audioController.replay(
                path = introduction.resolvedPromptAudio,
                isLooping = false,
                onPlaybackEvent = { event ->
                    if (event is AndroidAudioPlaybackEvent.Completed) finish()
                },
                onState = { playbackState ->
                    if (generation == quickReviewTransitionGeneration && itemKey == acceptedItemKey) {
                        when (playbackState) {
                            AndroidAudioState.Playing -> quickReviewQuestionPlaying = true
                            is AndroidAudioState.Failed, AndroidAudioState.Unavailable -> finish()
                            else -> Unit
                        }
                    }
                }
            )
            if (initial is AndroidAudioState.Failed || initial is AndroidAudioState.Unavailable) finish()
        }
    }

    LaunchedEffect(
        itemKey,
        audioOwnerToken,
        autoplayGateOpen,
        (state as? AndroidStudyState.Introduction)?.revealed,
        (state as? AndroidStudyState.Introduction)?.historyPreview
    ) {
        val introduction = state as? AndroidStudyState.Introduction ?: return@LaunchedEffect
        if (!introduction.revealed && !introduction.historyPreview && autoplayGateOpen &&
            !introduction.resolvedMeaningAudio.isNullOrBlank() &&
            audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.MEANING)
        ) {
            restartAudio(AudioRole.MEANING, introduction.resolvedMeaningAudio, false)
        }
    }

    LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen) {
        val typing = state as? AndroidStudyState.Typing ?: return@LaunchedEffect
        if (autoplayGateOpen &&
            !typing.completed && !typing.revealed && !typing.viAutoplayMuted &&
            !typing.resolvedMeaningAudio.isNullOrBlank() &&
            audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.MEANING)
        ) {
            restartAudio(AudioRole.MEANING, typing.resolvedMeaningAudio, false)
        }
    }

    DisposableEffect(audioController, state, quickReviewTransitionPending, onAutoPlay) {
        val replayer: () -> Boolean = {
            if (!quickReviewTransitionPending) {
                val promptAudio = when (state) {
                    is AndroidStudyState.Introduction -> state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio
                    is AndroidStudyState.Typing -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.MultipleChoice -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.Listening -> state.resolvedPromptAudio
                    is AndroidStudyState.ImageRecall -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.ExampleCompletion -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                }
                if (!promptAudio.isNullOrBlank()) {
                    val role = if (state is AndroidStudyState.Introduction) AudioRole.EXPECTED_ANSWER else AudioRole.PROMPT
                    restartAudio(role, promptAudio, false)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        val exampleEnglishPlayer: () -> Boolean = {
            val exampleAudio = when (state) {
                is AndroidStudyState.Introduction -> state.resolvedExampleEnglishAudio
                is AndroidStudyState.Typing -> state.resolvedExampleEnglishAudio
                is AndroidStudyState.MultipleChoice -> state.resolvedExampleEnglishAudio
                is AndroidStudyState.Listening -> state.resolvedExampleEnglishAudio
                is AndroidStudyState.ImageRecall -> state.resolvedExampleEnglishAudio
                is AndroidStudyState.ExampleCompletion -> state.resolvedExampleEnglishAudio
            }
            if (!exampleAudio.isNullOrBlank()) {
                restartAudio(AudioRole.EXAMPLE_ENGLISH, exampleAudio, false)
                true
            } else false
        }

        val exampleVietnamesePlayer: () -> Boolean = {
            val exampleAudio = when (state) {
                is AndroidStudyState.Introduction -> state.resolvedExampleVietnameseAudio
                is AndroidStudyState.Typing -> state.resolvedExampleVietnameseAudio
                is AndroidStudyState.MultipleChoice -> state.resolvedExampleVietnameseAudio
                is AndroidStudyState.Listening -> state.resolvedExampleVietnameseAudio
                is AndroidStudyState.ImageRecall -> state.resolvedExampleVietnameseAudio
                is AndroidStudyState.ExampleCompletion -> state.resolvedExampleVietnameseAudio
            }
            if (!exampleAudio.isNullOrBlank()) {
                restartAudio(AudioRole.EXAMPLE_VIETNAMESE, exampleAudio, false)
                true
            } else false
        }

        val primaryEnglishPlayer: () -> Boolean = {
            if (!quickReviewTransitionPending) {
                val promptAudio = when (state) {
                    is AndroidStudyState.Introduction -> state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio
                    is AndroidStudyState.Typing -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.MultipleChoice -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.Listening -> state.resolvedPromptAudio
                    is AndroidStudyState.ImageRecall -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                    is AndroidStudyState.ExampleCompletion -> state.resolvedPromptAudio ?: state.resolvedExpectedAnswerAudio
                }
                if (!promptAudio.isNullOrBlank()) {
                    val role = if (state is AndroidStudyState.Introduction) AudioRole.EXPECTED_ANSWER else AudioRole.PROMPT
                    restartAudio(role, promptAudio, false)
                    true
                } else false
            } else false
        }

        val primaryVietnamesePlayer: () -> Boolean = {
            if (!quickReviewTransitionPending) {
                val meaningAudio = when (state) {
                    is AndroidStudyState.Introduction -> state.resolvedMeaningAudio
                    is AndroidStudyState.Typing -> state.resolvedMeaningAudio
                    is AndroidStudyState.MultipleChoice -> state.resolvedMeaningAudio
                    is AndroidStudyState.Listening -> state.resolvedMeaningAudio
                    is AndroidStudyState.ImageRecall -> state.resolvedMeaningAudio
                    is AndroidStudyState.ExampleCompletion -> state.resolvedMeaningAudio
                }
                if (!meaningAudio.isNullOrBlank()) {
                    restartAudio(AudioRole.MEANING, meaningAudio, false)
                    true
                } else false
            } else false
        }

        val startAutoPlay: () -> Boolean = {
            if (onAutoPlay != null) {
                onAutoPlay()
                true
            } else false
        }

        vn.loi.learning.android.controller.StudyControllerBridge.registerAudioReplayer(replayer)
        vn.loi.learning.android.controller.StudyControllerBridge.registerPrimaryEnglishPlayer(primaryEnglishPlayer)
        vn.loi.learning.android.controller.StudyControllerBridge.registerPrimaryVietnamesePlayer(primaryVietnamesePlayer)
        vn.loi.learning.android.controller.StudyControllerBridge.registerExampleEnglishPlayer(exampleEnglishPlayer)
        vn.loi.learning.android.controller.StudyControllerBridge.registerExampleVietnamesePlayer(exampleVietnamesePlayer)
        vn.loi.learning.android.controller.StudyControllerBridge.registerStartAutoPlay(startAutoPlay)
        onDispose {
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterAudioReplayer(replayer)
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterPrimaryEnglishPlayer(primaryEnglishPlayer)
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterPrimaryVietnamesePlayer(primaryVietnamesePlayer)
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterExampleEnglishPlayer(exampleEnglishPlayer)
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterExampleVietnamesePlayer(exampleVietnamesePlayer)
            vn.loi.learning.android.controller.StudyControllerBridge.unregisterStartAutoPlay(startAutoPlay)
        }
    }

    LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen) {
        if (state is AndroidStudyState.Listening &&
            !state.completed && !state.resolvedPromptAudio.isNullOrBlank()
            && audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.PROMPT)
        ) {
            restartAudio(AudioRole.PROMPT, state.resolvedPromptAudio, false)
        }
    }

    LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen) {
        val multipleChoice = state as? AndroidStudyState.MultipleChoice ?: return@LaunchedEffect
        if (shouldAutoplayMultipleChoiceQuestion(multipleChoice.completed, multipleChoice.resolvedPromptAudio) &&
            autoplayGateOpen &&
            audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.PROMPT)
        ) {
            restartAudio(AudioRole.PROMPT, multipleChoice.resolvedPromptAudio, false)
        }
    }

    LaunchedEffect(itemKey, (state as? AndroidStudyState.MultipleChoice)?.completed,
        (state as? AndroidStudyState.MultipleChoice)?.outcome) {
        val multipleChoice = state as? AndroidStudyState.MultipleChoice ?: return@LaunchedEffect
        if (!multipleChoice.completed) return@LaunchedEffect
        val feedbackStartedAt = System.currentTimeMillis()
        when (multipleChoice.outcome) {
            RecallOutcome.CORRECT -> {
                vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
                    vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
                )
                audioController.stop()
                activeRole = null
                val audioFinished = CompletableDeferred<Unit>()
                val initial = audioController.replay(
                    multipleChoice.resolvedPromptAudio,
                    isLooping = false,
                    onPlaybackEvent = { event ->
                        if (event is AndroidAudioPlaybackEvent.Completed) audioFinished.complete(Unit)
                    },
                    onState = { playback ->
                        when (playback) {
                            AndroidAudioState.Playing -> activeRole = AudioRole.PROMPT
                            AndroidAudioState.Idle, AndroidAudioState.Unavailable,
                            is AndroidAudioState.Failed -> {
                                activeRole = null
                                audioFinished.complete(Unit)
                            }
                            else -> Unit
                        }
                    }
                )
                if (initial is AndroidAudioState.Unavailable || initial is AndroidAudioState.Failed) {
                    audioFinished.complete(Unit)
                }
                withTimeoutOrNull(MULTIPLE_CHOICE_AUDIO_WATCHDOG_MILLIS) { audioFinished.await() }
                val visibleMillis = System.currentTimeMillis() - feedbackStartedAt
                delay((MULTIPLE_CHOICE_MINIMUM_FEEDBACK_MILLIS - visibleMillis).coerceAtLeast(0L))
                onEvent(AndroidStudyEvent.NextVisited)
            }
            RecallOutcome.INCORRECT -> {
                delay(MULTIPLE_CHOICE_MINIMUM_FEEDBACK_MILLIS)
                multipleChoiceWrongRevealReady = true
            }
            else -> Unit
        }
    }

    LaunchedEffect(
        itemKey,
        audioOwnerToken,
        autoplayGateOpen,
        multipleChoiceWrongRevealReady,
        (state as? AndroidStudyState.MultipleChoice)?.outcome
    ) {
        val multipleChoice = state as? AndroidStudyState.MultipleChoice ?: return@LaunchedEffect
        if (!autoplayGateOpen ||
            !multipleChoiceWrongRevealReady ||
            multipleChoice.outcome != RecallOutcome.INCORRECT
        ) return@LaunchedEffect

        val route = multipleChoiceWrongRevealAudioRoute(
            promptAudio = multipleChoice.resolvedPromptAudio,
            englishAnswerAudio = multipleChoice.resolvedExpectedAnswerAudio,
            vietnameseAnswerAudio = multipleChoice.resolvedMeaningAudio,
            selectedChoiceText = selectedMultipleChoiceAnswer(multipleChoice)
        ) ?: return@LaunchedEffect

        if (audioOwnership.claimAutoplay(audioOwnerToken, route.role)) {
            audioController.stop()
            activeRole = null
            restartAudio(route.role, route.path, false)
        }
    }

    LaunchedEffect(
        itemKey,
        (state as? AndroidStudyState.ImageRecall)?.completionPending,
        imageRecallCompactSuccessRendered
    ) {
        val imageRecall = state as? AndroidStudyState.ImageRecall ?: return@LaunchedEffect
        if (!shouldStartListeningSuccessAutoAdvance(
                imageRecall.completionPending,
                imageRecall.outcome,
                imageRecallCompactSuccessRendered
            )
        ) return@LaunchedEffect

        withFrameNanos { }
        val successVisibleAt = System.currentTimeMillis()
        audioController.stop()
        activeRole = AudioRole.EXPECTED_ANSWER
        val audioFinished = CompletableDeferred<Unit>()
        val initial = audioController.replay(
            imageRecall.resolvedExpectedAnswerAudio ?: imageRecall.resolvedPromptAudio,
            isLooping = false,
            onPlaybackEvent = { event ->
                if (event is AndroidAudioPlaybackEvent.Completed) {
                    audioFinished.complete(Unit)
                }
            },
            onState = { playback ->
                when (playback) {
                    AndroidAudioState.Idle, AndroidAudioState.Unavailable,
                    is AndroidAudioState.Failed -> {
                        activeRole = null
                        audioFinished.complete(Unit)
                    }
                    else -> Unit
                }
            }
        )
        if (initial is AndroidAudioState.Unavailable || initial is AndroidAudioState.Failed) {
            activeRole = null
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(AndroidTypingSuccessPresentationPolicy.audioWatchdogMillis) {
            audioFinished.await()
        }
        val visibleMillis = System.currentTimeMillis() - successVisibleAt
        delay((AndroidTypingSuccessPresentationPolicy.minimumDwellMillis - visibleMillis).coerceAtLeast(0L))
        onEvent(AndroidStudyEvent.NextVisited)
    }

    LaunchedEffect(
        itemKey,
        (state as? AndroidStudyState.Listening)?.completionPending,
        listeningCompactSuccessRendered
    ) {
        val listening = state as? AndroidStudyState.Listening ?: return@LaunchedEffect
        if (!shouldStartListeningSuccessAutoAdvance(
                completionPending = listening.completionPending,
                outcome = listening.outcome,
                compactSuccessRendered = listeningCompactSuccessRendered
            )
        ) return@LaunchedEffect

        withFrameNanos { }
        val successVisibleAt = System.currentTimeMillis()
        audioController.stop()
        activeRole = AudioRole.EXPECTED_ANSWER
        val audioFinished = CompletableDeferred<Unit>()
        val initial = audioController.replay(
            listening.resolvedExpectedAnswerAudio ?: listening.resolvedPromptAudio,
            isLooping = false,
            onPlaybackEvent = { event ->
                if (event is AndroidAudioPlaybackEvent.Completed) audioFinished.complete(Unit)
            },
            onState = { playback ->
                when (playback) {
                    AndroidAudioState.Idle, AndroidAudioState.Unavailable,
                    is AndroidAudioState.Failed -> {
                        activeRole = null
                        audioFinished.complete(Unit)
                    }
                    else -> Unit
                }
            }
        )
        if (initial is AndroidAudioState.Unavailable || initial is AndroidAudioState.Failed) {
            activeRole = null
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(AndroidTypingSuccessPresentationPolicy.audioWatchdogMillis) {
            audioFinished.await()
        }
        val visibleMillis = System.currentTimeMillis() - successVisibleAt
        delay(
            (AndroidTypingSuccessPresentationPolicy.minimumDwellMillis - visibleMillis)
                .coerceAtLeast(0L)
        )
        onEvent(AndroidStudyEvent.NextVisited)
    }

    LaunchedEffect(itemKey, audioOwnerToken, autoplayGateOpen, (state as? AndroidStudyState.Introduction)?.revealed) {
        val introduction = state as? AndroidStudyState.Introduction ?: return@LaunchedEffect
        if (introduction.revealed && !revealAudioStarted &&
            audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.EXPECTED_ANSWER)
        ) {
            revealAudioStarted = true
            restartAudio(
                AudioRole.EXPECTED_ANSWER,
                introduction.resolvedExpectedAnswerAudio ?: introduction.resolvedPromptAudio,
                true
            )
        }
    }

    LaunchedEffect(
        itemKey,
        (state as? AndroidStudyState.Typing)?.revealed,
        (state as? AndroidStudyState.Typing)?.completionPending
    ) {
        val typing = state as? AndroidStudyState.Typing ?: return@LaunchedEffect
        if (!shouldStartTypingRevealAnswerAutoplay(
                revealed = typing.revealed,
                completionPending = typing.completionPending,
                alreadyStarted = revealAudioStarted
            )
        ) return@LaunchedEffect

        revealAudioStarted = true
        typingLoopFocus = null
        vn.loi.learning.android.controller.StudyControllerBridge.stopAudio(
            vn.loi.learning.android.controller.StudyAudioReason.CONTINUE_EXIT
        )
        audioController.stop()
        activeRole = null
        restartAudio(
            AudioRole.EXPECTED_ANSWER,
            typing.resolvedExpectedAnswerAudio,
            false
        )
    }

    LaunchedEffect(
        itemKey,
        (state as? AndroidStudyState.Listening)?.revealed,
        (state as? AndroidStudyState.Listening)?.completionPending
    ) {
        val listening = state as? AndroidStudyState.Listening ?: return@LaunchedEffect
        if (!listening.revealed || listening.completionPending || revealAudioStarted) {
            return@LaunchedEffect
        }
        revealAudioStarted = true
        audioController.stop()
        activeRole = null
        restartAudio(
            AudioRole.EXPECTED_ANSWER,
            listening.resolvedExpectedAnswerAudio,
            false
        )
    }

    LaunchedEffect(
        itemKey,
        (state as? AndroidStudyState.ImageRecall)?.revealed,
        (state as? AndroidStudyState.ImageRecall)?.completionPending
    ) {
        val imageRecall = state as? AndroidStudyState.ImageRecall ?: return@LaunchedEffect
        if (!imageRecall.revealed || imageRecall.completionPending || revealAudioStarted) {
            return@LaunchedEffect
        }
        revealAudioStarted = true
        audioController.stop()
        activeRole = null
        restartAudio(
            AudioRole.EXPECTED_ANSWER,
            imageRecall.resolvedExpectedAnswerAudio,
            false
        )
    }

    LaunchedEffect(itemKey, (state as? AndroidStudyState.Typing)?.completionPending) {
        val typing = state as? AndroidStudyState.Typing ?: return@LaunchedEffect
        if (!typing.completionPending) return@LaunchedEffect
        AndroidTypingSuccessTrace.event(
            "compactSuccessVisible", typing.plan.planId.value, false, false, activeRole
        )
        audioController.stop()
        activeRole = AudioRole.EXPECTED_ANSWER
        AndroidTypingSuccessTrace.event(
            "audioPrepare", typing.plan.planId.value, false, false, activeRole,
            "audio=${typing.resolvedExpectedAnswerAudio.orEmpty()}"
        )
        val audioFinished = CompletableDeferred<Unit>()
        val initial = audioController.replay(
            typing.resolvedExpectedAnswerAudio,
            isLooping = false,
            onPlaybackEvent = { event ->
                when (event) {
                    is AndroidAudioPlaybackEvent.Prepared -> AndroidTypingSuccessTrace.event(
                        "audioStart", typing.plan.planId.value, false, false, activeRole,
                        "durationMs=${event.durationMillis}"
                    )
                    is AndroidAudioPlaybackEvent.Completed -> AndroidTypingSuccessTrace.event(
                        "audioCompletionCallback", typing.plan.planId.value, true, false, activeRole,
                        "durationMs=${event.durationMillis} positionMs=${event.positionMillis}"
                    )
                }
            }
        ) { playback ->
            if (playback is AndroidAudioState.Idle || playback is AndroidAudioState.Failed) {
                if (playback is AndroidAudioState.Failed) {
                    AndroidTypingSuccessTrace.event(
                        "audioCompletionCallback", typing.plan.planId.value, true, false, activeRole,
                        "fallback=Failed reason=${playback.reason.orEmpty()}"
                    )
                }
                activeRole = null
                audioFinished.complete(Unit)
            }
        }
        if (initial is AndroidAudioState.Unavailable || initial is AndroidAudioState.Failed) {
            activeRole = null
            AndroidTypingSuccessTrace.event(
                "audioCompletionCallback", typing.plan.planId.value, true, false, activeRole,
                "fallback=${initial::class.simpleName}"
            )
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(AndroidTypingSuccessPresentationPolicy.audioWatchdogMillis) {
            audioFinished.await()
        }
        onEvent(AndroidStudyEvent.TypingSuccessAudioCompleted)
    }


    val isRevealed = when (state) {
        is AndroidStudyState.Introduction -> state.revealed
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.Listening -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }
    val typingSuccessPending = (state as? AndroidStudyState.Typing)?.completionPending == true ||
            (state as? AndroidStudyState.ImageRecall)?.completionPending == true
    val isEnded = state.completed || isRevealed
    val multipleChoicePath = (state as? AndroidStudyState.MultipleChoice)?.let {
        multipleChoiceCompletionPath(it.completed, it.outcome, multipleChoiceWrongRevealReady)
    }
    val preserveTypingIme = state is AndroidStudyState.Typing &&
            state.completionPending && state.outcome == RecallOutcome.CORRECT

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()
    val reducedMotion = isReducedMotionEnabled()

    LaunchedEffect(isEnded, itemKey) {
        if (isEnded && !preserveTypingIme) {
            keyboardController?.hide()
            focusManager.clearFocus()
            bringIntoViewRequester.bringIntoView()
        }
    }

    val layoutPolicy = LocalLayoutPolicy.current
    val contentDensity = when {
        layoutPolicy.maxMediaHeightDp <= 180 -> StudyContentDensity.DENSE
        layoutPolicy.maxMediaHeightDp >= 420 -> StudyContentDensity.RELAXED
        else -> StudyContentDensity.STANDARD
    }

    StudyRuntimeShell(
        title = state.contextTitle ?: "Study",
        modeLabel = modeLabel,
        currentPosition = if (focusedSkimUx) null else
            (state as? AndroidStudyState.Introduction)?.packagePosition ?: state.currentPosition,
        totalItems = if (focusedSkimUx) null else
            (state as? AndroidStudyState.Introduction)?.packageTotal ?: state.totalItems,
        onBack = { stopAudioAndDispatch(AndroidStudyEvent.Home) },
        onAutoPlay = onAutoPlay,
        header = {
            state.hud?.let { hud ->
                if (state is AndroidStudyState.Introduction) {
                    when (state.focusedPracticeKind) {
                        vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT ->
                            DifficultPracticeHud(hud)
                        vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW ->
                            QuickReviewProgressHeader(state, hud)
                        else -> LearnNewProgressHeader(
                            hud,
                            pendingIntroductionHudRating,
                            onEditNew = { limitEditor = "new" },
                            onEditReview = { limitEditor = "review" }
                        )
                    }
                }
                else LearningEngineCompactHud(hud)
            }
        }
    ) {
        LearningEngineLearningStage(
            modifier = (when (state) {
                is AndroidStudyState.Introduction,
                is AndroidStudyState.Typing,
                is AndroidStudyState.Listening,
                is AndroidStudyState.ImageRecall -> Modifier.fillMaxWidth().weight(1f)
                else -> Modifier.fillMaxWidth().verticalScroll(scrollState)
            }).reviewNavigationGestures(
                enabled = state !is AndroidStudyState.Introduction && isEnded && !typingSuccessPending &&
                        (state !is AndroidStudyState.MultipleChoice ||
                                multipleChoicePath == MultipleChoiceCompletionPath.WRONG_FULL_ANSWER),
                canPrevious = state.navigation.canPrevious,
                canNext = state.navigation.canNext,
                onPrevious = { stopAudioAndDispatch(AndroidStudyEvent.PreviousVisited) },
                onNext = { stopAudioAndDispatch(AndroidStudyEvent.NextVisited) }
            ),
            state = state,
            activeRole = activeRole,
            playAudio = playAudio,
            restartAudio = restartAudio,
            contentDensity = contentDensity,
            availableMediaHeightDp = layoutPolicy.maxMediaHeightDp * 2,
            revealBringIntoViewRequester = bringIntoViewRequester,
            introductionImageExpanded = introductionImageExpanded,
            swipeRatingSubmitted = swipeRatingSubmitted,
            quickReviewTransitionPending = quickReviewTransitionPending,
            quickReviewQuestionPlaying = quickReviewQuestionPlaying,
            feedbackRating = feedbackRating,
            feedbackOrigin = feedbackOrigin,
            onIntroductionImageExpandedChange = { introductionImageExpanded = it },
            onIntroductionStageTap = {
                if (state is AndroidStudyState.Introduction && !quickReviewTransitionPending) {
                    if (!state.revealed) {
                        stopAudioAndDispatch(AndroidStudyEvent.RevealIntroduction)
                    } else {
                        toggleIntroductionEnglishLoop()
                    }
                }
            },
            onIntroductionSwipeGood = {
                if (focusedSkimUx && state is AndroidStudyState.Introduction) {
                    when {
                        state.historyPreview -> stopAudioAndDispatch(AndroidStudyEvent.NextVisited)
                        state.revealed -> startFocusedPracticeQuestionGate(
                            if (quickReview) AndroidStudyEvent.QuickReviewUnratedAdvance
                            else AndroidStudyEvent.DifficultPracticeAdvance
                        )
                        else -> stopAudioAndDispatch(
                            if (quickReview) AndroidStudyEvent.QuickReviewUnratedAdvance
                            else AndroidStudyEvent.DifficultPracticeAdvance
                        )
                    }
                } else if (state is AndroidStudyState.Introduction && state.historyPreview) {
                    if (state.navigation.canNext) stopAudioAndDispatch(AndroidStudyEvent.NextVisited)
                } else {
                    submitIntroductionRating(ReviewRating.GOOD, IntroductionRatingFeedbackOrigin.SWIPE_GOOD)
                }
            },
            onIntroductionPrevious = {
                if (!quickReviewTransitionPending && state.navigation.canPrevious) {
                    stopAudioAndDispatch(AndroidStudyEvent.PreviousVisited)
                }
            },
            onIntroductionNext = {
                if (!quickReviewTransitionPending && state.navigation.canNext) {
                    stopAudioAndDispatch(AndroidStudyEvent.NextVisited)
                }
            },
            onIntroductionRating = {
                if (state is AndroidStudyState.Introduction && (!state.historyPreview || state.navigation.canCorrectRating)) {
                    submitIntroductionRating(it, IntroductionRatingFeedbackOrigin.MANUAL_BUTTON)
                }
            },
            onTypingStageTap = toggleTypingRevealedAudioLoop,
            onListeningStageTap = toggleListeningRevealedAudioLoop,
            onListeningCompactSuccessRendered = { listeningCompactSuccessRendered = true },
            onImageRecallStageTap = toggleImageRecallRevealedAudioLoop,
            onImageRecallCompactSuccessRendered = { imageRecallCompactSuccessRendered = true },
            multipleChoiceWrongRevealReady = multipleChoiceWrongRevealReady,
            onEvent = stopAudioAndDispatch,
            onOpenFullscreenImage = onOpenFullscreenImage,
            isMuted = isMuted,
            onToggleMute = onToggleMute,
            onAutoPlay = onAutoPlay,
            isDifficult = isDifficult,
            onToggleDifficult = onToggleDifficult,
            onEditItem = onEditItem
        )
    }

    limitEditor?.let { target ->
        val hud = state.hud ?: return@let
        var input by remember(target) { mutableStateOf(
            if (target == "new") hud.newConfiguredTarget.toString() else hud.reviewConfiguredTarget.toString()
        ) }
        val value = input.toIntOrNull()
        val minimum = if (target == "new") hud.newCompleted else hud.reviewCompleted
        AlertDialog(
            onDismissRequest = { limitEditor = null },
            title = { Text(if (target == "new") "Từ mới mỗi ngày" else "Ôn tập mỗi ngày") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit) },
                    label = { Text("Giới hạn từ 1 đến 999") },
                    supportingText = {
                        if (value == null || value !in maxOf(1, minimum)..999) {
                            Text("Giá trị phải từ ${maxOf(1, minimum)} đến 999.")
                        }
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = value != null && value in maxOf(1, minimum)..999,
                    onClick = {
                        val newLimit = if (target == "new") value!! else hud.newConfiguredTarget
                        val reviewLimit = if (target == "review") value!! else hud.reviewConfiguredTarget
                        onEvent(AndroidStudyEvent.UpdateDailyLimits(newLimit, reviewLimit))
                        limitEditor = null
                    }
                ) { Text("Áp dụng") }
            },
            dismissButton = { TextButton(onClick = { limitEditor = null }) { Text("Hủy") } }
        )
    }
}

internal fun androidStudyRuntimeModeLabel(
    state: AndroidStudyState.Runtime,
    recallModeLabel: String
): String {
    val identity = state.runtimeIdentity ?: return recallModeLabel
    return when {
        identity.focusedPracticeKind ==
                vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW -> "Quick Review"
        identity.studyMode == StudyMode.ADAPTIVE && identity.practiceLoopPolicy ==
                vn.loi.learning.domain.study.session.model.PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED ->
            "Adaptive · Continuous practice"
        identity.studyMode == StudyMode.ADAPTIVE -> "Adaptive"
        else -> recallModeLabel
    }
}

@Composable
private fun LearningEngineCompactHud(hud: AndroidStudySessionHud) {
    val newDescription = progressDescription("New", hud.newCompleted, hud.newTarget, hud.newConfiguredTarget)
    val reviewDescription = progressDescription(
        "Review", hud.reviewCompleted, hud.reviewTarget, hud.reviewConfiguredTarget
    )
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$newDescription. $reviewDescription. Total learned ${hud.totalLearned}. " +
                    "Due ${hud.dueCount}. Again ${hud.againCount}, Hard ${hud.hardCount}, " +
                    "Good ${hud.goodCount}, Easy ${hud.easyCount}."
        },
        shape = LearningEngineShapes.extraSmall,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LearningSpacing.extraSmall, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            hud.skimStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HudInlineMetric(stringResource(R.string.hud_total), hud.totalLearned.toString())
                HudInlineMetric(stringResource(R.string.hud_new), "${hud.newCompleted}/${hud.newTarget}", MaterialTheme.colorScheme.primary)
                HudInlineMetric(stringResource(R.string.hud_review), "${hud.reviewCompleted}/${hud.reviewTarget}", MaterialTheme.colorScheme.secondary)
                HudInlineMetric(stringResource(R.string.hud_due), hud.dueCount.toString())
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HudRating(stringResource(R.string.rating_again), hud.againCount, MaterialTheme.colorScheme.error)
                HudRating(stringResource(R.string.rating_hard), hud.hardCount, LearningEngineThemeTokens.semanticColors.warning)
                HudRating(stringResource(R.string.rating_good), hud.goodCount, LearningEngineThemeTokens.semanticColors.success)
                HudRating(stringResource(R.string.rating_easy), hud.easyCount, MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun DifficultPracticeHud(hud: AndroidStudySessionHud) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Học lại ${hud.againCount}. Khó ${hud.hardCount}."
        },
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HudRating(stringResource(R.string.rating_again), hud.againCount, MaterialTheme.colorScheme.error)
        HudRating(stringResource(R.string.rating_hard), hud.hardCount, LearningEngineThemeTokens.semanticColors.warning)
    }
}

@Composable
private fun LearnNewProgressHeader(
    hud: AndroidStudySessionHud,
    pendingRating: PendingIntroductionHudRating?,
    onEditNew: () -> Unit,
    onEditReview: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Học từ mới. Hôm nay ${hud.newCompleted} trên ${hud.newConfiguredTarget}. " +
                    "Due ${hud.dueCount}."
        },
        color = Color.Transparent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CompactLearnMetric(stringResource(R.string.hud_total), hud.totalLearned.toString())
                CompactLearnMetric(stringResource(R.string.hud_new), "${hud.newCompleted}/${hud.newConfiguredTarget}", onLongPress = onEditNew)
                CompactLearnMetric(stringResource(R.string.hud_review), "${hud.reviewCompleted}/${hud.reviewConfiguredTarget}", onLongPress = onEditReview)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CompactLearnMetric(stringResource(R.string.hud_due), hud.dueCount.toString())
                CompactLearnMetric(stringResource(R.string.rating_again), displayedIntroductionRatingCount(hud, ReviewRating.AGAIN, pendingRating).toString(), MaterialTheme.colorScheme.error, pendingRating?.takeIf { it.rating == ReviewRating.AGAIN }?.feedbackId)
                CompactLearnMetric(stringResource(R.string.rating_hard), displayedIntroductionRatingCount(hud, ReviewRating.HARD, pendingRating).toString(), LearningEngineThemeTokens.semanticColors.warning, pendingRating?.takeIf { it.rating == ReviewRating.HARD }?.feedbackId)
                CompactLearnMetric(stringResource(R.string.rating_good), displayedIntroductionRatingCount(hud, ReviewRating.GOOD, pendingRating).toString(), LearningEngineThemeTokens.semanticColors.success, pendingRating?.takeIf { it.rating == ReviewRating.GOOD }?.feedbackId)
                CompactLearnMetric(stringResource(R.string.rating_easy), displayedIntroductionRatingCount(hud, ReviewRating.EASY, pendingRating).toString(), MaterialTheme.colorScheme.tertiary, pendingRating?.takeIf { it.rating == ReviewRating.EASY }?.feedbackId)
            }
        }
    }
}

@Composable
private fun QuickReviewProgressHeader(state: AndroidStudyState.Introduction, hud: AndroidStudySessionHud) {
    val position = state.quickReviewPassPosition ?: 1
    val poolSize = state.quickReviewPoolSize ?: hud.totalLearned
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Ôn nhanh. Lượt $position trong nhóm $poolSize mục đã học " +
                    "in the current review pass. Endless learned vocabulary review."
        },
        color = Color.Transparent
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Quick Review · $position / $poolSize",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$poolSize learned",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactLearnMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    pulseKey: String? = null,
    onLongPress: (() -> Unit)? = null
) {
    val reducedMotion = isReducedMotionEnabled()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != null && !reducedMotion) {
            scale.snapTo(1f)
            scale.animateTo(1.14f, tween(180))
            scale.animateTo(1f, tween(220))
        }
    }
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .then(if (onLongPress != null) Modifier.pointerInput(onLongPress) {
                detectTapGestures(onLongPress = { onLongPress() })
            } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun HudInlineMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun HudRating(label: String, value: Int, valueColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun LearningEngineLearningStage(
    modifier: Modifier = Modifier,
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    restartAudio: (AudioRole, String?, Boolean) -> Unit,
    contentDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    revealBringIntoViewRequester: BringIntoViewRequester,
    introductionImageExpanded: Boolean,
    swipeRatingSubmitted: Boolean,
    quickReviewTransitionPending: Boolean,
    quickReviewQuestionPlaying: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    onIntroductionImageExpandedChange: (Boolean) -> Unit,
    onIntroductionStageTap: () -> Unit,
    onIntroductionSwipeGood: () -> Unit,
    onIntroductionPrevious: () -> Unit,
    onIntroductionNext: () -> Unit,
    onIntroductionRating: (ReviewRating) -> Unit,
    onTypingStageTap: () -> Unit = {},
    onListeningStageTap: () -> Unit = {},
    onListeningCompactSuccessRendered: () -> Unit = {},
    onImageRecallStageTap: () -> Unit = {},
    onImageRecallCompactSuccessRendered: () -> Unit = {},
    multipleChoiceWrongRevealReady: Boolean = false,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    onAutoPlay: (() -> Unit)? = null,
    isDifficult: Boolean = false,
    onToggleDifficult: (() -> Unit)? = null,
    onEditItem: (() -> Unit)? = null
) {
    if (state is AndroidStudyState.Introduction) {
        IntroductionLearningStage(
            modifier = modifier,
            state = state,
            activeRole = activeRole,
            playAudio = playAudio,
            restartAudio = restartAudio,
            imageExpanded = introductionImageExpanded,
            swipeRatingSubmitted = swipeRatingSubmitted,
            quickReviewTransitionPending = quickReviewTransitionPending,
            quickReviewQuestionPlaying = quickReviewQuestionPlaying,
            feedbackRating = feedbackRating,
            feedbackOrigin = feedbackOrigin,
            onImageExpandedChange = onIntroductionImageExpandedChange,
            onGenericStageTap = onIntroductionStageTap,
            onSwipeGood = onIntroductionSwipeGood,
            onPrevious = onIntroductionPrevious,
            onNext = onIntroductionNext,
            onRating = onIntroductionRating,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            isMuted = isMuted,
            onToggleMute = onToggleMute,
            onAutoPlay = onAutoPlay,
            isDifficult = isDifficult,
            onToggleDifficult = onToggleDifficult,
            onEditItem = onEditItem
        )
        return
    }
    val feedbackContent: @Composable () -> Unit = {
        Box(modifier = Modifier.bringIntoViewRequester(revealBringIntoViewRequester)) {
            StudyRevealAndFeedbackContent(
                state,
                activeRole,
                playAudio,
                onEvent,
                showResponseActions = when (state) {
                    is AndroidStudyState.MultipleChoice -> multipleChoiceAllowsManualRating()
                    is AndroidStudyState.Typing,
                    is AndroidStudyState.Listening,
                    is AndroidStudyState.ImageRecall -> false
                    else -> true
                },
                onTypingStageTap = when (state) {
                    is AndroidStudyState.Listening -> onListeningStageTap
                    is AndroidStudyState.ImageRecall -> onImageRecallStageTap
                    else -> onTypingStageTap
                },
                typingLeadContent = when {
                    state is AndroidStudyState.Typing && state.revealed -> {
                        {
                            if (state.answer.isNotBlank()) {
                                TypingDifferenceComparison(state.answer, state.plan.answerContract.canonicalAnswer)
                            }
                            ReviewImageNavigationOverlay(
                                canPrevious = state.navigation.canPrevious,
                                canNext = state.navigation.canNext,
                                onPrevious = { onEvent(AndroidStudyEvent.PreviousVisited) },
                                onNext = { onEvent(AndroidStudyEvent.NextVisited) },
                                gesturesEnabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StudyMedia(state.resolvedImage, StudyMediaRole.STANDARD, StudyContentDensity.DENSE,
                                    availableMediaHeightDp, onOpenFullscreenImage)
                            }
                        }
                    }
                    state is AndroidStudyState.Listening && state.revealed -> {
                        {
                            if (state.answer.isNotBlank()) {
                                TypingDifferenceComparison(state.answer, state.plan.answerContract.canonicalAnswer)
                            }
                            ReviewImageNavigationOverlay(
                                canPrevious = state.navigation.canPrevious,
                                canNext = state.navigation.canNext,
                                onPrevious = { onEvent(AndroidStudyEvent.PreviousVisited) },
                                onNext = { onEvent(AndroidStudyEvent.NextVisited) },
                                gesturesEnabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StudyMedia(state.resolvedImage, StudyMediaRole.STANDARD, StudyContentDensity.DENSE,
                                    availableMediaHeightDp, onOpenFullscreenImage)
                            }
                        }
                    }
                    state is AndroidStudyState.ImageRecall && state.completed &&
                            state.outcome != RecallOutcome.CORRECT -> {
                        {
                            if (state.answer.isNotBlank()) {
                                TypingDifferenceComparison(state.answer, state.plan.answerContract.canonicalAnswer)
                            }
                            ReviewImageNavigationOverlay(
                                canPrevious = state.navigation.canPrevious,
                                canNext = state.navigation.canNext,
                                onPrevious = { onEvent(AndroidStudyEvent.PreviousVisited) },
                                onNext = { onEvent(AndroidStudyEvent.NextVisited) },
                                gesturesEnabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StudyMedia(state.resolvedImage, StudyMediaRole.STANDARD, StudyContentDensity.DENSE,
                                    availableMediaHeightDp, onOpenFullscreenImage)
                            }
                        }
                    }
                    else -> null
                }
            )
        }
    }
    if (state is AndroidStudyState.Typing) {
        TypingStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            onStageTap = if (state.revealed && !state.completionPending) onTypingStageTap else null,
            onSwipeNext = if (state.revealed && !state.completionPending) {
                { onEvent(AndroidStudyEvent.NextVisited) }
            } else null,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.Listening) {
        ListeningStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            onCompactSuccessRendered = onListeningCompactSuccessRendered,
            onStageTap = if (state.revealed && !state.completionPending) {
                onListeningStageTap
            } else null,
            onSwipeNext = if (state.revealed && !state.completionPending) {
                { onEvent(AndroidStudyEvent.NextVisited) }
            } else null,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.MultipleChoice) {
        MultipleChoiceStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            showFullAnswer = state.outcome == RecallOutcome.INCORRECT && multipleChoiceWrongRevealReady,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.ImageRecall) {
        ImageRecallStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            onCompactSuccessRendered = onImageRecallCompactSuccessRendered,
            onStageTap = if (state.revealed && !state.completionPending) onImageRecallStageTap else null,
            onSwipeNext = if (state.revealed && !state.completionPending) {
                { onEvent(AndroidStudyEvent.NextVisited) }
            } else null,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.ExampleCompletion) {
        ExampleCompletionStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            playAudio = playAudio,
            onEvent = onEvent,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
}

@Composable
private fun IntroductionLearningStage(
    modifier: Modifier = Modifier,
    state: AndroidStudyState.Introduction,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    restartAudio: (AudioRole, String?, Boolean) -> Unit,
    imageExpanded: Boolean,
    swipeRatingSubmitted: Boolean,
    quickReviewTransitionPending: Boolean,
    quickReviewQuestionPlaying: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    onImageExpandedChange: (Boolean) -> Unit,
    onGenericStageTap: () -> Unit,
    onSwipeGood: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRating: (ReviewRating) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    onAutoPlay: (() -> Unit)? = null,
    isDifficult: Boolean = false,
    onToggleDifficult: (() -> Unit)? = null,
    onEditItem: (() -> Unit)? = null
) {
    val difficultSkim = state.focusedPracticeKind ==
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT
    val quickReview = state.focusedPracticeKind ==
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.QUICK_REVIEW
    val focusedSkimUx = usesFocusedSkimUx(state.focusedPracticeKind)
    val isPlayingExpected = activeRole == AudioRole.EXPECTED_ANSWER
    val isPlayingMeaning = activeRole == AudioRole.MEANING
    val isPlayingExampleEng = activeRole == AudioRole.EXAMPLE_ENGLISH
    val isPlayingExampleVie = activeRole == AudioRole.EXAMPLE_VIETNAMESE
    val reducedMotion = isReducedMotionEnabled()
    val feedbackVisual = when (feedbackRating) {
        ReviewRating.AGAIN -> StudyFeedbackVisualState.RATING_AGAIN
        ReviewRating.HARD -> StudyFeedbackVisualState.RATING_HARD
        ReviewRating.GOOD -> StudyFeedbackVisualState.RATING_GOOD
        ReviewRating.EASY -> StudyFeedbackVisualState.RATING_EASY
        null -> StudyFeedbackVisualState.NEUTRAL
    }
    val presentationKey = state.presentationVisitId ?: state.learningItemId
    var swipeOffsetTarget by remember(presentationKey) { mutableFloatStateOf(0f) }
    var horizontalOffsetTarget by remember(presentationKey) { mutableFloatStateOf(0f) }
    var swipeCommitPending by remember(presentationKey) { mutableStateOf(false) }
    val backgroundInteraction = remember(presentationKey) { MutableInteractionSource() }
    val stagePressed by backgroundInteraction.collectIsPressedAsState()
    val swipeOffset by animateFloatAsState(
        targetValue = swipeOffsetTarget,
        animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.PRESS, reducedMotion)),
        label = "Learn new swipe position"
    )
    val horizontalOffset by animateFloatAsState(
        targetValue = horizontalOffsetTarget,
        animationSpec = tween(if (reducedMotion) 0 else 170),
        label = "Introduction horizontal traversal"
    )
    val stageScale by animateFloatAsState(
        targetValue = if (stagePressed && !reducedMotion) 0.994f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 100),
        label = "Learn new press feedback"
    )
    val imageFeedbackScale by animateFloatAsState(
        targetValue = if (feedbackRating != null && !reducedMotion) 1.02f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 190),
        label = "Introduction rating image feedback"
    )
    val quickReviewPulse = rememberInfiniteTransition(label = "Quick Review question audio pulse")
    val quickReviewPulseScale by quickReviewPulse.animateFloat(
        initialValue = 1f,
        targetValue = if (quickReviewQuestionPlaying && !reducedMotion) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Quick Review question image scale"
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bounds = resolveIntroductionImageBounds(maxHeight.value.toInt())
        val maximumContentOffsetPx = with(LocalDensity.current) { 8.dp.toPx() }
        val maximumHorizontalOffsetPx = with(LocalDensity.current) { 48.dp.toPx() }
        val introductionScrollState = rememberLazyListState()
        val targetMaxHeightDp = when {
            !state.revealed || imageExpanded -> bounds.frontMaxHeightDp
            else -> bounds.revealMaxHeightDp
        }
        val imageMaxHeight by animateDpAsState(
            targetValue = targetMaxHeightDp.dp,
            animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.MEDIA_RESIZE, reducedMotion)),
            label = "Introduction hero transformation"
        )
        StudyStageCard(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = stageScale
                scaleY = stageScale
            }.introductionStageGestures(
                itemKey = presentationKey,
                alreadySubmitted = swipeRatingSubmitted || swipeCommitPending || quickReviewTransitionPending,
                ratingEnabled = !focusedSkimUx && introductionRatingInputEnabled(
                    revealed = state.revealed,
                    historyPreview = state.historyPreview,
                    interactionPending = quickReviewTransitionPending
                ),
                navigationEnabled = (state.revealed || focusedSkimUx || state.navigation.canPrevious) && !quickReviewTransitionPending,
                gatedUpwardNavigation = focusedSkimUx,
                revealed = state.revealed,
                historyPreview = state.historyPreview,
                onDragOffset = { swipeOffsetTarget = it },
                onHorizontalDragOffset = { horizontalOffsetTarget = it },
                onGestureEnd = {
                    swipeOffsetTarget = 0f
                    horizontalOffsetTarget = 0f
                },
                onSwipeGood = {
                    if (!swipeCommitPending && !swipeRatingSubmitted) {
                        swipeCommitPending = true
                        onSwipeGood()
                    }
                },
                onPrevious = onPrevious,
                onNext = onNext
            ),
            feedback = feedbackVisual
        ) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = introductionScrollState,
                    modifier = Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                        translationY = swipeOffset.coerceIn(-maximumContentOffsetPx, 0f)
                        translationX = if (reducedMotion) 0f else horizontalOffset.coerceIn(
                            -maximumHorizontalOffsetPx, maximumHorizontalOffsetPx
                        )
                    }.clickable(
                        interactionSource = backgroundInteraction,
                        indication = null,
                        onClickLabel = if (!state.revealed) "Reveal answer" else null,
                        onClick = onGenericStageTap
                    ),
                    contentPadding = PaddingValues(horizontal = LearningSpacing.medium, vertical = LearningSpacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                ) {
                    item("introduction-content") {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                        ) {
                            val meaning = state.meaning ?: "Nghĩa tiếng Việt"
                            IntroductionHeroMedia(
                                state = state,
                                focusedSkimUx = focusedSkimUx,
                                imageExpanded = imageExpanded,
                                imageMaxHeight = imageMaxHeight,
                                imageScale = imageFeedbackScale * quickReviewPulseScale,
                                interactionEnabled = !quickReviewTransitionPending,
                                onReveal = onGenericStageTap,
                                onImageExpandedChange = onImageExpandedChange,
                                onOpenFullscreenImage = onOpenFullscreenImage,
                                onPrevious = onPrevious,
                                onNext = onNext
                            )

                            // FRONT hierarchy:
                            // IMAGE -> 12dp -> POS (prominent) -> 14dp -> VI meaning -> 14dp -> reveal hint
                            AnimatedContent(
                                targetState = state.revealed,
                                transitionSpec = {
                                    val duration = studyMotionDurationMillis(StudyMotionRole.REVEAL, reducedMotion)
                                    val enter = fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
                                            if (reducedMotion) EnterTransition.None else slideInVertically(
                                                tween(duration, easing = FastOutSlowInEasing)
                                            ) { it / 18 }
                                    enter togetherWith fadeOut(tween(duration, easing = FastOutSlowInEasing))
                                },
                                label = "Introduction coordinated reveal"
                            ) { revealed ->
                                if (!revealed) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        partOfSpeechPresentation(state.partOfSpeech)?.let { pos ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            PartOfSpeechBadge(pos, prominent = true)
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        StudyAudioTextTarget(
                                            text = meaning,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = introductionClueTextSizeSp(meaning.length).sp,
                                                lineHeight = (introductionClueTextSizeSp(meaning.length) + 6).sp
                                            ),
                                            audioPath = null,
                                            isPlaying = isPlayingMeaning,
                                            isLooping = false,
                                            onToggleAudio = null,
                                            centered = true,
                                            maxLines = 3,
                                            headingSemantics = true,
                                            interactionEnabled = false,
                                            interaction = StudyTextInteraction.PASSIVE
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        IntroductionInteractionHint(
                                            primary = if (difficultSkim) "Xem đáp án" else "Tap to reveal",
                                            secondary = if (difficultSkim) "Ôn nhanh từ khó" else "Recall the English word",
                                            emphasized = false
                                        )
                                    }
                                } else {
                                    StudyAnswerSection(
                                        englishAnswer = state.answer,
                                        pronunciation = normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation),
                                        partOfSpeech = partOfSpeechPresentation(state.partOfSpeech),
                                        vietnameseAnswer = meaning,
                                        englishExample = if (state.compactRatingExit) null else state.example,
                                        vietnameseExample = if (state.compactRatingExit) null else state.translation,
                                        answerAudioPath = state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                        englishExampleAudioPath = state.resolvedExampleEnglishAudio,
                                        isPlayingAnswer = isPlayingExpected,
                                        isPlayingVietnamese = isPlayingMeaning,
                                        isPlayingEnglishExample = isPlayingExampleEng,
                                        isPlayingVietnameseExample = isPlayingExampleVie,
                                        onAnswerAudio = {
                                            onGenericStageTap()
                                        },
                                        onEnglishExampleAudio = {
                                            restartAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = StudyContentSpacing.imageToAnswer),
                                        vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio,
                                        onVietnameseExampleAudio = {
                                            restartAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false)
                                        },
                                        answerHero = true,
                                        swipeSuccessGlowActive = focusedPracticeHeadwordGlowActive(
                                            focusedSkimUx = focusedSkimUx,
                                            revealed = state.revealed,
                                            transitionPending = quickReviewTransitionPending,
                                            historyPreview = state.historyPreview
                                        ),
                                        interactionEnabled = !quickReviewTransitionPending
                                    )
                                }
                            }
                        }
                    }
                }

                if (!difficultSkim) {
                    if (!quickReview || state.revealed) {
                        StudyRatingBar(
                            onRating = onRating,
                            selectedRating = feedbackRating,
                            underlinedRating = state.navigation.previousRating
                                ?: (if (quickReview) state.latestEffectiveRating else null),
                            enabled = introductionRatingInputEnabled(
                                revealed = state.revealed,
                                historyPreview = state.historyPreview,
                                interactionPending = quickReviewTransitionPending,
                                canCorrectRating = state.navigation.canCorrectRating
                            ),
                            modifier = Modifier.fillMaxWidth().padding(
                                start = LearningSpacing.medium,
                                end = LearningSpacing.medium,
                                top = StudyContentSpacing.examplesToRating,
                                bottom = StudyContentSpacing.ratingToActions
                            )
                        )
                    }
                }
                if (state.revealed && !state.compactRatingExit) {
                    StudyActionDock(
                        isMuted = isMuted,
                        onToggleMute = onToggleMute,
                        onAutoPlay = onAutoPlay,
                        onEditItem = onEditItem,
                        isDifficult = isDifficult,
                        onToggleDifficult = onToggleDifficult,
                        modifier = Modifier.fillMaxWidth().padding(
                            start = LearningSpacing.medium,
                            end = LearningSpacing.medium,
                            bottom = LearningSpacing.extraSmall
                        ),
                        enabled = !quickReviewTransitionPending
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroductionHeroMedia(
    state: AndroidStudyState.Introduction,
    focusedSkimUx: Boolean,
    imageExpanded: Boolean,
    imageMaxHeight: androidx.compose.ui.unit.Dp,
    imageScale: Float,
    interactionEnabled: Boolean,
    onReveal: () -> Unit,
    onImageExpandedChange: (Boolean) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val media: @Composable BoxScope.() -> Unit = {
        state.resolvedImage?.let { imageUri ->
            LearningEngineImage(
                imagePath = imageUri,
                imageUnavailable = false,
                onOpenFullscreen = {
                    if (!state.revealed) onReveal()
                    else if (interactionEnabled) {
                        onImageExpandedChange(!imageExpanded)
                        onReveal()
                    }
                },
                onOpenFullscreenSecondary = { image ->
                    if (!state.revealed) onReveal()
                    else if (interactionEnabled) onOpenFullscreenImage(image)
                },
                fillCanvas = true,
                adaptiveFitBounds = LearningImageFitBounds(120, imageMaxHeight.value.toInt()),
                interactionDescription = if (!state.revealed) {
                    "Learning image, reveal answer"
                } else if (imageExpanded) {
                    "Learning image expanded, tap to reduce"
                } else "Learning image, tap to expand",
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    scaleX = imageScale
                    scaleY = imageScale
                }
            )
        } ?: Box(
            Modifier.fillMaxWidth().heightIn(min = 120.dp, max = imageMaxHeight).semantics {
                contentDescription = if (state.revealed) "Learning canvas"
                else "Learning canvas, reveal the English word"
            }
        )
    }
    if (focusedSkimUx || state.revealed) {
        ReviewImageNavigationOverlay(
            canPrevious = state.navigation.canPrevious,
            canNext = state.navigation.canNext,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier.fillMaxWidth(),
            imageContent = media
        )
    } else {
        Box(Modifier.fillMaxWidth(), content = media)
    }
}

@Composable
private fun IntroductionInteractionHint(
    primary: String,
    secondary: String,
    emphasized: Boolean
) {
    Surface(
        shape = LearningEngineShapes.large,
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$primary. $secondary"
        }
    ) {
        Column(
            Modifier.padding(
                horizontal = if (emphasized) LearningSpacing.medium else LearningSpacing.small + 4.dp,
                vertical = if (emphasized) LearningSpacing.extraSmall else 3.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                primary,
                style = if (emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                secondary,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = if (emphasized) 0.78f else 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyAudioButton(audioPath: String?) = LearningEngineAudioButton(audioPath = audioPath)

@Composable
fun StudyImage(
    imagePath: String?,
    imageUnavailable: Boolean = imagePath == null,
    onOpenFullscreen: (String) -> Unit = {}
) = LearningEngineImage(imagePath = imagePath, imageUnavailable = imageUnavailable, onOpenFullscreen = onOpenFullscreen)

@Composable
fun FullscreenStudyImage(
    imagePath: String,
    onDismiss: () -> Unit
) = FullscreenLearningImage(imagePath = imagePath, onDismiss = onDismiss)

@Composable
private fun StudyRevealAndFeedbackContent(
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    showResponseActions: Boolean = true,
    onTypingStageTap: () -> Unit = {},
    typingLeadContent: (@Composable () -> Unit)? = null
) {
    if (state is AndroidStudyState.Introduction) return
    val plan = state.plan ?: return
    val isRevealed = when (state) {
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.Listening -> state.revealed
        is AndroidStudyState.ImageRecall -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }
    val typingSuccessPending = (state as? AndroidStudyState.Typing)?.completionPending == true

    val allowReveal = when (state) {
        is AndroidStudyState.ExampleCompletion -> true
        else -> false
    }
    val revealMotionDuration = studyMotionDurationMillis(StudyMotionRole.REVEAL, isReducedMotionEnabled())

    val badgeScale by animateFloatAsState(
        targetValue = if (state.completed || isRevealed) 1.0f else 0.96f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = revealMotionDuration),
        label = "badge scale"
    )

    AnimatedVisibility(
        visible = state.completed || isRevealed,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) +
                slideInVertically(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) { fullHeight -> fullHeight / 10 },
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) +
                shrinkVertically(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration))
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                if (state is AndroidStudyState.Typing ||
                    state is AndroidStudyState.ImageRecall || state is AndroidStudyState.Listening
                ) {
                    LearningSpacing.small
                } else {
                    LearningSpacing.medium
                }
            )
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
            if (!typingSuccessPending &&
                state !is AndroidStudyState.Typing &&
                !(state is AndroidStudyState.Listening &&
                        state.outcome != RecallOutcome.CORRECT) &&
                !(state is AndroidStudyState.ImageRecall &&
                        state.outcome != RecallOutcome.CORRECT) &&
                !(state is AndroidStudyState.MultipleChoice &&
                        state.outcome == RecallOutcome.INCORRECT)
            ) {
                Box(modifier = Modifier.graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }) {
                    LearningEngineStatusBadge(
                        label = if (state is AndroidStudyState.MultipleChoice &&
                            state.outcome == RecallOutcome.INCORRECT
                        ) stringResource(R.string.study_answer_incorrect) else badgeText,
                        tone = tone,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }

            if (state is AndroidStudyState.MultipleChoice && state.outcome == RecallOutcome.INCORRECT) {
                selectedMultipleChoiceAnswer(state)?.let { selectedAnswer ->
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                    ) {
                        Text(
                            stringResource(R.string.study_mcq_you_selected),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            selectedAnswer,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val clozePresentation = (state as? AndroidStudyState.ExampleCompletion)?.let {
                resolveClozePresentation(it.prefix, it.blank, it.suffix, it.example, it.translation)
            }
            val forcedTypedReveal = when (state) {
                is AndroidStudyState.Typing -> state.revealed && !state.completionPending
                is AndroidStudyState.Listening -> state.revealed && !state.completionPending
                is AndroidStudyState.ImageRecall -> state.revealed && !state.completionPending
                else -> false
            }
            val forcedTypingReveal = state is AndroidStudyState.Typing && forcedTypedReveal
            val listeningWrongReveal = state is AndroidStudyState.Listening && forcedTypedReveal
            val imageRecallWrongReveal = state is AndroidStudyState.ImageRecall && forcedTypedReveal
            val rawAnswerExample = when {
                forcedTypedReveal -> state.example
                state is AndroidStudyState.Typing -> null
                clozePresentation != null -> clozePresentation.supportingExample
                else -> state.example
            }
            val rawAnswerExampleTranslation = when {
                forcedTypedReveal -> state.translation
                state is AndroidStudyState.Typing -> null
                clozePresentation != null -> clozePresentation.supportingExampleTranslation
                else -> state.translation
            }
            val revealExamplePair =
                if (imageRecallWrongReveal) {
                    val imageRecallExampleLines = rawAnswerExample
                        ?.lineSequence()
                        ?.map(String::trim)
                        ?.filter(String::isNotBlank)
                        ?.toList()
                        .orEmpty()
                    val firstVietnameseLine = imageRecallExampleLines.indexOfFirst(::looksVietnameseText)
                    RevealExamplePair(
                        english = if (firstVietnameseLine > 0) {
                            imageRecallExampleLines.take(firstVietnameseLine).joinToString("\n")
                        } else {
                            rawAnswerExample?.trim()?.takeIf(String::isNotBlank)
                        },
                        vietnamese = if (firstVietnameseLine > 0) {
                            imageRecallExampleLines.drop(firstVietnameseLine).joinToString("\n")
                        } else {
                            rawAnswerExampleTranslation?.trim()?.takeIf(String::isNotBlank)
                        }
                    )
                } else if (listeningWrongReveal || forcedTypingReveal ||
                    state is AndroidStudyState.MultipleChoice && state.outcome == RecallOutcome.INCORRECT
                ) {
                    resolveRevealExamplePair(
                        englishExample = rawAnswerExample,
                        vietnameseExample = rawAnswerExampleTranslation
                    )
                } else {
                    RevealExamplePair(rawAnswerExample, rawAnswerExampleTranslation)
                }
            typingLeadContent?.invoke()

            StudyAnswerSection(
                englishAnswer = plan.answerContract.canonicalAnswer,
                pronunciation = normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation),
                partOfSpeech = partOfSpeechPresentation(state.partOfSpeech),
                vietnameseAnswer = state.meaning,
                englishExample = revealExamplePair.english,
                vietnameseExample = revealExamplePair.vietnamese,
                answerAudioPath = state.resolvedExpectedAnswerAudio,
                englishExampleAudioPath = state.resolvedExampleEnglishAudio,
                isPlayingAnswer = activeRole == AudioRole.EXPECTED_ANSWER,
                isPlayingVietnamese = activeRole == AudioRole.MEANING,
                isPlayingEnglishExample = activeRole == AudioRole.EXAMPLE_ENGLISH,
                isPlayingVietnameseExample = activeRole == AudioRole.EXAMPLE_VIETNAMESE,
                onAnswerAudio = if (forcedTypingReveal || listeningWrongReveal || imageRecallWrongReveal) onTypingStageTap else {
                    { playAudio(AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio, true) }
                },
                onEnglishExampleAudio = {
                    playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                },
                vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio,
                onVietnameseExampleAudio = {
                    playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false)
                },
                answerHero = state is AndroidStudyState.Typing || state is AndroidStudyState.Listening,
                allowStandaloneVietnameseExample = forcedTypedReveal
                        || state is AndroidStudyState.MultipleChoice,
                englishExampleSectionLabel = null,
                vietnameseExampleSectionLabel = null,
                vietnameseAnswerAudioPath = if (forcedTypingReveal || listeningWrongReveal || imageRecallWrongReveal) {
                    state.resolvedMeaningAudio
                } else null,
                onVietnameseAnswerAudio = if (forcedTypingReveal || listeningWrongReveal || imageRecallWrongReveal) {
                    { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) }
                } else null,
                answerInteractionEnabled = !forcedTypingReveal && !listeningWrongReveal && !imageRecallWrongReveal
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Response Actions: Continue, Undo, Rating override. Objective MCQ evidence advances
            // automatically and never exposes a manual rating authority.
            if (showResponseActions) Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                if (typingSuccessPending) {
                    val typing = state as AndroidStudyState.Typing
                    Text(
                        if (typing.manualRating == null) "Automatic rating pending" else "Manual rating selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                    ) {
                        LearningEnginePrimaryButton(
                            label = stringResource(R.string.action_continue),
                            onClick = { onEvent(AndroidStudyEvent.NextVisited) },
                            modifier = Modifier.weight(1f)
                        )
                        if (state.hud?.focusedPractice != true) {
                            LearningEngineSecondaryButton(
                                label = stringResource(R.string.action_undo),
                                onClick = { onEvent(AndroidStudyEvent.Undo) },
                                modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            )
                        }
                    }

                    if (plan.provenance == RecallProvenance.PRACTICE && state.hud?.focusedPractice != true) {
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
            onClick = { onEvent(AndroidStudyEvent.Reveal()) },
            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
        ) {
            Text(stringResource(R.string.study_reveal_answer))
        }
    }
}

@Composable
private fun Completion(state: AndroidStudyState.Completion, onEvent: (AndroidStudyEvent) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen),
        contentAlignment = Alignment.Center
    ) {
        LearningEngineCompletionCard(
            title = stringResource(R.string.study_session_complete),
            modeLabel = state.modeFamily,
            summary = completionResultDescription(
                state.totalCompleted,
                state.newCompleted,
                state.reviewCompleted
            ),
            detail = state.dailyBudget?.let { daily ->
                when {
                    daily.targetsComplete -> "Today's NEW and REVIEW targets are complete."
                    daily.newRemainingToday == 0 -> "This session is complete. Today's NEW target is reached; REVIEW may remain."
                    daily.reviewRemainingToday == 0 -> "This session is complete. Today's REVIEW target is reached; NEW may remain."
                    daily.hasEligibleWork -> "This session is complete. More daily Study work is available."
                    else -> "This session is complete. No eligible content is currently available."
                }
            } ?: "Great work! You have completed all items in this study session.",
            canUndo = state.canUndo,
            onUndo = { onEvent(AndroidStudyEvent.Undo) },
            onHome = { onEvent(AndroidStudyEvent.Home) }
        )
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
            title = stringResource(R.string.study_session_unavailable),
            message = state.message,
            onRetry = if (state.retryable) { { onEvent(AndroidStudyEvent.Retry) } } else null
        )
    }
}
