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

        val ratio = intrinsicWidthDp.toFloat() / intrinsicHeightDp
        val aspectClass = when {
            ratio >= 2.4f || ratio <= 0.45f -> StudyImageAspectClass.EXTREME
            ratio >= 1.55f -> StudyImageAspectClass.WIDE_LANDSCAPE
            ratio >= 1.12f -> StudyImageAspectClass.STANDARD_LANDSCAPE
            ratio >= 0.82f -> StudyImageAspectClass.SQUARE
            else -> StudyImageAspectClass.PORTRAIT
        }
        val heightFraction = when (aspectClass) {
            StudyImageAspectClass.WIDE_LANDSCAPE -> 0.68f
            StudyImageAspectClass.STANDARD_LANDSCAPE -> 0.82f
            StudyImageAspectClass.SQUARE,
            StudyImageAspectClass.PORTRAIT -> 1f
            StudyImageAspectClass.EXTREME -> if (ratio > 1f) 0.58f else 1f
        }
        val frameHeight = (heightBudgetDp * heightFraction).roundToInt()
            .coerceIn(heightBudgetDp.coerceAtMost(112), heightBudgetDp)
        val antiUpscaleWidth = (intrinsicWidthDp * 1.35f).roundToInt()
        val maximumWidth = minOf(
            availableWidthDp,
            maxOf(160.coerceAtMost(availableWidthDp), antiUpscaleWidth)
        )
        return AdaptiveStudyImagePresentation(aspectClass, maximumWidth, frameHeight)
    }
}

internal object GoldenAnswerImageHeightResolver {
    fun heightDp(
        availableBodyHeightDp: Int,
        viewport: SignatureStudyViewport,
        examplesExpanded: Boolean
    ): Int {
        require(availableBodyHeightDp > 0)
        val fraction = when (viewport) {
            SignatureStudyViewport.EXPANDED -> if (examplesExpanded) 0.38f else 0.52f
            SignatureStudyViewport.STANDARD -> if (examplesExpanded) 0.34f else 0.48f
            SignatureStudyViewport.COMPACT -> if (examplesExpanded) 0.30f else 0.43f
            SignatureStudyViewport.COMPRESSED -> if (examplesExpanded) 0.24f else 0.34f
        }
        return (availableBodyHeightDp * fraction).roundToInt().coerceAtLeast(120)
    }
}
