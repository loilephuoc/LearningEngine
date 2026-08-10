package vn.loi.learning.application.typing

import vn.loi.learning.application.confidence.MemoryConfidenceRatingGate
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingExpectedPrefixError
import vn.loi.learning.application.learningexperience.TypingDifferenceKind
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

data class TypingRecallSuccessRequest(
    val context: ExperienceRotationContext,
    val inputRevision: Long,
    val metrics: TypingAttemptMetrics,
    val decision: TypingAutoRatingDecision,
    val submissionText: String = ""
)

data class TypingRecallRevealRequest(
    val context: ExperienceRotationContext,
    val attemptGeneration: Long,
    val metrics: TypingAttemptMetrics,
    val submissionText: String = ""
)

fun interface TypingAttemptTimeSource {
    fun nowMillis(): Long

    companion object {
        val MONOTONIC = TypingAttemptTimeSource { System.nanoTime() / 1_000_000L }
    }
}

enum class TypingAttemptPhase {
    ACTIVE,
    COMPLETED_EXACTLY,
    REVEALED,
    CANCELLED
}

enum class TypingTimingPolicy { MEASURE_FROM_PRESENTATION, MEASURE_FROM_FIRST_INPUT }

data class TypingAttemptMetrics(
    val context: ExperienceRotationContext,
    val attemptGeneration: Long,
    val startedAtMillis: Long,
    val firstInputAtMillis: Long?,
    val completedAtMillis: Long,
    val totalElapsedMillis: Long,
    val recallLatencyMillis: Long,
    val typingDurationMillis: Long,
    val canonicalCodePointCount: Int,
    val materialInputChangeCount: Int,
    val mismatchEventCount: Int,
    val correctionEventCount: Int,
    val hadMismatch: Boolean,
    val revealUsed: Boolean,
    val completedExactly: Boolean,
    val finalInputCodePointCount: Int,
    val itemOrigin: SessionItemOrigin,
    val learningStage: LearningStage?,
    val previousRating: ReviewRating?,
    val previousReviewAtMillis: Long? = null,
    val reviewedEarlierInCurrentSession: Boolean = false,
    val lapsedEarlierInCurrentSession: Boolean = false,
    val memoryContextReliable: Boolean = false,
    val itemPresentedAtEpochMillis: Long? = null,
    val easyConfidenceProjection: MemoryConfidenceProjection? = null,
    val typingQuality: TypingAttemptQuality = TypingAttemptQuality.CLEAN
) {
    val presentedAtMillis: Long get() = startedAtMillis
    val totalAttemptElapsedMillis: Long get() = totalElapsedMillis
    val preTypingLatencyMillis: Long get() = recallLatencyMillis
    val activeTypingDurationMillis: Long get() = typingDurationMillis
}

