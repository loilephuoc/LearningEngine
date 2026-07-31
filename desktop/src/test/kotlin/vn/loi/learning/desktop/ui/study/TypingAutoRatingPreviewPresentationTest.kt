package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.confidence.model.MemoryConfidence
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceReason
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceScore
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceTier

class TypingAutoRatingPreviewPresentationTest {
    @Test
    fun `before first input preview stays Ready at zero without a fake rating`() {
        val preview = preview(attempt(firstInputAtMillis = null), 20_000L)

        assertEquals(TypingRatingPreviewState.READY, preview.state)
        assertEquals(0L, preview.elapsedMillis)
        assertEquals(null, preview.decision)
        assertEquals(TypingRatingColorRole.READY, preview.colorRole)
    }

    @Test
    fun `preview shares spaced memory guard with final policy`() {
        val eligible = preview(attempt(previousRating = ReviewRating.GOOD), 1_500L)
        val sameSession =
            preview(
                attempt(previousRating = ReviewRating.GOOD)
                    .copy(reviewedEarlierInCurrentSession = true),
                1_500L
            )
        val recent =
            preview(
                attempt(previousRating = ReviewRating.GOOD)
                    .copy(itemPresentedAtEpochMillis = 60_000L),
                1_500L
            )

        assertEquals(ReviewRating.EASY, eligible.rating)
        listOf(sameSession, recent).forEach {
            assertEquals(ReviewRating.GOOD, it.rating)
            assertFalse(it.thresholds.easyAvailable)
        }
    }

    @Test
    fun `forced or revealed attempt previews Again without elapsed threshold`() {
        val active =
            TypingAutoRatingPreviewResolver.resolve(
                attempt(),
                1_000L,
                TypingRatingMode.FORCED_AGAIN
            )
        val revealed =
            TypingAutoRatingPreviewResolver.resolve(
                attempt().copy(
                    phase = TypingAttemptPhase.REVEALED,
                    stoppedAtMillis = 2_000L
                ),
                1_000L,
                TypingRatingMode.STANDARD
            )

        listOf(active, revealed).forEach {
            assertEquals(ReviewRating.AGAIN, assertNotNull(it).rating)
            assertEquals(TypingRatingColorRole.AGAIN, it.colorRole)
        }
    }

    @Test
    fun `eligible fast clean attempt previews Easy and blue role`() {
        val preview = preview(attempt(firstInputAtMillis = 500L), 1_500L)

        assertEquals(ReviewRating.EASY, preview.rating)
        assertEquals(TypingRatingColorRole.EASY, preview.colorRole)
        assertTrue(preview.thresholds.easyAvailable)
    }

    @Test
    fun `Easy speed remains blue when confidence gates final rating to Good`() {
        val medium =
            MemoryConfidence(
                score = MemoryConfidenceScore.of(40),
                tier = MemoryConfidenceTier.MEDIUM,
                evaluatedReviewCount = 3,
                reliable = true,
                primaryReason = MemoryConfidenceReason.FIRST_REVIEW_SUCCESS
            )
        val preview =
            preview(
                attempt(firstInputAtMillis = 500L).copy(
                    easyConfidenceProjection =
                        highConfidenceProjection().copy(
                            projectedConfidence = medium,
                            delta = -20
                        )
                ),
                2_500L
            )

        assertEquals(TypingSpeedBand.EASY, preview.speedBand)
        assertEquals(TypingRatingColorRole.EASY, preview.colorRole)
        assertEquals(ReviewRating.GOOD, preview.rating)
        assertEquals(TypingAutoRatingReason.CONFIDENCE_BELOW_HIGH, preview.decision?.reason)
    }

    @Test
    fun `mismatch Relearning and previous Again conservatively block Easy`() {
        val cases =
            listOf(
                attempt(hadMismatch = true, mismatchEventCount = 1),
                attempt(learningStage = LearningStage.RELEARNING),
                attempt(previousRating = ReviewRating.AGAIN)
            )

        cases.forEach {
            val preview = preview(it.copy(firstInputAtMillis = 500L), 1_500L)
            assertNotEquals(ReviewRating.EASY, preview.rating)
            assertFalse(preview.thresholds.easyAvailable)
        }
    }

    @Test
    fun `normal timing is Good and slow timing is Hard`() {
        val normal = preview(attempt(firstInputAtMillis = 4_000L), 7_000L)
        val hardAt = normal.thresholds.hardMinimumElapsedMillis
        val slow = preview(attempt(firstInputAtMillis = 1_000L), 1_000L + hardAt)

        assertEquals(ReviewRating.GOOD, normal.rating)
        assertEquals(TypingRatingColorRole.GOOD, normal.colorRole)
        assertEquals(ReviewRating.HARD, slow.rating)
        assertEquals(TypingRatingColorRole.HARD, slow.colorRole)
    }

    @Test
    fun `high recall remains Hard while raw counters are non-authoritative`() {
        val expected = TypingAutoRatingPolicy.expectedMillis(5)
        val highRecall =
            preview(
                attempt(firstInputAtMillis = maxOf(8_000L, expected)),
                maxOf(8_000L, expected) + 1_000L
            )
        val mismatches = preview(attempt(mismatchEventCount = 3), 1_000L)
        val corrections = preview(attempt(correctionEventCount = 3), 1_000L)

        assertEquals(ReviewRating.HARD, highRecall.rating)
        listOf(mismatches, corrections).forEach { assertNotEquals(ReviewRating.HARD, it.rating) }
    }

