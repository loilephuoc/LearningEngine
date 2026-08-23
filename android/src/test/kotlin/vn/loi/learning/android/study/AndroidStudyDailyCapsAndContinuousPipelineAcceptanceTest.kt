package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyDailyCapsAndContinuousPipelineAcceptanceTest {

    private val learnerId = LearnerId("default-learner")

    // ==========================================
    // 1. DAILY NEW CAP & QUOTA TESTS
    // ==========================================

    @Test
    fun `test 1 daily New Limit = 10, completedToday = 0 admits exactly 10 NEW items`() {
        val fixture = createFixture(itemCount = 20)
        var continuousSkim = false
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100) },
            continuousSkimEnabled = { continuousSkim }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        var newCompleted = 0
        while (current is AndroidStudyState.Introduction) {
            newCompleted++
            current = facade.rateIntroduction(current, ReviewRating.GOOD)
        }

        assertEquals(10, newCompleted, "Must admit exactly 10 NEW items when daily limit is 10")
        assertIs<AndroidStudyState.Completion>(current)
    }

    @Test
    fun `test 2 & 3 completedToday = 9 admits only 1 more NEW, completedToday = 10 admits 0 NEW`() {
        val fixture = createFixture(itemCount = 20)
        var continuousSkim = false
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100) },
            continuousSkimEnabled = { continuousSkim }
        )

        // Session 1: complete 9 items
        val session1 = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(session1)
        for (i in 1..9) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }
        // Leave session 1
        fixture.context.engine.leaveActiveStudySession(learnerId, Moment(10_000L))

        // Session 2: start again -> should admit only 1 more NEW item
        val session2 = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val intro2 = assertIs<AndroidStudyState.Introduction>(session2)
        val afterRate1 = facade.rateIntroduction(intro2, ReviewRating.GOOD)
        assertIs<AndroidStudyState.Completion>(afterRate1)

        // Session 3: daily new limit reached -> cannot start new items
        val homeState = facade.home()
        assertFalse(homeState.availability.canLearnNew, "canLearnNew must be false when daily cap reached and Continuous Skim is OFF")
    }

    @Test
    fun `test 4 completedToday = 12, New Limit lowered to 10 admits zero NEW without mutating history`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { currentDailyLimits },
            continuousSkimEnabled = { false }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..12) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }
        fixture.context.engine.leaveActiveStudySession(learnerId, Moment(10_000L))

        // Lower limit from 15 to 10
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100)

        val home = facade.home()
        assertEquals(12, home.model.dailyBudget?.newCompletedToday)
        assertEquals(0, home.model.dailyBudget?.newRemainingToday)
        assertFalse(home.availability.canLearnNew)
    }

    @Test
    fun `test 5 live change completedToday = 7, limit lowered 20 to 10 allows only 3 additional NEW`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { currentDailyLimits },
            continuousSkimEnabled = { false }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..7) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Live update: 20 -> 10 (completed 7, so remaining = 3)
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 100)
        current = facade.updateDailyLimits(assertIs<AndroidStudyState.Introduction>(current), newLimit = 10, reviewLimit = 100)

        var additionalCompleted = 0
        while (current is AndroidStudyState.Introduction) {
            additionalCompleted++
            current = facade.rateIntroduction(current, ReviewRating.GOOD)
        }

        assertEquals(3, additionalCompleted, "Must admit only 3 additional NEW items after lowering limit to 10")
        assertIs<AndroidStudyState.Completion>(current)
    }

    @Test
    fun `test 6 live change completedToday = 7, limit lowered 20 to 5 finishes current card safely and admits zero more NEW`() {
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { currentDailyLimits },
            continuousSkimEnabled = { false }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..7) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val card8 = assertIs<AndroidStudyState.Introduction>(current)
        val item8Id = card8.learningItemId

        // Live update: 20 -> 8 (already completed 7, card 8 is on screen)
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(8, 100)
        val afterUpdate = facade.updateDailyLimits(card8, newLimit = 8, reviewLimit = 100)
        val introAfterUpdate = assertIs<AndroidStudyState.Introduction>(afterUpdate)
        assertEquals(item8Id, introAfterUpdate.learningItemId, "Card 8 must be preserved on screen")

        // Rate card 8 -> session must complete immediately without card 9
        val afterRate = facade.rateIntroduction(introAfterUpdate, ReviewRating.GOOD)
        assertIs<AndroidStudyState.Completion>(afterRate)
    }

    @Test
    fun `multi-session live increase from 39 completed to daily limit 45 admits exactly six NEW`() {
        val fixture = createFixture(itemCount = 60)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(50, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { currentDailyLimits },
            continuousSkimEnabled = { false }
        )

        var previousSession: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        repeat(39) {
            previousSession = facade.rateIntroduction(
                assertIs<AndroidStudyState.Introduction>(previousSession),
                ReviewRating.GOOD
            )
        }
        fixture.context.engine.leaveActiveStudySession(learnerId, Moment(10_000L))

        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(45, 100)
        current = facade.updateDailyLimits(
            assertIs<AndroidStudyState.Introduction>(current),
            newLimit = 45,
            reviewLimit = 100
        )

        var additionalNew = 0
        while (current is AndroidStudyState.Introduction) {
            assertEquals(SessionItemOrigin.NEW, current.origin)
            additionalNew++
            current = facade.rateIntroduction(current, ReviewRating.GOOD)
        }

        assertEquals(6, additionalNew)
        assertIs<AndroidStudyState.Completion>(current)
        val daily = facade.home().model.dailyBudget
        assertEquals(45, daily?.newCompletedToday)
        assertEquals(0, daily?.newRemainingToday)
    }

    @Test
    fun `multi-session review completions leave only remaining daily Review capacity`() {
        val fixture = createFixture(itemCount = 10)
        val items = fixture.context.learningItemRepository!!.findAllEnabled()
        repeat(39) { index ->
            val item = items[index % items.size]
            val before = MemoryState(
                learnerId = learnerId,
                learningItemId = item.id,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 2.5,
                dueAt = Moment(1_000L),
                lastReviewedAt = Moment(500L),
                reviewCount = 1,
                lapseCount = 0
            )
            val after = before.copy(lastReviewedAt = Moment(1_000L), dueAt = Moment(2_000L), reviewCount = 2)
            fixture.context.reviewEventRepository!!.append(
                ReviewEvent(
                    id = ReviewEventId("previous-review-$index"),
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(1_000L),
                    responseTime = null,
                    stateBefore = before,
                    stateAfter = after
                )
            )
            fixture.context.memoryStateRepository!!.save(after)
        }
        var limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(1, 50)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 100_000L },
            dailyLimits = { limits },
            continuousSkimEnabled = { false }
        )

        val started = assertIs<AndroidStudyState.Runtime>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE)
        )
        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(1, 45)
        val updated = assertIs<AndroidStudyState.Runtime>(facade.updateDailyLimits(started, 1, 45))
        val sessionId = requireNotNull(updated.plan?.sessionId)
        val session = requireNotNull(fixture.context.engine.getSession(sessionId))
        val queue = requireNotNull(fixture.context.studyQueue.get(sessionId))

        assertEquals(39, facade.home().model.dailyBudget?.reviewCompletedToday)
        assertEquals(6, session.policy.reviewItemLimit)
        assertEquals(6, queue.itemOrigins.values.count { it == SessionItemOrigin.REVIEW })
    }

    // ==========================================
    // 2. CONTINUOUS PIPELINE: NEW -> DUE -> SKIM
    // ==========================================

    @Test
    fun `test 8 New Limit=10, Review Limit=20, due=0, Continuous Skim=ON transitions NEW to SKIM directly`() {
        val fixture = createFixture(itemCount = 15)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20) },
            continuousSkimEnabled = { true }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = facade.refreshHud(assertIs<AndroidStudyState.Introduction>(started))

        for (i in 1..10) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            assertEquals("Học từ mới", intro.hud?.skimStatus)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Card 11 should be in SKIM phase!
        val skimCard = assertIs<AndroidStudyState.Introduction>(current)
        assertTrue(skimCard.hud?.skimStatus?.startsWith("Ôn lướt") == true, "Must transition to Skim with status 'Ôn lướt'")
        assertEquals(10, skimCard.hud?.newCompleted)
        assertEquals(10, skimCard.hud?.newConfiguredTarget)
    }

    @Test
    fun `test 9 New Limit=10, Review Limit=20, due=5 transitions 10 NEW to 5 DUE to SKIM`() {
        val fixture = createFixture(itemCount = 20)
        // Simulate 5 due items by creating memory states with past due dates
        val contentIds = fixture.context.learningItemRepository!!.findAllEnabled().take(5)
        contentIds.forEach { item ->
            val stateBefore = MemoryState(
                learnerId = learnerId,
                learningItemId = item.id,
                stage = LearningStage.NEW,
                difficulty = 5.0,
                stabilityDays = 2.5,
                dueAt = Moment(1_000L),
                lastReviewedAt = null,
                reviewCount = 0,
                lapseCount = 0
            )
            val stateAfter = MemoryState(
                learnerId = learnerId,
                learningItemId = item.id,
                stage = LearningStage.REVIEW,
                difficulty = 5.0,
                stabilityDays = 2.5,
                dueAt = Moment(2_000L),
                lastReviewedAt = Moment(1_000L),
                reviewCount = 1,
                lapseCount = 0
            )
            val event = ReviewEvent(
                id = ReviewEventId("event-${item.id.value}"),
                rating = ReviewRating.GOOD,
                reviewedAt = Moment(1_000L),
                responseTime = null,
                stateBefore = stateBefore,
                stateAfter = stateAfter
            )
            fixture.context.reviewEventRepository!!.append(event)
            fixture.context.memoryStateRepository!!.save(stateAfter)
        }

        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 100_000L }, // now > dueAt so items are due
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20) },
            continuousSkimEnabled = { true }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = facade.refreshHud(assertIs<AndroidStudyState.Introduction>(started))

        // 5 NEW items were completed earlier today, so remaining NEW quota is 10 - 5 = 5
        // Complete remaining 5 NEW items
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            assertEquals("Học từ mới", intro.hud?.skimStatus)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Complete 5 DUE items
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            assertEquals("Ôn tập đến hạn", intro.hud?.skimStatus)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Next item must be SKIM
        val skimIntro = assertIs<AndroidStudyState.Introduction>(current)
        assertTrue(skimIntro.hud?.skimStatus?.startsWith("Ôn lướt") == true)
        assertEquals(10, skimIntro.hud?.newCompleted)
        assertEquals(5, skimIntro.hud?.reviewCompleted)
    }

    // ==========================================
    // 3. SKIM PRESENTATION & QUOTA IMMUTABILITY
    // ==========================================

    @Test
    fun `test 14, 15, 16, 17, 18 SKIM is Introduction-style, never Typing or Recall, and never mutates daily quotas`() {
        val fixture = createFixture(itemCount = 10)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 20) },
            continuousSkimEnabled = { true }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Complete 5 NEW items
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // Now in SKIM: view and rate 20 skim cards
        val eventsCountBefore = fixture.context.reviewEventRepository!!.findAll(learnerId).size
        assertEquals(5, eventsCountBefore)

        for (i in 1..20) {
            val skimIntro = assertIs<AndroidStudyState.Introduction>(current, "Skim cards must always be Introduction style")
            assertTrue(skimIntro.hud?.skimStatus?.startsWith("Ôn lướt") == true)
            assertEquals(5, skimIntro.hud?.newCompleted, "Daily New counter must stay at 5 during Skim")

            // Reveal then rate
            val revealed = facade.revealIntroduction(skimIntro)
            val revealedIntro = assertIs<AndroidStudyState.Introduction>(revealed)
            assertTrue(revealedIntro.revealed)
            current = facade.rateIntroduction(revealedIntro, ReviewRating.GOOD)
        }

        // Review events must not have grown from Skim cards
        val eventsCountAfter = fixture.context.reviewEventRepository!!.findAll(learnerId).size
        assertEquals(5, eventsCountAfter, "Skim must not record review events or mutate daily counters")
    }

    @Test
    fun `test 21 & 22 Continuous Skim ON vs OFF behavior`() {
        val fixture = createFixture(itemCount = 10)
        var continuousSkim = true
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 20) },
            continuousSkimEnabled = { continuousSkim }
        )

        // When ON: completes 5 NEW and transitions to SKIM
        val startedOn = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var currentOn: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(startedOn)
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(currentOn)
            currentOn = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }
        val introOn = assertIs<AndroidStudyState.Introduction>(currentOn)
        assertTrue(introOn.hud?.skimStatus?.startsWith("Ôn lướt") == true)
        fixture.context.engine.leaveActiveStudySession(learnerId, Moment(10_000L))

        // When OFF: completes 5 NEW and transitions to Completion
        continuousSkim = false
        val fixture2 = createFixture(itemCount = 10)
        val facadeOff = AndroidStudyFacade(
            context = fixture2.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 20) },
            continuousSkimEnabled = { false }
        )
        val startedOff = facadeOff.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var currentOff: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(startedOff)
        for (i in 1..5) {
            val intro = assertIs<AndroidStudyState.Introduction>(currentOff)
            currentOff = facadeOff.rateIntroduction(intro, ReviewRating.GOOD)
        }
        assertIs<AndroidStudyState.Completion>(currentOff, "Continuous Skim OFF must complete session when quota is exhausted")
    }

    @Test
    fun `live increase while SKIM is active preserves current card then reopens NEW`() {
        val fixture = createFixture(itemCount = 15)
        var limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(5, 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 10_000L },
            dailyLimits = { limits },
            continuousSkimEnabled = { true }
        )

        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        repeat(5) {
            current = facade.rateIntroduction(assertIs<AndroidStudyState.Introduction>(current), ReviewRating.GOOD)
        }
        val skim = assertIs<AndroidStudyState.Introduction>(current)
        assertEquals(vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy.PRACTICE_ONLY, skim.runtimeIdentity?.evaluationPolicy)

        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20)
        val updated = facade.updateDailyLimits(skim, 10, 20)
        val preserved = assertIs<AndroidStudyState.Introduction>(updated)
        assertEquals(skim.learningItemId, preserved.learningItemId)

        val reopened = assertIs<AndroidStudyState.Introduction>(
            facade.rateIntroduction(preserved, ReviewRating.GOOD)
        )
        assertEquals(SessionItemOrigin.NEW, reopened.origin)
        assertEquals(vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy.EVALUATIVE, reopened.runtimeIdentity?.evaluationPolicy)
    }

    @Test
    fun `live increase while DUE is active preserves current DUE then reopens NEW without losing remaining DUE`() {
        val fixture = createFixture(itemCount = 15)
        seedDueItems(fixture.context, count = 5)
        var limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 100_000L },
            dailyLimits = { limits },
            continuousSkimEnabled = { true }
        )

        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        repeat(5) {
            current = facade.rateIntroduction(assertIs<AndroidStudyState.Introduction>(current), ReviewRating.GOOD)
        }
        val due = assertIs<AndroidStudyState.Introduction>(current)
        assertEquals(SessionItemOrigin.REVIEW, due.origin)

        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 20)
        val preserved = assertIs<AndroidStudyState.Introduction>(facade.updateDailyLimits(due, 15, 20))
        assertEquals(due.learningItemId, preserved.learningItemId)

        current = facade.rateIntroduction(preserved, ReviewRating.GOOD)
        repeat(5) {
            val fresh = assertIs<AndroidStudyState.Introduction>(current)
            assertEquals(SessionItemOrigin.NEW, fresh.origin)
            current = facade.rateIntroduction(fresh, ReviewRating.GOOD)
        }
        val returnedDue = assertIs<AndroidStudyState.Introduction>(current)
        assertEquals(SessionItemOrigin.REVIEW, returnedDue.origin)
        assertEquals(15, returnedDue.hud?.newCompleted)
    }

    @Test
    fun `repeated increase after DUE reentry admits only the new daily remainder without duplicates`() {
        val fixture = createFixture(itemCount = 30)
        seedDueItems(fixture.context, count = 5)
        var limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 100_000L },
            dailyLimits = { limits },
            continuousSkimEnabled = { true }
        )
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        repeat(5) { current = facade.rateIntroduction(assertIs<AndroidStudyState.Introduction>(current), ReviewRating.GOOD) }
        val due = assertIs<AndroidStudyState.Introduction>(current)
        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 20)
        current = facade.rateIntroduction(
            assertIs<AndroidStudyState.Introduction>(facade.updateDailyLimits(due, 15, 20)),
            ReviewRating.GOOD
        )
        repeat(2) { current = facade.rateIntroduction(assertIs<AndroidStudyState.Introduction>(current), ReviewRating.GOOD) }

        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 20)
        current = facade.updateDailyLimits(assertIs<AndroidStudyState.Introduction>(current), 20, 20)
        val seen = linkedSetOf<String>()
        repeat(8) {
            val fresh = assertIs<AndroidStudyState.Introduction>(current)
            assertEquals(SessionItemOrigin.NEW, fresh.origin)
            assertTrue(seen.add(fresh.learningItemId), "Repeated increases must not duplicate NEW membership")
            current = facade.rateIntroduction(fresh, ReviewRating.GOOD)
        }
        val daily = facade.home().model.dailyBudget
        assertEquals(20, daily?.newCompletedToday)
        assertEquals(0, daily?.newRemainingToday)
    }

    @Test
    fun `live decrease in DUE preserves phase and does not reopen NEW`() {
        val fixture = createFixture(itemCount = 15)
        seedDueItems(fixture.context, count = 5)
        var limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(10, 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 100_000L },
            dailyLimits = { limits },
            continuousSkimEnabled = { true }
        )
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(
            facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        )
        repeat(5) { current = facade.rateIntroduction(assertIs<AndroidStudyState.Introduction>(current), ReviewRating.GOOD) }
        val due = assertIs<AndroidStudyState.Introduction>(current)
        limits = vn.loi.learning.application.study.DailyStudyBudgetLimits(8, 20)
        val preserved = assertIs<AndroidStudyState.Introduction>(facade.updateDailyLimits(due, 8, 20))
        assertEquals(due.learningItemId, preserved.learningItemId)

        val next = assertIs<AndroidStudyState.Introduction>(facade.rateIntroduction(preserved, ReviewRating.GOOD))
        assertEquals(SessionItemOrigin.REVIEW, next.origin)
        assertEquals(due.sessionId, next.sessionId)
    }

    private fun createFixture(itemCount: Int): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val installedId = install(context, "opd-pkg", itemCount)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installedId)
        return Fixture(context, installedId)
    }

    private fun seedDueItems(context: LearningApplicationContext, count: Int) {
        context.learningItemRepository!!.findAllEnabled().take(count).forEachIndexed { index, item ->
            val before = MemoryState(
                learnerId = learnerId,
                learningItemId = item.id,
                stage = LearningStage.NEW,
                difficulty = 5.0,
                stabilityDays = 2.5,
                dueAt = Moment(1_000L),
                lastReviewedAt = null,
                reviewCount = 0,
                lapseCount = 0
            )
            val after = before.copy(
                stage = LearningStage.REVIEW,
                dueAt = Moment(2_000L),
                lastReviewedAt = Moment(1_000L),
                reviewCount = 1
            )
            context.reviewEventRepository!!.append(
                ReviewEvent(
                    id = ReviewEventId("reentry-due-$index"),
                    rating = ReviewRating.GOOD,
                    reviewedAt = Moment(1_000L),
                    responseTime = null,
                    stateBefore = before,
                    stateAfter = after
                )
            )
            context.memoryStateRepository!!.save(after)
        }
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
        val contentLibraryId = vn.loi.learning.domain.content.library.model.ContentLibraryId(name)
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
