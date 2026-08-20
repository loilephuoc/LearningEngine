package vn.loi.learning.desktop.notification

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

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

data class DesktopVocabularyReminderVerticalPresentationPolicy(
    val widthWithImageDp: Int = 340,
    val widthWithoutImageDp: Int = 300,
    val heightWithImageDp: Int = 360,
    val heightWithoutImageDp: Int = 180,
    val minWidthDp: Int = 260,
    val maxWidthDp: Int = 480,
    val minHeightWithoutImageDp: Int = 140,
    val imageMaxWidthDp: Int = 450,
    val imageMaxHeightDp: Int = 200,
    val imageMinHeightDp: Int = 80,
    val outerPaddingDp: Int = 3,
    val contentPaddingDp: Int = 12,
    val questionMaxLines: Int = 2,
    val secondaryTextMaxLines: Int = 3,
    val actionRowHeightDp: Int = 36,
    val showLessonOrSection: Boolean = false
)

internal val VERTICAL_PRESENTATION = DesktopVocabularyReminderVerticalPresentationPolicy()

object DesktopVocabularyReminderImageDimensionCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Int>>()

    fun resolve(reference: String?, storage: ContentMediaStorage?): Pair<Int, Int>? {
        if (reference.isNullOrBlank() || storage == null) return null
        cache[reference]?.let { return it }
        val resolved = runCatching {
            val path = storage.resolve(reference) ?: return@runCatching null
            if (!Files.exists(path)) return@runCatching null
            javax.imageio.ImageIO.createImageInputStream(path.toFile())?.use { iis ->
                val readers = javax.imageio.ImageIO.getImageReaders(iis)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    try {
                        reader.input = iis
                        val w = reader.getWidth(0)
                        val h = reader.getHeight(0)
                        if (w > 0 && h > 0) w to h else null
                    } finally {
                        reader.dispose()
                    }
                } else null
            }
        }.getOrNull()
        if (resolved != null) cache[reference] = resolved
        return resolved
    }
}

data class DesktopVocabularyReminderVerticalDimensions(
    val popupWidthDp: Int,
    val popupHeightDp: Int,
    val imageWidthDp: Int,
    val imageHeightDp: Int
)

