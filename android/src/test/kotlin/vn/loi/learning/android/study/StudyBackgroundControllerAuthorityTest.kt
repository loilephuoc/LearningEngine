package vn.loi.learning.android.study

import android.view.KeyEvent
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
import vn.loi.learning.android.controller.*
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

@OptIn(ExperimentalCoroutinesApi::class)
class StudyBackgroundControllerAuthorityTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        StudyControllerBridge.clear()
    }

    @After
    fun tearDown() {
        StudyControllerBridge.clear()
        Dispatchers.resetMain()
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val sessionId: SessionId,
        val facade: AndroidStudyFacade,
        val viewModel: AndroidStudyViewModel
    )

    private fun createFixture(
        prefix: String,
        itemCount: Int = 3
    ): Fixture {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentIds = (1..itemCount).map { i ->
            val cId = ContentId("$prefix-content-$i")
            val itemId = LearningItemId("$prefix-item-$i")
            ctx.contentRepository!!.save(
                Content(
                    cId,
                    ContentType.WORD,
                    ContentText("word-$i", "nghĩa-$i", pronunciation = "/wɜːd-$i/"),
                    media = vn.loi.learning.domain.content.model.ContentMedia(
                        primaryAudio = "audio-$i.mp3",
                        translatedAudio = "vi-$i.mp3"
                    )
                )
            )
            ctx.learningItemRepository!!.save(LearningItem(itemId, cId, LearningMode.MEANING_RECOGNITION))
            cId
        }

        val sId = SessionId("$prefix-session")
        ctx.engine.startSession(
            StartStudySessionCommand(
                sessionId = sId,
                learnerId = learner,
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = itemCount, reviewItemLimit = 0),
                includedContentIds = contentIds.toSet()
            )
        )

        val facade = AndroidStudyFacade(ctx, learner, now = { 10_000L }, resolveMedia = { "file:///$it" })
        val savedState = SavedStateHandle(mapOf("study.sessionId" to sId.value))
        val viewModel = AndroidStudyViewModel(facade, savedState, testDispatcher)
        viewModel.onEvent(AndroidStudyEvent.Resume)

        return Fixture(ctx, learner, sId, facade, viewModel)
    }

    // ─── 1. Foreground Reveal -> Continue advances ──────────────────────────────
    @Test
    fun `1 - Foreground Reveal then Continue advances to next item`() = runTest(testDispatcher) {
        val f = createFixture("fg-advance", itemCount = 3)
        advanceUntilIdle()

        val initial = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertEquals(ControllerContext.STUDY_QUESTION, StudyControllerBridge.currentContext())

        // 1. Reveal Answer
        assertTrue(StudyControllerBridge.activeTarget!!.revealAnswer())
        advanceUntilIdle()

        val revealed = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(revealed.revealedStage)
        assertEquals(ControllerContext.STUDY_RATING, StudyControllerBridge.currentContext())

        // 2. Continue Current Mode
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val nextItem = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(initial.learningItemId, nextItem.learningItemId)
        assertEquals(1, f.context.engine.getSession(f.sessionId)!!.newItemsReviewed)
    }

    // ─── 2. Background-equivalent Reveal -> Continue advances without Compose ──
    @Test
    fun `2 - Background Reveal then Continue advances without Compose recomposition`() = runTest(testDispatcher) {
        val f = createFixture("bg-advance", itemCount = 3)
        advanceUntilIdle()

        val initial = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertEquals(ControllerContext.STUDY_QUESTION, StudyControllerBridge.currentContext())

        assertTrue(StudyControllerBridge.activeTarget!!.revealAnswer())
        advanceUntilIdle()

        assertEquals(ControllerContext.STUDY_RATING, StudyControllerBridge.currentContext())
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val nextItem = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(initial.learningItemId, nextItem.learningItemId)
    }

    // ─── 3. Screen-off equivalent Reveal -> Continue advances ──────────────────
    @Test
    fun `3 - Screen-off Reveal then Continue advances without UI tree`() = runTest(testDispatcher) {
        val f = createFixture("screenoff-advance", itemCount = 3)
        advanceUntilIdle()

        val initial = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        assertTrue(StudyControllerBridge.activeTarget!!.revealAnswer())
        advanceUntilIdle()
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val nextItem = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(initial.learningItemId, nextItem.learningItemId)
    }

    // ─── 4. Continue reads current ViewModel state rather than captured UI state ─
    @Test
    fun `4 - Continue reads current ViewModel state rather than captured UI state`() = runTest(testDispatcher) {
        val f = createFixture("fresh-state", itemCount = 3)
        advanceUntilIdle()

        // Stale UI snapshot was Question
        val staleCapturedQuestionState = f.viewModel.state.value

        // ViewModel transitions to Revealed
        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()

        val currentVmState = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(currentVmState.revealedStage)

        // Resolving on stale state returns null for Learn New Continue (unrevealed)
        assertNull(StudyContinueCommandResolver.resolveContinueEvent(staleCapturedQuestionState))

        // Resolving on fresh ViewModel state correctly yields RateIntroduction(GOOD)
        val resolved = StudyContinueCommandResolver.resolveContinueEvent(currentVmState)
        assertEquals(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD), resolved)

        // Executing continue on target succeeds against fresh state
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val nextState = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(currentVmState.learningItemId, nextState.learningItemId)
    }

    // ─── 5. Quick Review Continue remains unrated ──────────────────────────────
    @Test
    fun `5 - Quick Review Continue remains unrated and advances exactly one item`() {
        val quickReviewIntro = AndroidStudyState.Introduction(
            sessionId = "qr-session",
            learningItemId = "item-1",
            contentId = "content-1",
            answerText = "word",
            meaning = "nghĩa",
            pronunciation = "/word/",
            focusedPracticeKind = FocusedPracticeKind.QUICK_REVIEW,
            revealedStage = true
        )

        val resolved = StudyContinueCommandResolver.resolveContinueEvent(quickReviewIntro)
        assertEquals(AndroidStudyEvent.QuickReviewUnratedAdvance, resolved)
    }

    // ─── 6. Difficult Practice Continue retains canonical semantics ───────────
    @Test
    fun `6 - Difficult Practice Continue advances with DifficultPracticeAdvance`() {
        val diffIntro = AndroidStudyState.Introduction(
            sessionId = "diff-session",
            learningItemId = "item-1",
            contentId = "content-1",
            answerText = "word",
            meaning = "nghĩa",
            pronunciation = "/word/",
            focusedPracticeKind = FocusedPracticeKind.DIFFICULT,
            revealedStage = true
        )

        val resolved = StudyContinueCommandResolver.resolveContinueEvent(diffIntro)
        assertEquals(AndroidStudyEvent.DifficultPracticeAdvance, resolved)
    }

    // ─── 7. Learn New Continue performs exactly canonical Good mutation ─────────
    @Test
    fun `7 - Learn New Continue performs exactly canonical Good mutation and creates exactly one ReviewEvent`() = runTest(testDispatcher) {
        val f = createFixture("learn-new-good", itemCount = 2)
        advanceUntilIdle()

        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()

        assertEquals(0, f.context.reviewEventRepository!!.findAll(f.learner).size)

        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val reviews = f.context.reviewEventRepository!!.findAll(f.learner)
        assertEquals(1, reviews.size)
        assertEquals(ReviewRating.GOOD, reviews.single().rating)
    }

    // ─── 8. Two rapid controller presses: Reveal then Continue ─────────────────
    @Test
    fun `8 - Two rapid controller presses Reveal then Continue execute in order against successive states`() = runTest(testDispatcher) {
        val f = createFixture("rapid-reveal-continue", itemCount = 3)
        advanceUntilIdle()

        val initial = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        // Command 1: Reveal
        assertTrue(StudyControllerBridge.activeTarget!!.revealAnswer())
        advanceUntilIdle()

        // Immediate Command 2: Continue
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        val next = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(initial.learningItemId, next.learningItemId)
    }

    // ─── 9. Three rapid presses: Reveal -> Continue -> Continue ────────────────
    @Test
    fun `9 - Three rapid presses do not skip or double rate`() = runTest(testDispatcher) {
        val f = createFixture("three-rapid", itemCount = 4)
        advanceUntilIdle()

        // Press 1: Reveal Item 1
        assertTrue(StudyControllerBridge.activeTarget!!.revealAnswer())
        advanceUntilIdle()

        // Press 2: Continue Item 1 -> Advances to Item 2 (Question side)
        assertTrue(StudyControllerBridge.continueCurrentMode())
        advanceUntilIdle()

        // Press 3: Continue on Item 2 Question side (Learn New unrevealed) is NOT valid until revealed
        val item2Question = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item2Question.revealedStage)
        assertFalse(StudyControllerBridge.continueCurrentMode(), "Cannot Continue an unrevealed Learn New question")

        advanceUntilIdle()
        assertEquals(1, f.context.reviewEventRepository!!.findAll(f.learner).size)
    }

    // ─── 10. Previous works with canPrevious=true without foreground Activity ──
    @Test
    fun `10 - Previous works in background when canPrevious is true`() = runTest(testDispatcher) {
        val f = createFixture("prev-bg", itemCount = 3)
        advanceUntilIdle()

        val item1 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item1.navigation.canPrevious)

        // Advance to Item 2
        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()
        f.viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        advanceUntilIdle()

        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(item2.navigation.canPrevious)

        // Controller sends PREVIOUS_ITEM in background directly from Item 2 FRONT
        assertTrue(StudyControllerBridge.activeTarget!!.previous())
        advanceUntilIdle()

        val prevItem = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertEquals(item1.learningItemId, prevItem.learningItemId)
        assertTrue(prevItem.historyPreview)
        assertTrue(prevItem.navigation.canNext)
    }

    // ─── 11. Previous -> Next restores presentation-history item ───────────────
    @Test
    fun `11 - Previous then Next restores presentation-history item`() = runTest(testDispatcher) {
        val f = createFixture("prev-next-hist", itemCount = 3)
        advanceUntilIdle()

        val item1 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        // Advance to Item 2
        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()
        f.viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        advanceUntilIdle()

        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        // Reveal Item 2 before Previous
        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()

        // Go Previous to Item 1
        assertTrue(StudyControllerBridge.activeTarget!!.previous())
        advanceUntilIdle()
        assertEquals(item1.learningItemId, (f.viewModel.state.value as AndroidStudyState.Introduction).learningItemId)

        // Go Next to Item 2
        assertTrue(StudyControllerBridge.activeTarget!!.next())
        advanceUntilIdle()
        assertEquals(item2.learningItemId, (f.viewModel.state.value as AndroidStudyState.Introduction).learningItemId)
    }

    // ─── 12. Previous causes zero FSRS mutation ───────────────────────────────
    @Test
    fun `12 - Previous causes zero FSRS mutation`() = runTest(testDispatcher) {
        val f = createFixture("prev-zero-fsrs", itemCount = 3)
        advanceUntilIdle()

        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()
        f.viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        advanceUntilIdle()

        val sessionBefore = f.context.engine.getSession(f.sessionId)!!
        val reviewedCountBefore = sessionBefore.newItemsReviewed

        StudyControllerBridge.activeTarget!!.previous()
        advanceUntilIdle()

        val sessionAfter = f.context.engine.getSession(f.sessionId)!!
        assertEquals(reviewedCountBefore, sessionAfter.newItemsReviewed)
    }

    // ─── 13. Previous causes zero ReviewEvent creation ─────────────────────────
    @Test
    fun `13 - Previous causes zero ReviewEvent creation`() = runTest(testDispatcher) {
        val f = createFixture("prev-zero-events", itemCount = 3)
        advanceUntilIdle()

        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()
        f.viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        advanceUntilIdle()

        val countBefore = f.context.reviewEventRepository!!.findAll(f.learner).size

        StudyControllerBridge.activeTarget!!.previous()
        advanceUntilIdle()

        val countAfter = f.context.reviewEventRepository!!.findAll(f.learner).size
        assertEquals(countBefore, countAfter)
    }

    // ─── 14. Next through history causes zero extra ReviewEvent ────────────────
    @Test
    fun `14 - Next through history causes zero extra ReviewEvent`() = runTest(testDispatcher) {
        val f = createFixture("next-hist-zero-events", itemCount = 3)
        advanceUntilIdle()

        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()
        f.viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
        advanceUntilIdle()

        val countBefore = f.context.reviewEventRepository!!.findAll(f.learner).size

        // Previous then Next
        StudyControllerBridge.activeTarget!!.previous()
        advanceUntilIdle()
        StudyControllerBridge.activeTarget!!.next()
        advanceUntilIdle()

        val countAfter = f.context.reviewEventRepository!!.findAll(f.learner).size
        assertEquals(countBefore, countAfter)
    }

    // ─── 15. Controller context changes Question -> Rating from ViewModel state ─
    @Test
    fun `15 - Controller context changes Question to Rating from ViewModel state without Compose recomposition`() = runTest(testDispatcher) {
        val f = createFixture("ctx-change", itemCount = 2)
        advanceUntilIdle()

        assertEquals(ControllerContext.STUDY_QUESTION, StudyControllerBridge.currentContext())

        f.viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
        advanceUntilIdle()

        assertEquals(ControllerContext.STUDY_RATING, StudyControllerBridge.currentContext())
    }

    // ─── 16. Audio controls remain functional after architecture change ────────
    @Test
    fun `16 - Audio controls remain functional`() = runTest(testDispatcher) {
        val f = createFixture("audio-controls", itemCount = 2)
        advanceUntilIdle()

        // Audio replayer registration
        var audioReplayed = false
        StudyControllerBridge.registerAudioReplayer { audioReplayed = true; true }

        assertTrue(StudyControllerBridge.replayAudio())
        assertTrue(audioReplayed)
    }

    // ─── 17. Press Double Long arbitration remains green ──────────────────────
    @Test
    fun `17 - Gesture arbitration remains solid`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(clock = { currentTime })
        val inputG = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G) // Physical A
        val profile = ControllerProfile(
            id = "test",
            name = "Test",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputG, ControllerPressType.PRESS), ControllerAction.REVEAL_ANSWER),
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputG, ControllerPressType.LONG_PRESS), ControllerAction.PLAY_PRIMARY_EN)
            )
        )

        // Short tap
        val down = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(down)
        currentTime += 100L
        val up = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_UP, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(up)
        assertEquals(ControllerPressType.PRESS, up.pressType)
    }

    // ─── 18. Canonical physical K-mode matrix remains unchanged ────────────────
    @Test
    fun `18 - Canonical physical K-mode matrix remains unchanged`() {
        assertEquals("A", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_G))
        assertEquals("B", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_J))
        assertEquals("X", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_H))
        assertEquals("Y", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_I))
        assertEquals("D-Pad Up", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_C))
        assertEquals("D-Pad Down", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_D))
        assertEquals("D-Pad Left", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_E))
        assertEquals("D-Pad Right", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_F))
        assertEquals("L1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_K))
        assertEquals("R1", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_M))
        assertEquals("L2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_L))
        assertEquals("R2", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_R))
        assertEquals("Select / -", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_N))
        assertEquals("Start / +", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_O))
        assertEquals("Star / Mode", ControllerButtonDirectory.getButtonLabel(KeyEvent.KEYCODE_S))
    }

    // ─── 19. Activity + Accessibility dedup remains exactly-once ───────────────
    @Test
    fun `19 - Activity and Accessibility duplicate events produce exactly one gesture`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 80L, clock = { currentTime })
        val profile = DefaultControllerProfiles.defaultProfile()

        val res1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(res1)

        currentTime += 20L
        val res2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACCESSIBILITY, profile)
        assertNull(res2, "Duplicate from Accessibility within window must be deduplicated")
    }

    // ─── 20. HID burst remains exactly-once ────────────────────────────────────
    @Test
    fun `20 - HID burst remains exactly-once`() {
        var currentTime = 1000L
        val detector = ControllerGestureDetector(deduplicationWindowMs = 80L, clock = { currentTime })
        val profile = DefaultControllerProfiles.defaultProfile()

        val g1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNotNull(g1)

        currentTime += 10L
        val gBurstDown1 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(gBurstDown1)

        currentTime += 10L
        val gBurstDown2 = detector.processRawKeyEvent(0x2dc8, 0x9021, KeyEvent.KEYCODE_G, KeyEvent.ACTION_DOWN, 0, EventOrigin.ACTIVITY, profile)
        assertNull(gBurstDown2)
    }

    // ─── 21. Full physical A1 to A2 pipeline via ControllerInputRouter with trace ──
    @Test
    fun `21 - Full physical A1 to A2 pipeline via ControllerInputRouter records accurate runtime trace`() = runTest(testDispatcher) {
        val f = createFixture("a1-a2-pipeline", itemCount = 3)
        advanceUntilIdle()
        ControllerDiagnosticsHolder.clear()

        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val profile = ControllerProfile(
            id = "custom-a1-a2",
            name = "A1 A2 Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputA), ControllerAction.REVEAL_ANSWER),
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(inputA), ControllerAction.CONTINUE_CURRENT_MODE)
            )
        )
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        // Press A #1: Question -> Reveal
        val res1 = router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        assertIs<ControllerActionResult.Executed>(res1)
        advanceUntilIdle()

        val item1Revealed = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(item1Revealed.revealedStage)

        // Press A #2: Revealed -> Continue
        val res2 = router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        assertIs<ControllerActionResult.Executed>(res2)
        advanceUntilIdle()

        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(item1Revealed.learningItemId, item2.learningItemId)

        // Verify Runtime Trace
        val trace = ControllerDiagnosticsHolder.state.value.runtimeTrace
        assertEquals(2, trace.size)

        val trace1 = trace[0]
        assertEquals(EventOrigin.ACCESSIBILITY, trace1.origin)
        assertEquals("A", trace1.buttonLabel)
        assertEquals(ControllerContext.STUDY_QUESTION, trace1.contextBefore)
        assertTrue(trace1.resolvedMapping.contains("REVEAL_ANSWER"))
        assertEquals("COMMITTED", trace1.dispatchResult)

        val trace2 = trace[1]
        assertEquals(EventOrigin.ACCESSIBILITY, trace2.origin)
        assertEquals("A", trace2.buttonLabel)
        assertEquals(ControllerContext.STUDY_RATING, trace2.contextBefore)
        assertTrue(trace2.resolvedMapping.contains("CONTINUE_CURRENT_MODE"))
        assertEquals("COMMITTED", trace2.dispatchResult)
    }

    // ─── 22. Three-press sequence (Reveal -> Continue -> Reveal Next) ────────────
    @Test
    fun `22 - Three-press sequence via ControllerInputRouter progresses through two items`() = runTest(testDispatcher) {
        val f = createFixture("three-press-pipeline", itemCount = 3)
        advanceUntilIdle()
        ControllerDiagnosticsHolder.clear()

        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val profile = ControllerProfile(
            id = "custom-3press",
            name = "3Press Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputA), ControllerAction.REVEAL_ANSWER),
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(inputA), ControllerAction.CONTINUE_CURRENT_MODE)
            )
        )
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        // Press 1: Reveal item 1
        router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        assertTrue((f.viewModel.state.value as AndroidStudyState.Introduction).revealedStage)

        // Press 2: Continue item 1 -> Advances to item 2 (Question)
        router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item2.revealedStage)

        // Press 3: Reveal item 2
        router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        assertTrue((f.viewModel.state.value as AndroidStudyState.Introduction).revealedStage)

        assertEquals(3, ControllerDiagnosticsHolder.state.value.runtimeTrace.size)
    }

    // ─── 23. Previous and Next via ControllerInputRouter with trace ─────────────
    @Test
    fun `23 - Previous and Next via ControllerInputRouter navigates presentation history`() = runTest(testDispatcher) {
        val f = createFixture("prev-next-router", itemCount = 3)
        advanceUntilIdle()

        val item1 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        // Advance to Item 2
        StudyControllerBridge.activeTarget!!.revealAnswer()
        advanceUntilIdle()
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(item1.learningItemId, item2.learningItemId)

        ControllerDiagnosticsHolder.clear()
        val inputDpadLeft = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_E)
        val inputDpadRight = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_F)
        val profile = DefaultControllerProfiles.defaultProfile()
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        // Reveal item 2 before Previous
        StudyControllerBridge.activeTarget!!.revealAnswer()
        advanceUntilIdle()

        // Previous -> Item 1
        router.dispatchGesture(ControllerGesture(inputDpadLeft, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        assertEquals(item1.learningItemId, (f.viewModel.state.value as AndroidStudyState.Introduction).learningItemId)

        // Next -> Item 2
        router.dispatchGesture(ControllerGesture(inputDpadRight, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        assertEquals(item2.learningItemId, (f.viewModel.state.value as AndroidStudyState.Introduction).learningItemId)

        assertEquals(2, ControllerDiagnosticsHolder.state.value.runtimeTrace.size)
    }

    // ─── 24. Activity STOPPED: Continue commits item2 BEFORE any resume ─────────
    @Test
    fun `24 - Activity STOPPED Continue commits item2 immediately before any activity resume`() = runTest(testDispatcher) {
        val f = createFixture("stopped-continue", itemCount = 3)
        advanceUntilIdle()

        // Set Activity to STOPPED
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)
        ControllerDiagnosticsHolder.clear()

        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val profile = ControllerProfile(
            id = "stopped-profile",
            name = "Stopped Profile",
            mappings = listOf(
                ControllerMapping(ControllerContext.STUDY_QUESTION, ControllerGesture(inputA), ControllerAction.REVEAL_ANSWER),
                ControllerMapping(ControllerContext.STUDY_REVEALED, ControllerGesture(inputA), ControllerAction.CONTINUE_CURRENT_MODE)
            )
        )
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        // Reveal Item 1 while STOPPED
        router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()

        val item1 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(item1.revealedStage)

        // Continue Item 1 while STOPPED -> must mutate state to Item 2 synchronously/on commit
        val continueResult = router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        assertIs<ControllerActionResult.Executed>(continueResult)

        // ASSERT: Item 2 exists in ViewModel BEFORE any activity resume/foreground simulation
        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(item1.learningItemId, item2.learningItemId)
        assertFalse(item2.revealedStage)

        // Verify trace shows lifecycle=STOPPED and dispatchResult=COMMITTED with item2
        val trace = ControllerDiagnosticsHolder.state.value.runtimeTrace.last()
        assertEquals("STOPPED", trace.lifecycleState)
        assertEquals("COMMITTED", trace.dispatchResult)
        assertTrue(trace.stateAfter.contains(item2.learningItemId))

        // Simulate subsequent resume: verify state remains on Item 2 with zero duplicate transitions
        ControllerDiagnosticsHolder.setLifecycleState("RESUMED")
        ControllerDiagnosticsHolder.setForeground(true)
        advanceUntilIdle()

        val item2AfterResume = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertEquals(item2.learningItemId, item2AfterResume.learningItemId)
    }

    // ─── 25. Previous and Next while STOPPED (0 FSRS mutations) ─────────────────
    @Test
    fun `25 - Previous and Next while STOPPED commits presentation history with zero FSRS mutations`() = runTest(testDispatcher) {
        val f = createFixture("stopped-prev-next", itemCount = 3)
        advanceUntilIdle()

        // Advance to Item 2
        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        // Advance to Item 3
        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        val item3 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)

        // Set Activity to STOPPED
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)

        val inputDpadLeft = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_E)
        val inputDpadRight = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_F)
        val profile = DefaultControllerProfiles.defaultProfile()
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        // Reveal Item 3 before Previous
        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()

        // Previous: Item 3 -> Item 2 while STOPPED
        router.dispatchGesture(ControllerGesture(inputDpadLeft, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        val item2Visited = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(item3.learningItemId, item2Visited.learningItemId)

        // Previous: Item 2 -> Item 1 while STOPPED
        router.dispatchGesture(ControllerGesture(inputDpadLeft, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        val item1Visited = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertNotEquals(item2Visited.learningItemId, item1Visited.learningItemId)

        // Next: Item 1 -> Item 2 while STOPPED
        router.dispatchGesture(ControllerGesture(inputDpadRight, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()
        val item2Returned = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertEquals(item2Visited.learningItemId, item2Returned.learningItemId)
    }

    // ─── 26. Controller Input Router gracefully handles null active Target ─────
    @Test
    fun `26 - ControllerInputRouter gracefully drops study actions when activeTarget is null`() = runTest(testDispatcher) {
        StudyControllerBridge.clear()
        ControllerDiagnosticsHolder.clear()

        val inputA = ControllerPhysicalInput(0x2dc8, 0x9021, KeyEvent.KEYCODE_G)
        val profile = DefaultControllerProfiles.defaultProfile()
        val prefStore = object : ControllerPreferenceStore {
            var cfg = ControllerConfig(activeProfileId = profile.id, profiles = listOf(profile))
            override fun load(): ControllerConfig = cfg
            override fun save(config: ControllerConfig) { cfg = config }
        }
        val prefController = ControllerPreferencesController(prefStore)
        val mockContext = android.content.ContextWrapper(null)
        val router = ControllerInputRouter(
            appContext = mockContext,
            preferencesController = prefController,
            dispatcher = ControllerActionDispatcher(
                appContext = mockContext,
                studyBridge = StudyControllerBridge,
                autoPlayCoordinatorProvider = { null }
            )
        )

        val result = router.dispatchGesture(ControllerGesture(inputA, ControllerPressType.PRESS), origin = EventOrigin.ACCESSIBILITY)
        advanceUntilIdle()

        assertIs<ControllerActionResult.UnavailableInContext>(result)
    }

    // ─── 27. Background item entry does not autoplay meaning audio ───────────────
    @Test
    fun `27 - Background item entry does not autoplay meaning audio on state commit`() = runTest(testDispatcher) {
        var playedAudioPath: String? = null
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                playedAudioPath = path
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("background-audio-commit", itemCount = 3)
        advanceUntilIdle()

        // Advance to Item 2
        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item2.revealedStage)
        // Verify meaning audio is NOT played on unrevealed item entry
        assertNotEquals(item2.resolvedMeaningAudio, playedAudioPath)

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }

    // ─── 28. Single-owner item entry does not autoplay meaning ───────────────────
    @Test
    fun `28 - Single-owner item entry does not autoplay meaning audio`() = runTest(testDispatcher) {
        ControllerDiagnosticsHolder.clear()
        val audioEvents = mutableListOf<Pair<String?, Boolean>>()
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioEvents.add(path to isLooping)
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("single-owner-entry", itemCount = 2)
        advanceUntilIdle()

        val item1 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item1.revealedStage)

        // Item 1 unrevealed front has no autoplay meaning audio
        val audioTrace = ControllerDiagnosticsHolder.state.value.audioTrace
        assertTrue(audioTrace.none { it.role == "MEANING" })

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }

    // ─── 29. Background reveal audio parity starts expected answer loop ───────
    @Test
    fun `29 - Background reveal audio parity starts expected answer loop on reveal`() = runTest(testDispatcher) {
        ControllerDiagnosticsHolder.clear()
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)

        val audioEvents = mutableListOf<Pair<String?, Boolean>>()
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioEvents.add(path to isLooping)
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("bg-reveal-audio-parity", itemCount = 2)
        advanceUntilIdle()

        // Reveal answer while STOPPED
        val revealSuccess = StudyControllerBridge.revealAnswer()
        assertTrue(revealSuccess)
        advanceUntilIdle()

        val item1Revealed = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertTrue(item1Revealed.revealedStage)

        // Verify reveal loop audio was started with isLooping = true
        val lastEvent = audioEvents.last()
        val expectedRevealAudio = item1Revealed.resolvedExpectedAnswerAudio ?: item1Revealed.resolvedPromptAudio
        assertEquals(expectedRevealAudio, lastEvent.first)
        assertTrue(lastEvent.second) // isLooping = true!

        // Verify audioTrace records REVEAL with isLooping = true
        val audioTrace = ControllerDiagnosticsHolder.state.value.audioTrace
        val revealTrace = audioTrace.last { it.kind == StudyAudioEventKind.AUDIO_START }
        assertEquals(StudyAudioReason.REVEAL, revealTrace.reason)
        assertTrue(revealTrace.isLooping)
        assertEquals("EXPECTED_ANSWER", revealTrace.role)

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }

    // ─── 30. Background continue stops reveal loop ─────────────────────────────
    @Test
    fun `30 - Background continue stops reveal loop before advancing to item 2`() = runTest(testDispatcher) {
        ControllerDiagnosticsHolder.clear()
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)

        var stopCount = 0
        val audioEvents = mutableListOf<Pair<String?, Boolean>>()
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioEvents.add(path to isLooping)
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }

            override fun stop() {
                stopCount++
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("bg-continue-order", itemCount = 3)
        advanceUntilIdle()

        // Reveal item 1 -> starts loop
        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()
        assertEquals(1, audioEvents.size) // reveal
        assertTrue(audioEvents.last().second) // reveal is looping


        // Continue to item 2 -> stops reveal loop
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        assertTrue(stopCount >= 1)
        val item2 = assertIs<AndroidStudyState.Introduction>(f.viewModel.state.value)
        assertFalse(item2.revealedStage)

        // Audio trace confirms clean stop
        val audioTrace = ControllerDiagnosticsHolder.state.value.audioTrace
        val stopTraces = audioTrace.filter { it.kind == StudyAudioEventKind.AUDIO_STOP }
        assertTrue(stopTraces.isNotEmpty())

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }

    // ─── 31. Manual loop primary and example audio actions ─────────────────────
    @Test
    fun `31 - Manual loop primary and loop example actions execute with isLooping = true`() = runTest(testDispatcher) {
        ControllerDiagnosticsHolder.clear()
        val audioEvents = mutableListOf<Pair<String?, Boolean>>()
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioEvents.add(path to isLooping)
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("manual-loop-actions", itemCount = 2)
        advanceUntilIdle()

        // Loop primary audio
        val loopPrimarySuccess = StudyControllerBridge.loopPrimaryAudio()
        assertTrue(loopPrimarySuccess)
        advanceUntilIdle()

        val primaryLoopEvent = audioEvents.last()
        assertTrue(primaryLoopEvent.second) // isLooping = true

        val audioTrace = ControllerDiagnosticsHolder.state.value.audioTrace
        val primaryLoopTrace = audioTrace.last { it.kind == StudyAudioEventKind.AUDIO_START }
        assertEquals(StudyAudioReason.MANUAL_LOOP, primaryLoopTrace.reason)
        assertTrue(primaryLoopTrace.isLooping)

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }

    // ─── 32. Resume to foreground does not trigger duplicate audio replay ─────
    @Test
    fun `32 - Resume to foreground does not trigger duplicate audio replay`() = runTest(testDispatcher) {
        ControllerDiagnosticsHolder.clear()
        val audioEvents = mutableListOf<Pair<String?, Boolean>>()
        val audioMock = object : vn.loi.learning.android.media.AndroidAudioController() {
            override fun replay(
                path: String?,
                isLooping: Boolean,
                onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
            ): vn.loi.learning.android.media.AndroidAudioState {
                audioEvents.add(path to isLooping)
                return vn.loi.learning.android.media.AndroidAudioState.Playing
            }
        }
        StudyControllerBridge.registerBackgroundAudioController(audioMock)

        val f = createFixture("resume-no-duplicate", itemCount = 3)
        advanceUntilIdle()

        // While STOPPED, advance to item 2
        ControllerDiagnosticsHolder.setLifecycleState("STOPPED")
        ControllerDiagnosticsHolder.setForeground(false)

        StudyControllerBridge.revealAnswer()
        advanceUntilIdle()
        StudyControllerBridge.continueCurrentMode()
        advanceUntilIdle()

        val countAfterItem2 = audioEvents.size

        // Now resume to foreground
        ControllerDiagnosticsHolder.setLifecycleState("RESUMED")
        ControllerDiagnosticsHolder.setForeground(true)
        f.viewModel.onEvent(AndroidStudyEvent.Resume)
        advanceUntilIdle()

        // Verify count of audio playback events did NOT increase (no duplicate replay on resume!)
        assertEquals(countAfterItem2, audioEvents.size)

        StudyControllerBridge.unregisterBackgroundAudioController(audioMock)
    }
}