data class TypingAttemptState(
    val context: ExperienceRotationContext,
    val attemptGeneration: Long,
    val startedAtMillis: Long,
    val canonicalCodePointCount: Int,
    val itemOrigin: SessionItemOrigin,
    val learningStage: LearningStage?,
    val previousRating: ReviewRating?,
    val previousReviewAtMillis: Long? = null,
    val reviewedEarlierInCurrentSession: Boolean = false,
    val lapsedEarlierInCurrentSession: Boolean = false,
    val memoryContextReliable: Boolean = false,
    val itemPresentedAtEpochMillis: Long? = null,
    val easyConfidenceProjection: MemoryConfidenceProjection? = null,
    val committedInput: String = "",
    val firstInputAtMillis: Long? = null,
    val materialInputChangeCount: Int = 0,
    val mismatchEventCount: Int = 0,
    val correctionEventCount: Int = 0,
    val lastMismatchSpanCount: Int = 0,
    val hadMismatch: Boolean = false,
    val phase: TypingAttemptPhase = TypingAttemptPhase.ACTIVE,
    val stoppedAtMillis: Long? = null,
    val mistakeEpisodeCount: Int = 0,
    val mistakeActive: Boolean = false,
    val maximumEditDistance: Int = 0,
    val maximumErrorPermille: Int = 0,
    val timingPolicy: TypingTimingPolicy = TypingTimingPolicy.MEASURE_FROM_PRESENTATION,
    val pausedAtMillis: Long? = null,
    val accumulatedPausedMillis: Long = 0L
) {
    val active: Boolean
        get() = phase == TypingAttemptPhase.ACTIVE

    fun elapsedMillis(nowMillis: Long): Long =
        ((stoppedAtMillis ?: nowMillis) - startedAtMillis).coerceAtLeast(0L)

    fun activeTypingElapsedMillis(nowMillis: Long): Long {
        val firstInput = firstInputAtMillis ?: return 0L
        val end = stoppedAtMillis ?: pausedAtMillis ?: nowMillis
        return (end - firstInput - accumulatedPausedMillis).coerceAtLeast(0L)
    }

    fun pause(nowMillis: Long): TypingAttemptState =
        if (!active || firstInputAtMillis == null || pausedAtMillis != null) this else copy(pausedAtMillis = nowMillis)

    fun resume(nowMillis: Long): TypingAttemptState = pausedAtMillis?.let {
        copy(pausedAtMillis = null, accumulatedPausedMillis = accumulatedPausedMillis + (nowMillis - it).coerceAtLeast(0L))
    } ?: this

    fun snapshot(revealUsed: Boolean): TypingAttemptMetrics {
        val completedAt = requireNotNull(stoppedAtMillis) {
            "Typing attempt metrics require a stopped attempt."
        }
        return metricsAt(completedAt, revealUsed, phase == TypingAttemptPhase.COMPLETED_EXACTLY)
    }

    fun projectedMetrics(
        nowMillis: Long,
        revealUsed: Boolean = false
    ): TypingAttemptMetrics? {
        if (firstInputAtMillis == null && !revealUsed) return null
        val completedAt = if (active) nowMillis else requireNotNull(stoppedAtMillis)
        return metricsAt(completedAt, revealUsed, completedExactly = !revealUsed)
    }

    private fun metricsAt(
        completedAt: Long,
        revealUsed: Boolean,
        completedExactly: Boolean
    ): TypingAttemptMetrics {
        val firstInput = firstInputAtMillis
        return TypingAttemptMetrics(
            context = context,
            attemptGeneration = attemptGeneration,
            startedAtMillis = startedAtMillis,
            firstInputAtMillis = firstInput,
            completedAtMillis = completedAt,
            totalElapsedMillis = (completedAt - startedAtMillis).coerceAtLeast(0L),
            recallLatencyMillis = if (timingPolicy == TypingTimingPolicy.MEASURE_FROM_FIRST_INPUT) 0L
                else ((firstInput ?: completedAt) - startedAtMillis).coerceAtLeast(0L),
            typingDurationMillis =
                if (firstInput == null) 0L else (completedAt - firstInput - accumulatedPausedMillis).coerceAtLeast(0L),
            canonicalCodePointCount = canonicalCodePointCount,
            materialInputChangeCount = materialInputChangeCount,
            mismatchEventCount = mismatchEventCount,
            correctionEventCount = correctionEventCount,
            hadMismatch = hadMismatch,
            revealUsed = revealUsed,
            completedExactly = completedExactly,
            finalInputCodePointCount = committedInput.codePointCount(0, committedInput.length),
            itemOrigin = itemOrigin,
            learningStage = learningStage,
            previousRating = previousRating,
            previousReviewAtMillis = previousReviewAtMillis,
            reviewedEarlierInCurrentSession = reviewedEarlierInCurrentSession,
            lapsedEarlierInCurrentSession = lapsedEarlierInCurrentSession,
            memoryContextReliable = memoryContextReliable,
            itemPresentedAtEpochMillis = itemPresentedAtEpochMillis,
            easyConfidenceProjection = easyConfidenceProjection,
            typingQuality =
                TypingAttemptQuality.from(
                    canonicalCodePointCount = canonicalCodePointCount,
                    hadAnyMistake = hadMismatch,
                    mistakeEpisodeCount = mistakeEpisodeCount,
                    maximumEditDistance = maximumEditDistance,
                    maximumErrorPermille = maximumErrorPermille,
                    mistakeCorrected = hadMismatch && !mistakeActive,
                    finalExactCompletion = completedExactly
                )
        )
    }
}