internal fun resolveVerticalPopupDimensions(
    candidate: DesktopVocabularyCandidate,
    englishFontSizeSp: Float = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP,
    imagePixelDimensions: Pair<Int, Int>? = null,
    measuredQuestionWidthDp: Int? = null,
    measuredIpaAndPosWidthDp: Int? = null,
    measuredTranslationWidthDp: Int? = null,
    maxMonitorWidthDp: Int = 1920,
    maxMonitorHeightDp: Int = 1080,
    policy: DesktopVocabularyReminderVerticalPresentationPolicy = VERTICAL_PRESENTATION
): DesktopVocabularyReminderVerticalDimensions {
    val hasImage = candidate.imageReference != null
    val aspectRatio = if (hasImage && imagePixelDimensions != null && imagePixelDimensions.second > 0) {
        imagePixelDimensions.first.toDouble() / imagePixelDimensions.second
    } else if (hasImage) 1.0 else null

    val horizontalPaddingTotal = (policy.contentPaddingDp + policy.outerPaddingDp) * 2

    // 1. Text width demands
    val questionWidth = measuredQuestionWidthDp
        ?: (candidate.primaryText.length * (englishFontSizeSp * 0.58f)).roundToInt()
    val ipaPosWidth = measuredIpaAndPosWidthDp
        ?: (((candidate.ipa?.length ?: 0) * 6.5f) + ((candidate.partOfSpeech?.length ?: 0) * 7f) + (if (candidate.ipa != null || candidate.partOfSpeech != null) 20f else 0f)).roundToInt()
    val translationWidth = measuredTranslationWidthDp
        ?: (((candidate.translation?.length ?: 0) * 7.5f).roundToInt())

    val textPreferredWidth = maxOf(
        if (questionWidth <= 340) questionWidth else (questionWidth / 2 + 30),
        if (translationWidth <= 300) translationWidth else (translationWidth / 2 + 20),
        ipaPosWidth,
        180
    )

    // 2. Image width demands
    val imagePreferredWidth = when {
        aspectRatio == null -> 0
        aspectRatio < 0.85 -> (policy.imageMaxHeightDp * aspectRatio).roundToInt().coerceIn(120, 240)
        aspectRatio <= 1.15 -> 260
        else -> (policy.imageMaxHeightDp * aspectRatio).roundToInt().coerceIn(260, 380)
    }

    val rawWidth = maxOf(imagePreferredWidth, textPreferredWidth) + horizontalPaddingTotal
    val maxAllowedWidth = minOf(policy.maxWidthDp, maxMonitorWidthDp - 40)
    val popupWidthDp = rawWidth.coerceIn(policy.minWidthDp, maxAllowedWidth)

    val availableContentWidth = popupWidthDp - horizontalPaddingTotal

    // 3. Image rendered dimensions (initial)
    val (rawImageWidth, rawImageHeight) = when {
        !hasImage -> 0 to 0
        imagePixelDimensions != null && imagePixelDimensions.second > 0 -> {
            val scale = minOf(
                availableContentWidth.toDouble() / imagePixelDimensions.first,
                policy.imageMaxHeightDp.toDouble() / imagePixelDimensions.second
            )
            (imagePixelDimensions.first * scale).roundToInt().coerceAtLeast(1) to
                (imagePixelDimensions.second * scale).roundToInt().coerceAtLeast(1)
        }
        else -> {
            val side = minOf(availableContentWidth, policy.imageMaxHeightDp)
            side to side
        }
    }

    // 4. Text section height
    val englishLineHeight = (englishFontSizeSp * 1.3f).roundToInt()
    val englishLines = if (questionWidth > availableContentWidth) 2 else 1
    val englishHeight = englishLines * englishLineHeight

    val ipaPosHeight = if (candidate.ipa != null || candidate.partOfSpeech != null) 24 else 0
    val ipaPosSpacing = if (ipaPosHeight > 0) 6 else 0

    val translationLines = when {
        candidate.translation.isNullOrBlank() -> 0
        translationWidth > availableContentWidth * 1.8 -> 3
        translationWidth > availableContentWidth * 0.9 -> 2
        else -> 1
    }
    val translationHeight = translationLines * 22
    val translationSpacing = if (translationHeight > 0) 6 else 0

    val totalTextHeight = englishHeight + ipaPosSpacing + ipaPosHeight + translationSpacing + translationHeight

    val imageSpacing = if (rawImageHeight > 0) 8 else 0
    val actionRowSpacing = 8
    val actionRowHeight = policy.actionRowHeightDp
    val verticalPaddingTotal = (policy.contentPaddingDp + policy.outerPaddingDp) * 2

    val unconstrainedTotalHeight = verticalPaddingTotal + rawImageHeight + imageSpacing + totalTextHeight + actionRowSpacing + actionRowHeight

    // 5. Monitor work-area height limit & image shrink priority
    val maxAllowedHeight = maxMonitorHeightDp - 60
    val (finalImageHeight, finalImageWidth, finalTotalHeight) = if (unconstrainedTotalHeight > maxAllowedHeight && rawImageHeight > 0) {
        val excess = unconstrainedTotalHeight - maxAllowedHeight
        val shrinkable = maxOf(0, rawImageHeight - policy.imageMinHeightDp)
        val actualShrink = minOf(excess, shrinkable)
        val newImgHeight = rawImageHeight - actualShrink
        val newImgWidth = if (aspectRatio != null && aspectRatio > 0) (newImgHeight * aspectRatio).roundToInt() else rawImageWidth
        val newTotal = unconstrainedTotalHeight - actualShrink
        Triple(newImgHeight, newImgWidth, newTotal)
    } else {
        Triple(rawImageHeight, rawImageWidth, unconstrainedTotalHeight)
    }

    val clampedTotalHeight = if (hasImage) {
        finalTotalHeight.coerceAtLeast(200)
    } else {
        finalTotalHeight.coerceAtLeast(policy.minHeightWithoutImageDp)
    }

    return DesktopVocabularyReminderVerticalDimensions(
        popupWidthDp = popupWidthDp,
        popupHeightDp = clampedTotalHeight,
        imageWidthDp = finalImageWidth,
        imageHeightDp = finalImageHeight
    )
}

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

