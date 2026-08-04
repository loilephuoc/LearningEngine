package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.recall.RecallPlanFactory
import vn.loi.learning.application.recall.RecallPlanFactoryResult
import vn.loi.learning.application.recall.RecallPlanPolicy
import vn.loi.learning.application.recall.RecallPlanRequest
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyFacadeTest {
    @Test fun `exact session handoff loads requested plan and never falls back to active session`() {
        val f=fixture()
        val exact=assertIs<AndroidStudyState.Typing>(f.facade.loadExact("android-session-review"))
        assertEquals("android-session-review",exact.plan.sessionId.value)
        assertIs<AndroidStudyState.Failed>(f.facade.loadExact("stale-session"))
    }
    @Test fun `typing uses shared plan execution bridge completion and undo exactly once`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        assertEquals("xin chào", initial.prompt)
        assertEquals(TypingAnswerEvaluationStatus.VALID_PREFIX, f.facade.updateAnswer(initial, "hel").let { assertIs<AndroidStudyState.Typing>(it).evaluation })
        val mismatch = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hex"))
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, mismatch.evaluation)

        val corrected = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(mismatch, "hello"))
        val completed = assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(corrected))
        assertTrue(completed.completed)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        f.facade.submitTypingIfCorrect(completed)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
        val completion = assertIs<AndroidStudyState.Completion>(f.facade.next(completed))
        assertIs<AndroidStudyState.Typing>(f.facade.undo(completion))
        assertTrue(f.context.engine.getReviewHistory(f.learner, f.itemId).isEmpty())
    }

    @Test fun `reveal crosses shared lapse path and resume preserves current session`() {
        val f = fixture()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        val resumed = assertIs<AndroidStudyState.Typing>(
            AndroidStudyFacade(f.context, f.learner, now = { 2_000 }).load(initial.plan.sessionId.value)
        )
        assertEquals(initial.plan.planId, resumed.plan.planId)
        val revealed = assertIs<AndroidStudyState.Typing>(f.facade.reveal(initial))
        assertTrue(revealed.revealed)
        assertEquals(RecallOutcome.REVEALED, revealed.outcome)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `android projects MCQ option order and one-shot presentation from shared plan`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val choices = listOf(
            RecallChoice("a", "Alpha", false), RecallChoice("b", "Beta", true),
            RecallChoice("c", "Gamma", false), RecallChoice("d", "Delta", false)
        )
        val state = assertIs<AndroidStudyState.MultipleChoice>(
            f.facade.present(base.copy(mode = RecallMode.MULTIPLE_CHOICE, prompt = RecallPrompt.MultipleChoice("Choose", choices)))
        )
        assertEquals(choices, state.choices)
        assertEquals("Choose", state.question)
    }

    @Test fun `MCQ click submits stable choice once through shared execution and bridge`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val choices = listOf(RecallChoice("a", "Wrong", false), RecallChoice("b", "hello", true))
        val plan = base.copy(
            mode = RecallMode.MULTIPLE_CHOICE,
            prompt = RecallPrompt.MultipleChoice("Choose", choices),
            answerContract = base.answerContract.copy(canonicalAnswer = "b", kind = RecallAnswerKind.CHOICE),
            platformRequirements = RecallPlatformRequirements(requiresChoiceSelection = true)
        )
        val state = assertIs<AndroidStudyState.MultipleChoice>(f.facade.present(plan))
        val completed = assertIs<AndroidStudyState.MultipleChoice>(f.facade.choose(state, "b"))
        assertTrue(completed.completed)
        assertEquals(RecallOutcome.CORRECT, completed.outcome)
        f.facade.choose(completed, "b")
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)
    }

    @Test fun `android listening resolves media and represents unavailable audio`() {
        val f = fixture(resolveMedia = { if (it == "audio/word.mp3") "C:/media/word.mp3" else null })
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val resolved = assertIs<AndroidStudyState.Listening>(
            f.facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("audio/word.mp3"))))
        )
        val missing = assertIs<AndroidStudyState.Listening>(
            f.facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("missing.mp3"))))
        )
        assertEquals("C:/media/word.mp3", resolved.audioPath)
        assertTrue(missing.audioUnavailable)
    }

    @Test fun `android image represents resolved and unavailable resources`() {
        val f = fixture(resolveMedia = { if (it == "image/prompt.png") "C:/media/prompt.png" else null })
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val resolved = assertIs<AndroidStudyState.ImageRecall>(
            f.facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("image/prompt.png"))))
        )
        val missing = assertIs<AndroidStudyState.ImageRecall>(
            f.facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("missing.png"))))
        )
        assertEquals("C:/media/prompt.png", resolved.imagePath)
        assertTrue(missing.imageUnavailable)
    }

    @Test fun `android example preserves exact prefix blank and suffix`() {
        val f = fixture()
        val base = assertIs<AndroidStudyState.Typing>(f.facade.load()).plan
        val state = assertIs<AndroidStudyState.ExampleCompletion>(
            f.facade.present(base.copy(
                mode = RecallMode.EXAMPLE_COMPLETION,
                prompt = RecallPrompt.ExampleCompletion("Please make the bed now.", RecallTextSpan(7, 19))
            ))
        )
        assertEquals("Please ", state.prefix)
        assertEquals("make the bed", state.blank)
        assertEquals(" now.", state.suffix)
    }

    @Test fun `practice completion remains isolated and queue policy stays shared`() {
        listOf(
            PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED,
            PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
        ).forEach { policy ->
            val f = fixture(practiceLoopPolicy = policy)
            val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
            val corrected = assertIs<AndroidStudyState.Typing>(f.facade.updateAnswer(initial, "hello"))
            assertTrue(assertIs<AndroidStudyState.Typing>(f.facade.submitTypingIfCorrect(corrected)).completed)
            assertTrue(f.context.engine.getReviewHistory(f.learner, f.itemId).isEmpty())
            assertEquals(policy, f.context.studyQueue.get(initial.plan.sessionId)?.practiceLoopPolicy)
            assertNotNull(f.context.engine.getPracticeProgress(initial.plan.sessionId))
        }
    }

    @Test fun `difficult practice manual Good updates dynamic membership and undo restores it`() {
        val f = fixture(practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP)
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())
        val afterOverride = f.facade.overridePracticeRating(initial, ReviewRating.GOOD)
        assertIs<AndroidStudyState.Typing>(afterOverride)
        assertEquals(1, f.context.engine.getReviewHistory(f.learner, f.itemId).size)

        assertIs<AndroidStudyState.Typing>(f.facade.undo(afterOverride))
        assertTrue(f.context.engine.getReviewHistory(f.learner, f.itemId).isEmpty())
        assertEquals(1, f.context.engine.getPracticeProgress(initial.plan.sessionId)?.membershipSize)
    }

    private fun fixture(
        resolveMedia: (String) -> String? = { null },
        practiceLoopPolicy: PracticeLoopPolicy? = null
    ): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("android-content")
        val itemId = LearningItemId("android-item")
        context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("hello", "xin chào")))
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("android-session-${practiceLoopPolicy?.name ?: "review"}")
        if (practiceLoopPolicy == null) {
            context.engine.startSession(
                StartStudySessionCommand(sessionId, learner, Moment(1_000),
                    SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
            )
        } else {
            val session = StudySession.start(
                sessionId, learner, Moment(1_000),
                SessionPolicy(
                    newItemLimit = 0,
                    reviewItemLimit = 1,
                    allowRepeatInSameSession = true,
                    evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
                    practiceLoopPolicy = practiceLoopPolicy
                ),
                includedContentIds = setOf(contentId)
            )
            context.studySessionRepository!!.save(session)
            context.studyQueue.create(
                sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW),
                mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1,
                practiceSeed = 1L, practiceLoopPolicy = practiceLoopPolicy
            )
        }
        return Fixture(context, learner, itemId, AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia))
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val itemId: LearningItemId,
        val facade: AndroidStudyFacade
    )
}
