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
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId

class EvidenceChainTest {
    private val contentId = ContentId("content")

    @Test
    fun `chain starts from a typed anchor and advances chronologically`() {
        val anchor = evidence("again", 0, ReviewRating.AGAIN, RecallResult.INCORRECT)
        val chain = start(anchor).advance(evidence("correct", 1, ReviewRating.AGAIN))

        assertEquals(PromotionStage.AGAIN_TO_HARD, chain.stage)
        assertEquals(anchor, chain.anchor.evidence)
        assertEquals(listOf("again", "correct"), chain.recallEvidence.map { it.reviewEventId.value })
        assertEquals(null, chain.closedBy)
    }

    @Test
    fun `promotion closes old chain and starts empty Hard chain at new anchor`() {
        val old = start(evidence("again", 0, ReviewRating.AGAIN))
            .advance(evidence("old-correct", 1, ReviewRating.AGAIN))
        val hard = anchor("hard", 2, ReviewRating.HARD, ChainAnchorReason.PROMOTED)

        val reset = old.reset(hard, ChainResetReason.PROMOTED)

        assertEquals(ChainResetReason.PROMOTED, reset.completedChain.closedBy)
        assertEquals(listOf("again", "old-correct"), reset.completedChain.ids())
        assertEquals(PromotionStage.HARD_TO_GOOD, reset.newChain.stage)
        assertEquals(listOf("hard"), reset.newChain.ids())
    }

    @Test
    fun `new Again kills prior chain and trajectory keeps both chains`() {
        val initial = start(evidence("hard", 0, ReviewRating.HARD))
            .advance(evidence("correct", 1, ReviewRating.HARD))
        val trajectory = LearningTrajectory.start(initial).reset(
            anchor("again", 2, ReviewRating.AGAIN, ChainAnchorReason.NEW_AGAIN),
            ChainResetReason.NEW_AGAIN
        )

        assertEquals(2, trajectory.chains.size)
        assertEquals(ChainResetReason.NEW_AGAIN, trajectory.chains.first().closedBy)
        assertEquals(listOf("again"), trajectory.currentChain().ids())
        assertEquals(PromotionStage.AGAIN_TO_HARD, trajectory.currentChain().stage)
    }

    @Test
    fun `new lapse closes Good chain and anchors a fresh Again chain`() {
        val trajectory = LearningTrajectory.start(start(evidence("good", 0, ReviewRating.GOOD)))
            .reset(
                anchor("lapse", 1, ReviewRating.AGAIN, ChainAnchorReason.NEW_LAPSE),
                ChainResetReason.NEW_LAPSE
            )

        assertEquals(ChainResetReason.NEW_LAPSE, trajectory.chains.first().closedBy)
        assertEquals(PromotionStage.AGAIN_TO_HARD, trajectory.currentChain().stage)
        assertEquals(listOf("lapse"), trajectory.currentChain().ids())
    }

    @Test
    fun `Hard and Good anchors each start a new stage without prior evidence`() {
        val again = LearningTrajectory.start(start(evidence("again", 0, ReviewRating.AGAIN)))
        val hard = again.reset(
            anchor("hard", 1, ReviewRating.HARD, ChainAnchorReason.PROMOTED),
            ChainResetReason.PROMOTED
        )
        val good = hard.reset(
            anchor("good", 2, ReviewRating.GOOD, ChainAnchorReason.PROMOTED),
            ChainResetReason.PROMOTED
        )

        assertEquals(PromotionStage.HARD_TO_GOOD, hard.currentChain().stage)
        assertEquals(listOf("hard"), hard.currentChain().ids())
        assertEquals(PromotionStage.GOOD_TO_EASY, good.currentChain().stage)
        assertEquals(listOf("good"), good.currentChain().ids())
        assertEquals(3, good.chains.size)
    }

    @Test
    fun `manual rating is a non-evidence event and does not reset trajectory`() {
        val initial = LearningTrajectory.start(start(evidence("anchor", 0, ReviewRating.HARD)))
        val updated = initial.recordNonEvidence(
            NonEvidenceEvent(
                contentId = contentId,
                timestamp = moment(1),
                sessionId = SessionId("manual-session"),
                rating = ReviewRating.GOOD,
                provenance = RatingSource.MANUAL_USER
            )
        )

        assertEquals(1, updated.chains.size)
        assertEquals(listOf("anchor"), updated.currentChain().ids())
        assertEquals(1, updated.currentChain().nonEvidenceEvents.size)
        assertEquals(NonEvidenceEventKind.MANUAL_RATING, updated.currentChain().nonEvidenceEvents.single().kind)
    }