internal fun resolveVerticalReminderImageBounds(
    pixelWidth: Int,
    pixelHeight: Int,
    policy: DesktopVocabularyReminderVerticalPresentationPolicy = VERTICAL_PRESENTATION
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

internal fun resolvePopupDimensions(
    candidate: DesktopVocabularyCandidate,
    layout: DesktopVocabularyReminderPopupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
    englishFontSizeSp: Float = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP,
    imagePixelDimensions: Pair<Int, Int>? = null,
    maxMonitorWidthDp: Int = 1920,
    maxMonitorHeightDp: Int = 1080
): Pair<Int, Int> =
    when (layout) {
        DesktopVocabularyReminderPopupLayout.COMPACT -> {
            if (candidate.imageReference == null) {
                COMPACT_PRESENTATION.widthWithoutImageDp to COMPACT_PRESENTATION.heightWithoutImageDp
            } else {
                COMPACT_PRESENTATION.widthWithImageDp to COMPACT_PRESENTATION.heightWithImageDp
            }
        }
        DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL -> {
            val dims = resolveVerticalPopupDimensions(
                candidate = candidate,
                englishFontSizeSp = englishFontSizeSp,
                imagePixelDimensions = imagePixelDimensions,
                maxMonitorWidthDp = maxMonitorWidthDp,
                maxMonitorHeightDp = maxMonitorHeightDp
            )
            dims.popupWidthDp to dims.popupHeightDp
        }
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopVocabularyReminderPopupWindow(
    visible: DesktopVocabularyReminderPopupState.Visible,
    controller: DesktopVocabularyReminderPopupController,
    contentMediaStorage: ContentMediaStorage,
    mainGraphicsConfiguration: GraphicsConfiguration?,
    themePreference: DesktopThemePreference,
    policy: DesktopVocabularyReminderPopupWindowPolicy = DesktopVocabularyReminderPopupWindowPolicy(),
    onLocationChanged: (DesktopVocabularyReminderPopupLocation) -> Unit = {}
) {
    val monitor = DesktopVocabularyReminderPopupPositioning.resolveMonitor(visible.popupLocation.monitorId)
    val imageDimensions = remember(visible.candidate.imageReference) {
        DesktopVocabularyReminderImageDimensionCache.resolve(visible.candidate.imageReference, contentMediaStorage)
    }
    val initialDimensions = resolvePopupDimensions(
        candidate = visible.candidate,
        layout = visible.popupLayout,
        englishFontSizeSp = visible.englishTextFontSizeSp,
        imagePixelDimensions = imageDimensions,
        maxMonitorWidthDp = monitor.usableBounds.width,
        maxMonitorHeightDp = monitor.usableBounds.height
    )
    val initialPlacement = if (visible.popupLocation.customPosition && visible.popupLocation.normalizedX != null && visible.popupLocation.normalizedY != null) {
        DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(
            monitor = monitor,
            normalizedX = visible.popupLocation.normalizedX,
            normalizedY = visible.popupLocation.normalizedY,
            requestedWidthDp = initialDimensions.first,
            requestedHeightDp = initialDimensions.second
        )
    } else {
        DesktopVocabularyReminderPopupPositioning.bottomRight(
            monitor = monitor,
            requestedWidthDp = initialDimensions.first,
            requestedHeightDp = initialDimensions.second
        )
    }
    val windowState = rememberWindowState(
        position = WindowPosition(initialPlacement.xDp.dp, initialPlacement.yDp.dp),
        size = DpSize(initialPlacement.widthDp.dp, initialPlacement.heightDp.dp)
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
        val window = this.window
        LearningTheme(preference = themePreference) {
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            fun measured(text: String?, style: TextStyle) = if (text.isNullOrBlank()) 0 else
                with(density) {
                    textMeasurer.measure(text, style = style, maxLines = 1).size.width.toDp().value.roundToInt()
                }
            val questionWidth = measured(
                visible.candidate.primaryText,
                TextStyle(fontSize = visible.englishTextFontSizeSp.sp, fontWeight = FontWeight.Bold)
            )
            val ipaWidth = measured(visible.candidate.ipa, TextStyle(fontSize = 12.sp))
            val posWidth = measured(visible.candidate.partOfSpeech, TextStyle(fontSize = 10.sp)) +
                if (visible.candidate.partOfSpeech == null) 0 else 16
            val ipaAndPosWidth = ipaWidth + (if (ipaWidth > 0 && posWidth > 0) 6 else 0) + posWidth
            val translationWidth = measured(visible.candidate.translation, TextStyle(fontSize = 14.sp))

            val (effectiveWidth, effectiveHeight, maxImageHeight) = if (visible.popupLayout == DesktopVocabularyReminderPopupLayout.COMPACT) {
                val w = resolveAdaptivePopupWidth(
                    questionWidth,
                    ipaAndPosWidth,
                    translationWidth,
                    if (visible.candidate.imageReference == null) 0 else COMPACT_PRESENTATION.imageMaxWidthDp
                )
                Triple(w, initialDimensions.second, VERTICAL_PRESENTATION.imageMaxHeightDp)
            } else {
                val vertDims = resolveVerticalPopupDimensions(
                    candidate = visible.candidate,
                    englishFontSizeSp = visible.englishTextFontSizeSp,
                    imagePixelDimensions = imageDimensions,
                    measuredQuestionWidthDp = questionWidth,
                    measuredIpaAndPosWidthDp = ipaAndPosWidth,
                    measuredTranslationWidthDp = translationWidth,
                    maxMonitorWidthDp = monitor.usableBounds.width,
                    maxMonitorHeightDp = monitor.usableBounds.height
                )
                Triple(vertDims.popupWidthDp, vertDims.popupHeightDp, vertDims.imageHeightDp)
            }
            val adaptivePlacement = if (visible.popupLocation.customPosition && visible.popupLocation.normalizedX != null && visible.popupLocation.normalizedY != null) {
                DesktopVocabularyReminderPopupPositioning.resolveCustomPosition(
                    monitor = monitor,
                    normalizedX = visible.popupLocation.normalizedX,
                    normalizedY = visible.popupLocation.normalizedY,
                    requestedWidthDp = effectiveWidth,
                    requestedHeightDp = effectiveHeight
                )
            } else {
                DesktopVocabularyReminderPopupPositioning.bottomRight(
                    monitor = monitor,
                    effectiveWidth,
                    effectiveHeight
                )
            }
            LaunchedEffect(visible.generation, adaptivePlacement) {
                windowState.position = WindowPosition(adaptivePlacement.xDp.dp, adaptivePlacement.yDp.dp)
                windowState.size = DpSize(adaptivePlacement.widthDp.dp, adaptivePlacement.heightDp.dp)
            }

            var isDragging by remember { mutableStateOf(false) }
            var dragStartMouse by remember { mutableStateOf<java.awt.Point?>(null) }
            var dragStartWinPos by remember { mutableStateOf<java.awt.Point?>(null) }
            var customSnoozeVisible by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                val mouseLoc = java.awt.MouseInfo.getPointerInfo()?.location
                                if (mouseLoc != null) {
                                    dragStartMouse = mouseLoc
                                    dragStartWinPos = java.awt.Point(window.x, window.y)
                                    isDragging = true
                                    controller.dragStarted()
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val mouseLoc = java.awt.MouseInfo.getPointerInfo()?.location
                                val startM = dragStartMouse
                                val startW = dragStartWinPos
                                if (mouseLoc != null && startM != null && startW != null) {
                                    val dx = mouseLoc.x - startM.x
                                    val dy = mouseLoc.y - startM.y
                                    window.setLocation(startW.x + dx, startW.y + dy)
                                }
                            },
                            onDragEnd = {
                                if (isDragging) {
                                    isDragging = false
                                    controller.dragEnded()
                                    val (targetMonId, norm) = DesktopVocabularyReminderPopupPositioning.calculateNormalizedPosition(
                                        logicalX = window.x,
                                        logicalY = window.y,
                                        widthDp = window.width,
                                        heightDp = window.height
                                    )
                                    val newLoc = DesktopVocabularyReminderPopupLocation(
                                        monitorId = targetMonId,
                                        normalizedX = norm.first,
                                        normalizedY = norm.second,
                                        customPosition = true
                                    )
                                    onLocationChanged(newLoc)
                                }
                            },
                            onDragCancel = {
                                if (isDragging) {
                                    isDragging = false
                                    controller.dragEnded()
                                }
                            }
                        )
                    }
            ) {
                when (visible.popupLayout) {
                    DesktopVocabularyReminderPopupLayout.COMPACT -> {
                        DesktopVocabularyReminderCard(
                            candidate = visible.candidate,
                            audioAvailable = visible.audioAvailable,
                            audioPlaying = visible.audioPlaying,
                            autoPlayPronunciation = visible.autoPlayPronunciation,
                            markedDifficult = visible.markedDifficult,
                            contentMediaStorage = contentMediaStorage,
                            englishTextFontSizeSp = visible.englishTextFontSizeSp,
                            onClose = controller::closePopup,
                            onPointerEnter = controller::pointerEntered,
                            onPointerExit = controller::pointerExited,
                            onToggleAudio = controller::toggleAudio,
                            onToggleDifficult = controller::toggleDifficultMarker,
                            onToggleLayout = controller::toggleLayout,
                            onSnooze = controller::snooze,
                            onRequestCustomSnooze = { customSnoozeVisible = true },
                            onOpenImage = controller::openFullImage
                        )
                    }
                    DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL -> {
                        DesktopVocabularyReminderVerticalCard(
                            candidate = visible.candidate,
                            audioAvailable = visible.audioAvailable,
                            audioPlaying = visible.audioPlaying,
                            autoPlayPronunciation = visible.autoPlayPronunciation,
                            markedDifficult = visible.markedDifficult,
                            contentMediaStorage = contentMediaStorage,
                            englishTextFontSizeSp = visible.englishTextFontSizeSp,
                            maxImageHeightDp = maxImageHeight,
                            onClose = controller::closePopup,
                            onPointerEnter = controller::pointerEntered,
                            onPointerExit = controller::pointerExited,
                            onToggleAudio = controller::toggleAudio,
                            onToggleDifficult = controller::toggleDifficultMarker,
                            onToggleLayout = controller::toggleLayout,
                            onSnooze = controller::snooze,
                            onRequestCustomSnooze = { customSnoozeVisible = true },
                            onOpenImage = controller::openFullImage
                        )
                    }
                }

                if (customSnoozeVisible) {
                    CustomSnoozeDialog(
                        onDismiss = { customSnoozeVisible = false },
                        onConfirm = { minutes ->
                            customSnoozeVisible = false
                            controller.snooze(minutes)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun DesktopVocabularyReminderCard(
    candidate: DesktopVocabularyCandidate,
    audioAvailable: Boolean,
    audioPlaying: Boolean,
    autoPlayPronunciation: Boolean,
    markedDifficult: Boolean,
    contentMediaStorage: ContentMediaStorage,
    englishTextFontSizeSp: Float = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP,
    onClose: () -> Unit,
    onPointerEnter: () -> Unit,
    onPointerExit: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleDifficult: () -> Unit,
    onToggleLayout: () -> Unit = {},
    onSnooze: (durationMinutes: Int) -> Unit = {},
    onRequestCustomSnooze: () -> Unit = {},
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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = englishTextFontSizeSp.sp),
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
                    modifier = Modifier.size(20.dp).semantics { contentDescription = "Close vocabulary reminder" }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                }
                IconButton(
                    onClick = onToggleDifficult,
                    modifier = Modifier.size(20.dp).semantics {
                        contentDescription = if (markedDifficult) "Remove difficult marker" else "Mark vocabulary difficult"
                    }
                ) {
                    Icon(
                        if (markedDifficult) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (markedDifficult) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                var snoozeMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { onSnooze(5) },
                                onLongClick = { snoozeMenuExpanded = true }
                            )
                            .semantics {
                                contentDescription = "Remind me again in 5 minutes. Long press for more options"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = snoozeMenuExpanded,
                        onDismissRequest = { snoozeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("5 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(5) })
                        DropdownMenuItem(text = { Text("10 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(10) })
                        DropdownMenuItem(text = { Text("30 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(30) })
                        DropdownMenuItem(text = { Text("60 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(60) })
                        DropdownMenuItem(text = { Text("Custom...") }, onClick = { snoozeMenuExpanded = false; onRequestCustomSnooze() })
                    }
                }
                IconButton(
                    onClick = onToggleLayout,
                    modifier = Modifier.size(20.dp).semantics {
                        contentDescription = "Switch to vertical layout"
                    }
                ) {
                    Icon(
                        Icons.Default.ViewAgenda,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = onToggleAudio,
                    enabled = audioAvailable,
                    modifier = Modifier.size(20.dp).semantics {
                        contentDescription = if (autoPlayPronunciation) "Turn off reminder audio" else "Turn on reminder audio"
                    }
                ) {
                    Icon(
                        if (autoPlayPronunciation) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (!autoPlayPronunciation) MaterialTheme.colorScheme.error
                        else if (!audioAvailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else if (audioPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun DesktopVocabularyReminderVerticalCard(
    candidate: DesktopVocabularyCandidate,
    audioAvailable: Boolean,
    audioPlaying: Boolean,
    autoPlayPronunciation: Boolean,
    markedDifficult: Boolean,
    contentMediaStorage: ContentMediaStorage,
    englishTextFontSizeSp: Float = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP,
    maxImageHeightDp: Int = VERTICAL_PRESENTATION.imageMaxHeightDp,
    onClose: () -> Unit,
    onPointerEnter: () -> Unit,
    onPointerExit: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleDifficult: () -> Unit,
    onToggleLayout: () -> Unit = {},
    onSnooze: (durationMinutes: Int) -> Unit = {},
    onRequestCustomSnooze: () -> Unit = {},
    onOpenImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(VERTICAL_PRESENTATION.outerPaddingDp.dp)
            .onPointerEvent(PointerEventType.Enter) { onPointerEnter() }
            .onPointerEvent(PointerEventType.Exit) { onPointerExit() }
            .semantics {
                contentDescription = buildPopupContentDescription(candidate)
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VERTICAL_PRESENTATION.contentPaddingDp.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (candidate.imageReference != null) {
                    CandidateVerticalImage(
                        reference = candidate.imageReference,
                        storage = contentMediaStorage,
                        onOpenImage = onOpenImage,
                        maxImageHeightDp = maxImageHeightDp
                    )
                }
                Text(
                    candidate.primaryText,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = englishTextFontSizeSp.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = VERTICAL_PRESENTATION.questionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (candidate.ipa != null || candidate.partOfSpeech != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                        maxLines = VERTICAL_PRESENTATION.secondaryTextMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(VERTICAL_PRESENTATION.actionRowHeightDp.dp).padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleAudio,
                    enabled = audioAvailable,
                    modifier = Modifier.size(28.dp).semantics {
                        contentDescription = if (autoPlayPronunciation) "Turn off reminder audio" else "Turn on reminder audio"
                    }
                ) {
                    Icon(
                        if (autoPlayPronunciation) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (!autoPlayPronunciation) MaterialTheme.colorScheme.error
                        else if (!audioAvailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else if (audioPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleLayout,
                    modifier = Modifier.size(28.dp).semantics {
                        contentDescription = "Switch to compact layout"
                    }
                ) {
                    Icon(
                        Icons.Default.ViewAgenda,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                var snoozeMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { onSnooze(5) },
                                onLongClick = { snoozeMenuExpanded = true }
                            )
                            .semantics {
                                contentDescription = "Remind me again in 5 minutes. Long press for more options"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = snoozeMenuExpanded,
                        onDismissRequest = { snoozeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("5 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(5) })
                        DropdownMenuItem(text = { Text("10 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(10) })
                        DropdownMenuItem(text = { Text("30 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(30) })
                        DropdownMenuItem(text = { Text("60 minutes") }, onClick = { snoozeMenuExpanded = false; onSnooze(60) })
                        DropdownMenuItem(text = { Text("Custom...") }, onClick = { snoozeMenuExpanded = false; onRequestCustomSnooze() })
                    }
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
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp).semantics { contentDescription = "Close vocabulary reminder" }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomSnoozeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(onClick = {})
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Remind me again in",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = { Text("Minutes (1–1440)") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val parsed = text.trim().toIntOrNull()
                            if (parsed == null || parsed !in 1..1440) {
                                error = "Enter 1 to 1440 minutes."
                            } else {
                                onConfirm(parsed)
                            }
                        }
                    ) {
                        Text("Remind later")
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateVerticalImage(
    reference: String?,
    storage: ContentMediaStorage,
    onOpenImage: () -> Unit,
    maxImageHeightDp: Int = VERTICAL_PRESENTATION.imageMaxHeightDp
) {
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
        val bounds = resolveVerticalReminderImageBounds(
            resolved.width,
            resolved.height,
            VERTICAL_PRESENTATION.copy(imageMaxHeightDp = maxImageHeightDp)
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(bounds.heightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = resolved,
                contentDescription = "Vocabulary image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(bounds.widthDp.dp)
                    .height(bounds.heightDp.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenImage)
            )
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
