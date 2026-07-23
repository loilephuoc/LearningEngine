package vn.loi.learning.application.session.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ProductBrainSessionCompletionEndToEndTest {
    @Test
    fun `bootstrap scene evidence decision explanation scheduler and persistence complete session`() {
        val context = LearningApplicationFactory.createInMemory()
        val content = Content(ContentId("content"), ContentType.WORD, ContentText("question", "answer"))
        val item = LearningItem(LearningItemId("item"), content.id, LearningMode.MEANING_RECOGNITION)
        context.engine.registerContent(content)
        context.engine.registerLearningItem(item)

        val sessionId = SessionId("session")
        val learnerId = LearnerId("learner")
        val startedAt = Moment(1_000L)
        context.engine.startSession(StartStudySessionCommand(sessionId, learnerId, startedAt))
        val current = assertNotNull(context.engine.getNextSessionItem(sessionId, startedAt))

        val planner = ProductBrainPlanner()
        val overview = planner.bootstrapSession(learnerId.value, "Physics", itemCount = 1)
        val scene = planner.selectFirstScene("question", "answer", item.id.value, learnerId.value)
        val sceneResult = scene.evaluate("answer", 1_100L)
        val evidence = scene.toEvidence(sceneResult, learnerId.value, item.id.value)
        val adaptive = planner.evaluateAndAdapt(evidence, overview.timeline)
        val plan =
            planner.prepareSessionCompletion(
                SessionCompletionInput(
                    context = overview.context,
                    goal = overview.goal,
                    sceneResult = sceneResult,
                    evidence = evidence,
                    decision = adaptive.decision,
                    decisionTrace = adaptive.trace,
                    decisionExplanation = adaptive.explanation,
                    timeline = adaptive.updatedTimeline,
                    finalDifficultyLevel = adaptive.newDifficultyLevel
                )
            )

        context.engine.revealSessionItem(sessionId, current.item.learningItem.id)
        val reviewed =
            context.engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId = ReviewEventId("review"),
                    learningItemId = current.item.learningItem.id,
                    rating = plan.recommendedRating,
                    reviewedAt = Moment(2_000L)
                )
            )
        val scheduling =
            planner.projectSchedulingOutcome(
                rating = plan.recommendedRating,
                scheduledIntervalMillis = reviewed.reviewResult.scheduledInterval.millis,
                nextReviewAtEpochMillis = reviewed.reviewResult.memoryState.dueAt.epochMillis
            )
        val completion = planner.completeSession(plan, scheduling, sessionId.value, 2_000L)
        val snapshot =
            SessionCompletionSnapshot(
                whatWasLearned = completion.summary.whatWasLearned,
                overallOutcome = completion.summary.overallOutcome,
                reflection = completion.reflection.encouragement,
                reinforcement = completion.reflection.reinforcement,
                whatHappensNext = completion.summary.whatHappensNext,
                schedulingGuidance = completion.schedulingOutcome.guidance,
                scheduledIntervalMillis = completion.schedulingOutcome.scheduledIntervalMillis,
                nextReviewAtEpochMillis = completion.schedulingOutcome.nextReviewAtEpochMillis
            )
        val finished = context.engine.finishSession(sessionId, Moment(2_000L), snapshot)

        assertEquals(SessionStatus.FINISHED, finished.status)
        assertEquals(snapshot, context.engine.getSession(sessionId)?.completionSnapshot)
        assertEquals(1, context.engine.getReviewHistory(learnerId, item.id).size)
        assertNotNull(context.engine.getMemoryState(learnerId, item.id))
        assertTrue(completion.schedulingOutcome.guidance.contains("scheduled"))
    }
}
