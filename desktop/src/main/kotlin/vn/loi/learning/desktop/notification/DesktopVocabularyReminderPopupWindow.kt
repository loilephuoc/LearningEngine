package vn.loi.learning.desktop.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsConfiguration
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.runtime.DesktopThemePreference
import vn.loi.learning.desktop.ui.theme.LearningTheme
import vn.loi.learning.desktop.ui.study.StudyPosBadge
import kotlin.math.roundToInt

data class DesktopVocabularyReminderPopupWindowPolicy(
    val undecorated: Boolean = true,
    val transparent: Boolean = true,
    val resizable: Boolean = false,
    val focusable: Boolean = false,
    val alwaysOnTop: Boolean = true
)

data class DesktopVocabularyReminderPopupPresentationPolicy(
    val widthWithImageDp: Int = 326,
    val widthWithoutImageDp: Int = 280,
    val heightWithImageDp: Int = 124,
    val heightWithoutImageDp: Int = 104,
    val imageMaxWidthDp: Int = 118,
    val imageMaxHeightDp: Int = 108,
    val outerPaddingDp: Int = 3,
    val contentPaddingDp: Int = 9,
    val questionMaxLines: Int = 2,
    val secondaryTextMaxLines: Int = 2,
    val showLessonOrSection: Boolean = false
)

internal fun resolveAdaptivePopupWidth(
    questionWidthDp: Int,
    ipaAndPosWidthDp: Int,
    translationWidthDp: Int,
    imageWidthDp: Int
): Int {
    val text = maxOf(questionWidthDp, ipaAndPosWidthDp, translationWidthDp).coerceAtLeast(140)
    val fixed = COMPACT_PRESENTATION.contentPaddingDp * 2 + imageWidthDp +
        (if (imageWidthDp > 0) 8 else 0) + 30 + 8
    return (fixed + text).coerceIn(308, 450)
}

data class DesktopVocabularyReminderImageBounds(val widthDp: Int, val heightDp: Int)

internal fun resolveReminderImageBounds(
    pixelWidth: Int,
    pixelHeight: Int,
    policy: DesktopVocabularyReminderPopupPresentationPolicy = COMPACT_PRESENTATION
): DesktopVocabularyReminderImageBounds {
    require(pixelWidth > 0 && pixelHeight > 0)
    val scale = minOf(
        policy.imageMaxWidthDp.toDouble() / pixelWidth,
        policy.imageMaxHeightDp.toDouble() / pixelHeight
    )
    return DesktopVocabularyReminderImageBounds(
        widthDp = (pixelWidth * scale).roundToInt().coerceAtLeast(1),
        heightDp = (pixelHeight * scale).roundToInt().coerceAtLeast(1)
    )
}

