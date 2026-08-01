package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.learningexperience.TypingExpectedPrefixError
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.application.confidence.MemoryConfidenceRatingGate

data class TypingRecallSuccessRequest(
    val context: ExperienceRotationContext,
    val inputRevision: Long,
    val metrics: TypingAttemptMetrics,
    val decision: TypingAutoRatingDecision
)

data class TypingRecallRevealRequest(
    val context: ExperienceRotationContext,
    val attemptGeneration: Long,
    val metrics: TypingAttemptMetrics
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
    val maximumErrorPermille: Int = 0
) {
    val active: Boolean
        get() = phase == TypingAttemptPhase.ACTIVE

    fun elapsedMillis(nowMillis: Long): Long =
        ((stoppedAtMillis ?: nowMillis) - startedAtMillis).coerceAtLeast(0L)

    fun activeTypingElapsedMillis(nowMillis: Long): Long {
        val firstInput = firstInputAtMillis ?: return 0L
        return ((stoppedAtMillis ?: nowMillis) - firstInput).coerceAtLeast(0L)
    }

    fun snapshot(revealUsed: Boolean): TypingAttemptMetrics {
        val completedAt = requireNotNull(stoppedAtMillis) {
            "Typing attempt metrics require a stopped attempt."
        }
        return metricsAt(completedAt, revealUsed, phase == TypingAttemptPhase.COMPLETED_EXACTLY)
    }

    internal fun projectedMetrics(
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
            recallLatencyMillis = ((firstInput ?: completedAt) - startedAtMillis).coerceAtLeast(0L),
            typingDurationMillis =
                if (firstInput == null) 0L else (completedAt - firstInput).coerceAtLeast(0L),
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
            isImmediatePostLapseRecovery(metrics)
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

    private fun isImmediatePostLapseRecovery(metrics: TypingAttemptMetrics): Boolean =
        metrics.itemOrigin == SessionItemOrigin.REVIEW &&
            metrics.previousRating == ReviewRating.AGAIN &&
            metrics.learningStage == LearningStage.RELEARNING &&
            metrics.reviewedEarlierInCurrentSession

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

enum class TypingRatingMode {
    STANDARD,
    AUTOMATIC_PENDING,
    FORCED_AGAIN
}

data class TypingRecallUiState(
    val itemId: String? = null,
    val input: String = "",
    val selection: TextRange = TextRange(input.length),
    val composition: TextRange? = null,
    val liveEvaluation: TypingAnswerEvaluation? = null,
    val revealEvaluation: TypingAnswerEvaluation? = null,
    val explicitIncorrectFeedback: Boolean = false,
    val automaticSuccessRequested: Boolean = false,
    val successInProgress: Boolean = false,
    val inputRevision: Long = 0,
    val attempt: TypingAttemptState? = null
) {
    val textFieldValue: TextFieldValue
        get() = TextFieldValue(input, selection, composition)
}

data class TypingRecallSubmissionOutcome(
    val state: TypingRecallUiState,
    val shouldRevealAnswer: Boolean
)

data class TypingInputPresentation(
    val minimumHeightDp: Int,
    val maximumHeightDp: Int,
    val typedTextFontSizeSp: Int,
    val typedTextLineHeightSp: Int,
    val typedTextFontWeight: FontWeight,
    val placeholderFontSizeSp: Int,
    val placeholderLineHeightSp: Int,
    val placeholderAlpha: Float,
    val labelFontSizeSp: Int,
    val horizontalAlignment: TextAlign,
    val letterSpacingSp: Int,
    val revealWidthFraction: Float
)

data class TypingInputLinePresentation(
    val singleLine: Boolean,
    val minimumLines: Int,
    val maximumLines: Int
)

data class TypingMeaningPresentation(
    val meaningFontSizeSp: Int,
    val meaningLineHeightSp: Int,
    val posFontSizeSp: Int
)

data class TypingSuccessOverlayPresentation(
    val answerFontSizeSp: Int,
    val answerLineHeightSp: Int,
    val horizontalMarginDp: Int
)

internal object TypingPresentationResolver {
    fun input(viewportClass: StudyViewportClass): TypingInputPresentation =
        input(viewportClass, StudyHeightMode.COMFORTABLE)

    fun input(layout: StudyVisualLayout): TypingInputPresentation =
        input(layout.viewportClass, layout.heightMode)

    private fun input(
        viewportClass: StudyViewportClass,
        heightMode: StudyHeightMode
    ): TypingInputPresentation {
        val base = when (viewportClass) {
            StudyViewportClass.WIDE ->
                TypingInputPresentation(
                    116, 208, 48, 56, FontWeight.SemiBold,
                    40, 48, 0.70f, 15, TextAlign.Center, 0, 0.70f
                )
            StudyViewportClass.STANDARD ->
                TypingInputPresentation(
                    106, 196, 43, 51, FontWeight.SemiBold,
                    36, 44, 0.70f, 15, TextAlign.Center, 0, 0.82f
                )
            StudyViewportClass.COMPACT ->
                TypingInputPresentation(
                    96, 184, 38, 46, FontWeight.SemiBold,
                    32, 40, 0.70f, 14, TextAlign.Center, 0, 1f
                )
        }
        val reclaimedHeightDp = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 32
            StudyHeightMode.COMPACT_HEIGHT -> 24
            StudyHeightMode.MINIMUM_HEIGHT -> 8
        }
        return base.copy(
            minimumHeightDp =
                (base.minimumHeightDp + reclaimedHeightDp)
                    .coerceAtMost(base.maximumHeightDp)
        )
    }

    fun lineLayout(input: String): TypingInputLinePresentation {
        val multiline = input.codePointCount(0, input.length) > 24 || '\n' in input
        return if (multiline) {
            TypingInputLinePresentation(singleLine = false, minimumLines = 2, maximumLines = 5)
        } else {
            TypingInputLinePresentation(singleLine = true, minimumLines = 1, maximumLines = 1)
        }
    }

    fun meaning(viewportClass: StudyViewportClass): TypingMeaningPresentation =
        when (viewportClass) {
            StudyViewportClass.WIDE -> TypingMeaningPresentation(32, 40, 15)
            StudyViewportClass.STANDARD -> TypingMeaningPresentation(28, 36, 14)
            StudyViewportClass.COMPACT -> TypingMeaningPresentation(24, 32, 13)
        }

    fun successOverlay(
        viewportClass: StudyViewportClass,
        canonicalAnswer: String
    ): TypingSuccessOverlayPresentation {
        val longAnswer = canonicalAnswer.length > 24
        return when (viewportClass) {
            StudyViewportClass.WIDE ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 30 else 44,
                    if (longAnswer) 38 else 52,
                    48
                )
            StudyViewportClass.STANDARD ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 27 else 38,
                    if (longAnswer) 35 else 46,
                    32
                )
            StudyViewportClass.COMPACT ->
                TypingSuccessOverlayPresentation(
                    if (longAnswer) 24 else 30,
                    if (longAnswer) 32 else 38,
                    16
                )
        }
    }
}

