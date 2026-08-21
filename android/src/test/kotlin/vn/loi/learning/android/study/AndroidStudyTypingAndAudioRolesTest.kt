package vn.loi.learning.android.study

import vn.loi.learning.application.review.ReviewCommand
import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.application.typing.TypingForcedAgainPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidStudyTypingAndAudioRolesTest {
    private val dispatcher = StandardTestDispatcher()

    @org.junit.Before fun setUp() = Dispatchers.setMain(dispatcher)
    @org.junit.After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `Typing forced Again timeout scales deterministically with canonical answer length`() {
        val short = TypingForcedAgainPolicy.timeoutMillis(3)
        val long = TypingForcedAgainPolicy.timeoutMillis(30)
        assertTrue(short > 0)
        assertTrue(long > short)
        assertEquals(short, TypingForcedAgainPolicy.timeoutMillis(3))
    }

    @Test
    fun `canonical audio roles map primaryAudio to expected answer and translatedAudio to meaning`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-tea")
        val itemId = LearningItemId("item-tea")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "tea",
                translatedText = "trà",
                exampleText = "I drink tea",
                exampleTranslation = "Tôi uống trà"
            ),
            media = ContentMedia(
                primaryAudio = "tea.mp3",
                translatedAudio = "tra.mp3",
                exampleAudio = "example_tea.mp3",
                exampleTranslatedAudio = "example_tra.mp3"
            )
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-tea"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("session-tea")
        val sessionTea = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId)
        )
        context.studySessionRepository!!.save(sessionTea)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
        val state = assertIs<AndroidStudyState.Runtime>(facade.load(sessionId.value))

        // 1. Expected Answer uses primaryAudio (tea.mp3)
        assertEquals("/resolved/tea.mp3", state.resolvedExpectedAnswerAudio)

        // 2. Meaning uses translatedAudio (tra.mp3)
        assertEquals("/resolved/tra.mp3", state.resolvedMeaningAudio)

        // 3. Example English uses exampleAudio (example_tea.mp3)
        assertEquals("/resolved/example_tea.mp3", state.resolvedExampleEnglishAudio)

        // 4. Translation uses exampleTranslatedAudio (example_tra.mp3)
        assertEquals("/resolved/example_tra.mp3", state.resolvedExampleVietnameseAudio)

        // Verifying tapping 'tea' plays tea.mp3, NOT tra.mp3
        assertNotEquals(state.resolvedExpectedAnswerAudio, state.resolvedMeaningAudio)
    }

    @Test
    fun `absent audio source maps only its own role to null`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-partial-audio")
        val itemId = LearningItemId("item-partial-audio")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "snowy",
                translatedText = "có tuyết"
            ),
            media = ContentMedia(
                primaryAudio = "snowy.mp3"
                // translatedAudio, exampleAudio, exampleTranslatedAudio are null
            )
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-partial"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))

        val sessionId = SessionId("session-partial")
        val sessionPartial = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId)
        )
        context.studySessionRepository!!.save(sessionPartial)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
        val state = assertIs<AndroidStudyState.Runtime>(facade.load(sessionId.value))

        assertEquals("/resolved/snowy.mp3", state.resolvedExpectedAnswerAudio)
        assertNull(state.resolvedMeaningAudio)
        assertNull(state.resolvedExampleEnglishAudio)
        assertNull(state.resolvedExampleVietnameseAudio)
    }

    @Test
    fun `submitText and reveal receive exact typed text passed`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-submit-test")
        val itemId = LearningItemId("item-submit-test")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(primaryText = "married", translatedText = "kết hôn"),
            media = ContentMedia(primaryAudio = "married.mp3")
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-submit"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))

        val sessionId = SessionId("session-submit-test")
        val sessionSubmit = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1),
            setOf(contentId), studyMode = StudyMode.TYPING
        )
        context.studySessionRepository!!.save(sessionSubmit)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
        val state = assertIs<AndroidStudyState.Typing>(facade.load(sessionId.value))

        // Submit with exact visible typed text
        val submitted = assertIs<AndroidStudyState.Typing>(facade.submitText(state, "married"))
        assertTrue(submitted.completed)
        assertEquals(RecallOutcome.CORRECT, submitted.outcome)
    }

    @Test
    fun `study audio micro-interactions keep English targets while Vietnamese text stays passive`() {
        val componentsSource = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/ui/LearningEngineComponents.kt"))
        val screenSource = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))

        // 1. Audio text row component defined with full row clickability & accessibility
        assertTrue(componentsSource.contains("fun LearningEngineAudioTextRow"))
        assertTrue(componentsSource.contains("Surface("))
        assertTrue(componentsSource.contains("onClick = onToggleAudio"))
        assertTrue(componentsSource.contains("Role.Button"))
        assertTrue(componentsSource.contains("touchTarget"))

        // 2. StudyScreen delegates shared answer audio rows instead of duplicating them inline
        assertTrue(screenSource.contains("StudyAnswerSection("))

        // 3. English text and Example Vietnamese audio remain interactive.
        // Vietnamese meaning text is passive on tap and does not auto-play on next item.
        assertTrue(screenSource.contains("AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio, true"))
        assertTrue(screenSource.contains("AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true"))
        assertTrue(screenSource.contains("AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false"))
    }

    @Test
    fun `transition to unrevealed next card does not autoplay Vietnamese meaning audio`() =
        kotlinx.coroutines.test.runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val learner = LearnerId("default-learner")
            val content1 = ContentId("content-c1")
            val item1 = LearningItemId("item-i1")
            val content2 = ContentId("content-c2")
            val item2 = LearningItemId("item-i2")

            context.contentRepository!!.save(Content(
                content1, ContentType.WORD, ContentText("tea", "trà"),
                media = ContentMedia(primaryAudio = "tea.mp3", translatedAudio = "tra.mp3")
            ))
            context.learningItemRepository!!.save(LearningItem(item1, content1, LearningMode.MEANING_RECALL))
            context.contentRepository!!.save(Content(
                content2, ContentType.WORD, ContentText("coffee", "cà phê"),
                media = ContentMedia(primaryAudio = "coffee.mp3", translatedAudio = "caphe.mp3")
            ))
            context.learningItemRepository!!.save(LearningItem(item2, content2, LearningMode.MEANING_RECALL))

            val sessionId = SessionId("session-audio-isolation")
            context.studySessionRepository!!.save(StudySession.start(
                sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 2, reviewItemLimit = 0),
                setOf(content1, content2), studyMode = StudyMode.LEARN_NEW
            ))
            context.studyQueue.create(
                sessionId, Moment(1_000), listOf(item1, item2),
                mapOf(item1 to SessionItemOrigin.NEW, item2 to SessionItemOrigin.NEW),
                mapOf(item1 to content1, item2 to content2),
                configuredNewTarget = 2, effectiveNewWorkload = 2
            )

            try {
                vn.loi.learning.android.controller.StudyControllerBridge.clear()
                val fakeController = object : vn.loi.learning.android.media.AndroidAudioController(null) {
                    override fun replay(
                        path: String?,
                        isLooping: Boolean,
                        onPlaybackEvent: (vn.loi.learning.android.media.AndroidAudioPlaybackEvent) -> Unit,
                        onState: (vn.loi.learning.android.media.AndroidAudioState) -> Unit
                    ): vn.loi.learning.android.media.AndroidAudioState {
                        onState(vn.loi.learning.android.media.AndroidAudioState.Playing)
                        return vn.loi.learning.android.media.AndroidAudioState.Playing
                    }
                }
                vn.loi.learning.android.controller.StudyControllerBridge.registerBackgroundAudioController(fakeController)

                val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
                val viewModel = AndroidStudyViewModel(
                    facade,
                    androidx.lifecycle.SavedStateHandle(mapOf("study.sessionId" to sessionId.value)),
                    dispatcher
                )

                // Step 1: Open card A
                viewModel.onEvent(AndroidStudyEvent.OpenSession(sessionId.value))
                advanceUntilIdle()
                val stateA = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
                assertFalse(stateA.revealed)
                // Assert item entry of card A does NOT autoplay MEANING
                assertNotEquals("MEANING", vn.loi.learning.android.controller.StudyControllerBridge.currentPlayback?.role)

                // Step 2: Reveal card A
                viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
                advanceUntilIdle()
                val stateARevealed = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
                assertTrue(stateARevealed.revealed)
                // Assert Reveal plays EXPECTED_ANSWER (English)
                assertEquals("EXPECTED_ANSWER", vn.loi.learning.android.controller.StudyControllerBridge.currentPlayback?.role)

                // Step 3: Rate card A and transition toward card B
                viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
                advanceUntilIdle()
                val stateB = assertIs<AndroidStudyState.Introduction>(viewModel.state.value)
                assertEquals(item2.value, stateB.learningItemId)
                assertFalse(stateB.revealed)

                // Assert card B front does NOT autoplay MEANING directly at VM level (UI controls ordered playback)
                assertNotEquals("MEANING", vn.loi.learning.android.controller.StudyControllerBridge.currentPlayback?.role)
            } finally {
                vn.loi.learning.android.controller.StudyControllerBridge.clear()
            }
        }

    @Test
    fun `StudyAudioOwnership enforces ordered front MEANING autoplay without overlap or duplicate replay`() {
        val ownership = StudyAudioOwnership()

        // 1. Card B is active and revealed
        val tokenB = ownership.update("card-B", feedbackActive = false)
        assertNotNull(tokenB)
        assertTrue(ownership.claimAutoplay(tokenB, AudioRole.EXPECTED_ANSWER))
        assertFalse(ownership.claimAutoplay(tokenB, AudioRole.EXPECTED_ANSWER)) // duplicate rejected

        // 2. Card B rated -> transition toward Card C while Card B feedback is active
        val tokenC = ownership.update("card-C", feedbackActive = true)
        assertNotNull(tokenC)
        assertNotEquals(tokenB, tokenC)

        // While Card B feedback is active, Card C cannot autoplay (no overlap)
        assertFalse(ownership.claimAutoplay(tokenC, AudioRole.MEANING))

        // 3. Card B feedback completes -> autoplay gate opens for Card C
        val activeTokenC = ownership.update("card-C", feedbackActive = false)
        assertNotNull(activeTokenC)
        assertTrue(ownership.claimAutoplay(activeTokenC, AudioRole.MEANING)) // Card C plays MEANING once

        // 4. Recomposition / state change on Card C does not duplicate autoplay
        assertFalse(ownership.claimAutoplay(activeTokenC, AudioRole.MEANING))

        // 5. User navigates back to Card B for rating correction
        val tokenBHistory = ownership.update("card-B", feedbackActive = false)
        assertNotNull(tokenBHistory)

        // 6. User corrects Card B rating and returns to Card C (same card key)
        val tokenCReturned = ownership.update("card-C", feedbackActive = false)
        assertNotNull(tokenCReturned)

        // Returning to existing Card C does not duplicate MEANING autoplay
        assertFalse(ownership.claimAutoplay(tokenCReturned, AudioRole.MEANING))
    }

    @Test
    fun `StudyScreen UI connects front MEANING autoplay and allows immediate correction navigation`() {
        val screenSource = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        assertTrue(screenSource.contains("audioOwnership.claimAutoplay(audioOwnerToken, AudioRole.MEANING)"))
        assertTrue(screenSource.contains("restartAudio(AudioRole.MEANING, introduction.resolvedMeaningAudio, false)"))
        assertTrue(screenSource.contains("!introduction.revealed && !introduction.historyPreview && autoplayGateOpen"))
        assertTrue(screenSource.contains("navigationEnabled = (state.revealed || focusedSkimUx || state.navigation.canPrevious) && !quickReviewTransitionPending"))
    }
}
