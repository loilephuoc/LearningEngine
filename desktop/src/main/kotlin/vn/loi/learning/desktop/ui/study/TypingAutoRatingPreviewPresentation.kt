package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.memory.model.ReviewRating

internal enum class TypingRatingColorRole {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

internal enum class TypingLegendLayout {
    SINGLE_ROW,
    TWO_BY_TWO
}

internal data class TypingTimerVisualPresentation(
    val valueFontSizeSp: Int,
    val iconSizeDp: Int,
    val legendLayout: TypingLegendLayout
)

internal object TypingTimerPresentationResolver {
    fun resolve(viewportClass: StudyViewportClass): TypingTimerVisualPresentation =
        when (viewportClass) {
            StudyViewportClass.WIDE ->
                TypingTimerVisualPresentation(48, 44, TypingLegendLayout.SINGLE_ROW)
            StudyViewportClass.STANDARD ->
                TypingTimerVisualPresentation(40, 38, TypingLegendLayout.TWO_BY_TWO)
            StudyViewportClass.COMPACT ->
                TypingTimerVisualPresentation(30, 30, TypingLegendLayout.TWO_BY_TWO)
        }
}

internal data class TypingRatingThresholdPresentation(
    val expectedMillis: Long,
    val easyMaximumElapsedMillis: Long,
    val hardMinimumElapsedMillis: Long,
    val easyAvailable: Boolean
)

internal data class TypingRatingPreview(
    val decision: TypingAutoRatingDecision,
    val colorRole: TypingRatingColorRole,
    val elapsedMillis: Long,
    val thresholds: TypingRatingThresholdPresentation
)

internal object TypingAutoRatingPreviewResolver {
    fun resolve(
        attempt: TypingAttemptState?,
        elapsedMillis: Long,
        ratingMode: TypingRatingMode
    ): TypingRatingPreview? {
        if (attempt == null || attempt.phase == TypingAttemptPhase.CANCELLED) return null
        val effectiveElapsed =
            if (attempt.active) {
                elapsedMillis.coerceAtLeast(0L)
            } else {
                attempt.elapsedMillis(attempt.stoppedAtMillis ?: attempt.startedAtMillis)
            }
        val forcedAgain =
            ratingMode == TypingRatingMode.FORCED_AGAIN ||
                attempt.phase == TypingAttemptPhase.REVEALED
        val metrics = attempt.projectedMetrics(effectiveElapsed, revealUsed = forcedAgain)
        val decision = TypingAutoRatingPolicy.decide(metrics)
        val expected = decision.normalizedExpectedMillis
        return TypingRatingPreview(
            decision = decision,
            colorRole = decision.rating.toColorRole(),
            elapsedMillis = effectiveElapsed,
            thresholds =
                TypingRatingThresholdPresentation(
                    expectedMillis = expected,
                    easyMaximumElapsedMillis =
                        expected * TypingAutoRatingPolicy.EASY_TOTAL_PERCENT / 100,
                    hardMinimumElapsedMillis =
                        divideRoundingUp(
                            expected * TypingAutoRatingPolicy.HARD_TOTAL_PERCENT,
                            100L
                        ),
                    easyAvailable =
                        TypingAutoRatingPolicy.isEasyAvailable(metrics, expected)
                )
        )
    }

    private fun ReviewRating.toColorRole(): TypingRatingColorRole =
        when (this) {
            ReviewRating.AGAIN -> TypingRatingColorRole.AGAIN
            ReviewRating.HARD -> TypingRatingColorRole.HARD
            ReviewRating.GOOD -> TypingRatingColorRole.GOOD
            ReviewRating.EASY -> TypingRatingColorRole.EASY
        }

    private fun divideRoundingUp(value: Long, divisor: Long): Long =
        (value + divisor - 1L) / divisor
}

internal fun formatTypingThreshold(milliseconds: Long): String {
    val tenths = (milliseconds.coerceAtLeast(0L) + 50L) / 100L
    return "${tenths / 10L}.${tenths % 10L}s"
}