internal fun shouldRequestTypingInputFocus(
    enabled: Boolean,
    successInProgress: Boolean
): Boolean = enabled && !successInProgress

internal fun formatTypingElapsed(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

object TypingRecallInteraction {
    fun initial(itemId: String?): TypingRecallUiState =
        TypingRecallUiState(itemId = itemId)

    fun beginAttempt(
        state: TypingRecallUiState,
        context: ExperienceRotationContext,
        prompt: TypingRecallPrompt,
        itemOrigin: SessionItemOrigin,
        learningStage: LearningStage?,
        previousRating: ReviewRating?,
        previousReviewAtMillis: Long? = null,
        reviewedEarlierInCurrentSession: Boolean = false,
        memoryContextReliable: Boolean = false,
        itemPresentedAtEpochMillis: Long? = null,
        easyConfidenceProjection: MemoryConfidenceProjection? = null,
        nowMillis: Long
    ): TypingRecallUiState {
        if (state.attempt?.context == context && state.attempt.active) return state
        return state.copy(
            attempt =
                TypingAttemptState(
                    context = context,
                    attemptGeneration = state.inputRevision + 1,
                    startedAtMillis = nowMillis,
                    canonicalCodePointCount =
                        prompt.expectedAnswer.codePointCount(0, prompt.expectedAnswer.length),
                    itemOrigin = itemOrigin,
                    learningStage = learningStage,
                    previousRating = previousRating,
                    previousReviewAtMillis = previousReviewAtMillis,
                    reviewedEarlierInCurrentSession = reviewedEarlierInCurrentSession,
                    memoryContextReliable = memoryContextReliable,
                    itemPresentedAtEpochMillis = itemPresentedAtEpochMillis,
                    easyConfidenceProjection = easyConfidenceProjection
                )
        )
    }

    fun updateInput(
        state: TypingRecallUiState,
        value: TextFieldValue,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        nowMillis: Long = TypingAttemptTimeSource.MONOTONIC.nowMillis()
    ): TypingRecallUiState {
        val rawChanged = value.text != state.input
        val compositionChanged = value.composition != state.composition
        if (!rawChanged && !compositionChanged) {
            return state.copy(selection = value.selection)
        }
        val evaluation = evaluator.evaluate(prompt, value.text)
        val prefixError = evaluator.evaluateExpectedPrefix(prompt, value.text)
        val updatedAttempt =
            updateAttemptForInput(
                attempt = state.attempt,
                value = value,
                evaluation = evaluation,
                prefixError = prefixError,
                nowMillis = nowMillis
            )
        return state.copy(
            input = value.text,
            selection = value.selection,
            composition = value.composition,
            liveEvaluation = evaluation,
            revealEvaluation = null,
            explicitIncorrectFeedback = false,
            automaticSuccessRequested =
                evaluation.status == TypingAnswerEvaluationStatus.CORRECT &&
                    value.composition == null,
            successInProgress = false,
            inputRevision = state.inputRevision + 1,
            attempt = updatedAttempt
        )
    }

    fun updateInput(
        state: TypingRecallUiState,
        input: String,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        nowMillis: Long = TypingAttemptTimeSource.MONOTONIC.nowMillis()
    ): TypingRecallUiState =
        updateInput(
            state,
            TextFieldValue(input, selection = TextRange(input.length)),
            prompt,
            evaluator,
            nowMillis
        )

    fun submit(
        state: TypingRecallUiState,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        actionInProgress: Boolean = false
    ): TypingRecallSubmissionOutcome? {
        if (actionInProgress || state.successInProgress) return null
        val evaluation = evaluator.evaluate(prompt, state.input)
        return TypingRecallSubmissionOutcome(
            state =
                state.copy(
                    liveEvaluation = evaluation,
                    explicitIncorrectFeedback =
                        evaluation.status == TypingAnswerEvaluationStatus.INCORRECT,
                    automaticSuccessRequested =
                        evaluation.status == TypingAnswerEvaluationStatus.CORRECT &&
                            state.composition == null,
                    inputRevision =
                        if (state.automaticSuccessRequested) state.inputRevision
                        else state.inputRevision + 1
                ),
            shouldRevealAnswer = false
        )
    }

    fun confirmRealtimeSuccess(
        state: TypingRecallUiState,
        expectedRevision: Long
    ): TypingRecallUiState =
        if (
            state.inputRevision == expectedRevision &&
            state.automaticSuccessRequested &&
            state.composition == null &&
            state.liveEvaluation?.status == TypingAnswerEvaluationStatus.CORRECT
        ) {
            state.copy(successInProgress = true)
        } else {
            state
        }

    fun cancelAutomaticSuccess(state: TypingRecallUiState): TypingRecallUiState =
        state.copy(
            automaticSuccessRequested = false,
            successInProgress = false,
            inputRevision = state.inputRevision + 1
        )

    fun evaluateForReveal(
        state: TypingRecallUiState,
        prompt: TypingRecallPrompt,
        evaluator: TypingAnswerEvaluator,
        nowMillis: Long = TypingAttemptTimeSource.MONOTONIC.nowMillis()
    ): TypingRecallUiState {
        val evaluation = evaluator.evaluate(prompt, state.input)
        return state.copy(
            liveEvaluation = evaluation,
            revealEvaluation = evaluation,
            explicitIncorrectFeedback = false,
            automaticSuccessRequested = false,
            successInProgress = false,
            inputRevision = state.inputRevision + 1,
            attempt =
                state.attempt?.takeIf { it.active }?.copy(
                    committedInput = state.input,
                    phase = TypingAttemptPhase.REVEALED,
                    stoppedAtMillis = nowMillis
                ) ?: state.attempt
        )
    }

    fun cancelAttempt(
        state: TypingRecallUiState,
        nowMillis: Long = TypingAttemptTimeSource.MONOTONIC.nowMillis()
    ): TypingRecallUiState =
        state.copy(
            attempt =
                state.attempt?.takeIf { it.active }?.copy(
                    phase = TypingAttemptPhase.CANCELLED,
                    stoppedAtMillis = nowMillis
                ) ?: state.attempt
        )

    private fun updateAttemptForInput(
        attempt: TypingAttemptState?,
        value: TextFieldValue,
        evaluation: TypingAnswerEvaluation,
        prefixError: TypingExpectedPrefixError,
        nowMillis: Long
    ): TypingAttemptState? {
        if (attempt == null || !attempt.active || value.composition != null) return attempt
        if (value.text == attempt.committedInput) return attempt
        val mismatchSpanCount =
            resolvePositionalTypingLiveDiff(value.text, evaluation)
                ?.mismatchSpans
                ?.size
                ?: 0
        val correction =
            attempt.committedInput.isNotEmpty() &&
                (
                    value.text.codePointCount(0, value.text.length) <
                        attempt.committedInput.codePointCount(0, attempt.committedInput.length) ||
                        mismatchSpanCount < attempt.lastMismatchSpanCount ||
                        attempt.lastMismatchSpanCount > 0
                )
        val exact = evaluation.status == TypingAnswerEvaluationStatus.CORRECT
        val hasError = prefixError.hasError
        val startsEpisode = hasError && !attempt.mistakeActive
        val errorPermille =
            prefixError.editDistance * 1_000 /
                attempt.canonicalCodePointCount.coerceAtLeast(1)
        return attempt.copy(
            committedInput = value.text,
            firstInputAtMillis =
                attempt.firstInputAtMillis ?: nowMillis.takeIf { value.text.isNotEmpty() },
            materialInputChangeCount = attempt.materialInputChangeCount + 1,
            mismatchEventCount =
                attempt.mismatchEventCount + if (mismatchSpanCount > 0) 1 else 0,
            correctionEventCount = attempt.correctionEventCount + if (correction) 1 else 0,
            lastMismatchSpanCount = mismatchSpanCount,
            hadMismatch = attempt.hadMismatch || hasError,
            mistakeEpisodeCount = attempt.mistakeEpisodeCount + if (startsEpisode) 1 else 0,
            mistakeActive = hasError && !exact,
            maximumEditDistance = maxOf(attempt.maximumEditDistance, prefixError.editDistance),
            maximumErrorPermille =
                maxOf(attempt.maximumErrorPermille, errorPermille.coerceAtMost(1_000)),
            phase =
                if (exact) TypingAttemptPhase.COMPLETED_EXACTLY
                else TypingAttemptPhase.ACTIVE,
            stoppedAtMillis = nowMillis.takeIf { exact }
        )
    }
}
