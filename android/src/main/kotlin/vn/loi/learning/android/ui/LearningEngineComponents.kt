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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.vector.ImageVector
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
    shape = LearningEngineShapes.medium,
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
    shape = LearningEngineShapes.medium,
    modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics { role = Role.Button }
) { Text(label) }

@Composable
fun LearningEngineCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = ElevatedCard(
    modifier = modifier,
    shape = LearningEngineShapes.medium,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
) { Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small), content = content) }

@Composable
fun LearningEngineScreenShell(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().then(modifier)
            .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.section)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = LearningTextRole.screenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                subtitle?.let { Text(it, style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            action?.invoke()
        }
        content()
    }
}

@Composable
fun LearningEnginePrimaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = ElevatedCard(
    modifier = modifier,
    shape = LearningEngineShapes.large,
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.raised)
) { Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium), content = content) }

@Composable
fun LearningEngineCompactCard(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) = Surface(
    modifier = modifier,
    shape = LearningEngineShapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
    tonalElevation = LearningElevation.card
) { Row(Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium), content = content) }

@Composable
fun LearningEngineSettingsRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) = LearningEngineCompactCard(
    modifier.fillMaxWidth().clickable(onClick = onClick).defaultMinSize(minHeight = LearningSpacing.touchTarget)
        .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "$title. $detail" }
) {
    Icon(icon, null, Modifier.size(LearningIconSize.action), tint = MaterialTheme.colorScheme.primary)
    Column(Modifier.weight(1f)) {
        Text(title, style = LearningTextRole.cardTitle)
        Text(detail, style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    trailing()
}

@Composable
fun LearningEngineHeroCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable ColumnScope.() -> Unit = {}
) = ElevatedCard(
    modifier = modifier,
    shape = LearningEngineShapes.large,
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.raised)
) {
    Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
            Surface(shape = LearningEngineShapes.medium, color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(icon, null, Modifier.padding(LearningSpacing.small).size(LearningIconSize.card))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(eyebrow, style = LearningTextRole.caption, color = MaterialTheme.colorScheme.primary)
                Text(title, style = LearningTextRole.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                Text(detail, style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        supportingContent()
        LearningEnginePrimaryButton(actionLabel, onAction, Modifier.wrapContentWidth())
    }
}

@Composable
fun LearningEngineStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) = Surface(
    modifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = listOfNotNull(label, value, detail).joinToString(", ")
    },
    shape = LearningEngineShapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
    tonalElevation = LearningElevation.card
) {
    Column(Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.small),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = LearningTextRole.statistic, maxLines = 1)
        Text(label, style = LearningTextRole.caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        detail?.let { Text(it, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun LearningEngineActionCard(
    icon: ImageVector,
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = LearningEngineCompactCard(modifier) {
    Icon(icon, null, Modifier.size(LearningIconSize.card), tint = MaterialTheme.colorScheme.primary)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = LearningTextRole.cardTitle)
        Text(detail, style = LearningTextRole.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LearningEngineSecondaryButton(actionLabel, onAction, enabled = enabled)
}

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
        Row(
            Modifier.fillMaxWidth().heightIn(min = LearningSpacing.touchTarget)
                .padding(end = LearningSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(LearningSpacing.touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit study session")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                buildString {
                    append(modeLabel)
                    if (currentPosition != null && totalItems != null && totalItems > 0) {
                        append("  ").append(currentPosition).append('/').append(totalItems)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.semantics {
                    if (currentPosition != null && totalItems != null && totalItems > 0) {
                        contentDescription = "$modeLabel, item $currentPosition of $totalItems"
                    }
                }
            )
        }
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
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0.0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun LearningEngineAudioIndicator(
    isPlaying: Boolean,
    isLooping: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val reducedMotion = isReducedMotionEnabled()
    val transition = rememberInfiniteTransition(label = "audio pulse")
    val alphaState by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha pulse"
    )
    val alpha = if (reducedMotion) 1.0f else alphaState

    val rowModifier = if (onClick != null) {
        modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    } else {
        modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    }

    Row(
        modifier = rowModifier,
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
fun LearningEngineAudioTextRow(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    audioPath: String?,
    isPlaying: Boolean,
    isLooping: Boolean,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier,
    headingSemantics: Boolean = false
) {
    LearningEngineAudioTextRow(
        annotatedText = AnnotatedString(text),
        style = style,
        color = color,
        audioPath = audioPath,
        isPlaying = isPlaying,
        isLooping = isLooping,
        onToggleAudio = onToggleAudio,
        modifier = modifier,
        headingSemantics = headingSemantics,
        contentDescriptionOverride = null
    )
}

@Composable
fun LearningEngineAudioTextRow(
    annotatedText: AnnotatedString,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    audioPath: String?,
    isPlaying: Boolean,
    isLooping: Boolean,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier,
    headingSemantics: Boolean = false,
    contentDescriptionOverride: String? = null
) {
    val hasAudio = !audioPath.isNullOrBlank()
    val shape = LearningEngineShapes.small
    val desc = contentDescriptionOverride ?: annotatedText.text

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                .padding(horizontal = LearningSpacing.small, vertical = LearningSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = annotatedText,
                style = style,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else color,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics { if (headingSemantics) heading() }
            )
            if (hasAudio) {
                LearningEngineAudioIndicator(
                    isPlaying = isPlaying,
                    isLooping = isLooping
                )
            }
        }
    }

    if (hasAudio) {
        Surface(
            onClick = onToggleAudio,
            shape = shape,
            color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    stateDescription = if (isPlaying) "Playing" else "Idle"
                    contentDescription = "$desc. ${if (isPlaying) "Audio playing, tap to stop" else "Tap to play audio"}"
                }
        ) {
            rowContent()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LearningSpacing.small, vertical = LearningSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = annotatedText,
                    style = style,
                    color = color,
                    modifier = Modifier.semantics {
                        if (headingSemantics) heading()
                        if (contentDescriptionOverride != null) contentDescription = contentDescriptionOverride
                    }
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

data class LearningImageFitBounds(val minHeightDp: Int, val maxHeightDp: Int) {
    init {
        require(minHeightDp > 0 && maxHeightDp >= minHeightDp)
    }
}

internal data class AspectAwareImageSize(val widthDp: Int, val heightDp: Int)

internal fun resolveAspectAwareImageSize(
    availableWidthDp: Int,
    intrinsicAspectRatio: Float,
    bounds: LearningImageFitBounds
): AspectAwareImageSize {
    require(availableWidthDp > 0 && intrinsicAspectRatio > 0f)
    val naturalHeight = availableWidthDp / intrinsicAspectRatio
    val height = naturalHeight.toInt().coerceIn(bounds.minHeightDp, bounds.maxHeightDp)
    val width = if (naturalHeight in bounds.minHeightDp.toFloat()..bounds.maxHeightDp.toFloat()) {
        availableWidthDp
    } else {
        minOf(availableWidthDp, (height * intrinsicAspectRatio).toInt())
    }
    return AspectAwareImageSize(width, height)
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
    onOpenFullscreenSecondary: ((String) -> Unit)? = null,
    fillCanvas: Boolean = false,
    adaptiveFitBounds: LearningImageFitBounds? = null,
    interactionDescription: String = "View full size image",
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
            BoxWithConstraints(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val aspectRatio = presentation.bitmap.width.toFloat() / presentation.bitmap.height.toFloat()
                val adaptiveSize = adaptiveFitBounds?.let {
                    resolveAspectAwareImageSize(maxWidth.value.toInt(), aspectRatio, it)
                }
                Card(
                    shape = LearningEngineShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (fillCanvas) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = (adaptiveSize?.let { Modifier.width(it.widthDp.dp).height(it.heightDp.dp) }
                        ?: if (fillCanvas) Modifier.fillMaxSize() else Modifier.wrapContentSize())
                        .clickable { imagePath?.let(onOpenFullscreen) }
                        .semantics {
                            role = Role.Button
                            contentDescription = interactionDescription
                            onOpenFullscreenSecondary?.let { openFullscreen ->
                                customActions = listOf(
                                    CustomAccessibilityAction("Open image fullscreen") {
                                        imagePath?.let(openFullscreen)
                                        true
                                    }
                                )
                            }
                        }
                ) {
                    Image(
                        bitmap = presentation.bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = if (fillCanvas) Modifier.fillMaxSize() else Modifier
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
    val reducedMotion = isReducedMotionEnabled()
    val imageState by produceState<ImagePresentationState>(ImagePresentationState.Loading, imagePath) {
        value = withContext(Dispatchers.IO) {
            decodeBoundedImage(imagePath, 2400, 2400)?.let(ImagePresentationState::Ready)
                ?: ImagePresentationState.Failed
        }
    }

    BackHandler(onBack = onDismiss)

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
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close full size image"
                        }
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
                        .clickable(onClick = onDismiss)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Full size image overlay, tap to dismiss"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (val presentation = imageState) {
                        ImagePresentationState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        is ImagePresentationState.Ready -> {
                            AnimatedVisibility(
                                visible = true,
                                enter = if (reducedMotion) fadeIn(tween(0)) else fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.95f),
                                exit = if (reducedMotion) fadeOut(tween(0)) else fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f)
                            ) {
                                Image(
                                    bitmap = presentation.bitmap.asImageBitmap(),
                                    contentDescription = "Full size image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
