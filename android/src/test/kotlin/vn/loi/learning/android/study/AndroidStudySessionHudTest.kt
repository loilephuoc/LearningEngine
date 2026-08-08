package vn.loi.learning.android.study

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Test
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudyPackageLearningStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressStatistics
import vn.loi.learning.application.review.ReviewCommand
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
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudySessionHudTest {
    @Test
    fun `projection uses effective workload and maps canonical package totals`() {
        val statistics = StudyHeaderStatistics(
            session = StudySessionProgressStatistics(
                sessionId = "session",
                newCompleted = 2,
                newConfiguredTarget = 20,
                newEffectiveWorkload = 3,
                reviewCompleted = 4,
                reviewConfiguredTarget = 15,
                reviewEffectiveWorkload = 6
            ),
            packageLearning = StudyPackageLearningStatistics(
                scopeId = "package:one",
                calculatedAt = Moment(2_000),
                totalLearned = 11,
                dueCount = 2,
                againCount = 1,
                hardCount = 2,
                goodCount = 5,
                easyCount = 3,
                nearestFutureDueAt = null
            )
        )

        assertEquals(
            AndroidStudySessionHud(2, 3, 20, 4, 6, 15, 11, 1, 2, 5, 3),
            statistics.toAndroidStudySessionHud()
        )
    }

    @Test
    fun `introduction rating undo and resume refresh HUD from canonical state`() {
        val fixture = newSessionFixture("hud-good")
        val first = assertIs<AndroidStudyState.Introduction>(fixture.facade.load(fixture.sessionId.value))
        assertEquals(AndroidStudySessionHud(0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0), first.hud)

        val second = assertIs<AndroidStudyState.Introduction>(
            fixture.facade.rateIntroduction(first, ReviewRating.GOOD)
        )
        assertEquals(1, second.hud?.newCompleted)
        assertEquals(1, second.hud?.totalLearned)
        assertEquals(1, second.hud?.goodCount)

        val resumed = assertIs<AndroidStudyState.Runtime>(
            AndroidStudyFacade(fixture.context, fixture.learner, now = { 3_000 }).load(fixture.sessionId.value)
        )
        assertEquals(second.hud, resumed.hud)

        val undone = assertIs<AndroidStudyState.Runtime>(fixture.facade.undo(second))
        assertEquals(0, undone.hud?.newCompleted)
        assertEquals(0, undone.hud?.totalLearned)
        assertEquals(0, undone.hud?.goodCount)
    }

    @Test
    fun `Introduction Again is projected without compose side counting`() {
        val fixture = newSessionFixture("hud-again")
        val first = assertIs<AndroidStudyState.Introduction>(fixture.facade.load(fixture.sessionId.value))
        val second = assertIs<AndroidStudyState.Introduction>(
            fixture.facade.rateIntroduction(first, ReviewRating.AGAIN)
        )
        assertEquals(1, assertNotNull(second.hud).againCount)
        assertEquals(0, second.hud?.goodCount)
    }

    @Test
    fun `review commit and undo replace latest canonical rating in HUD`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("hud-review-content")
        val itemId = LearningItemId("hud-review-item")
        context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("answer", "meaning")))
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        context.engine.review(ReviewCommand(ReviewEventId("hud-review-seed"), learner, itemId, ReviewRating.AGAIN, Moment(1_000)))
        val sessionId = SessionId("hud-review-session")
        val session = StudySession.start(
            sessionId, learner, Moment(1_500), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId)
        )
        context.studySessionRepository!!.save(session)
        context.studyQueue.create(
            sessionId,
            Moment(1_500),
            listOf(itemId),
            mapOf(itemId to SessionItemOrigin.REVIEW),
            mapOf(itemId to contentId),
            configuredReviewTarget = 1,
            effectiveReviewWorkload = 1
        )
        val facade = AndroidStudyFacade(context, learner, now = { 2_000 })
        val initial = assertIs<AndroidStudyState.Typing>(facade.load(sessionId.value))
        assertEquals(1, initial.hud?.againCount)

        val committed = assertIs<AndroidStudyState.Typing>(facade.submitText(initial, "answer"))
        assertEquals(1, committed.hud?.reviewCompleted)
        assertEquals(0, committed.hud?.againCount)
        assertEquals(1, committed.hud?.goodCount)

        val undone = assertIs<AndroidStudyState.Typing>(facade.undo(committed))
        assertEquals(0, undone.hud?.reviewCompleted)
        assertEquals(1, undone.hud?.againCount)
        assertEquals(0, undone.hud?.goodCount)
    }

    private fun newSessionFixture(prefix: String): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentIds = (1..2).map { ContentId("$prefix-content-$it") }
        contentIds.forEachIndexed { index, contentId ->
            context.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("word-$index", "meaning-$index"))
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$prefix-item-${index + 1}"), contentId, LearningMode.MEANING_RECOGNITION)
            )
        }
        val sessionId = SessionId("$prefix-session")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId,
                learner,
                Moment(1_000),
                SessionPolicy(newItemLimit = 2, reviewItemLimit = 0),
                contentIds.toSet()
            )
        )
        return Fixture(context, learner, sessionId, AndroidStudyFacade(context, learner, now = { 2_000 }))
    }

    private data class Fixture(
        val context: vn.loi.learning.infrastructure.LearningApplicationContext,
        val learner: LearnerId,
        val sessionId: SessionId,
        val facade: AndroidStudyFacade
    )
}
