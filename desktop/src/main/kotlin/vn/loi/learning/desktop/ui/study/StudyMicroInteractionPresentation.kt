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
        return StudyRevealTransitionPresentation(
            answerAlpha = bounded,
            meaningAlpha = stagedProgress(bounded, start = 0.2f),
            schedulerAlpha = stagedProgress(bounded, start = 0.45f),
            answerTranslationFraction = 1f - bounded
        )
    }

    private fun stagedProgress(progress: Float, start: Float): Float =
        ((progress - start) / (1f - start)).coerceIn(0f, 1f)
}