enum class TypingSpeedBand { READY, EASY, GOOD, HARD }

object TypingSuccessLifecyclePolicy {
    const val FULL_REVEAL_MILLIS = 265L
    const val VISUAL_HOLD_MILLIS = 600L
    const val TARGET_TOTAL_MILLIS = FULL_REVEAL_MILLIS + VISUAL_HOLD_MILLIS

    fun remainingDwellMillis(elapsedMillis: Long): Long =
        (TARGET_TOTAL_MILLIS - elapsedMillis).coerceAtLeast(0L)
}

object TypingSpeedBandResolver {
    fun resolve(hasFirstInput: Boolean, activeTypingMillis: Long, expectedMillis: Long): TypingSpeedBand =
        when {
            !hasFirstInput -> TypingSpeedBand.READY
            activeTypingMillis >= TypingAutoRatingPolicy.hardActiveTypingMinimumMillis(expectedMillis) -> TypingSpeedBand.HARD
            activeTypingMillis <= TypingAutoRatingPolicy.easyActiveTypingMaximumMillis(expectedMillis) -> TypingSpeedBand.EASY
            else -> TypingSpeedBand.GOOD
        }
}

object TypingAttemptTracker {
    fun update(
        attempt: TypingAttemptState,
        input: String,
        evaluation: TypingAnswerEvaluation,
        prefixError: TypingExpectedPrefixError,
        nowMillis: Long
    ): TypingAttemptState {
        if (!attempt.active || input == attempt.committedInput) return attempt
        val mismatchCount = evaluation.differences.count { it.kind != TypingDifferenceKind.MATCH }
        val correction = attempt.committedInput.isNotEmpty() &&
            (input.codePointCount(0, input.length) < attempt.committedInput.codePointCount(0, attempt.committedInput.length) ||
                mismatchCount < attempt.lastMismatchSpanCount || attempt.lastMismatchSpanCount > 0)
        val exact = evaluation.status == TypingAnswerEvaluationStatus.CORRECT
        val startsEpisode = prefixError.hasError && !attempt.mistakeActive
        val errorPermille = prefixError.editDistance * 1_000 / attempt.canonicalCodePointCount.coerceAtLeast(1)
        return attempt.copy(
            committedInput = input,
            firstInputAtMillis = attempt.firstInputAtMillis ?: nowMillis.takeIf { input.isNotEmpty() },
            materialInputChangeCount = attempt.materialInputChangeCount + 1,
            mismatchEventCount = attempt.mismatchEventCount + if (mismatchCount > 0) 1 else 0,
            correctionEventCount = attempt.correctionEventCount + if (correction) 1 else 0,
            lastMismatchSpanCount = mismatchCount,
            hadMismatch = attempt.hadMismatch || prefixError.hasError,
            mistakeEpisodeCount = attempt.mistakeEpisodeCount + if (startsEpisode) 1 else 0,
            mistakeActive = prefixError.hasError && !exact,
            maximumEditDistance = maxOf(attempt.maximumEditDistance, prefixError.editDistance),
            maximumErrorPermille = maxOf(attempt.maximumErrorPermille, errorPermille.coerceAtMost(1_000)),
            phase = if (exact) TypingAttemptPhase.COMPLETED_EXACTLY else TypingAttemptPhase.ACTIVE,
            stoppedAtMillis = nowMillis.takeIf { exact }
        )
    }
}

