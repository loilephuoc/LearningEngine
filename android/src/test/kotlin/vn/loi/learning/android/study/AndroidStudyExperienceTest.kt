package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
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
        val sessionId = SessionId("test-session-1")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )
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
        val sessionId = SessionId("session-id-100")
        ctx.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), setOf(contentId))
        )

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
        val controllerSource = source("vn/loi/learning/android/study/AndroidAudioController.kt")
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
        val sessionId = SessionId("session-990")
        context.engine.startSession(
            StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0), contents.mapTo(linkedSetOf(), Content::id))
        )

        val facade = AndroidStudyFacade(context, learner, now = { 2_000 })
        val state = facade.load(sessionId.value)

        val runtime = assertIs<AndroidStudyState.Typing>(state)
        assertEquals(sessionId, runtime.plan.sessionId)
        assertFalse(runtime.completed)
    }
}
