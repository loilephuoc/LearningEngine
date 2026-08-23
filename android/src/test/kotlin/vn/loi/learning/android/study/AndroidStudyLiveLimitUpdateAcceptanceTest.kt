package vn.loi.learning.android.study

import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyLiveLimitUpdateAcceptanceTest {

    private val learnerId = LearnerId("default-learner")

    @Test
    fun `long press metric live update 100 to 15 keeps session open and updates hud immediately`() {
        val fixture = createFixture(itemCount = 25)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val initialIntro = assertIs<AndroidStudyState.Introduction>(started)

        var current: AndroidStudyState = initialIntro
        for (i in 1..3) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val runtime = assertIs<AndroidStudyState.Introduction>(current)
        val hudBefore = assertNotNull(runtime.hud)
        assertEquals(3, hudBefore.newCompleted)
        assertEquals(100, hudBefore.newConfiguredTarget)

        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val updatedState = facade.updateDailyLimits(runtime, newLimit = 15, reviewLimit = 100)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hudAfter = assertNotNull(updatedIntro.hud)

        assertEquals(3, hudAfter.newCompleted)
        assertEquals(15, hudAfter.newConfiguredTarget)

        val next = facade.rateIntroduction(updatedIntro, ReviewRating.GOOD)
        val nextIntro = assertIs<AndroidStudyState.Introduction>(next)
        val nextHud = assertNotNull(nextIntro.hud)
        assertEquals(4, nextHud.newCompleted)
        assertEquals(15, nextHud.newConfiguredTarget)
    }

    @Test
    fun `live update 100 to 20 with 17 completed continues past new quota into practice recall without error`() {
        val fixture = createFixture(itemCount = 30)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Complete 17 new items
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val introAt17 = assertIs<AndroidStudyState.Introduction>(current)
        val hud17 = assertNotNull(introAt17.hud)
        assertEquals(17, hud17.newCompleted)
        assertEquals(100, hud17.newConfiguredTarget)

        // Live update new limit from 100 to 20
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val updatedState = facade.updateDailyLimits(introAt17, newLimit = 20, reviewLimit = 100)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hud20 = assertNotNull(updatedIntro.hud)
        assertEquals(17, hud20.newCompleted)
        assertEquals(20, hud20.newConfiguredTarget)

        // Complete items 18, 19, 20
        current = updatedIntro
        for (i in 18..20) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // After completing all 20 new items, session is cleanly completed without error
        assertFalse(current is AndroidStudyState.Failed)
        val completion = assertIs<AndroidStudyState.Completion>(current)
        assertEquals(20, completion.newCompleted)
    }

    @Test
    fun `recovery of existing session with 17 of 20 completed loads recall plan without error`() {
        val fixture = createFixture(itemCount = 30)
        val facade1 = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100) }
        )

        val started = facade1.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade1.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val sessionId = assertIs<AndroidStudyState.Introduction>(current).sessionId

        // Simulate limit update to 20
        facade1.updateDailyLimits(current, newLimit = 20, reviewLimit = 100)

        // Simulate app restart / reopen: load exact session from scratch
        val restoredFacade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 3_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100) }
        )
        val reloadedState = restoredFacade.loadExact(sessionId)

        val runtime = assertIs<AndroidStudyState.Runtime>(reloadedState)
        val reloadedHud = assertNotNull(runtime.hud)
        assertEquals(17, reloadedHud.newCompleted)
        assertEquals(20, reloadedHud.newConfiguredTarget)
    }

    @Test
    fun `invalid limit update fails validation atomically and keeps session and queue intact`() {
        val fixture = createFixture(itemCount = 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100) }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val introAt17 = assertIs<AndroidStudyState.Introduction>(current)

        // Attempting to set limit to 5 (less than 17 already completed) must fail
        val failedState = facade.updateDailyLimits(introAt17, newLimit = 5, reviewLimit = 100)
        assertIs<AndroidStudyState.Failed>(failedState)

        // The session and queue are not corrupted, continuing with old limits
        val reloaded = facade.loadExact(introAt17.sessionId)
        val runtime = assertIs<AndroidStudyState.Introduction>(reloaded)
        val hud = assertNotNull(runtime.hud)
        assertEquals(17, hud.newCompleted)
        assertEquals(100, hud.newConfiguredTarget)
    }

    @Test
    fun `review limit live update works atomically`() {
        val fixture = createFixture(itemCount = 20)
        var currentLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val intro = assertIs<AndroidStudyState.Introduction>(started)

        currentLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 30)
        val updatedState = facade.updateDailyLimits(intro, newLimit = 20, reviewLimit = 30)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hud = assertNotNull(updatedIntro.hud)

        assertEquals(20, hud.newConfiguredTarget)
        assertEquals(30, hud.reviewConfiguredTarget)
    }

    @Test
    fun `LEARN_NEW mode preserves Introduction flow across multiple subsequent items after live New Limit increase`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Complete 2 new items
        for (i in 1..2) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Live increase New Limit from 5 to 15
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val updated = facade.updateDailyLimits(assertIs<AndroidStudyState.Introduction>(current), newLimit = 15, reviewLimit = 100)
        current = assertIs<AndroidStudyState.Introduction>(updated)

        // Next 5 items MUST ALL BE Introduction state (never Typing or recall plan)
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(current, "Item $i after limit increase must be Introduction")
            assertEquals(StudyMode.LEARN_NEW, facade.loadExact(intro.sessionId).let {
                val session = fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(intro.sessionId))!!
                session.studyMode
            })
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }
    }

    @Test
    fun `LEARN_NEW mode does not inject review items when live review limit is changed`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val initialIntro = assertIs<AndroidStudyState.Introduction>(started)
        val session = fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(initialIntro.sessionId))!!
        assertEquals(0, session.policy.reviewItemLimit)

        // Change review limit live from 100 to 500
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 500)
        val updated = facade.updateDailyLimits(initialIntro, newLimit = 10, reviewLimit = 500)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updated)

        val updatedSession = fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(updatedIntro.sessionId))!!
        assertEquals(0, updatedSession.policy.reviewItemLimit, "Active LEARN_NEW review quota must strictly remain 0")
        assertEquals(StudyMode.LEARN_NEW, updatedSession.studyMode)
    }

    @Test
    fun `settings limit increase recomputes canLearnNew immediately on fresh home query`() {
        val fixture = createFixture(itemCount = 10)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(2, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        // Start session and complete 2 new items (exhausting daily new limit of 2)
        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..2) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Daily new budget is now exhausted
        val homeExhausted = facade.home()
        assertFalse(homeExhausted.availability.canLearnNew)

        // User increases daily new limit in settings from 2 to 10
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100)
        val homeRefreshed = facade.home()
        assertTrue(homeRefreshed.availability.canLearnNew, "Fresh home query must reflect updated daily limits immediately")
    }

    @Test
    fun `LEARN_NEW strict projection sequence invariant forbids Recall Typing Listening across live limit update`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Item 1: Introduction -> rate GOOD
        assertIs<AndroidStudyState.Introduction>(current)
        current = facade.rateIntroduction(current, ReviewRating.GOOD)

        // Item 2: Introduction -> reveal -> live limit update from 5 to 10
        val intro2 = assertIs<AndroidStudyState.Introduction>(current)
        val revealed2 = facade.revealIntroduction(intro2)
        assertIs<AndroidStudyState.Introduction>(revealed2)
        assertTrue(revealed2.revealedStage)

        // Update limit while card is revealed: MUST NOT convert to Recall/Typing or fail
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100)
        val updated2 = facade.updateDailyLimits(revealed2, newLimit = 10, reviewLimit = 100)
        assertFalse(updated2 is AndroidStudyState.Failed, "Update limit while card is revealed must never fail with memory plan error")
        val introUpdated2 = assertIs<AndroidStudyState.Introduction>(updated2)

        // Rate item 2
        current = facade.rateIntroduction(introUpdated2, ReviewRating.GOOD)

        // Items 3, 4, 5, 6, 7, 8, 9, 10 must ALL be Introduction
        val projectionTypes = mutableListOf<String>()
        for (i in 3..10) {
            when (current) {
                is AndroidStudyState.Introduction -> projectionTypes.add("Introduction")
                is AndroidStudyState.Typing -> projectionTypes.add("Typing")
                is AndroidStudyState.Listening -> projectionTypes.add("Listening")
                is AndroidStudyState.MultipleChoice -> projectionTypes.add("MultipleChoice")
                is AndroidStudyState.ImageRecall -> projectionTypes.add("ImageRecall")
                is AndroidStudyState.ExampleCompletion -> projectionTypes.add("ExampleCompletion")
                is AndroidStudyState.Completion -> projectionTypes.add("Completion")
                is AndroidStudyState.Failed -> projectionTypes.add("Failed: ${current.message}")
                else -> projectionTypes.add(current::class.java.simpleName)
            }
            val intro = assertIs<AndroidStudyState.Introduction>(current, "Item $i must be Introduction, but got $current")
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Verify that only Introduction was emitted
        assertEquals(List(8) { "Introduction" }, projectionTypes)
        assertIs<AndroidStudyState.Completion>(current)
    }

    @Test
    fun `LEARN_NEW decrease New Limit adjusts remaining capacity safely without corrupting session`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        for (i in 1..3) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Decrease limit from 15 to 4 (1 item remaining)
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(4, 100)
        val updated = facade.updateDailyLimits(assertIs<AndroidStudyState.Introduction>(current), newLimit = 4, reviewLimit = 100)
        val intro4 = assertIs<AndroidStudyState.Introduction>(updated)
        assertEquals(4, intro4.hud?.newConfiguredTarget)
        assertEquals(3, intro4.hud?.newCompleted)

        // Complete 4th item -> session completes
        val finalState = facade.rateIntroduction(intro4, ReviewRating.GOOD)
        val completion = assertIs<AndroidStudyState.Completion>(finalState)
        assertEquals(4, completion.newCompleted)
    }

    @Test
    fun `repeated live limit updates do not create mixed mode queue`() {
        val fixture = createFixture(itemCount = 30)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Repeatedly change limits: 5 -> 10 -> 8 -> 12
        val limits = listOf(10, 8, 12)
        for (lim in limits) {
            currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(lim, 100)
            val updated = facade.updateDailyLimits(assertIs<AndroidStudyState.Introduction>(current), newLimit = lim, reviewLimit = 100)
            current = assertIs<AndroidStudyState.Introduction>(updated)
        }

        val sessionId = vn.loi.learning.domain.study.session.model.SessionId((current as AndroidStudyState.Introduction).sessionId)
        val queue = fixture.context.studyQueue.get(sessionId)!!
        assertTrue(queue.itemOrigins.values.all { it == vn.loi.learning.domain.study.session.model.SessionItemOrigin.NEW }, "Queue must contain ONLY NEW items")
        assertEquals(0, queue.configuredReviewTarget)
        assertEquals(0, queue.effectiveReviewWorkload)
    }

    @Test
    fun `REVIEW and TYPING explicit modes maintain their invariants across limit updates`() {
        val fixture = createFixture(itemCount = 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100) }
        )

        for (i in 0 until 5) {
            fixture.context.engine.review(
                vn.loi.learning.application.review.ReviewCommand(
                    vn.loi.learning.domain.study.memory.model.ReviewEventId("seed-eff-$i"),
                    learnerId,
                    LearningItemId("opd-2nd-pkg-item-$i"),
                    ReviewRating.GOOD,
                    vn.loi.learning.domain.study.memory.model.Moment(1_000L)
                )
            )
        }

        // Start TYPING session
        val startedTyping = facade.start(AndroidSessionEntry.REVIEW, StudyMode.TYPING)
        val typingState = assertIs<AndroidStudyState.Typing>(startedTyping)
        val session = fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(typingState.plan.sessionId.value))!!
        assertEquals(0, session.policy.newItemLimit, "TYPING session must have newItemLimit = 0")

        // Update limits on TYPING session: new items must remain 0
        val updatedTyping = facade.updateDailyLimits(typingState, newLimit = 50, reviewLimit = 80)
        val updatedTypingRuntime = assertIs<AndroidStudyState.Typing>(updatedTyping)
        val updatedSession = fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(updatedTypingRuntime.plan.sessionId.value))!!
        assertEquals(0, updatedSession.policy.newItemLimit, "TYPING session must keep newItemLimit = 0 even if newLimit was passed")
        assertEquals(80, updatedSession.policy.reviewItemLimit)
    }

    @Test
    fun `passive limit replanning does not mutate daily counters or review events`() {
        val fixture = createFixture(itemCount = 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100) }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val intro = assertIs<AndroidStudyState.Introduction>(started)

        val beforeBudget = facade.home().availability
        facade.updateDailyLimits(intro, newLimit = 15, reviewLimit = 100)
        val afterBudget = facade.home().availability

        // Daily budget availability should remain accurate and consistent
        assertTrue(afterBudget.canLearnNew)
    }

    @Test
    fun `exact reproduction sequence start LEARN_NEW, reveal A, update New Limit, assert state consistency, rate A, next B Introduction`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val savedState = androidx.lifecycle.SavedStateHandle()
        val viewModel = AndroidStudyViewModel(
            facade = facade,
            savedState = savedState,
            onDailyLimitsChanged = { newLim, revLim ->
                currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(newLim, revLim)
                true
            }
        )

        // 1. Start LEARN_NEW -> load Introduction item A
        val started = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
        }
        val introA = assertIs<AndroidStudyState.Introduction>(started)
        val itemA = introA.learningItemId
        val sessionId = vn.loi.learning.domain.study.session.model.SessionId(introA.sessionId)
        assertFalse(introA.revealedStage)

        // 2. Reveal item A
        val revealedState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.RevealIntroduction)
        }
        val revealedA = assertIs<AndroidStudyState.Introduction>(revealedState)
        assertEquals(itemA, revealedA.learningItemId)
        assertTrue(revealedA.revealedStage)

        // 3. Update New Limit while A remains displayed
        val updatedState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.UpdateDailyLimits(10, 100))
        }
        assertFalse(updatedState is AndroidStudyState.Failed, "Update limit must not fail")
        val updatedA = assertIs<AndroidStudyState.Introduction>(updatedState)

        // 4. Assert UI / currentItem / session / queue all still point to item A with revealed state preserved
        assertEquals(itemA, updatedA.learningItemId, "UI state must still point to item A")
        assertTrue(updatedA.revealedStage, "UI state must preserve revealedStage = true")
        assertEquals(10, updatedA.hud?.newConfiguredTarget)

        val persistedSession = fixture.context.engine.getSession(sessionId)!!
        assertEquals(itemA, persistedSession.currentLearningItemId?.value, "Session currentLearningItemId must be item A")
        assertTrue(persistedSession.answerRevealed, "Session answerRevealed must be true")
        assertEquals(10, persistedSession.policy.newItemLimit)

        val persistedQueue = fixture.context.studyQueue.get(sessionId)!!
        assertEquals(itemA, persistedQueue.currentLearningItemId?.value, "Queue currentLearningItemId must be item A")
        assertEquals(0, persistedQueue.currentIndex, "Queue currentIndex must be 0 (uncompleted item A)")

        // 5. Rate item A -> assert commit succeeds
        val nextState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        }
        assertFalse(nextState is AndroidStudyState.Failed, "RateIntroduction after limit update must not fail with: ${(nextState as? AndroidStudyState.Failed)?.message}")

        // 6. Next item B -> assert B is Introduction (not Recall/Typing/Failed)
        val introB = assertIs<AndroidStudyState.Introduction>(nextState)
        val itemB = introB.learningItemId
        assertFalse(itemA == itemB, "Next item must be different from item A")
        assertFalse(introB.revealedStage, "Item B must start unrevealed")
    }

    @Test
    fun `update New Limit BEFORE reveal preserves unrevealed item A and permits smooth reveal and rate`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )
        val viewModel = AndroidStudyViewModel(
            facade = facade,
            savedState = androidx.lifecycle.SavedStateHandle(),
            onDailyLimitsChanged = { newLim, revLim ->
                currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(newLim, revLim)
                true
            }
        )

        // Start LEARN_NEW -> Item A unrevealed
        val started = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
        }
        val introA = assertIs<AndroidStudyState.Introduction>(started)
        val itemA = introA.learningItemId
        assertFalse(introA.revealedStage)

        // Update limit BEFORE reveal
        val updatedState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.UpdateDailyLimits(15, 100))
        }
        val updatedA = assertIs<AndroidStudyState.Introduction>(updatedState)
        assertEquals(itemA, updatedA.learningItemId)
        assertFalse(updatedA.revealedStage, "Item A must remain unrevealed after limit update")

        // Now reveal item A
        val revealedState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.RevealIntroduction)
        }
        val revealedA = assertIs<AndroidStudyState.Introduction>(revealedState)
        assertEquals(itemA, revealedA.learningItemId)
        assertTrue(revealedA.revealedStage)

        // Rate item A -> succeeds smoothly to item B
        val nextState = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        }
        val introB = assertIs<AndroidStudyState.Introduction>(nextState)
        assertFalse(itemA == introB.learningItemId)
    }

    @Test
    fun `repeated live edits on the same current item keep item stable across all edits`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )
        val viewModel = AndroidStudyViewModel(
            facade = facade,
            savedState = androidx.lifecycle.SavedStateHandle(),
            onDailyLimitsChanged = { newLim, revLim ->
                currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(newLim, revLim)
                true
            }
        )

        val started = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
        }
        val introA = assertIs<AndroidStudyState.Introduction>(started)
        val itemA = introA.learningItemId
        val sessionId = vn.loi.learning.domain.study.session.model.SessionId(introA.sessionId)

        // Reveal item A
        runBlocking { viewModel.executeEventSync(AndroidStudyEvent.RevealIntroduction) }

        // Multiple rapid edits on same item A: 5 -> 12 -> 8 -> 20
        val targetLimits = listOf(12, 8, 20)
        for (target in targetLimits) {
            val edited = runBlocking {
                viewModel.executeEventSync(AndroidStudyEvent.UpdateDailyLimits(target, 100))
            }
            val intro = assertIs<AndroidStudyState.Introduction>(edited)
            assertEquals(itemA, intro.learningItemId, "Item must remain item A across edits")
            assertTrue(intro.revealedStage, "Item A must remain revealed")
            assertEquals(target, intro.hud?.newConfiguredTarget)
        }

        // Final rate on item A
        val finalRate = runBlocking {
            viewModel.executeEventSync(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        }
        assertFalse(finalRate is AndroidStudyState.Failed)
        val introB = assertIs<AndroidStudyState.Introduction>(finalRate)
        assertFalse(itemA == introB.learningItemId)
    }

    private fun createFixture(itemCount: Int): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val installedId = install(context, "opd-2nd-pkg", itemCount)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installedId)
        return Fixture(context, installedId)
    }

    private fun install(
        context: vn.loi.learning.infrastructure.LearningApplicationContext,
        name: String,
        count: Int = 1
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = if (count == 1) "" else "-$index"
            val contentId = ContentId("$name-content$suffix")
            context.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("$name$suffix", "$name-answer$suffix"))
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$name-item$suffix"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }
        val contentLibraryId = vn.loi.learning.domain.content.library.model.ContentLibraryId("$name-library")
        val packageId = PackageId(name)
        val installedId = InstalledPackageId(name)
        context.contentLibraryRepository!!.save(
            vn.loi.learning.domain.content.library.model.ContentLibrary(
                contentLibraryId,
                vn.loi.learning.domain.content.library.model.LibraryDescriptor(name),
                contentIds
            )
        )
        context.contentPackageRepository!!.save(
            vn.loi.learning.domain.content.packaging.model.ContentPackage(
                packageId,
                vn.loi.learning.domain.content.packaging.model.PackageDescriptor(name, "1.0.0", "OPD3"),
                setOf(contentLibraryId)
            )
        )
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
                PackageState.ACTIVE, java.time.Instant.EPOCH, count, count
            )
        )
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, java.time.Instant.EPOCH))
        return installedId
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val packageId: InstalledPackageId
    )
}