enum class TypingQualityClassification {
    CLEAN,
    MINOR_ERROR,
    SIGNIFICANT_ERROR,
    REPEATED_ERROR
}

data class TypingAttemptQuality(
    val canonicalCodePointCount: Int,
    val hadAnyMistake: Boolean,
    val mistakeEpisodeCount: Int,
    val maximumEditDistance: Int,
    val maximumErrorPermille: Int,
    val mistakeCorrected: Boolean,
    val finalExactCompletion: Boolean,
    val classification: TypingQualityClassification
) {
    val isHardEvidence: Boolean
        get() = classification in
            setOf(
                TypingQualityClassification.SIGNIFICANT_ERROR,
                TypingQualityClassification.REPEATED_ERROR
            )

    companion object {
        val CLEAN =
            from(1, false, 0, 0, 0, false, false)

        fun from(
            canonicalCodePointCount: Int,
            hadAnyMistake: Boolean,
            mistakeEpisodeCount: Int,
            maximumEditDistance: Int,
            maximumErrorPermille: Int,
            mistakeCorrected: Boolean,
            finalExactCompletion: Boolean
        ): TypingAttemptQuality {
            val classification =
                when {
                    mistakeEpisodeCount >= 3 -> TypingQualityClassification.REPEATED_ERROR
                    maximumEditDistance >= 2 && maximumErrorPermille >= 350 ->
                        TypingQualityClassification.SIGNIFICANT_ERROR
                    hadAnyMistake -> TypingQualityClassification.MINOR_ERROR
                    else -> TypingQualityClassification.CLEAN
                }
            return TypingAttemptQuality(
                canonicalCodePointCount.coerceAtLeast(1),
                hadAnyMistake,
                mistakeEpisodeCount.coerceAtLeast(0),
                maximumEditDistance.coerceAtLeast(0),
                maximumErrorPermille.coerceIn(0, 1_000),
                mistakeCorrected,
                finalExactCompletion,
                classification
            )
        }
    }
}

enum class TypingAutoRatingReason {
    REVEAL_USED,
    SLOW_ACTIVE_TYPING,
    VERY_SLOW_RECALL,
    SIGNIFICANT_TYPING_ERROR,
    REPEATED_TYPING_ERRORS,
    FAST_CLEAN_REVIEW,
    SHORT_TERM_MEMORY_GUARD,
    MINOR_TYPO_CORRECTED,
    STANDARD_EXACT,
    CONFIDENCE_BELOW_HIGH,
    CONFIDENCE_UNAVAILABLE,
    CONFIDENCE_UNRELIABLE
}

data class TypingAutoRatingDecision(
    val rating: ReviewRating,
    val reason: TypingAutoRatingReason,
    val normalizedExpectedMillis: Long,
    val unconstrainedAttemptRating: ReviewRating = rating,
    val easyEligible: Boolean = rating == ReviewRating.EASY
)

object TypingAutomaticRatingResolver {
    fun decide(metrics: TypingAttemptMetrics): TypingAutoRatingDecision {
        val candidate = TypingAutoRatingPolicy.decide(metrics)
        val finalRating =
            MemoryConfidenceRatingGate.apply(
                candidate.rating,
                metrics.easyConfidenceProjection
            )
        return if (finalRating == candidate.rating) {
            candidate
        } else {
            candidate.copy(
                rating = finalRating,
                reason =
                    when {
                        metrics.easyConfidenceProjection == null ->
                            TypingAutoRatingReason.CONFIDENCE_UNAVAILABLE
                        metrics.easyConfidenceProjection.projectedConfidence.reliable.not() ->
                            TypingAutoRatingReason.CONFIDENCE_UNRELIABLE
                        else -> TypingAutoRatingReason.CONFIDENCE_BELOW_HIGH
                    },
                easyEligible = false
            )
        }
    }
}

