package vn.loi.learning.domain.study.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId

class EvidencePromotionAuthorityTest {
    private val clock = FakeEvidenceClock(at = day(30))
    private val authority = EvidencePromotionAuthority(clock)

    @Test
    fun `Again to Hard is blocked at 23 hours and eligible at 24 hours`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.AGAIN, RecallResult.INCORRECT)
        val recall = evidence("recall", hour(23), ReviewRating.AGAIN, RecallResult.CORRECT)
        clock.at = hour(23)

        val early = evaluate(ReviewRating.AGAIN, anchor, recall)

        assertFalse(early.eligible)
        assertEquals(TimeSpan.hours(1), early.remainingTime)
        assertTrue(PromotionReason.EVIDENCE_WINDOW_NOT_ELAPSED in early.reasons)

        clock.at = hour(24)
        val boundary = evaluate(ReviewRating.AGAIN, anchor, recall)
        assertTrue(boundary.eligible)
        assertEquals(ReviewRating.HARD, boundary.targetRating)
        assertEquals(listOf(PromotionReason.ELIGIBLE), boundary.reasons)
    }

    @Test
    fun `practice reveal and manual override recalls do not count`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.AGAIN, RecallResult.INCORRECT)
        clock.at = hour(48)
        val decision = evaluate(
            ReviewRating.AGAIN,
            anchor,
            evidence("practice", hour(25), ReviewRating.AGAIN, sessionPolicy = SessionEvaluationPolicy.PRACTICE_ONLY),
            evidence("reveal", hour(26), ReviewRating.AGAIN, wasRevealUsed = true),
            evidence("override", hour(27), ReviewRating.AGAIN, provenance = RatingSource.MANUAL_USER_OVERRIDE)
        )

        assertFalse(decision.eligible)
        assertEquals(1, decision.missingCorrectRecalls)
        assertTrue(PromotionReason.PRACTICE_EVIDENCE_EXCLUDED in decision.reasons)
        assertTrue(PromotionReason.REVEAL_EVIDENCE_EXCLUDED in decision.reasons)
        assertTrue(PromotionReason.MANUAL_OVERRIDE_EVIDENCE_EXCLUDED in decision.reasons)
    }

    @Test
    fun `manual rating replay and undone evidence do not count`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.AGAIN, RecallResult.INCORRECT)
        val duplicate = evidence("duplicate", hour(25), ReviewRating.AGAIN)
        clock.at = hour(48)
        val decision = evaluate(
            ReviewRating.AGAIN,
            anchor,
            evidence("manual", hour(25), ReviewRating.AGAIN, provenance = RatingSource.MANUAL_USER),
            evidence("replay", hour(26), ReviewRating.AGAIN, origin = RecallEvidenceOrigin.REPLAY),
            evidence("undone", hour(27), ReviewRating.AGAIN, commitStatus = RecallEvidenceCommitStatus.UNDONE),
            duplicate
        )

        assertTrue(decision.eligible)
        assertEquals(listOf(PromotionReason.ELIGIBLE), decision.reasons)

        val excludedOnly = evaluate(
            ReviewRating.AGAIN,
            anchor,
            evidence("manual", hour(25), ReviewRating.AGAIN, provenance = RatingSource.MANUAL_USER),
            evidence("replay", hour(26), ReviewRating.AGAIN, origin = RecallEvidenceOrigin.REPLAY),
            evidence("undone", hour(27), ReviewRating.AGAIN, commitStatus = RecallEvidenceCommitStatus.UNDONE)
        )
        assertFalse(excludedOnly.eligible)
        assertTrue(PromotionReason.MANUAL_RATING_EVIDENCE_EXCLUDED in excludedOnly.reasons)
        assertTrue(PromotionReason.REPLAY_EVIDENCE_EXCLUDED in excludedOnly.reasons)
        assertTrue(PromotionReason.UNDONE_EVIDENCE_EXCLUDED in excludedOnly.reasons)
    }

    @Test
    fun `duplicate evidence identity is rejected by the chain`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.HARD)
        val recall = evidence("same", hour(73), ReviewRating.HARD)
        val chain = chain(anchor).advance(recall)

        assertFailsWith<IllegalArgumentException> {
            chain.advance(recall.copy(timestamp = hour(74)))
        }
    }

    @Test
    fun `Hard to Good needs two correct recalls at independent times after 72 hours`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.HARD)
        clock.at = hour(72)

        val missing = evaluate(
            ReviewRating.HARD,
            anchor,
            evidence("one", hour(24), ReviewRating.HARD)
        )
        assertFalse(missing.eligible)
        assertEquals(1, missing.missingCorrectRecalls)

        val eligible = evaluate(
            ReviewRating.HARD,
            anchor,
            evidence("one", hour(24), ReviewRating.HARD),
            evidence("two", hour(48), ReviewRating.HARD)
        )
        assertTrue(eligible.eligible)
        assertEquals(ReviewRating.GOOD, eligible.targetRating)
    }

    @Test
    fun `recalls at the same timestamp count as one independent observation`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.HARD)
        clock.at = hour(72)

        val decision = evaluate(
            ReviewRating.HARD,
            anchor,
            evidence("one", hour(24), ReviewRating.HARD),
            evidence("two", hour(24), ReviewRating.HARD)
        )

        assertFalse(decision.eligible)
        assertEquals(1, decision.missingCorrectRecalls)
    }

    @Test
    fun `typed policy can change a window without changing the resolver`() {
        val custom = EvidencePromotionAuthority(
            clock,
            PromotionPolicy(
                againToHard = EvidenceWindow(TimeSpan.hours(48), 2)
            )
        )
        val anchor = evidence("anchor", hour(0), ReviewRating.AGAIN, RecallResult.INCORRECT)
        clock.at = hour(48)

        val decision = custom.evaluatePromotion(
            PromotionCandidate(
                chain = chain(anchor)
                    .advance(evidence("one", hour(24), ReviewRating.AGAIN))
                    .advance(evidence("two", hour(36), ReviewRating.AGAIN))
            )
        )

        assertTrue(decision.eligible)
    }

    @Test
    fun `Hard to Good rejects an intervening Again`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.HARD)
        clock.at = hour(80)

        val decision = evaluate(
            ReviewRating.HARD,
            anchor,
            evidence("one", hour(24), ReviewRating.HARD),
            evidence("lapse", hour(36), ReviewRating.AGAIN, RecallResult.INCORRECT),
            evidence("two", hour(48), ReviewRating.HARD)
        )

        assertFalse(decision.eligible)
        assertTrue(PromotionReason.INTERVENING_AGAIN in decision.reasons)
    }

    @Test
    fun `Good to Easy is blocked at 13 days and eligible at 14 days with three recalls`() {
        val anchor = evidence("anchor", day(0), ReviewRating.GOOD)
        val recalls = arrayOf(
            evidence("one", day(3), ReviewRating.GOOD),
            evidence("two", day(7), ReviewRating.GOOD),
            evidence("three", day(10), ReviewRating.GOOD)
        )
        clock.at = day(13)

        val early = evaluate(ReviewRating.GOOD, anchor, *recalls)
        assertFalse(early.eligible)
        assertEquals(TimeSpan.days(1), early.remainingTime)

        clock.at = day(14)
        val boundary = evaluate(ReviewRating.GOOD, anchor, *recalls)
        assertTrue(boundary.eligible)
        assertEquals(ReviewRating.EASY, boundary.targetRating)
    }

    @Test
    fun `Good to Easy rejects lapse after anchor`() {
        val anchor = evidence("anchor", day(0), ReviewRating.GOOD)
        clock.at = day(14)
        val decision = evaluate(
            ReviewRating.GOOD,
            anchor,
            evidence("one", day(2), ReviewRating.GOOD),
            evidence("lapse", day(4), ReviewRating.AGAIN, RecallResult.INCORRECT),
            evidence("two", day(6), ReviewRating.GOOD),
            evidence("three", day(8), ReviewRating.GOOD)
        )

        assertFalse(decision.eligible)
        assertTrue(PromotionReason.LAPSE_AFTER_ANCHOR in decision.reasons)
    }

    @Test
    fun `fake clock controls decision and Easy has no further promotion`() {
        val anchor = evidence("anchor", hour(0), ReviewRating.AGAIN, RecallResult.INCORRECT)
        val recall = evidence("recall", hour(25), ReviewRating.AGAIN)

        clock.at = hour(10)
        assertFalse(evaluate(ReviewRating.AGAIN, anchor, recall).eligible)
        clock.at = hour(25)
        assertTrue(evaluate(ReviewRating.AGAIN, anchor, recall).eligible)

        val easy = evaluate(ReviewRating.EASY, evidence("easy", hour(0), ReviewRating.EASY))
        assertFalse(easy.eligible)
        assertEquals(null, easy.targetRating)
        assertEquals(listOf(PromotionReason.ALREADY_AT_HIGHEST_LEVEL), easy.reasons)
    }

    @Test
    fun `excluded evidence cannot become a chain anchor`() {
        val excludedAnchor = evidence(
            "anchor",
            hour(0),
            ReviewRating.AGAIN,
            sessionPolicy = SessionEvaluationPolicy.PRACTICE_ONLY
        )

        assertFailsWith<IllegalArgumentException> { chain(excludedAnchor) }
    }

    private fun evaluate(
        currentRating: ReviewRating,
        anchor: RecallEvidence,
        vararg evidence: RecallEvidence
    ): PromotionDecision = authority.evaluatePromotion(
        PromotionCandidate(
            chain = evidence.sortedBy { it.timestamp }.fold(chain(anchor)) { current, item ->
                if (item.provenance == RatingSource.STANDARD_REVIEW) {
                    current.advance(item)
                } else {
                    current.recordNonEvidence(
                        NonEvidenceEvent(
                            contentId = item.contentId,
                            timestamp = item.timestamp,
                            sessionId = item.sessionId,
                            rating = item.currentRating,
                            provenance = item.provenance
                        )
                    )
                }
            }
        ).also { require(it.currentRating == currentRating) }
    )

    private fun chain(anchor: RecallEvidence): EvidenceChain = EvidenceChain.start(
        contentId = anchor.contentId,
        stage = PromotionStage.fromAnchorRating(anchor.currentRating),
        anchor = ChainAnchor(
            rating = anchor.currentRating,
            timestamp = anchor.timestamp,
            evidence = anchor,
            reason = ChainAnchorReason.INITIAL_RATING
        )
    )

    private fun evidence(
        id: String,
        at: Moment,
        rating: ReviewRating,
        result: RecallResult = RecallResult.CORRECT,
        sessionPolicy: SessionEvaluationPolicy = SessionEvaluationPolicy.EVALUATIVE,
        provenance: RatingSource = RatingSource.STANDARD_REVIEW,
        wasRevealUsed: Boolean = false,
        origin: RecallEvidenceOrigin = RecallEvidenceOrigin.EVALUATIVE_RECALL,
        commitStatus: RecallEvidenceCommitStatus = RecallEvidenceCommitStatus.COMMITTED
    ) = RecallEvidence(
        reviewEventId = ReviewEventId(id),
        contentId = ContentId("content"),
        timestamp = at,
        currentRating = rating,
        result = result,
        sessionId = SessionId("session-$id"),
        sessionPolicy = sessionPolicy,
        provenance = provenance,
        wasRevealUsed = wasRevealUsed,
        schedulerDue = at,
        typingLatency = TimeSpan.seconds(2),
        origin = origin,
        commitStatus = commitStatus
    )

    private fun hour(value: Long) = Moment(TimeSpan.hours(value).millis)
    private fun day(value: Long) = Moment(TimeSpan.days(value).millis)

    private class FakeEvidenceClock(var at: Moment) : EvidenceClock {
        override fun now(): Moment = at
    }
}