internal fun resolvePopupDimensions(candidate: DesktopVocabularyCandidate): Pair<Int, Int> =
    if (candidate.imageReference == null) {
        COMPACT_PRESENTATION.widthWithoutImageDp to COMPACT_PRESENTATION.heightWithoutImageDp
    } else {
        COMPACT_PRESENTATION.widthWithImageDp to COMPACT_PRESENTATION.heightWithImageDp
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopVocabularyReminderPopupWindow(
    visible: DesktopVocabularyReminderPopupState.Visible,
    controller: DesktopVocabularyReminderPopupController,
    contentMediaStorage: ContentMediaStorage,
    mainGraphicsConfiguration: GraphicsConfiguration?,
    themePreference: DesktopThemePreference,
    policy: DesktopVocabularyReminderPopupWindowPolicy = DesktopVocabularyReminderPopupWindowPolicy()
) {
    val geometry = DesktopVocabularyReminderPopupPositioning.resolveGeometry(mainGraphicsConfiguration)
    val initialDimensions = resolvePopupDimensions(visible.candidate)
    val placement = DesktopVocabularyReminderPopupPositioning.bottomRight(
        geometry,
        requestedWidthDp = initialDimensions.first.coerceAtLeast(308),
        requestedHeightDp = initialDimensions.second
    )
    val windowState = rememberWindowState(
        position = WindowPosition(placement.xDp.dp, placement.yDp.dp),
        size = DpSize(placement.widthDp.dp, placement.heightDp.dp)
    )
    Window(
        onCloseRequest = controller::closePopup,
        state = windowState,
        title = "Vocabulary reminder",
        undecorated = policy.undecorated,
        transparent = policy.transparent,
        resizable = policy.resizable,
        focusable = policy.focusable,
        alwaysOnTop = policy.alwaysOnTop
    ) {
        LearningTheme(preference = themePreference) {
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            fun measured(text: String?, style: TextStyle) = if (text.isNullOrBlank()) 0 else
                with(density) {
                    textMeasurer.measure(text, style = style, maxLines = 1).size.width.toDp().value.roundToInt()
                }
            val questionWidth = measured(
                visible.candidate.primaryText,
                TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            val ipaWidth = measured(visible.candidate.ipa, TextStyle(fontSize = 12.sp))
            val posWidth = measured(visible.candidate.partOfSpeech, TextStyle(fontSize = 10.sp)) +
                if (visible.candidate.partOfSpeech == null) 0 else 16
            val translationWidth = measured(visible.candidate.translation, TextStyle(fontSize = 14.sp))
            val adaptiveWidth = resolveAdaptivePopupWidth(
                questionWidth,
                ipaWidth + (if (ipaWidth > 0 && posWidth > 0) 6 else 0) + posWidth,
                translationWidth,
                if (visible.candidate.imageReference == null) 0 else COMPACT_PRESENTATION.imageMaxWidthDp
            )
            val adaptivePlacement = DesktopVocabularyReminderPopupPositioning.bottomRight(
                geometry,
                adaptiveWidth,
                initialDimensions.second
            )
            LaunchedEffect(visible.generation, adaptivePlacement) {
                windowState.position = WindowPosition(adaptivePlacement.xDp.dp, adaptivePlacement.yDp.dp)
                windowState.size = DpSize(adaptivePlacement.widthDp.dp, adaptivePlacement.heightDp.dp)
            }
            DesktopVocabularyReminderCard(
                candidate = visible.candidate,
                audioAvailable = visible.audioAvailable,
                audioPlaying = visible.audioPlaying,
                autoPlayPronunciation = visible.autoPlayPronunciation,
                markedDifficult = visible.markedDifficult,
                contentMediaStorage = contentMediaStorage,
                onClose = controller::closePopup,
                onPointerEnter = controller::pointerEntered,
                onPointerExit = controller::pointerExited,
                onToggleAudio = controller::toggleAudio,
                onToggleDifficult = controller::toggleDifficultMarker,
                onOpenImage = controller::openFullImage
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopVocabularyReminderCard(
    candidate: DesktopVocabularyCandidate,
    audioAvailable: Boolean,
    audioPlaying: Boolean,
    autoPlayPronunciation: Boolean,
    markedDifficult: Boolean,
    contentMediaStorage: ContentMediaStorage,
    onClose: () -> Unit,
    onPointerEnter: () -> Unit,
    onPointerExit: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleDifficult: () -> Unit,
    onOpenImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(COMPACT_PRESENTATION.outerPaddingDp.dp)
            .onPointerEvent(PointerEventType.Enter) { onPointerEnter() }
            .onPointerEvent(PointerEventType.Exit) { onPointerExit() }
            .semantics {
                contentDescription = buildPopupContentDescription(candidate)
            },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(COMPACT_PRESENTATION.contentPaddingDp.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CandidateImage(candidate.imageReference, contentMediaStorage, onOpenImage)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                    Text(
                        candidate.primaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = COMPACT_PRESENTATION.questionMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (candidate.ipa != null || candidate.partOfSpeech != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidate.ipa?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            candidate.partOfSpeech?.let {
                                StudyPosBadge(partOfSpeech = it, fontSizeSp = 10)
                            }
                        }
                    }
                    candidate.translation?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = COMPACT_PRESENTATION.secondaryTextMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            }
            Column(
                modifier = Modifier.width(30.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp).semantics { contentDescription = "Close vocabulary reminder" }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                }
                IconButton(
                    onClick = onToggleDifficult,
                    modifier = Modifier.size(28.dp).semantics {
                        contentDescription = if (markedDifficult) "Remove difficult marker" else "Mark vocabulary difficult"
                    }
                ) {
                    Icon(
                        if (markedDifficult) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (markedDifficult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier.size(28.dp).semantics {
                        contentDescription = if (autoPlayPronunciation) {
                            "Mute reminder pronunciation"
                        } else {
                            "Unmute reminder pronunciation" + if (audioAvailable) " and play current audio" else ""
                        }
                    }
                ) {
                    Icon(
                        if (autoPlayPronunciation) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (autoPlayPronunciation) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateImage(reference: String?, storage: ContentMediaStorage, onOpenImage: () -> Unit) {
    if (reference == null) return
    val bitmap by produceState<ImageBitmap?>(initialValue = null, reference) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val path = storage.resolve(reference) ?: return@runCatching null
                SkiaImage.makeFromEncoded(Files.readAllBytes(path)).toComposeImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let { resolved ->
        val bounds = resolveReminderImageBounds(resolved.width, resolved.height)
        Image(
            bitmap = resolved,
            contentDescription = "Vocabulary image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(bounds.widthDp.dp).height(bounds.heightDp.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenImage)
        )
    }
}

internal fun buildPopupContentDescription(candidate: DesktopVocabularyCandidate): String = buildList {
    add("Vocabulary reminder")
    add(candidate.primaryText)
    candidate.translation?.let { add(it) }
    candidate.ipa?.let { add(it) }
    candidate.partOfSpeech?.let { add(it) }
}.joinToString(". ")

internal val COMPACT_PRESENTATION = DesktopVocabularyReminderPopupPresentationPolicy()
