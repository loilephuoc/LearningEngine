package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class TypingQualityRemediationTest {
    private val evaluator = TypingAnswerEvaluator()

    @Test
    fun `one typo across several keystrokes is one episode and survives exact completion`() {
        var state = state("relations")
        listOf("r", "re", "relatx", "relatxi", "relatxio", "relation", "relations")
            .forEachIndexed { index, input ->
                state = update(state, input, "relations", index.toLong() + 1)
            }

        val attempt = requireNotNull(state.attempt)
        val quality = attempt.snapshot(false).typingQuality
        assertEquals(1, quality.mistakeEpisodeCount)
        assertTrue(quality.hadAnyMistake)
        assertTrue(quality.mistakeCorrected)
        assertEquals(TypingQualityClassification.MINOR_ERROR, quality.classification)
    }

    @Test
    fun `clean correction followed by new errors creates independent episodes`() {
        var state = state("relations")
        listOf("x", "r", "y", "r", "z", "relations").forEachIndexed { index, input ->
            state = update(state, input, "relations", index.toLong() + 1)
        }

        val quality = requireNotNull(state.attempt).snapshot(false).typingQuality
        assertEquals(3, quality.mistakeEpisodeCount)
        assertEquals(TypingQualityClassification.REPEATED_ERROR, quality.classification)
    }

    @Test
    fun `two errors are significant for bed but minor for longer answers`() {
        val bed = TypingAttemptQuality.from(3, true, 1, 2, 666, true, true)
        val relations = TypingAttemptQuality.from(9, true, 1, 2, 222, true, true)
        val flute = TypingAttemptQuality.from(12, true, 1, 2, 166, true, true)

        assertTrue(bed.isHardEvidence)
        assertFalse(relations.isHardEvidence)
        assertFalse(flute.isHardEvidence)
    }

    @Test
    fun `selection and composition do not add quality evidence`() {
        val initial = state("flute")
        val composing =
            TypingRecallInteraction.updateInput(
                initial,
                TextFieldValue("x", TextRange(1), TextRange(0, 1)),
                TypingRecallPrompt("flute"),
                evaluator,
                1L
            )
        val selected =
            TypingRecallInteraction.updateInput(
                composing,
                TextFieldValue("x", TextRange(0), TextRange(0, 1)),
                TypingRecallPrompt("flute"),
                evaluator,
                2L
            )

        assertEquals(0, selected.attempt?.mistakeEpisodeCount)
    }

    @Test
    fun `easy timing samples use bounded forty five percent calibration`() {
        fun threshold(text: String) =
            TypingAutoRatingPolicy.easyActiveTypingMaximumMillis(
                TypingAutoRatingPolicy.expectedMillis(text.codePointCount(0, text.length))
            )

        assertEquals(2_700L, threshold("bed"))
        assertEquals(3_622L, threshold("relations"))
        assertEquals(4_230L, threshold("flute player"))
        assertEquals(5_850L, threshold("x".repeat(20)))
        assertEquals(8_000L, threshold("x".repeat(58)))
    }

    @Test
    fun `speed band is independent from final rating context`() {
        val expected = TypingAutoRatingPolicy.expectedMillis(12)
        assertEquals(TypingSpeedBand.READY, TypingSpeedBandResolver.resolve(false, 0, expected))
        assertEquals(TypingSpeedBand.EASY, TypingSpeedBandResolver.resolve(true, 2_000, expected))
        assertEquals(TypingSpeedBand.GOOD, TypingSpeedBandResolver.resolve(true, 5_000, expected))
        assertEquals(
            TypingSpeedBand.HARD,
            TypingSpeedBandResolver.resolve(
                true,
                TypingAutoRatingPolicy.hardActiveTypingMinimumMillis(expected),
                expected
            )
        )
    }

    @Test
    fun `normalized severity drives Hard while minor corrected typo stays Good`() {
        fun completed(expected: String, inputs: List<String>): TypingAttemptMetrics {
            var state = state(expected)
            inputs.forEachIndexed { index, input ->
                state = update(state, input, expected, index.toLong() + 1)
            }
            return requireNotNull(state.attempt).snapshot(false)
        }

        val bed = TypingAutoRatingPolicy.decide(completed("bed", listOf("xx", "bed")))
        val relations =
            TypingAutoRatingPolicy.decide(completed("relations", listOf("xx", "relations")))

        assertEquals(ReviewRating.HARD, bed.rating)
        assertEquals(TypingAutoRatingReason.SIGNIFICANT_TYPING_ERROR, bed.reason)
        assertEquals(ReviewRating.GOOD, relations.rating)
        assertEquals(TypingAutoRatingReason.MINOR_TYPO_CORRECTED, relations.reason)
    }

    @Test
    fun `three independent episodes and immediate post-lapse relearning are Hard`() {
        var repeated = state("relations")
        listOf("x", "r", "y", "r", "z", "relations").forEachIndexed { index, input ->
            repeated = update(repeated, input, "relations", index.toLong() + 1)
        }
        val repeatedDecision =
            TypingAutoRatingPolicy.decide(requireNotNull(repeated.attempt).snapshot(false))

        var relearning =
            state(
                expected = "flute player",
                stage = LearningStage.RELEARNING,
                previousRating = ReviewRating.AGAIN,
                reviewedEarlierInCurrentSession = true
            )
        relearning = update(relearning, "flute player", "flute player", 1_000L)
        val relearningDecision =
            TypingAutoRatingPolicy.decide(requireNotNull(relearning.attempt).snapshot(false))

        assertEquals(ReviewRating.HARD, repeatedDecision.rating)
        assertEquals(TypingAutoRatingReason.REPEATED_TYPING_ERRORS, repeatedDecision.reason)
        assertEquals(ReviewRating.HARD, relearningDecision.rating)
        assertEquals(TypingAutoRatingReason.SHORT_TERM_MEMORY_GUARD, relearningDecision.reason)
    }

    private fun state(
        expected: String,
        stage: LearningStage = LearningStage.REVIEW,
        previousRating: ReviewRating = ReviewRating.GOOD,
        reviewedEarlierInCurrentSession: Boolean = false
    ): TypingRecallUiState =
        TypingRecallInteraction.beginAttempt(
            TypingRecallInteraction.initial("item"),
            ExperienceRotationContext(SessionId("session"), LearningItemId("item"), 0),
            TypingRecallPrompt(expected),
            SessionItemOrigin.REVIEW,
            stage,
            previousRating,
            reviewedEarlierInCurrentSession = reviewedEarlierInCurrentSession,
            nowMillis = 0L
        )

    private fun update(
        state: TypingRecallUiState,
        input: String,
        expected: String,
        now: Long
    ): TypingRecallUiState =
        TypingRecallInteraction.updateInput(
            state,
            input,
            TypingRecallPrompt(expected),
            evaluator,
            now
        )
}
