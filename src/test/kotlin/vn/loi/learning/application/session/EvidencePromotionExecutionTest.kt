package vn.loi.learning.application.session

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningTrajectoryRepository

class EvidencePromotionExecutionTest {
    private val repository = InMemoryLearningTrajectoryRepository()
    private val execution = EvidencePromotionExecution(repository)
    private val learner = LearnerId("learner")
    private val content = ContentId("content")

    @Test fun `automatic Again promotes only at 24 hours and opens fresh Hard chain`() {
        run("anchor", 0, ReviewRating.AGAIN, RecallResult.INCORRECT)
        assertEquals(ReviewRating.AGAIN, run("early", 23, ReviewRating.GOOD).committedRating)
        val promoted = run("boundary", 24, ReviewRating.GOOD)
        assertEquals(ReviewRating.HARD, promoted.committedRating)
        assertTrue(promoted.promotionDecision!!.eligible)
        val trajectory = repository.find(learner, content)!!
        assertEquals(ChainResetReason.PROMOTED, trajectory.chains.first().closedBy)
        assertEquals(listOf("boundary"), trajectory.currentChain().recallEvidence.map { it.reviewEventId.value })
    }

    @Test fun `Hard requires two later correct recalls before Good`() {
        run("anchor", 0, ReviewRating.HARD)
        assertEquals(ReviewRating.HARD, run("one", 72, ReviewRating.EASY).committedRating)
        assertEquals(ReviewRating.GOOD, run("two", 73, ReviewRating.EASY).committedRating)
    }

    @Test fun `Good requires three later correct recalls before Easy`() {
        run("anchor", 0, ReviewRating.GOOD)
        run("one", 24 * 14, ReviewRating.EASY)
        run("two", 24 * 14 + 1, ReviewRating.EASY)
        assertEquals(ReviewRating.EASY, run("three", 24 * 14 + 2, ReviewRating.EASY).committedRating)
    }

    @Test fun `manual and reveal bypass evidence and are never capped`() {
        run("anchor", 0, ReviewRating.AGAIN, RecallResult.INCORRECT)
        val manual = run("manual", 1, ReviewRating.EASY, source = RatingSource.MANUAL_USER)
        val reveal = run("reveal", 2, ReviewRating.EASY, reveal = true)
        assertEquals(ReviewRating.EASY, manual.committedRating)
        assertEquals(ReviewRating.EASY, reveal.committedRating)
        assertEquals(1, repository.find(learner, content)!!.currentChain().recallEvidence.size)
    }

    @Test fun `sibling calls share Content trajectory and duplicate event is not appended`() {
        run("anchor", 0, ReviewRating.AGAIN, RecallResult.INCORRECT)
        run("same", 24, ReviewRating.GOOD)
        run("same", 24, ReviewRating.GOOD)
        val trajectory = repository.find(learner, content)!!
        assertEquals(1, trajectory.chains.flatMap { it.recallEvidence }.count { it.reviewEventId.value == "same" })
    }

    @Test fun `Again resets an established chain`() {
        run("hard", 0, ReviewRating.HARD)
        run("again", 1, ReviewRating.AGAIN, RecallResult.INCORRECT)
        val trajectory = repository.find(learner, content)!!
        assertEquals(ChainResetReason.NEW_LAPSE, trajectory.chains.first().closedBy)
        assertEquals(PromotionStage.AGAIN_TO_HARD, trajectory.currentChain().stage)
    }

    private fun run(
        id: String,
        hour: Int,
        rating: ReviewRating,
        result: RecallResult = RecallResult.CORRECT,
        source: RatingSource = RatingSource.STANDARD_REVIEW,
        reveal: Boolean = false
    ) = execution.execute(
        learner, content, SessionId("session-$id"), ReviewEventId(id), rating,
        Moment(hour * 3_600_000L), Moment(0), source, SessionEvaluationPolicy.EVALUATIVE,
        AutomaticRecallEvidenceInput(result, reveal, TimeSpan(10))
    )
}