object TypingAutoRatingPolicy {
    const val MINIMUM_EASY_SPACED_INTERVAL_MILLIS = 12L * 60L * 60L * 1_000L
    const val BASE_TYPING_ALLOWANCE_MILLIS = 4_000L
    const val PER_CODE_POINT_TYPING_ALLOWANCE_MILLIS = 450L
    const val MINIMUM_EXPECTED_TYPING_MILLIS = 6_000L
    const val MAXIMUM_EXPECTED_TYPING_MILLIS = 30_000L
    const val HARD_ACTIVE_TYPING_PERCENT = 160L
    const val MINIMUM_HARD_PRE_TYPING_MILLIS = 8_000L
    const val EASY_ACTIVE_TYPING_PERCENT = 45L
    const val MINIMUM_EASY_ACTIVE_TYPING_MILLIS = 2_500L
    const val MAXIMUM_EASY_ACTIVE_TYPING_MILLIS = 8_000L
    const val EASY_MAXIMUM_PRE_TYPING_MILLIS = 3_000L

    fun expectedMillis(canonicalCodePointCount: Int): Long =
        (BASE_TYPING_ALLOWANCE_MILLIS +
            canonicalCodePointCount.coerceAtLeast(0) * PER_CODE_POINT_TYPING_ALLOWANCE_MILLIS)
            .coerceIn(MINIMUM_EXPECTED_TYPING_MILLIS, MAXIMUM_EXPECTED_TYPING_MILLIS)

    fun hardActiveTypingMinimumMillis(expectedMillis: Long): Long =
        (expectedMillis * HARD_ACTIVE_TYPING_PERCENT + 99L) / 100L

    fun hardPreTypingThresholdMillis(expectedMillis: Long): Long =
        maxOf(MINIMUM_HARD_PRE_TYPING_MILLIS, expectedMillis)

    fun easyActiveTypingMaximumMillis(expectedMillis: Long): Long =
        (expectedMillis * EASY_ACTIVE_TYPING_PERCENT / 100L)
            .coerceIn(MINIMUM_EASY_ACTIVE_TYPING_MILLIS, MAXIMUM_EASY_ACTIVE_TYPING_MILLIS)

    fun easyPreTypingMaximumMillis(expectedMillis: Long): Long =
        minOf(EASY_MAXIMUM_PRE_TYPING_MILLIS, expectedMillis / 2L)

    fun decide(metrics: TypingAttemptMetrics): TypingAutoRatingDecision {
        val expected = expectedMillis(metrics.canonicalCodePointCount)
        if (metrics.revealUsed) {
            return TypingAutoRatingDecision(
                ReviewRating.AGAIN,
                TypingAutoRatingReason.REVEAL_USED,
                expected
            )
        }
        require(metrics.completedExactly) {
            "Automatic Typing rating requires exact completion or Reveal evidence."
        }
        val hardReason =
            when {
                metrics.activeTypingDurationMillis >= hardActiveTypingMinimumMillis(expected) ->
                    TypingAutoRatingReason.SLOW_ACTIVE_TYPING
                metrics.preTypingLatencyMillis >= hardPreTypingThresholdMillis(expected) ->
                    TypingAutoRatingReason.VERY_SLOW_RECALL
                metrics.typingQuality.classification == TypingQualityClassification.SIGNIFICANT_ERROR ->
                    TypingAutoRatingReason.SIGNIFICANT_TYPING_ERROR
                metrics.typingQuality.classification == TypingQualityClassification.REPEATED_ERROR ->
                    TypingAutoRatingReason.REPEATED_TYPING_ERRORS
                else -> null
            }
        if (hardReason != null) {
            return TypingAutoRatingDecision(ReviewRating.HARD, hardReason, expected)
        }
        val fastCleanAttempt =
            metrics.activeTypingDurationMillis <= easyActiveTypingMaximumMillis(expected) &&
                !metrics.typingQuality.hadAnyMistake &&
                metrics.preTypingLatencyMillis <= easyPreTypingMaximumMillis(expected)
        val candidate = if (fastCleanAttempt && isEasyAvailable(metrics, expected)) {
            TypingAutoRatingDecision(
                ReviewRating.EASY,
                TypingAutoRatingReason.FAST_CLEAN_REVIEW,
                expected,
                unconstrainedAttemptRating = ReviewRating.EASY,
                easyEligible = true
            )
        } else if (fastCleanAttempt) {
            TypingAutoRatingDecision(
                ReviewRating.GOOD,
                TypingAutoRatingReason.SHORT_TERM_MEMORY_GUARD,
                expected,
                unconstrainedAttemptRating = ReviewRating.EASY,
                easyEligible = false
            )
        } else if (metrics.typingQuality.hadAnyMistake) {
            TypingAutoRatingDecision(
                ReviewRating.GOOD,
                TypingAutoRatingReason.MINOR_TYPO_CORRECTED,
                expected,
                easyEligible = false
            )
        } else {
            TypingAutoRatingDecision(
                ReviewRating.GOOD,
                TypingAutoRatingReason.STANDARD_EXACT,
                expected
            )
        }
        return if (candidate.rating in setOf(ReviewRating.GOOD, ReviewRating.EASY) &&
            metrics.lapsedEarlierInCurrentSession
        ) {
            candidate.copy(
                rating = ReviewRating.HARD,
                reason = TypingAutoRatingReason.SHORT_TERM_MEMORY_GUARD,
                easyEligible = false
            )
        } else {
            candidate
        }
    }

