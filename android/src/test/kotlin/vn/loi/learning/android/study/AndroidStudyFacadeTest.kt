package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
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
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyFacadeTest {
    @Test
    fun `typing uses shared plan execution and learning bridge exactly once`() {
        val fixture = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(fixture.facade.load())

        assertEquals("xin chào", initial.prompt)
        val prefix = fixture.facade.updateAnswer(initial, "hel")
        assertEquals(TypingAnswerEvaluationStatus.VALID_PREFIX, prefix.evaluation)
        val mismatch = fixture.facade.updateAnswer(prefix, "hex")
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, mismatch.evaluation)
        assertFalse(mismatch.completed)

        val corrected = fixture.facade.updateAnswer(mismatch.copy(answer = ""), "hello")
        val completed = assertIs<AndroidStudyState.Typing>(fixture.facade.submitIfCorrect(corrected))
        assertTrue(completed.completed)
        assertEquals(1, fixture.context.engine.getReviewHistory(fixture.learner, fixture.itemId).size)

        fixture.facade.submitIfCorrect(completed)
        assertEquals(1, fixture.context.engine.getReviewHistory(fixture.learner, fixture.itemId).size)
        assertIs<AndroidStudyState.Complete>(fixture.facade.next(completed))
    }

    @Test
    fun `reveal crosses shared lapse path and exposes comparison state`() {
        val fixture = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(fixture.facade.load())

        val revealed = assertIs<AndroidStudyState.Typing>(fixture.facade.reveal(initial))

        assertTrue(revealed.revealed)
        assertTrue(revealed.completed)
        assertEquals(1, fixture.context.engine.getReviewHistory(fixture.learner, fixture.itemId).size)
    }

    private fun fixture(): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("android-content")
        val itemId = LearningItemId("android-item")
        context.contentRepository!!.save(
            Content(contentId, ContentType.WORD, ContentText("hello", "xin chào"))
        )
        context.learningItemRepository!!.save(
            LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = SessionId("android-session"),
                learnerId = learner,
                startedAt = Moment(1_000),
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                includedContentIds = setOf(contentId)
            )
        )
        return Fixture(
            context,
            learner,
            itemId,
            AndroidStudyFacade(context, learner) { 2_000 }
        )
    }

    private data class Fixture(
        val context: vn.loi.learning.infrastructure.LearningApplicationContext,
        val learner: LearnerId,
        val itemId: LearningItemId,
        val facade: AndroidStudyFacade
    )
}
