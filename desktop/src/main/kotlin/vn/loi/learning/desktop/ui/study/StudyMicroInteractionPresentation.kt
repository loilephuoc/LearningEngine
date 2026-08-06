package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

@Immutable
internal data class StudyRevealTransitionPresentation(
    val answerAlpha: Float,
    val meaningAlpha: Float,
    val schedulerAlpha: Float,
    val answerTranslationFraction: Float
)

internal object StudyMicroInteractionResolver {
    fun reveal(progress: Float): StudyRevealTransitionPresentation {
        val bounded = progress.coerceIn(0f, 1f)
        val eased = smoothStep(bounded)
        return StudyRevealTransitionPresentation(
            answerAlpha = smoothStep(stagedProgress(bounded, start = 0.02f)),
            meaningAlpha = smoothStep(stagedProgress(bounded, start = 0.28f)),
            schedulerAlpha = smoothStep(stagedProgress(bounded, start = 0.62f)),
            answerTranslationFraction = 1f - eased
        )
    }

    private fun stagedProgress(progress: Float, start: Float): Float =
        ((progress - start) / (1f - start)).coerceIn(0f, 1f)

    private fun smoothStep(value: Float): Float {
        val bounded = value.coerceIn(0f, 1f)
        return bounded * bounded * (3f - 2f * bounded)
    }
}