    fun isEasyAvailable(
        metrics: TypingAttemptMetrics,
        expectedMillis: Long = expectedMillis(metrics.canonicalCodePointCount)
    ): Boolean =
        metrics.itemOrigin == SessionItemOrigin.REVIEW &&
            metrics.learningStage in setOf(LearningStage.REVIEW, LearningStage.MASTERED) &&
            metrics.previousRating != ReviewRating.AGAIN &&
            metrics.previousRating in setOf(ReviewRating.GOOD, ReviewRating.EASY) &&
            !metrics.typingQuality.hadAnyMistake &&
            metrics.preTypingLatencyMillis <= easyPreTypingMaximumMillis(expectedMillis) &&
            metrics.memoryContextReliable &&
            !metrics.reviewedEarlierInCurrentSession &&
            metrics.previousReviewAtMillis != null &&
            metrics.itemPresentedAtEpochMillis != null &&
            metrics.itemPresentedAtEpochMillis - metrics.previousReviewAtMillis >=
                MINIMUM_EASY_SPACED_INTERVAL_MILLIS

    fun hasSpacedMemoryEvidence(
        previousRating: ReviewRating?,
        previousReviewAtMillis: Long?,
        reviewedEarlierInCurrentSession: Boolean,
        memoryContextReliable: Boolean,
        itemPresentedAtEpochMillis: Long?
    ): Boolean =
        memoryContextReliable &&
            !reviewedEarlierInCurrentSession &&
            previousRating in setOf(ReviewRating.GOOD, ReviewRating.EASY) &&
            previousReviewAtMillis != null &&
            itemPresentedAtEpochMillis != null &&
            itemPresentedAtEpochMillis - previousReviewAtMillis >=
                MINIMUM_EASY_SPACED_INTERVAL_MILLIS
}

object TypingForcedAgainPolicy {
    const val TIMEOUT_PERCENT = 300L

    fun timeoutMillis(canonicalCodePointCount: Int): Long =
        TypingAutoRatingPolicy.expectedMillis(canonicalCodePointCount) * TIMEOUT_PERCENT / 100L

    fun hasTimedOut(attempt: TypingAttemptState, nowMillis: Long): Boolean =
        attempt.firstInputAtMillis != null &&
            attempt.activeTypingElapsedMillis(nowMillis) >= timeoutMillis(attempt.canonicalCodePointCount)
}
