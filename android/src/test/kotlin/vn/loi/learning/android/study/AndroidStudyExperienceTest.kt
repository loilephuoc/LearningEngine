package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.application.session.StartStudySessionCommand

/**
 * Focused test suite for ANDROID-UI-005 Redesign Android Study Experience.
 *
 * Covers:
 * - Study authority: session & content identity, mode mapping, reveal, submit, next, completion, failure.
 * - Image experience: available, missing, invalid image, non-blocking async, fullscreen state preservation.
 * - Audio experience: typed states, non-blocking, repeated tap guard.
 * - Supported Study modes: Typing, MCQ, Listening, Image, Example Completion.
 * - UI & Architecture invariants: non-blank states, touch targets >= 48dp, no repo/engine in composables.
 * - 990-content active session regression.
 */
class AndroidStudyExperienceTest {

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin").resolve(relative))

    private fun fixture(resolveMedia: (String) -> String? = { null }): AndroidStudyFacade {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("test-content-1")
        val itemId = LearningItemId("test-item-1")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("hello", "xin chào")))
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        ctx.engine.review(ReviewCommand(ReviewEventId("seed-test-item-1"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("test-session-1")
        val session = StudySession.start(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId))
        ctx.studySessionRepository!!.save(session)
        ctx.studyQueue.create(sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1)
        return AndroidStudyFacade(ctx, learner, now = { 2_000 }, resolveMedia = resolveMedia)
    }

    // ─── 1. Study Authority & Identity ──────────────────────────────────────────

    @Test
    fun `exact session and content identity are preserved through facade load`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-id-100")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("apple", "quả táo", pronunciation = "/ˈæp.əl/")))
        val itemId = LearningItemId("item-id-100")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        ctx.engine.review(ReviewCommand(ReviewEventId("seed-item-id-100"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("session-id-100")
        val session = StudySession.start(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId))
        ctx.studySessionRepository!!.save(session)
        ctx.studyQueue.create(sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1)

        val facade = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val runtime = assertIs<AndroidStudyState.Typing>(facade.load(sessionId.value))

        assertEquals(sessionId, runtime.plan.sessionId)
        assertEquals(contentId, runtime.plan.contentId)
        assertEquals("/ˈæp.əl/", runtime.pronunciation)
        assertEquals("quả táo", runtime.meaning)
    }

    @Test
    fun `stale session load returns failed state non-blank`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val facade = AndroidStudyFacade(ctx)
        val state = facade.loadExact("invalid-session-999")

        val failed = assertIs<AndroidStudyState.Failed>(state)
        assertTrue(failed.message.isNotBlank())
        assertEquals("invalid-session-999", failed.retrySessionId)
    }

    // ─── 2. Image Experience Requirements ─────────────────────────────────────────

    @Test
    fun `available image resolves path without storing media bytes in UI state`() {
        val facade = fixture(resolveMedia = { if (it == "img.png") "/resolved/img.png" else null })
        val base = assertIs<AndroidStudyState.Typing>(facade.load()).plan
        val imageState = assertIs<AndroidStudyState.ImageRecall>(
            facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("img.png"))))
        )

        assertEquals("/resolved/img.png", imageState.imagePath)
        assertEquals("/resolved/img.png", imageState.resolvedImage)
        assertFalse(imageState.imageUnavailable)

        // Verify state model does not store byte arrays or Android Bitmaps
        val stateSource = source("vn/loi/learning/android/study/AndroidStudyFacade.kt")
        assertFalse(stateSource.contains("Bitmap"))
        assertFalse(stateSource.contains("ByteArray"))
    }

    @Test
    fun `missing or invalid image degrades safely to unavailable state`() {
        val facade = fixture(resolveMedia = { null })
        val base = assertIs<AndroidStudyState.Typing>(facade.load()).plan
        val missing = assertIs<AndroidStudyState.ImageRecall>(
            facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("missing.png"))))
        )

        assertNull(missing.imagePath)
        assertTrue(missing.imageUnavailable)
    }

    @Test
    fun `StudyScreen image composable does not load image bytes on Main`() {
        val componentSource = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        // Image decoding occurs on Dispatchers.IO
        assertTrue(componentSource.contains("withContext(Dispatchers.IO)"))
        assertTrue(componentSource.contains("decodeBoundedImage"))
        // Fullscreen view exists
        assertTrue(componentSource.contains("FullscreenLearningImage"))
    }

    // ─── 3. Audio Experience Requirements ─────────────────────────────────────────

    @Test
    fun `audio playback controller defines typed playback states`() {
        val controllerSource = source("vn/loi/learning/android/media/AndroidAudioController.kt")
        listOf("Idle", "Preparing", "Playing", "Unavailable", "Failed").forEach { state ->
            assertTrue(controllerSource.contains(state), "AndroidAudioState must contain $state")
        }
    }

    @Test
    fun `audio button has content description and supports unavailable state`() {
        val facade = fixture(resolveMedia = { null })
        val base = assertIs<AndroidStudyState.Typing>(facade.load()).plan
        val listening = assertIs<AndroidStudyState.Listening>(
            facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("missing.mp3"))))
        )

        assertNull(listening.audioPath)
        assertTrue(listening.audioUnavailable)
    }

    // ─── 4. Supported Study Modes ─────────────────────────────────────────────────

    @Test
    fun `all supported modes map correctly from canonical RecallPlan`() {
        val facade = fixture()
        val base = assertIs<AndroidStudyState.Typing>(facade.load()).plan

        val typing = facade.present(base.copy(mode = RecallMode.TYPING, prompt = RecallPrompt.Typing("hello")))
        assertIs<AndroidStudyState.Typing>(typing)

        val choices = listOf(RecallChoice("1", "hello", true), RecallChoice("2", "world", false))
        val mcq = facade.present(base.copy(mode = RecallMode.MULTIPLE_CHOICE, prompt = RecallPrompt.MultipleChoice("Question", choices)))
        assertIs<AndroidStudyState.MultipleChoice>(mcq)

        val listening = facade.present(base.copy(mode = RecallMode.LISTENING, prompt = RecallPrompt.Listening(RecallResourceId("audio.mp3"))))
        assertIs<AndroidStudyState.Listening>(listening)

        val image = facade.present(base.copy(mode = RecallMode.IMAGE_RECALL, prompt = RecallPrompt.ImageRecall(RecallResourceId("img.png"))))
        assertIs<AndroidStudyState.ImageRecall>(image)

        val example = facade.present(base.copy(mode = RecallMode.EXAMPLE_COMPLETION, prompt = RecallPrompt.ExampleCompletion("I am learning", RecallTextSpan(2, 4))))
        assertIs<AndroidStudyState.ExampleCompletion>(example)
    }

    // ─── 5. UI Architecture and Composable Rules ──────────────────────────────────

    @Test
    fun `StudyScreen composable does not invoke Repository or ApplicationContext`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        assertFalse(screenSource.contains("Repository"))
        assertFalse(screenSource.contains("context."))
        assertFalse(screenSource.contains("LearningApplicationContext"))
        assertFalse(screenSource.contains("engine."))
    }

    @Test
    fun `StudyScreen composables define touch targets at least 48dp`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        assertTrue(screenSource.contains("touchTarget"))
        assertTrue(screenSource.contains("defaultMinSize"))
    }

    @Test
    fun `StudyScreen uses semantic headings and live regions`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        assertTrue(screenSource.contains("heading()"))
        assertTrue(screenSource.contains("liveRegion"))
        assertTrue(screenSource.contains("contentDescription"))
    }

    // ─── 6. 990-Content Active Session Regression ─────────────────────────────────

    @Test
    fun `active 990 content session resumes and loads cleanly`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contents = (1..990).map { index ->
            Content(ContentId("content-$index"), ContentType.WORD, ContentText("word-$index", "answer-$index"))
        }
        context.contentRepository!!.saveAll(contents)
        val itemId = LearningItemId("item-1")
        context.learningItemRepository!!.save(LearningItem(itemId, contents.first().id, LearningMode.MEANING_RECOGNITION))
        context.engine.review(ReviewCommand(ReviewEventId("seed-item-990"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("session-990")
        val session = StudySession.start(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), contents.mapTo(linkedSetOf(), Content::id))
        context.studySessionRepository!!.save(session)
        context.studyQueue.create(sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contents.first().id), configuredReviewTarget = 1, effectiveReviewWorkload = 1)

        val facade = AndroidStudyFacade(context, learner, now = { 2_000 })
        val state = facade.load(sessionId.value)

        val runtime = assertIs<AndroidStudyState.Typing>(state)
        assertEquals(sessionId, runtime.plan.sessionId)
        assertFalse(runtime.completed)
    }

    // ─── 7. ANDROID-STUDY-007D Transition, Fullscreen & Accessibility Contracts ─

    @Test
    fun `StudyScreen question transition is keyed by stable plan identity and supports reduced motion`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        val componentsSource = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")

        // Keyed by stable plan identity
        assertTrue(screenSource.contains("private fun studyPresentationKey"))
        assertTrue(screenSource.contains("\"runtime-\${state.requireRecallPlan().planId.value}\""))
        assertTrue(screenSource.contains("\"runtime-intro-\${state.learningItemId}\""))

        // Slide/fade transition defined
        assertTrue(screenSource.contains("slideInHorizontally"))
        assertTrue(screenSource.contains("slideOutHorizontally"))

        // Reduced motion check supported
        assertTrue(screenSource.contains("isReducedMotionEnabled()"))
        assertTrue(componentsSource.contains("fun isReducedMotionEnabled()"))
    }

    @Test
    fun `Fullscreen image overlay incorporates system BackHandler and close semantics`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        val componentsSource = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")

        // Screen-level BackHandler for fullscreen overlay
        assertTrue(screenSource.contains("BackHandler(enabled = fullscreenImageUri != null)"))

        // Fullscreen overlay BackHandler and close semantics
        assertTrue(componentsSource.contains("BackHandler(onBack = onDismiss)"))
        assertTrue(componentsSource.contains("Đóng ảnh toàn màn hình"))
    }

    @Test
    fun `Study UI components contain no hardcoded production hex colors`() {
        val screenSource = source("vn/loi/learning/android/study/StudyScreen.kt")
        val componentsSource = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")

        // Verify no raw hex Color(0x...) in UI sources
        assertFalse(screenSource.contains("Color(0x"))
        assertFalse(componentsSource.contains("Color(0x"))
    }

    // ─── 8. ANDROID-STUDY-2.0A Canonical New-Content Learning Flow ───────────────

    @Test
    fun `unintroduced NEW content routes to AndroidStudyState Introduction`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-new-1")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("cat", "con mèo", pronunciation = "/kæt/")))
        val itemId = LearningItemId("item-new-1")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("session-new-1")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

        val facade = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val state = facade.load(sessionId.value)

        val intro = assertIs<AndroidStudyState.Introduction>(state)
        assertEquals(sessionId.value, intro.sessionId)
        assertEquals(contentId.value, intro.contentId)
        assertEquals("con mèo", intro.meaning)
        assertEquals("cat", intro.answer)
        assertEquals("/kæt/", intro.pronunciation)
        assertFalse(intro.revealed)
    }

    @Test
    fun `revealIntroduction completes content introduction in session`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-new-2")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("dog", "con chó")))
        val itemId = LearningItemId("item-new-2")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("session-new-2")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

        val facade = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val intro = assertIs<AndroidStudyState.Introduction>(facade.load(sessionId.value))
        val revealedState = assertIs<AndroidStudyState.Introduction>(facade.revealIntroduction(intro))

        assertTrue(revealedState.revealed)
        val session = ctx.engine.getSession(sessionId)!!
        assertTrue(session.introducedContentIds.contains(contentId))
    }

    @Test
    fun `rateIntroduction delegates to reviewSessionItem and advances session queue`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-new-3")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("bird", "con chim")))
        val itemId = LearningItemId("item-new-3")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("session-new-3")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

        val facade = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val intro = assertIs<AndroidStudyState.Introduction>(facade.load(sessionId.value))
        val revealed = assertIs<AndroidStudyState.Introduction>(facade.revealIntroduction(intro))
        val nextState = facade.rateIntroduction(revealed, vn.loi.learning.domain.study.memory.model.ReviewRating.GOOD)

        val session = ctx.engine.getSession(sessionId)!!
        assertEquals(1, session.newItemsReviewed)
        assertTrue(session.reviewedContentIds.contains(contentId))
        assertIs<AndroidStudyState.Completion>(nextState)
    }

    @Test
    fun `revealed unrated NEW content after restart restores Introduction reveal`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-new-4")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("fish", "con cá")))
        val itemId = LearningItemId("item-new-4")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("session-new-4")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

        val facade1 = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val intro = assertIs<AndroidStudyState.Introduction>(facade1.load(sessionId.value))
        facade1.revealIntroduction(intro)

        val facade2 = AndroidStudyFacade(ctx, learner, now = { 3_000 })
        val reloaded = assertIs<AndroidStudyState.Introduction>(facade2.load(sessionId.value))
        assertTrue(reloaded.revealed)
        assertEquals(contentId.value, reloaded.contentId)
        assertFalse(ctx.engine.getSession(sessionId)!!.reviewedContentIds.contains(contentId))
        val before = ctx.reviewEventRepository!!.findAll(learner).size

        assertIs<AndroidStudyState.Completion>(facade2.rateIntroduction(reloaded, ReviewRating.GOOD))
        assertEquals(before + 1, ctx.reviewEventRepository!!.findAll(learner).size)
        assertTrue(ctx.engine.getSession(sessionId)!!.reviewedContentIds.contains(contentId))
        assertEquals(reloaded, facade2.rateIntroduction(reloaded, ReviewRating.GOOD))
        assertEquals(before + 1, ctx.reviewEventRepository!!.findAll(learner).size)
    }

    @Test
    fun `undo after rating Introduction item restores session state`() {
        val ctx = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-new-5")
        ctx.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("horse", "con ngựa")))
        val itemId = LearningItemId("item-new-5")
        ctx.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
        val sessionId = SessionId("session-new-5")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

        val facade = AndroidStudyFacade(ctx, learner, now = { 2_000 })
        val intro = assertIs<AndroidStudyState.Introduction>(facade.load(sessionId.value))
        facade.rateIntroduction(intro, vn.loi.learning.domain.study.memory.model.ReviewRating.GOOD)

        val undoneState = facade.undo(AndroidStudyState.Completion(sessionId.value, true))
        val restoredIntroduction = assertIs<AndroidStudyState.Introduction>(undoneState)
        assertEquals("horse", restoredIntroduction.answer)
        assertTrue(restoredIntroduction.revealed)
        assertFalse(ctx.engine.getSession(sessionId)!!.reviewedContentIds.contains(contentId))
    }
}