    @Test
    fun `trajectory authority is Content and rejects evidence from another Content`() {
        val trajectory = LearningTrajectory.start(start(evidence("anchor", 0, ReviewRating.AGAIN)))
        val siblingRecall = evidence("sibling", 1, ReviewRating.AGAIN)
        val otherContent = siblingRecall.copy(contentId = ContentId("other-content"))

        assertEquals(contentId, trajectory.contentId)
        assertTrue(trajectory.advance(siblingRecall).currentChain().ids().contains("sibling"))
        assertFailsWith<IllegalArgumentException> { trajectory.advance(otherContent) }
    }

    @Test
    fun `duplicate and out of order recall evidence are rejected`() {
        val recall = evidence("recall", 2, ReviewRating.AGAIN)
        val chain = start(evidence("anchor", 0, ReviewRating.AGAIN)).advance(recall)

        assertFailsWith<IllegalArgumentException> { chain.advance(recall.copy(timestamp = moment(3))) }
        assertFailsWith<IllegalArgumentException> {
            chain.advance(evidence("older", 1, ReviewRating.AGAIN))
        }
    }

    @Test
    fun `Undo reset is typed and leaves a fresh current chain`() {
        val trajectory = LearningTrajectory.start(
            start(evidence("good", 0, ReviewRating.GOOD)).advance(
                evidence("correct", 1, ReviewRating.GOOD)
            )
        ).reset(
            anchor("undo-good", 2, ReviewRating.GOOD, ChainAnchorReason.UNDO),
            ChainResetReason.UNDO
        )

        assertEquals(ChainResetReason.UNDO, trajectory.chains.first().closedBy)
        assertEquals(listOf("undo-good"), trajectory.currentChain().ids())
    }

    @Test
    fun `promotion decision reads only the current chain and cannot reuse old recalls`() {
        val clock = EvidenceClock { moment(100) }
        val authority = EvidencePromotionAuthority(clock)
        val old = start(evidence("again", 0, ReviewRating.AGAIN))
            .advance(evidence("one", 1, ReviewRating.AGAIN))
            .advance(evidence("two", 2, ReviewRating.AGAIN))
            .advance(evidence("three", 3, ReviewRating.AGAIN))
        val trajectory = LearningTrajectory.start(old).reset(
            anchor("hard", 4, ReviewRating.HARD, ChainAnchorReason.PROMOTED),
            ChainResetReason.PROMOTED
        )

        val decision = authority.evaluatePromotion(PromotionCandidate(trajectory.currentChain()))

        assertFalse(decision.eligible)
        assertEquals(2, decision.missingCorrectRecalls)
    }

    @Test
    fun `reset and anchor reasons must agree`() {
        val chain = start(evidence("anchor", 0, ReviewRating.AGAIN))

        assertFailsWith<IllegalArgumentException> {
            chain.reset(
                anchor("hard", 1, ReviewRating.HARD, ChainAnchorReason.DATA_RECOVERY),
                ChainResetReason.PROMOTED
            )
        }
        assertFailsWith<IllegalArgumentException> {
            chain.reset(
                anchor("good", 1, ReviewRating.GOOD, ChainAnchorReason.PROMOTED),
                ChainResetReason.PROMOTED
            )
        }
    }

    private fun start(evidence: RecallEvidence): EvidenceChain = EvidenceChain.start(
        contentId = evidence.contentId,
        stage = PromotionStage.fromAnchorRating(evidence.currentRating),
        anchor = ChainAnchor(
            rating = evidence.currentRating,
            timestamp = evidence.timestamp,
            evidence = evidence,
            reason = ChainAnchorReason.INITIAL_RATING
        )
    )

    private fun anchor(
        id: String,
        at: Long,
        rating: ReviewRating,
        reason: ChainAnchorReason
    ): ChainAnchor {
        val evidence = evidence(id, at, rating)
        return ChainAnchor(rating, evidence.timestamp, evidence, reason)
    }

    private fun evidence(
        id: String,
        at: Long,
        rating: ReviewRating,
        result: RecallResult = RecallResult.CORRECT
    ) = RecallEvidence(
        reviewEventId = ReviewEventId(id),
        contentId = contentId,
        timestamp = moment(at),
        currentRating = rating,
        result = result,
        sessionId = SessionId("session-$id"),
        sessionPolicy = SessionEvaluationPolicy.EVALUATIVE,
        provenance = RatingSource.STANDARD_REVIEW,
        wasRevealUsed = false,
        schedulerDue = moment(at)
    )

    private fun EvidenceChain.ids(): List<String> = recallEvidence.map { it.reviewEventId.value }
    private fun moment(value: Long) = Moment(value * 3_600_000L)
}
