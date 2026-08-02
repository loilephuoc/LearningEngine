package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import androidx.compose.ui.layout.ContentScale
import kotlin.math.roundToInt

internal enum class StudyImageAspectClass {
    WIDE_LANDSCAPE,
    STANDARD_LANDSCAPE,
    SQUARE,
    PORTRAIT,
    EXTREME
}

@Immutable
internal data class MediaPresentationMetrics(
    val aspectClass: StudyImageAspectClass,
    val frameWidthDp: Int,
    val frameHeightDp: Int,
    val renderedWidthDp: Int,
    val renderedHeightDp: Int,
    val contentScale: ContentScale,
    val sourceUpscaleAllowed: Boolean
)

internal object AdaptiveStudyImagePresentationResolver {
    fun classify(intrinsicWidthDp: Int, intrinsicHeightDp: Int): StudyImageAspectClass {
        require(intrinsicWidthDp > 0)
        require(intrinsicHeightDp > 0)
        val ratio = intrinsicWidthDp.toFloat() / intrinsicHeightDp
        return when {
            ratio >= 2.4f || ratio <= 0.45f -> StudyImageAspectClass.EXTREME
            ratio >= 1.55f -> StudyImageAspectClass.WIDE_LANDSCAPE
            ratio >= 1.12f -> StudyImageAspectClass.STANDARD_LANDSCAPE
            ratio >= 0.82f -> StudyImageAspectClass.SQUARE
            else -> StudyImageAspectClass.PORTRAIT
        }
    }

    fun resolve(
        intrinsicWidthDp: Int,
        intrinsicHeightDp: Int,
        availableWidthDp: Int,
        heightBudgetDp: Int
    ): MediaPresentationMetrics {
        require(intrinsicWidthDp > 0)
        require(intrinsicHeightDp > 0)
        require(availableWidthDp > 0)
        require(heightBudgetDp > 0)

        val aspectClass = classify(intrinsicWidthDp, intrinsicHeightDp)
        val fitScale = minOf(
            availableWidthDp.toFloat() / intrinsicWidthDp,
            heightBudgetDp.toFloat() / intrinsicHeightDp
        )
        val appliedScale = fitScale.coerceAtMost(MAXIMUM_SOURCE_SCALE)
        val renderedWidth = (intrinsicWidthDp * appliedScale).roundToInt().coerceAtLeast(1)
        val renderedHeight = (intrinsicHeightDp * appliedScale).roundToInt().coerceAtLeast(1)
        return MediaPresentationMetrics(
            aspectClass = aspectClass,
            frameWidthDp = renderedWidth,
            frameHeightDp = renderedHeight,
            renderedWidthDp = renderedWidth,
            renderedHeightDp = renderedHeight,
            contentScale = ContentScale.Fit,
            sourceUpscaleAllowed = fitScale <= MAXIMUM_SOURCE_SCALE
        )
    }

    private const val MAXIMUM_SOURCE_SCALE = 1.35f
}
