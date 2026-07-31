package vn.loi.learning.desktop.ui.study

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class TypingAttemptMeasurementTest {
    private val context =
        ExperienceRotationContext(SessionId("session"), LearningItemId("item"), 0)
    private val prompt = TypingRecallPrompt("socks")
    private val evaluator = TypingAnswerEvaluator()

    @Test
    fun `attempt starts at item bind and timer formatting is deterministic`() {
        val state = activeState(nowMillis = 1_000L)

        assertEquals(1_000L, state.attempt?.startedAtMillis)
        assertEquals("00:00", formatTypingElapsed(0L))
        assertEquals("00:09", formatTypingElapsed(9_999L))
        assertEquals("01:05", formatTypingElapsed(65_000L))
    }

    @Test
    fun `first committed input records latency once and later input preserves it`() {
        val first =
            TypingRecallInteraction.updateInput(
                activeState(),
                "s",
                prompt,
                evaluator,
                nowMillis = 1_400L
            )
        val second =
            TypingRecallInteraction.updateInput(
                first,
                "so",
                prompt,
                evaluator,
                nowMillis = 2_000L
            )

        assertEquals(1_400L, second.attempt?.firstInputAtMillis)
        assertEquals(2, second.attempt?.materialInputChangeCount)
    }

    @Test
    fun `selection-only and composing edits do not count material input`() {
        val initial = activeState()
        val composing =
            TypingRecallInteraction.updateInput(
                initial,
                TextFieldValue("s", TextRange(1), TextRange(0, 1)),
                prompt,
                evaluator,
                nowMillis = 1_100L
            )
        val selected =
            TypingRecallInteraction.updateInput(
                composing,
                TextFieldValue("s", TextRange(0), TextRange(0, 1)),
                prompt,
                evaluator,
                nowMillis = 1_200L
            )
        val committed =
            TypingRecallInteraction.updateInput(
                selected,
                TextFieldValue("s", TextRange(1), null),
                prompt,
                evaluator,
                nowMillis = 1_300L
            )

        assertEquals(0, composing.attempt?.materialInputChangeCount)
        assertEquals(0, selected.attempt?.materialInputChangeCount)
        assertEquals(1, committed.attempt?.materialInputChangeCount)
        assertEquals(1_300L, committed.attempt?.firstInputAtMillis)
        val revealedDuringComposition =
            TypingRecallInteraction.evaluateForReveal(
                composing,
                prompt,
                evaluator,
                nowMillis = 1_250L
            )
        assertEquals(
            1,
            assertNotNull(revealedDuringComposition.attempt)
                .snapshot(revealUsed = true)
                .finalInputCodePointCount
        )
    }

    @Test
    fun `mismatch and backspace correction evidence is counted once per committed state`() {
        val mismatch =
            TypingRecallInteraction.updateInput(
                activeState(),
                "x",
                prompt,
                evaluator,
                nowMillis = 1_100L
            )
        val correction =
            TypingRecallInteraction.updateInput(
                mismatch,
                "",
                prompt,
                evaluator,
                nowMillis = 1_200L
            )

        assertEquals(1, mismatch.attempt?.mismatchEventCount)
        assertTrue(mismatch.attempt?.hadMismatch == true)
        assertEquals(1, correction.attempt?.correctionEventCount)
    }

    @Test
    fun `paste is one material event and exact completion freezes precise metrics`() {
        val exact =
            TypingRecallInteraction.updateInput(
                activeState(),
                "socks",
                prompt,
                evaluator,
                nowMillis = 4_000L
            )
        val metrics = assertNotNull(exact.attempt).snapshot(revealUsed = false)

        assertEquals(TypingAttemptPhase.COMPLETED_EXACTLY, exact.attempt?.phase)
        assertEquals(1, metrics.materialInputChangeCount)
        assertEquals(3_000L, metrics.totalElapsedMillis)
        assertEquals(3_000L, metrics.recallLatencyMillis)
        assertEquals(0L, metrics.typingDurationMillis)
        assertTrue(metrics.completedExactly)
    }

    @Test
    fun `reveal and cancellation stop active elapsed authority`() {
        val live =
            TypingRecallInteraction.updateInput(
                activeState(),
                "so",
                prompt,
                evaluator,
                nowMillis = 1_500L
            )
        val revealed =
            TypingRecallInteraction.evaluateForReveal(
                live,
                prompt,
                evaluator,
                nowMillis = 3_000L
            )
        val cancelled =
            TypingRecallInteraction.cancelAttempt(activeState(), nowMillis = 2_500L)

        assertEquals(TypingAttemptPhase.REVEALED, revealed.attempt?.phase)
        assertEquals(2_000L, revealed.attempt?.elapsedMillis(99_000L))
        assertEquals(TypingAttemptPhase.CANCELLED, cancelled.attempt?.phase)
        assertFalse(cancelled.attempt?.active == true)
    }

    @Test
    fun `canonical code point count supports Unicode and spaces`() {
        val unicodePrompt = TypingRecallPrompt("café au")
        val state =
            TypingRecallInteraction.beginAttempt(
                TypingRecallInteraction.initial("item"),
                context,
                unicodePrompt,
                SessionItemOrigin.REVIEW,
                LearningStage.REVIEW,
                ReviewRating.GOOD,
                0L
            )

        assertEquals(7, state.attempt?.canonicalCodePointCount)
    }

    @Test
    fun `display ticks read elapsed time without mutating attempt metrics`() {
        val attempt = assertNotNull(activeState().attempt)

        assertEquals(1_000L, attempt.elapsedMillis(2_000L))
        assertEquals(2_000L, attempt.elapsedMillis(3_000L))
        assertEquals(0, attempt.materialInputChangeCount)
        assertEquals(TypingAttemptPhase.ACTIVE, attempt.phase)
    }

    private fun activeState(nowMillis: Long = 1_000L): TypingRecallUiState =
        TypingRecallInteraction.beginAttempt(
            TypingRecallInteraction.initial("item"),
            context,
            prompt,
            SessionItemOrigin.REVIEW,
            LearningStage.REVIEW,
            ReviewRating.GOOD,
            nowMillis
        )
}