    @Test
    fun `preview is pure deterministic and does not mutate attempt evidence`() {
        val state =
            attempt(
                firstInputAtMillis = 700L,
                mismatchEventCount = 1,
                correctionEventCount = 1,
                hadMismatch = true
            )
        val before = state.copy()

        val first = preview(state, 2_000L)
        val second = preview(state, 2_000L)

        assertEquals(first, second)
        assertEquals(before, state)
    }

    @Test
    fun `stopped attempt keeps authoritative elapsed and final preview`() {
        val stopped =
            attempt(firstInputAtMillis = 500L).copy(
                phase = TypingAttemptPhase.COMPLETED_EXACTLY,
                stoppedAtMillis = 3_000L
            )

        val preview = preview(stopped, 999_000L)

        assertEquals(2_500L, preview.elapsedMillis)
        assertEquals(ReviewRating.EASY, preview.rating)
    }

    @Test
    fun `item length and policy clamps produce authoritative dynamic bands`() {
        val short = preview(attempt(canonicalCodePointCount = 1), 2_000L)
        val long = preview(attempt(canonicalCodePointCount = 40), 2_000L)
        val maximum = preview(attempt(canonicalCodePointCount = 10_000), 2_000L)

        assertNotEquals(
            short.thresholds.hardMinimumElapsedMillis,
            long.thresholds.hardMinimumElapsedMillis
        )
        assertEquals(
            TypingAutoRatingPolicy.MINIMUM_EXPECTED_TYPING_MILLIS,
            short.thresholds.expectedMillis
        )
        assertEquals(
            TypingAutoRatingPolicy.MAXIMUM_EXPECTED_TYPING_MILLIS,
            maximum.thresholds.expectedMillis
        )
        assertEquals(
            TypingAutoRatingPolicy.easyActiveTypingMaximumMillis(short.thresholds.expectedMillis),
            short.thresholds.easyMaximumElapsedMillis
        )
    }

    @Test
    fun `timer visual targets are prominent responsive and never label sized`() {
        val wide = TypingTimerPresentationResolver.resolve(StudyViewportClass.WIDE)
        val medium = TypingTimerPresentationResolver.resolve(StudyViewportClass.STANDARD)
        val narrow = TypingTimerPresentationResolver.resolve(StudyViewportClass.COMPACT)

        assertEquals(48, wide.valueFontSizeSp)
        assertEquals(40, medium.valueFontSizeSp)
        assertEquals(30, narrow.valueFontSizeSp)
        assertTrue(wide.iconSizeDp > medium.iconSizeDp)
        assertTrue(medium.iconSizeDp > narrow.iconSizeDp)
        assertEquals(TypingLegendLayout.SINGLE_ROW, wide.legendLayout)
        assertEquals(TypingLegendLayout.TWO_BY_TWO, medium.legendLayout)
        assertEquals(TypingLegendLayout.TWO_BY_TWO, narrow.legendLayout)
    }

    @Test
    fun `threshold formatter is stable at tenths`() {
        assertEquals("0.0s", formatTypingThreshold(0L))
        assertEquals("2.3s", formatTypingThreshold(2_275L))
        assertEquals("18.0s", formatTypingThreshold(18_000L))
    }

    private fun preview(
        attempt: TypingAttemptState,
        elapsedMillis: Long
    ): TypingRatingPreview =
        assertNotNull(
            TypingAutoRatingPreviewResolver.resolve(
                attempt,
                elapsedMillis,
                TypingRatingMode.STANDARD
            )
        )

    private fun attempt(
        canonicalCodePointCount: Int = 5,
        firstInputAtMillis: Long? = 500L,
        mismatchEventCount: Int = 0,
        correctionEventCount: Int = 0,
        hadMismatch: Boolean = false,
        learningStage: LearningStage? = LearningStage.REVIEW,
        previousRating: ReviewRating? = ReviewRating.GOOD
    ): TypingAttemptState =
        TypingAttemptState(
            context =
                ExperienceRotationContext(
                    SessionId("session"),
                    LearningItemId("item"),
                    0
                ),
            attemptGeneration = 1L,
            startedAtMillis = 0L,
            canonicalCodePointCount = canonicalCodePointCount,
            itemOrigin = SessionItemOrigin.REVIEW,
            learningStage = learningStage,
            previousRating = previousRating,
            previousReviewAtMillis = 0L,
            memoryContextReliable = previousRating != null,
            itemPresentedAtEpochMillis =
                TypingAutoRatingPolicy.MINIMUM_EASY_SPACED_INTERVAL_MILLIS,
            easyConfidenceProjection = highConfidenceProjection(),
            firstInputAtMillis = firstInputAtMillis,
            mismatchEventCount = mismatchEventCount,
            correctionEventCount = correctionEventCount,
            hadMismatch = hadMismatch
        )

    private fun highConfidenceProjection(): MemoryConfidenceProjection {
        val confidence =
            MemoryConfidence(
                score = MemoryConfidenceScore.of(60),
                tier = MemoryConfidenceTier.HIGH,
                evaluatedReviewCount = 3,
                reliable = true,
                primaryReason = MemoryConfidenceReason.SPACED_SUCCESS
            )
        return MemoryConfidenceProjection(
            previousConfidence = confidence,
            projectedConfidence = confidence,
            delta = 0,
            appliedSpacingBand = null,
            pendingEvidenceApplied = true
        )
    }
}
