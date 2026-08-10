package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidStudyViewModelSerializationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `unexpected Study event failure is published instead of leaving stale Home`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        var failNow = false
        val viewModel = AndroidStudyViewModel(
            AndroidStudyFacade(context, now = {
                if (failNow) error("event execution exploded") else 1_700_000_000_000L
            }),
            SavedStateHandle(), dispatcher
        )
        advanceUntilIdle()
        assertIs<AndroidStudyState.Home>(viewModel.state.value)

        failNow = true
        viewModel.onEvent(AndroidStudyEvent.Home)
        advanceUntilIdle()

        val failed = assertIs<AndroidStudyState.Failed>(viewModel.state.value)
        assertEquals("Study action failed.", failed.message)
    }

    @Test
    fun `Start publishes immediate mode feedback and ignores duplicate invocation while preparing`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val viewModel = AndroidStudyViewModel(
            AndroidStudyFacade(context), SavedStateHandle(), dispatcher
        )
        advanceUntilIdle()
        assertIs<AndroidStudyState.Home>(viewModel.state.value)

        viewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
        assertEquals(StudyMode.LEARN_NEW, assertIs<AndroidStudyState.PreparingMode>(viewModel.state.value).mode)
        viewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.TYPING))
        assertEquals(StudyMode.LEARN_NEW, assertIs<AndroidStudyState.PreparingMode>(viewModel.state.value).mode)

        advanceUntilIdle()
        assertIs<AndroidStudyState.Failed>(viewModel.state.value)
    }

    @Test
    fun `correct AnswerChanged followed immediately by IME Submit publishes one completed result`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("typing-race-content")
        val itemId = LearningItemId("typing-race-item")
        val sessionId = SessionId("typing-race-session")
        context.contentRepository!!.save(Content(
            contentId, ContentType.WORD, ContentText("married", "kết hôn")
        ))
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(
            ReviewEventId("typing-race-seed"), learner, itemId, ReviewRating.GOOD, Moment(1_000)
        ))
        context.studySessionRepository!!.save(StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1),
            setOf(contentId), studyMode = StudyMode.TYPING
        ))
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW),
            mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )
        val facade = AndroidStudyFacade(context, learner, now = { 2_000 })
        val viewModel = AndroidStudyViewModel(
            facade, SavedStateHandle(mapOf("study.sessionId" to sessionId.value)), dispatcher
        )
        advanceUntilIdle()

        viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
        advanceUntilIdle()
        assertIs<AndroidStudyState.Typing>(viewModel.state.value)

        val liveTyping = viewModel.state.value
        viewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
        advanceUntilIdle()
        assertEquals(liveTyping, viewModel.state.value)

        viewModel.onEvent(AndroidStudyEvent.AnswerChanged("wrong"))
        advanceUntilIdle()
        assertEquals(
            TypingAnswerEvaluationStatus.INCORRECT,
            assertIs<AndroidStudyState.Typing>(viewModel.state.value).evaluation
        )
        viewModel.onEvent(AndroidStudyEvent.Retry)
        advanceUntilIdle()
        val before = context.reviewEventRepository!!.findAll(learner).size

        viewModel.onEvent(AndroidStudyEvent.AnswerChanged("married"))
        viewModel.onEvent(AndroidStudyEvent.Submit("married"))
        advanceUntilIdle()

        val completed = assertIs<AndroidStudyState.Completion>(viewModel.state.value)
        assertEquals(sessionId.value, completed.sessionId)
        assertEquals(StudyMode.TYPING, context.engine.getSession(sessionId)!!.studyMode)
        assertEquals(before + 1, context.reviewEventRepository!!.findAll(learner).size)

        viewModel.onEvent(AndroidStudyEvent.Submit("married"))
        advanceUntilIdle()
        assertIs<AndroidStudyState.Completion>(viewModel.state.value)
        assertEquals(before + 1, context.reviewEventRepository!!.findAll(learner).size)
    }
}
