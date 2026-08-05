package vn.loi.learning.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState

val LocalLayoutPolicy = staticCompositionLocalOf { androidLayoutPolicy(360, 800) }

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

@Composable
fun LearningEngineScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) = Scaffold(modifier = modifier, topBar = topBar, bottomBar = bottomBar, content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningEngineTopAppBar(title: String, navigation: @Composable () -> Unit = {}) =
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) }, navigationIcon = navigation)

@Composable
fun LearningEnginePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
        role = Role.Button
        stateDescription = if (enabled) "Enabled" else "Disabled"
    }
) { Text(label) }

@Composable
fun LearningEngineSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics { role = Role.Button }
) { Text(label) }

@Composable
fun LearningEngineCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = ElevatedCard(
    modifier = modifier,
    shape = LearningEngineShapes.large,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
) { Column(Modifier.padding(LearningSpacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small), content = content) }

@Composable
fun LearningEngineSectionHeader(title: String, supporting: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text(title, style = LearningContentTypography.sectionTitle, modifier = Modifier.semantics { heading() })
        supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun LearningEngineLoadingState(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().semantics { contentDescription = label; liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
        LearningEngineCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun LearningEngineEmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    LearningEngineCard(modifier.fillMaxWidth().semantics { contentDescription = "$title. $detail" }) {
        LearningEngineSectionHeader(title, detail)
        if (actionLabel != null && onAction != null) LearningEnginePrimaryButton(actionLabel, onAction)
    }
}

@Composable
fun LearningEngineErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    LearningEngineCard(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        LearningEngineSectionHeader(title)
        Text(message, color = MaterialTheme.colorScheme.error)
        onRetry?.let { LearningEnginePrimaryButton("Retry", it) }
    }
}

@Composable
fun LearningEngineProgress(progress: Float, label: String, modifier: Modifier = Modifier) {
    val bounded = progress.coerceIn(0f, 1f)
    val semantics = androidx.compose.ui.semantics.ProgressBarRangeInfo(bounded, 0f..1f)
    Column(modifier.semantics { contentDescription = label; progressBarRangeInfo = semantics }, verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text(label, style = LearningContentTypography.progressMetric)
        LinearProgressIndicator(
            progress = { bounded },
            modifier = Modifier.fillMaxWidth(),
            trackColor = LearningEngineThemeTokens.semanticColors.progressTrack
        )
    }
}

@Composable
fun LearningEngineStatusBadge(label: String, tone: LearningStatusTone, modifier: Modifier = Modifier) {
    val semantic = LearningEngineThemeTokens.semanticColors
    val color = when (tone) {
        LearningStatusTone.SUCCESS -> semantic.success
        LearningStatusTone.WARNING -> semantic.warning
        LearningStatusTone.ERROR -> semantic.overdueReview
        LearningStatusTone.INFO -> semantic.info
        LearningStatusTone.ACTIVE -> semantic.activeLearning
        LearningStatusTone.DUE -> semantic.dueReview
        LearningStatusTone.OVERDUE -> semantic.overdueReview
        LearningStatusTone.COMPLETED -> semantic.completed
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        modifier = modifier.semantics { contentDescription = label; stateDescription = tone.name.lowercase() },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = color)
    )
}

@Composable
fun LearningEngineFeedbackBadge(tone: LearningDifficultyTone, modifier: Modifier = Modifier) {
    val token = LearningEngineThemeTokens.semanticColors.feedback(tone)
    AssistChip(
        onClick = {},
        label = { Text(token.label) },
        leadingIcon = { Text(if (tone == LearningDifficultyTone.EASY) "✓" else if (tone == LearningDifficultyTone.MEDIUM) "•" else "!") },
        modifier = modifier.semantics { contentDescription = token.iconDescription; stateDescription = token.label },
        colors = AssistChipDefaults.assistChipColors(labelColor = token.color),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = token.color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningEngineStudyTopBar(
    title: String,
    modeLabel: String,
    currentPosition: Int?,
    totalItems: Int?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = LearningEngineShapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = LearningSpacing.small, vertical = 2.dp)
                        )
                    }
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        if (currentPosition != null && totalItems != null && totalItems > 0) {
            val progress = (currentPosition.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                trackColor = LearningEngineThemeTokens.semanticColors.progressTrack
            )
        }
    }
}

@Composable
fun LearningEngineAudioIndicator(
    isPlaying: Boolean,
    isLooping: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "audio pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha pulse"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isPlaying) "Audio playing, tap to stop" else "Play audio",
            tint = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = alpha) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )
        if (isPlaying && isLooping) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "∞",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                )
            }
        }
    }
}

@Composable
fun LearningEngineAudioButton(
    audioPath: String?,
    modifier: Modifier = Modifier,
    label: String = "Listen audio"
) {
    val context = LocalContext.current
    val controller = remember(context) { AndroidAudioController(context) }
    var audioState by remember(audioPath) {
        mutableStateOf<AndroidAudioState>(if (audioPath == null) AndroidAudioState.Unavailable else AndroidAudioState.Idle)
    }
    DisposableEffect(controller) { onDispose(controller::close) }

    Row(
        modifier = modifier,
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
                    is AndroidAudioState.Failed -> "Audio error"
                    AndroidAudioState.Unavailable -> "Audio unavailable"
                    else -> label
                }
            )
        }

        if (audioState is AndroidAudioState.Failed || audioState == AndroidAudioState.Unavailable) {
            Text(
                "Audio unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

sealed interface ImagePresentationState {
    data object Loading : ImagePresentationState
    data object Unavailable : ImagePresentationState
    data object Failed : ImagePresentationState
    data class Ready(val bitmap: Bitmap) : ImagePresentationState
}

fun decodeBoundedImage(path: String, maxWidth: Int, maxHeight: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

@Composable
fun LearningEngineImage(
    imagePath: String?,
    imageUnavailable: Boolean = imagePath == null,
    onOpenFullscreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (imagePath == null && !imageUnavailable) return

    if (imageUnavailable) {
        Text(
            "Image unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
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
                modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .semantics { contentDescription = accessibilityStrings().loading },
                    strokeWidth = 2.5.dp
                )
            }
        }
        ImagePresentationState.Failed -> {
            Text(
                "Image could not be loaded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        ImagePresentationState.Unavailable -> {
            Text(
                "Image unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(LearningSpacing.small).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        is ImagePresentationState.Ready -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .wrapContentSize()
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
                            .heightIn(max = LocalLayoutPolicy.current.maxMediaHeightDp.dp)
                            .padding(LearningSpacing.extraSmall)
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenLearningImage(
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

@Composable
fun LearningEngineCompletionCard(
    title: String = "Session complete",
    detail: String = "Great work! You have completed all items in this study session.",
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(0.9f),
        shape = LearningEngineShapes.large
    ) {
        Column(
            Modifier.padding(LearningSpacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
        ) {
            LearningEngineStatusBadge(label = "Complete", tone = LearningStatusTone.COMPLETED)
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (canUndo && onUndo != null) {
                    LearningEngineSecondaryButton(
                        label = "Undo latest",
                        onClick = onUndo,
                        modifier = Modifier.weight(1f)
                    )
                }
                LearningEnginePrimaryButton(
                    label = "Back to Home",
                    onClick = onHome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
