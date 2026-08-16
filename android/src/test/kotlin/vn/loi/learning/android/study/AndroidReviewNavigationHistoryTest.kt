package vn.loi.learning.android.study

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.study.components.ReviewNavigationGesture
import vn.loi.learning.android.study.components.resolveReviewNavigationGesture
import vn.loi.learning.application.review.ReviewCommand
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
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidReviewNavigationHistoryTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `Quick Review front skip advances immediately without learning mutation and counts once`() =
        runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val learner = LearnerId("default-learner")
            val contentId = ContentId("quick-front-content")
            val itemId = LearningItemId("quick-front-item")
            val sessionId = SessionId("quick-front-session")
            context.contentRepository!!.save(Content(
                contentId, ContentType.WORD, ContentText("answer", "meaning")
            ))
            context.learningItemRepository!!.save(LearningItem(
                itemId, contentId, LearningMode.MEANING_RECOGNITION
            ))
            context.engine.review(ReviewCommand(
                ReviewEventId("quick-front-seed"), learner, itemId, ReviewRating.GOOD, Moment(1_000)
            ))
            context.studySessionRepository!!.save(StudySession.start(
                sessionId, learner, Moment(2_000), SessionPolicy(
                    newItemLimit = 0, reviewItemLimit = 1, allowRepeatInSameSession = true,
                    evaluationPolicy = SessionEvaluationPolicy.EVALUATIVE,
                    practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
                    focusedPracticeKind = FocusedPracticeKind.QUICK_REVIEW
                ), setOf(contentId)
            ))
            context.studyQueue.create(
                sessionId, Moment(2_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW),
                mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1,
                practiceSeed = 42, practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
            )
            val viewModel = AndroidStudyViewModel(
                AndroidStudyFacade(context, learner, now = { 3_000 }),
                SavedStateHandle(mapOf("study.sessionId" to sessionId.value)), dispatcher
            )
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
            advanceUntilIdle()
            val front = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertFalse(front.revealed)
            val eventsBefore = context.reviewEventRepository!!.findAll(learner)
            val memoryBefore = context.memoryStateRepository!!.find(learner, itemId)

            viewModel.onEvent(AndroidStudyEvent.QuickReviewUnratedAdvance)
            advanceUntilIdle()

            val next = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertTrue(front.presentationVisitId != next.presentationVisitId)
            assertEquals(eventsBefore, context.reviewEventRepository!!.findAll(learner))
            assertEquals(memoryBefore, context.memoryStateRepository!!.find(learner, itemId))
            assertEquals(QuickReviewSessionInsights(sessionId.value, skipped = 1), viewModel.quickReviewSummary.value)

            viewModel.onEvent(AndroidStudyEvent.QuickReviewUnratedAdvance)
            advanceUntilIdle()
            assertEquals(2, viewModel.quickReviewSummary.value?.skipped)
        }

    @Test
    fun `repeated Quick Review exposure is a live tail while Previous remains historical`() =
        runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val learner = LearnerId("default-learner")
            val contentId = ContentId("quick-history-content")
            val itemId = LearningItemId("quick-history-item")
            val sessionId = SessionId("quick-history-session")
            context.contentRepository!!.save(Content(
                contentId, ContentType.WORD, ContentText("answer", "meaning")
            ))
            context.learningItemRepository!!.save(LearningItem(
                itemId, contentId, LearningMode.MEANING_RECOGNITION
            ))
            context.engine.review(ReviewCommand(
                ReviewEventId("quick-history-seed"), learner, itemId, ReviewRating.GOOD, Moment(1_000)
            ))
            context.studySessionRepository!!.save(StudySession.start(
                sessionId, learner, Moment(2_000), SessionPolicy(
                    newItemLimit = 0,
                    reviewItemLimit = 1,
                    allowRepeatInSameSession = true,
                    evaluationPolicy = SessionEvaluationPolicy.EVALUATIVE,
                    practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
                    focusedPracticeKind = FocusedPracticeKind.QUICK_REVIEW
                ), setOf(contentId)
            ))
            context.studyQueue.create(
                sessionId, Moment(2_000), listOf(itemId),
                mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId),
                configuredReviewTarget = 1, effectiveReviewWorkload = 1, practiceSeed = 42,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
            )
            val viewModel = AndroidStudyViewModel(
                AndroidStudyFacade(context, learner, now = { 3_000 }),
                SavedStateHandle(mapOf("study.sessionId" to sessionId.value)), dispatcher
            )
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
            advanceUntilIdle()
            val first = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertEquals(null, viewModel.quickReviewSummary.value)

            viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
            advanceUntilIdle()
            assertEquals(QuickReviewSessionInsights(sessionId.value, good = 1), viewModel.quickReviewSummary.value)
            val repeatedLive = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertEquals(first.learningItemId, repeatedLive.learningItemId)
            assertTrue(first.presentationVisitId != repeatedLive.presentationVisitId)
            assertFalse(repeatedLive.historyPreview)

            viewModel.onEvent(AndroidStudyEvent.PreviousVisited)
            advanceUntilIdle()
            assertTrue(assertIs<AndroidStudyState.Introduction>(viewModel.state.value).historyPreview)
            assertEquals(1, viewModel.quickReviewSummary.value?.totalExposures)
            viewModel.onEvent(AndroidStudyEvent.NextVisited)
            advanceUntilIdle()
            assertFalse(assertIs<AndroidStudyState.Introduction>(viewModel.state.value).historyPreview)
            assertEquals(1, viewModel.quickReviewSummary.value?.totalExposures)
            assertEquals(2, context.reviewEventRepository!!.findAll(learner).size)

            viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
            advanceUntilIdle()
            val eventsBeforeSkip = context.reviewEventRepository!!.findAll(learner)
            val memoryBeforeSkip = context.memoryStateRepository!!.find(learner, itemId)
            viewModel.onEvent(AndroidStudyEvent.QuickReviewUnratedAdvance)
            advanceUntilIdle()
            assertEquals(2, viewModel.quickReviewSummary.value?.totalExposures)
            assertEquals(1, viewModel.quickReviewSummary.value?.skipped)
            assertEquals(1, viewModel.quickReviewSummary.value?.good)
            assertEquals(eventsBeforeSkip, context.reviewEventRepository!!.findAll(learner))
            assertEquals(memoryBeforeSkip, context.memoryStateRepository!!.find(learner, itemId))

        }

    @Test
    fun `evaluated presentation history moves backward and forward without duplicate ReviewEvent`() =
        runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val learner = LearnerId("default-learner")
            val sessionId = SessionId("navigation-history-session")
            val answers = listOf("alpha", "bravo", "charlie")
            val contentIds = answers.map { ContentId("navigation-$it") }
            val itemIds = answers.map { LearningItemId("navigation-item-$it") }
            answers.indices.forEach { index ->
                context.contentRepository!!.save(Content(
                    contentIds[index], ContentType.WORD, ContentText(answers[index], "meaning-$index")
                ))
                context.learningItemRepository!!.save(LearningItem(
                    itemIds[index], contentIds[index], LearningMode.MEANING_RECALL
                ))
                context.engine.review(ReviewCommand(
                    ReviewEventId("navigation-seed-$index"), learner, itemIds[index],
                    ReviewRating.GOOD, Moment(1_000L + index)
                ))
            }
            context.studySessionRepository!!.save(StudySession.start(
                sessionId, learner, Moment(2_000),
                SessionPolicy(newItemLimit = 0, reviewItemLimit = 3),
                contentIds.toSet(), studyMode = StudyMode.TYPING
            ))
            context.studyQueue.create(
                sessionId, Moment(2_000), itemIds,
                itemIds.associateWith { SessionItemOrigin.REVIEW },
                itemIds.indices.associate { itemIds[it] to contentIds[it] },
                configuredReviewTarget = 3, effectiveReviewWorkload = 3
            )
            val viewModel = AndroidStudyViewModel(
                AndroidStudyFacade(context, learner, now = { 3_000 }),
                SavedStateHandle(mapOf("study.sessionId" to sessionId.value)), dispatcher
            )
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
            advanceUntilIdle()
            assertIs<AndroidStudyState.Typing>(viewModel.state.value)

            evaluateAndAdvance(viewModel, answers[0])
            evaluateAndAdvance(viewModel, answers[1])
            val third = assertIs<AndroidStudyState.Typing>(viewModel.state.value)
            assertEquals(answers[2], third.plan.answerContract.canonicalAnswer)
            val afterEvaluation = context.reviewEventRepository!!.findAll(learner).size

            viewModel.onEvent(AndroidStudyEvent.PreviousVisited)
            advanceUntilIdle()
            val second = assertIs<AndroidStudyState.Typing>(viewModel.state.value)
            assertEquals(answers[1], second.plan.answerContract.canonicalAnswer)
            assertTrue(second.completed)
            assertTrue(second.navigation.historyPreview)

            viewModel.onEvent(AndroidStudyEvent.PreviousVisited)
            advanceUntilIdle()
            assertEquals(answers[0], assertIs<AndroidStudyState.Typing>(viewModel.state.value)
                .plan.answerContract.canonicalAnswer)

            viewModel.onEvent(AndroidStudyEvent.NextVisited)
            advanceUntilIdle()
            assertEquals(answers[1], assertIs<AndroidStudyState.Typing>(viewModel.state.value)
                .plan.answerContract.canonicalAnswer)
            viewModel.onEvent(AndroidStudyEvent.NextVisited)
            advanceUntilIdle()
            val restoredThird = assertIs<AndroidStudyState.Typing>(viewModel.state.value)
            assertEquals(answers[2], restoredThird.plan.answerContract.canonicalAnswer)
            assertFalse(restoredThird.completed)
            assertEquals(afterEvaluation, context.reviewEventRepository!!.findAll(learner).size)
        }

    @Test
    fun `Learn New historical Introduction rejects stale rating and preserves repositories`() =
        runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val learner = LearnerId("default-learner")
            val sessionId = SessionId("learn-new-history-session")
            val contentIds = listOf(ContentId("learn-new-a"), ContentId("learn-new-b"))
            val itemIds = listOf(LearningItemId("learn-new-item-a"), LearningItemId("learn-new-item-b"))
            contentIds.indices.forEach { index ->
                context.contentRepository!!.save(Content(
                    contentIds[index], ContentType.WORD, ContentText("answer-$index", "meaning-$index")
                ))
                context.learningItemRepository!!.save(LearningItem(
                    itemIds[index], contentIds[index], LearningMode.MEANING_RECOGNITION
                ))
            }
            context.studySessionRepository!!.save(StudySession.start(
                sessionId, learner, Moment(2_000),
                SessionPolicy(newItemLimit = 2, reviewItemLimit = 0),
                contentIds.toSet(), studyMode = StudyMode.LEARN_NEW
            ))
            context.studyQueue.create(
                sessionId, Moment(2_000), itemIds,
                itemIds.associateWith { SessionItemOrigin.NEW },
                itemIds.indices.associate { itemIds[it] to contentIds[it] },
                configuredNewTarget = 2, effectiveNewWorkload = 2
            )
            val viewModel = AndroidStudyViewModel(
                AndroidStudyFacade(context, learner, now = { 3_000 }),
                SavedStateHandle(mapOf("study.sessionId" to sessionId.value)), dispatcher
            )
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
            advanceUntilIdle()
            val liveTail = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertEquals(itemIds[1].value, liveTail.learningItemId)

            viewModel.onEvent(AndroidStudyEvent.PreviousVisited)
            advanceUntilIdle()
            val history = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertTrue(history.historyPreview)
            val eventsBefore = context.reviewEventRepository!!.findAll(learner)
            val firstMemoryBefore = context.memoryStateRepository!!.find(learner, itemIds[0])
            val secondMemoryBefore = context.memoryStateRepository!!.find(learner, itemIds[1])

            viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.EASY))
            advanceUntilIdle()

            val unchangedHistory = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertTrue(unchangedHistory.historyPreview)
            assertEquals(eventsBefore, context.reviewEventRepository!!.findAll(learner))
            assertEquals(firstMemoryBefore, context.memoryStateRepository!!.find(learner, itemIds[0]))
            assertEquals(secondMemoryBefore, context.memoryStateRepository!!.find(learner, itemIds[1]))

            viewModel.onEvent(AndroidStudyEvent.NextVisited)
            advanceUntilIdle()
            val restored = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
            assertFalse(restored.historyPreview)
            assertEquals(liveTail.learningItemId, restored.learningItemId)
        }

    @Test
    fun `swipe vocabulary uses deliberate dominant threshold`() {
        assertEquals(ReviewNavigationGesture.NEXT, resolveReviewNavigationGesture(Offset(-100f, 12f), 44f))
        assertEquals(ReviewNavigationGesture.PREVIOUS, resolveReviewNavigationGesture(Offset(100f, 12f), 44f))
        assertEquals(ReviewNavigationGesture.NEXT, resolveReviewNavigationGesture(Offset(12f, -100f), 44f))
        assertEquals(ReviewNavigationGesture.NONE, resolveReviewNavigationGesture(Offset(-20f, -8f), 44f))
        assertEquals(ReviewNavigationGesture.NONE, resolveReviewNavigationGesture(Offset(-90f, -80f), 44f))
    }

    @Test
    fun `navigation controls overlay image content and difficult HUD exposes only canonical buckets`() {
        val overlay = source("vn/loi/learning/android/study/components/ReviewImageNavigationOverlay.kt")
        assertTrue(overlay.contains("Box(modifier.then(gestureModifier))"))
        assertTrue(overlay.indexOf("imageContent()") < overlay.indexOf("NavigationEdgeButton(false"))
        assertTrue(overlay.contains("Modifier.align(Alignment.CenterStart)"))
        assertTrue(overlay.contains("Modifier.align(Alignment.CenterEnd)"))
        assertTrue(overlay.contains("modifier.size(48.dp)"))
        assertTrue(overlay.contains("contentDescription = if (next) \"Next item\" else \"Previous item\""))

        val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
        val hud = screen.substringAfter("private fun DifficultPracticeHud(")
            .substringBefore("@Composable\nprivate fun LearnNewProgressHeader")
        assertTrue(hud.contains("HudRating(\"Again\""))
        assertTrue(hud.contains("HudRating(\"Hard\""))
        listOf("Total", "New", "Review", "Due", "Good", "Easy").forEach {
            assertFalse(hud.contains("\"$it\""), it)
        }
    }

    private suspend fun TestScope.evaluateAndAdvance(viewModel: AndroidStudyViewModel, answer: String) {
        val incorrect = "not-$answer"
        viewModel.onEvent(AndroidStudyEvent.AnswerChanged(incorrect))
        advanceUntilIdle()
        viewModel.onEvent(AndroidStudyEvent.Submit(incorrect))
        advanceUntilIdle()
        assertTrue(assertIs<AndroidStudyState.Typing>(viewModel.state.value).completed)
        viewModel.onEvent(AndroidStudyEvent.NextVisited)
        advanceUntilIdle()
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin", relative))
}
