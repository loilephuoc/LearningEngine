package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

internal enum class StudyImageAspectClass {
    WIDE_LANDSCAPE,
    STANDARD_LANDSCAPE,
    SQUARE,
    PORTRAIT,
    EXTREME
}

@Immutable
internal data class AdaptiveStudyImagePresentation(
    val aspectClass: StudyImageAspectClass,
    val maximumWidthDp: Int,
    val frameHeightDp: Int
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
    ): AdaptiveStudyImagePresentation {
        require(intrinsicWidthDp > 0)
        require(intrinsicHeightDp > 0)
        require(availableWidthDp > 0)
        require(heightBudgetDp > 0)

        val aspectClass = classify(intrinsicWidthDp, intrinsicHeightDp)
        val frameHeight = heightBudgetDp
        val antiUpscaleWidth = (intrinsicWidthDp * 1.35f).roundToInt()
        val maximumWidth = minOf(
            availableWidthDp,
            maxOf(160.coerceAtMost(availableWidthDp), antiUpscaleWidth)
        )
        return AdaptiveStudyImagePresentation(aspectClass, maximumWidth, frameHeight)
    }
}
