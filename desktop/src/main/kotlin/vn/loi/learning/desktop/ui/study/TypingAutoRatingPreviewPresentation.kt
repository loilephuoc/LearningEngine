package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

internal enum class TypingRatingColorRole {
    READY,
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
    val state: TypingRatingPreviewState,
    val decision: TypingAutoRatingDecision?,
    val colorRole: TypingRatingColorRole,
    val elapsedMillis: Long,
    val thresholds: TypingRatingThresholdPresentation
) {
    val rating: ReviewRating? get() = decision?.rating
}

internal enum class TypingRatingPreviewState {
    READY,
    PROJECTED,
    FINAL,
    FORCED_AGAIN
}

internal object TypingAutoRatingPreviewResolver {
    fun resolve(
        attempt: TypingAttemptState?,
        elapsedMillis: Long,
        ratingMode: TypingRatingMode
    ): TypingRatingPreview? {
        if (attempt == null || attempt.phase == TypingAttemptPhase.CANCELLED) return null
        val forcedAgain =
            ratingMode == TypingRatingMode.FORCED_AGAIN ||
                attempt.phase == TypingAttemptPhase.REVEALED
        val expected = TypingAutoRatingPolicy.expectedMillis(attempt.canonicalCodePointCount)
        val metrics =
            when {
                forcedAgain ->
                    attempt.projectedMetrics(
                        attempt.startedAtMillis + elapsedMillis,
                        revealUsed = true
                    )
                attempt.active -> attempt.projectedMetrics(attempt.startedAtMillis + elapsedMillis)
                else -> attempt.snapshot(revealUsed = false)
            }
        val decision = metrics?.let(TypingAutomaticRatingResolver::decide)
        val state =
            when {
                forcedAgain -> TypingRatingPreviewState.FORCED_AGAIN
                metrics == null -> TypingRatingPreviewState.READY
                attempt.active -> TypingRatingPreviewState.PROJECTED
                else -> TypingRatingPreviewState.FINAL
            }
        return TypingRatingPreview(
            state = state,
            decision = decision,
            colorRole = decision?.rating?.toColorRole() ?: TypingRatingColorRole.READY,
            elapsedMillis = attempt.activeTypingElapsedMillis(attempt.startedAtMillis + elapsedMillis),
            thresholds =
                TypingRatingThresholdPresentation(
                    expectedMillis = expected,
                    easyMaximumElapsedMillis =
                        TypingAutoRatingPolicy.easyActiveTypingMaximumMillis(expected),
                    hardMinimumElapsedMillis =
                        TypingAutoRatingPolicy.hardActiveTypingMinimumMillis(expected),
                    easyAvailable =
                        metrics?.let { TypingAutoRatingPolicy.isEasyAvailable(it, expected) } ?:
                            (
                                attempt.itemOrigin == SessionItemOrigin.REVIEW &&
                                    attempt.learningStage in setOf(
                                        LearningStage.REVIEW,
                                        LearningStage.MASTERED
                                    ) &&
                                    TypingAutoRatingPolicy.hasSpacedMemoryEvidence(
                                        attempt.previousRating,
                                        attempt.previousReviewAtMillis,
                                        attempt.reviewedEarlierInCurrentSession,
                                        attempt.memoryContextReliable,
                                        attempt.itemPresentedAtEpochMillis
                                    )
                            )
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

}

internal fun formatTypingThreshold(milliseconds: Long): String {
    val tenths = (milliseconds.coerceAtLeast(0L) + 50L) / 100L
    return "${tenths / 10L}.${tenths % 10L}s"
}
