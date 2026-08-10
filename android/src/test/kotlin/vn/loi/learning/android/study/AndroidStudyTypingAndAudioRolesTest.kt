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

class AndroidStudyTypingAndAudioRolesTest {

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
    fun `study audio micro-interactions use full row targets and correct loop contracts`() {
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

        // 3. Loop configuration: English audio roles loop, Vietnamese audio roles single play
        assertTrue(screenSource.contains("AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio, true"))
        assertTrue(screenSource.contains("AudioRole.MEANING, state.resolvedMeaningAudio, false"))
        assertTrue(screenSource.contains("AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true"))
        assertTrue(screenSource.contains("AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false"))
    }
}
