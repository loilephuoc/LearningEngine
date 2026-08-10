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
import vn.loi.learning.application.partofspeech.normalizePronunciation

typealias TypingRecallSuccessRequest = vn.loi.learning.application.typing.TypingRecallSuccessRequest
typealias TypingRecallRevealRequest = vn.loi.learning.application.typing.TypingRecallRevealRequest
typealias TypingAttemptTimeSource = vn.loi.learning.application.typing.TypingAttemptTimeSource
typealias TypingAttemptPhase = vn.loi.learning.application.typing.TypingAttemptPhase
typealias TypingAttemptMetrics = vn.loi.learning.application.typing.TypingAttemptMetrics
typealias TypingAttemptState = vn.loi.learning.application.typing.TypingAttemptState
typealias TypingQualityClassification = vn.loi.learning.application.typing.TypingQualityClassification
typealias TypingAttemptQuality = vn.loi.learning.application.typing.TypingAttemptQuality
typealias TypingAutoRatingReason = vn.loi.learning.application.typing.TypingAutoRatingReason
typealias TypingAutoRatingDecision = vn.loi.learning.application.typing.TypingAutoRatingDecision
typealias TypingAutomaticRatingResolver = vn.loi.learning.application.typing.TypingAutomaticRatingResolver
typealias TypingAutoRatingPolicy = vn.loi.learning.application.typing.TypingAutoRatingPolicy
typealias TypingSuccessLifecyclePolicy = vn.loi.learning.application.typing.TypingSuccessLifecyclePolicy

data class PopupLexicalMetadataPresentation(
    val ipa: String?,
    val partOfSpeech: String?
) {
    val visible: Boolean get() = ipa != null || partOfSpeech != null
}

object PopupLexicalMetadataResolver {
    fun resolve(ipa: String?, partOfSpeech: String?): PopupLexicalMetadataPresentation =
        PopupLexicalMetadataPresentation(
            ipa = normalizePronunciation(ipa).ipa,
            partOfSpeech = partOfSpeech?.trim()?.takeIf(String::isNotBlank)
        )
}

enum class TypingSuccessRevealStage { ICON, ANSWER, TRANSLATION, LEXICAL_METADATA, RESULT }

data class TypingSuccessRevealSegment(
    val stage: TypingSuccessRevealStage,
    val startsAtMillis: Int,
    val durationMillis: Int
) {
    init {
        require(startsAtMillis >= 0)
        require(durationMillis > 0)
    }

    fun progressAt(elapsedMillis: Float): Float =
        ((elapsedMillis - startsAtMillis) / durationMillis).coerceIn(0f, 1f)
}

data class TypingSuccessRevealTimeline(val segments: List<TypingSuccessRevealSegment>) {
    val completesAtMillis: Int = segments.maxOf { it.startsAtMillis + it.durationMillis }
    fun segment(stage: TypingSuccessRevealStage): TypingSuccessRevealSegment =
        requireNotNull(segments.singleOrNull { it.stage == stage })
}

data class TypingSuccessRevealVisual(
    val alpha: Float,
    val upwardOffsetDp: Float,
    val scale: Float = 1f
)

fun resolveTypingSuccessRevealVisual(
    segment: TypingSuccessRevealSegment,
    elapsedMillis: Float
): TypingSuccessRevealVisual {
    val linear = segment.progressAt(elapsedMillis)
    val eased = linear * linear * (3f - 2f * linear)
    return TypingSuccessRevealVisual(
        alpha = eased,
        upwardOffsetDp = (1f - eased) * 3f,
        scale = if (segment.stage == TypingSuccessRevealStage.ICON) 0.88f + 0.12f * eased else 1f
    )
}

object TypingSuccessRevealTimelineResolver {
    fun resolve(hasTranslation: Boolean, hasLexicalMetadata: Boolean): TypingSuccessRevealTimeline {
        val stages = buildList {
            add(TypingSuccessRevealStage.ICON to 80)
            add(TypingSuccessRevealStage.ANSWER to 75)
            if (hasTranslation) add(TypingSuccessRevealStage.TRANSLATION to 75)
            if (hasLexicalMetadata) add(TypingSuccessRevealStage.LEXICAL_METADATA to 65)
            add(TypingSuccessRevealStage.RESULT to 65)
        }
        return TypingSuccessRevealTimeline(
            stages.mapIndexed { index, (stage, duration) ->
                TypingSuccessRevealSegment(stage, if (index == 0) 0 else 50 + (index - 1) * 50, duration)
            }
        )
    }
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
    val revealWidthFraction: Float,
    val lineBoxSafetyInsetDp: Int = 4
) {
    val resolvedLineBoxMinimumHeightDp: Int
        get() = maxOf(typedTextLineHeightSp, placeholderLineHeightSp) + lineBoxSafetyInsetDp * 2
}

data class TypingInputLinePresentation(
    val singleLine: Boolean,
    val minimumLines: Int,
    val maximumLines: Int
)

data class TypingFieldLayoutMetrics(
    val lineBoxHeightDp: Int,
    val labelHeightDp: Int,
    val topInsetDp: Int,
    val bottomInsetDp: Int,
    val labelToInputGapDp: Int,
    val trailingActionDiameterDp: Int,
    val outerMinimumHeightDp: Int
)

internal object TypingFieldLayoutMetricsResolver {
    fun resolve(
        input: TypingInputPresentation,
        line: TypingInputLinePresentation = TypingInputLinePresentation(true, 1, 1)
    ): TypingFieldLayoutMetrics {
        val lineBox = input.resolvedLineBoxMinimumHeightDp * line.minimumLines
        val labelHeight = input.labelFontSizeSp + 4
        val topInset = 8
        val bottomInset = 8
        val labelGap = 2
        val trailingActionDiameter = 48
        val inputRowHeight = maxOf(lineBox, trailingActionDiameter)
        return TypingFieldLayoutMetrics(
            lineBoxHeightDp = lineBox,
            labelHeightDp = labelHeight,
            topInsetDp = topInset,
            bottomInsetDp = bottomInset,
            labelToInputGapDp = labelGap,
            trailingActionDiameterDp = trailingActionDiameter,
            outerMinimumHeightDp =
                topInset + labelHeight + labelGap + inputRowHeight + bottomInset
        )
    }

    fun resolve(viewportWidthDp: Int): TypingFieldLayoutMetrics {
        val viewportClass = when {
            viewportWidthDp >= 1180 -> StudyViewportClass.WIDE
            viewportWidthDp >= 720 -> StudyViewportClass.STANDARD
            else -> StudyViewportClass.COMPACT
        }
        return resolve(TypingPresentationResolver.input(viewportClass))
    }
}

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
        resolveInput(viewportClass)

    fun input(layout: StudyVisualLayout): TypingInputPresentation =
        resolveInput(layout.viewportClass)

    private fun resolveInput(viewportClass: StudyViewportClass): TypingInputPresentation {
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
        return base
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
        lapsedEarlierInCurrentSession: Boolean = false,
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
                    lapsedEarlierInCurrentSession = lapsedEarlierInCurrentSession,
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