class TypingAutoRatingPolicyTest {
    @Test
    fun `Reveal is always Again and overrides fast timing`() {
        val decision =
            TypingAutoRatingPolicy.decide(
                metrics(revealUsed = true, completedExactly = false, total = 1L, recall = 1L)
            )

        assertEquals(ReviewRating.AGAIN, decision.rating)
        assertEquals(TypingAutoRatingReason.REVEAL_USED, decision.reason)
    }

    @Test
    fun `normal exact is Good while slow and high-latency exact are Hard`() {
        assertEquals(ReviewRating.GOOD, TypingAutoRatingPolicy.decide(metrics()).rating)
        assertEquals(
            TypingAutoRatingReason.SLOW_TOTAL,
            TypingAutoRatingPolicy.decide(metrics(total = 7_000L)).reason
        )
        assertEquals(
            TypingAutoRatingReason.SLOW_RECALL,
            TypingAutoRatingPolicy.decide(metrics(recall = 3_000L)).reason
        )
    }

    @Test
    fun `multiple mismatch or correction events make exact attempt Hard`() {
        assertEquals(
            ReviewRating.HARD,
            TypingAutoRatingPolicy.decide(metrics(mismatches = 3, hadMismatch = true)).rating
        )
        assertEquals(
            ReviewRating.HARD,
            TypingAutoRatingPolicy.decide(metrics(corrections = 3)).rating
        )
    }

    @Test
    fun `fast clean eligible Review is Easy`() {
        val decision =
            TypingAutoRatingPolicy.decide(
                metrics(total = 1_800L, recall = 900L, previousRating = ReviewRating.GOOD)
            )

        assertEquals(ReviewRating.EASY, decision.rating)
    }

    @Test
    fun `Easy is blocked by mismatch New Relearning previous Again or missing context`() {
        val cases =
            listOf(
                metrics(total = 1_800L, recall = 900L, mismatches = 1, hadMismatch = true),
                metrics(
                    total = 1_800L,
                    recall = 900L,
                    origin = SessionItemOrigin.NEW,
                    stage = LearningStage.NEW
                ),
                metrics(total = 1_800L, recall = 900L, stage = LearningStage.RELEARNING),
                metrics(total = 1_800L, recall = 900L, previousRating = ReviewRating.AGAIN),
                metrics(total = 1_800L, recall = 900L, stage = null, previousRating = null)
            )

        cases.forEach { assertEquals(ReviewRating.GOOD, TypingAutoRatingPolicy.decide(it).rating) }
    }

    @Test
    fun `expected time scales and clamps deterministically`() {
        assertEquals(3_500L, TypingAutoRatingPolicy.expectedMillis(0))
        assertTrue(
            TypingAutoRatingPolicy.expectedMillis(20) >
                TypingAutoRatingPolicy.expectedMillis(5)
        )
        assertEquals(18_000L, TypingAutoRatingPolicy.expectedMillis(10_000))
    }

    @Test
    fun `Hard total threshold is inclusive and just-below boundary remains Good`() {
        val expected = TypingAutoRatingPolicy.expectedMillis(5)

        assertEquals(
            ReviewRating.HARD,
            TypingAutoRatingPolicy.decide(metrics(total = expected * 160 / 100)).rating
        )
        assertEquals(
            ReviewRating.GOOD,
            TypingAutoRatingPolicy.decide(
                metrics(total = expected * 160 / 100 - 1, recall = 1_000L)
            ).rating
        )
    }

    private fun metrics(
        revealUsed: Boolean = false,
        completedExactly: Boolean = true,
        total: Long = 3_000L,
        recall: Long = 1_000L,
        mismatches: Int = 0,
        corrections: Int = 0,
        hadMismatch: Boolean = false,
        origin: SessionItemOrigin = SessionItemOrigin.REVIEW,
        stage: LearningStage? = LearningStage.REVIEW,
        previousRating: ReviewRating? = ReviewRating.GOOD
    ): TypingAttemptMetrics {
        val context =
            ExperienceRotationContext(SessionId("session"), LearningItemId("item"), 0)
        return TypingAttemptMetrics(
            context = context,
            attemptGeneration = 1,
            startedAtMillis = 0L,
            firstInputAtMillis = recall,
            completedAtMillis = total,
            totalElapsedMillis = total,
            recallLatencyMillis = recall,
            typingDurationMillis = (total - recall).coerceAtLeast(0L),
            canonicalCodePointCount = 5,
            materialInputChangeCount = 5,
            mismatchEventCount = mismatches,
            correctionEventCount = corrections,
            hadMismatch = hadMismatch,
            revealUsed = revealUsed,
            completedExactly = completedExactly,
            finalInputCodePointCount = 5,
            itemOrigin = origin,
            learningStage = stage,
            previousRating = previousRating
        )
    }
}
