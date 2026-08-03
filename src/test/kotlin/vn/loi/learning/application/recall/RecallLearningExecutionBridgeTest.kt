package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.session.*
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.persistence.memory.*

class RecallLearningExecutionBridgeTest {
    @Test fun `strong evaluative recall reuses one authoritative transaction`() {
        val f = fixture()
        val committed = assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(f.request(result = result())))
        assertEquals(ReviewRating.EASY, committed.decision.proposedRating)
        assertEquals(1, f.events.findAll(f.learner, f.item.id).size)
        assertEquals(1, f.runner.count)
        assertNotNull(f.memory.find(f.learner, f.item.id))
        assertTrue(f.queues.require(f.sessionId).isCompleted)
        assertEquals(1, f.trajectories.find(f.learner, f.contentId)?.currentChain()?.recallEvidence?.size)
    }

    @Test fun `standard correct is promotion capped by existing evidence authority`() {
        val f = fixture()
        val committed = assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(
            f.request(result = result(eligibility = RecallEvidenceEligibility.STANDARD))))
        assertEquals(ReviewRating.GOOD, committed.decision.proposedRating)
        assertNotNull(committed.result.reviewResult.promotionDecision)
        assertEquals(ReviewRating.GOOD, committed.result.reviewResult.reviewEvent.rating)
    }

    @Test fun `weak recognition commits without automatic evidence`() {
        val f = fixture()
        val recall = result(mode = RecallMode.MULTIPLE_CHOICE, eligibility = RecallEvidenceEligibility.WEAK)
        val committed = assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(f.request(result = recall)))
        assertEquals(ReviewRating.HARD, committed.decision.proposedRating)
        assertNull(f.trajectories.find(f.learner, f.contentId))
    }

    @Test fun `incorrect recall follows authoritative Again path`() {
        val f = fixture()
        val recall = result(outcome = RecallOutcome.INCORRECT, correct = false,
            correctness = RecallCorrectness.INCORRECT, eligibility = RecallEvidenceEligibility.INELIGIBLE)
        val committed = assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(f.request(result = recall)))
        assertEquals(RecallRatingIntent.AUTOMATIC_LAPSE, committed.decision.intent)
        assertEquals(ReviewRating.AGAIN, committed.result.reviewResult.reviewEvent.rating)
        assertContains(committed.result.session.lapsedContentIds, f.contentId)
    }

    @Test fun `reveal commits lapse without evidence while skip timeout invalid and partial do not commit`() {
        val revealFixture = fixture()
        val reveal = result(outcome = RecallOutcome.REVEALED, correct = false, correctness = RecallCorrectness.NOT_APPLICABLE,
            eligibility = RecallEvidenceEligibility.INELIGIBLE, reveal = true,
            assistance = setOf(RecallAssistance.ANSWER_REVEALED))
        val committed = assertIs<RecallLearningExecutionResult.Committed>(revealFixture.bridge.execute(revealFixture.request(result = reveal)))
        assertEquals(ReviewRating.AGAIN, committed.result.reviewResult.reviewEvent.rating)
        assertNull(revealFixture.trajectories.find(revealFixture.learner, revealFixture.contentId))

        listOf(RecallOutcome.SKIPPED, RecallOutcome.TIMED_OUT, RecallOutcome.INVALID_SUBMISSION).forEach { outcome ->
            val f = fixture()
            val r = result(outcome = outcome, correct = false, correctness = RecallCorrectness.NOT_APPLICABLE,
                eligibility = RecallEvidenceEligibility.INELIGIBLE)
            assertIs<RecallLearningExecutionResult.Ineligible>(f.bridge.execute(f.request(result = r)))
            assertTrue(f.events.findAll(f.learner).isEmpty()); assertEquals(0, f.runner.count)
        }
        val f = fixture()
        val partial = result(outcome = RecallOutcome.PARTIAL, correct = false, correctness = RecallCorrectness.PARTIAL,
            eligibility = RecallEvidenceEligibility.INELIGIBLE)
        assertIs<RecallLearningExecutionResult.ManualActionRequired>(f.bridge.execute(f.request(result = partial)))
    }

    @Test fun `practice result advances only practice local path`() {
        val f = fixture(practice = true)
        val response = f.bridge.execute(f.request(result = result(provenance = RecallProvenance.PRACTICE,
            eligibility = RecallEvidenceEligibility.INELIGIBLE), context = RecallStrategyContext.PRACTICE_ONLY))
        assertIs<RecallLearningExecutionResult.PracticeRecorded>(response)
        assertTrue(f.events.findAll(f.learner).isEmpty())
        assertNull(f.memory.find(f.learner, f.item.id)); assertNull(f.trajectories.find(f.learner, f.contentId))
        assertEquals(2, f.queues.require(f.sessionId).practiceProgress?.round)
    }

    @Test fun `manual direct rating keeps provenance and creates no automatic evidence`() {
        val f = fixture()
        val manual = RecallManualRatingIntent(ReviewRating.HARD, RatingSource.MANUAL_USER)
        val committed = assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(f.request(manual = manual)))
        assertEquals(RatingSource.MANUAL_USER, committed.result.reviewResult.reviewEvent.source)
        assertEquals(ReviewRating.HARD, committed.result.reviewResult.reviewEvent.rating)
        assertNull(f.trajectories.find(f.learner, f.contentId))
    }

    @Test fun `manual source must match evaluative or practice context`() {
        val evaluative = fixture()
        assertIs<RecallLearningExecutionResult.InvalidRequest>(evaluative.bridge.execute(
            evaluative.request(manual = RecallManualRatingIntent(ReviewRating.GOOD, RatingSource.MANUAL_USER_OVERRIDE))))
        val practice = fixture(practice = true)
        assertIs<RecallLearningExecutionResult.InvalidRequest>(practice.bridge.execute(practice.request(
            result = result(provenance = RecallProvenance.PRACTICE, eligibility = RecallEvidenceEligibility.INELIGIBLE),
            context = RecallStrategyContext.PRACTICE_ONLY,
            manual = RecallManualRatingIntent(ReviewRating.GOOD, RatingSource.MANUAL_USER))))
    }

    @Test fun `practice manual override keeps separate provenance and practice context`() {
        val f = fixture(practice = true, existingRating = ReviewRating.HARD)
        val recall = result(provenance = RecallProvenance.PRACTICE, eligibility = RecallEvidenceEligibility.INELIGIBLE)
        val response = f.bridge.execute(f.request(result = recall, context = RecallStrategyContext.PRACTICE_ONLY,
            manual = RecallManualRatingIntent(ReviewRating.GOOD, RatingSource.MANUAL_USER_OVERRIDE, ReviewRating.HARD)))
        val committed = assertIs<RecallLearningExecutionResult.ManualOverrideCommitted>(response)
        assertEquals(RatingSource.MANUAL_USER_OVERRIDE, committed.result.reviewResult.reviewEvent.source)
        assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, committed.result.session.policy.evaluationPolicy)
        assertEquals(f.item.id, f.queues.require(f.sessionId).currentLearningItemId)
        assertNull(f.trajectories.find(f.learner, f.contentId))
    }

    @Test fun `duplicate delivery does not commit or advance twice`() {
        val f = fixture()
        val request = f.request()
        assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(request))
        assertIs<RecallLearningExecutionResult.DuplicateAttempt>(f.bridge.execute(request))
        assertEquals(1, f.events.findAll(f.learner, f.item.id).size)
        assertEquals(1, f.runner.count)
        assertTrue(f.queues.require(f.sessionId).isCompleted)
        assertEquals(1, f.trajectories.find(f.learner, f.contentId)?.currentChain()?.recallEvidence?.size)
    }

    @Test fun `identity stale session and wrong queue item are rejected before mutation`() {
        val mismatch = fixture()
        assertIs<RecallLearningExecutionResult.IdentityMismatch>(mismatch.bridge.execute(
            mismatch.request(contentId = ContentId("other"))))
        val stale = fixture()
        stale.sessions.save(requireNotNull(stale.sessions.findById(stale.sessionId)).finish(Moment(3_000)))
        assertIs<RecallLearningExecutionResult.StaleSession>(stale.bridge.execute(stale.request()))
        assertTrue(stale.events.findAll(stale.learner).isEmpty())
    }

    @Test fun `undo restores review memory trajectory queue and duplicate marker`() {
        val f = fixture()
        val request = f.request()
        assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(request))
        val undo = UndoLatestSessionReviewUseCase(f.sessions, f.queues, f.memory, f.events, f.runner, f.trajectories)
        assertIs<UndoLatestSessionReviewResult.Undone>(undo.execute(f.sessionId))
        assertTrue(f.events.findAll(f.learner, f.item.id).isEmpty())
        assertNull(f.memory.find(f.learner, f.item.id)); assertNull(f.trajectories.find(f.learner, f.contentId))
        assertEquals(f.item.id, f.queues.require(f.sessionId).currentLearningItemId)
        assertIs<RecallLearningExecutionResult.Committed>(f.bridge.execute(request))
    }

    @Test fun `classifier is deterministic and stable wire ids are portable`() {
        val f = fixture()
        val request = f.request()
        assertEquals(RecallLearningExecutionClassifier.decision(request), RecallLearningExecutionClassifier.decision(request))
        assertEquals("automatic-success", RecallRatingIntent.AUTOMATIC_SUCCESS.wireId)
        assertEquals("partial-recall", RecallManualActionReason.PARTIAL_RECALL.wireId)
        assertEquals("transaction-rejected", RecallLearningCommitFailure.TRANSACTION_REJECTED.wireId)
    }

    private fun result(
        outcome: RecallOutcome = RecallOutcome.CORRECT, correct: Boolean = true,
        correctness: RecallCorrectness = RecallCorrectness.EXACT,
        eligibility: RecallEvidenceEligibility = RecallEvidenceEligibility.STRONG,
        mode: RecallMode = RecallMode.TYPING, provenance: RecallProvenance = RecallProvenance.EVALUATIVE,
        reveal: Boolean = false, assistance: Set<RecallAssistance> = setOf(RecallAssistance.NONE)
    ) = RecallResult(RecallContractVersion.CURRENT, RecallPlanId("plan"), RecallAttemptId("attempt"),
        LearnerId("learner"), ContentId("content"), mode, RecallDirection.SOURCE_TO_TARGET, outcome,
        correct, "word", TimeSpan(500), assistance, reveal, 0, provenance, eligibility, null,
        Moment(2_000), correctness, "recall-evaluator-v1", "recall-execution-v1", RecallPlatformKind.WEB)

    private fun fixture(practice: Boolean = false, existingRating: ReviewRating? = null): Fixture {
        val sessions = InMemoryStudySessionRepository(); val items = InMemoryLearningItemRepository()
        val memory = InMemoryMemoryStateRepository(); val events = InMemoryReviewEventRepository()
        val trajectories = InMemoryLearningTrajectoryRepository(); val queues = StudyQueueService(InMemoryStudyQueueRepository())
        val runner = CountingRunner(); val learner = LearnerId("learner"); val contentId = ContentId("content")
        val item = LearningItem(LearningItemId("item"), contentId, LearningMode.MEANING_RECOGNITION); items.save(item)
        val sessionId = SessionId("session")
        val policy = if (practice) SessionPolicy(evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED) else SessionPolicy()
        sessions.save(StudySession.start(sessionId, learner, Moment(1_000), policy).presentItem(item.id, Moment(1_100)))
        queues.create(sessionId, Moment(1_000), listOf(item.id), itemContentIds = mapOf(item.id to contentId),
            practiceSeed = if (practice) 7 else null)
        val contentQuery = ContentLearningStateQueryService(items, events)
        val reviewCore = ReviewLearningItemUseCase(memory, events, SimpleScheduler())
        existingRating?.let { rating ->
            reviewCore.execute(ReviewCommand(ReviewEventId("existing"), learner, item.id, rating, Moment(900), source = RatingSource.STANDARD_REVIEW))
        }
        val review = ReviewSessionItemUseCase(sessions, items, reviewCore, runner, queues, contentQuery, memory, trajectories)
        val practiceUseCase = CompletePracticeItemUseCase(sessions, queues, runner)
        val manual = ManualRatingOverrideUseCase(sessions, items, contentQuery, reviewCore, runner)
        val bridge = RecallLearningExecutionBridge(sessions, items, events, queues, review, practiceUseCase, manual)
        return Fixture(bridge, sessions, items, memory, events, trajectories, queues, runner, learner, contentId, item, sessionId)
    }

    private data class Fixture(
        val bridge: RecallLearningExecutionBridge, val sessions: InMemoryStudySessionRepository,
        val items: InMemoryLearningItemRepository, val memory: InMemoryMemoryStateRepository,
        val events: InMemoryReviewEventRepository, val trajectories: InMemoryLearningTrajectoryRepository,
        val queues: StudyQueueService, val runner: CountingRunner, val learner: LearnerId,
        val contentId: ContentId, val item: LearningItem, val sessionId: SessionId
    ) {
        fun request(
            result: RecallResult = defaultResult(), context: RecallStrategyContext = RecallStrategyContext.EVALUATIVE,
            manual: RecallManualRatingIntent? = null, contentId: ContentId = this.contentId
        ) = RecallLearningExecutionRequest(result, sessionId, item.id, learner, contentId, context, manual)

        private fun defaultResult() = RecallResult(
            RecallContractVersion.CURRENT, RecallPlanId("plan"), RecallAttemptId("attempt"), learner,
            this.contentId, RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET, RecallOutcome.CORRECT,
            true, "word", TimeSpan(500), setOf(RecallAssistance.NONE), false, 0,
            RecallProvenance.EVALUATIVE, RecallEvidenceEligibility.STRONG, null, Moment(2_000),
            RecallCorrectness.EXACT, "recall-evaluator-v1", "recall-execution-v1", RecallPlatformKind.WEB
        )
    }

    private class CountingRunner : TransactionRunner {
        var count = 0
        override fun <T> runInTransaction(block: () -> T): T { count++; return block() }
    }
}
