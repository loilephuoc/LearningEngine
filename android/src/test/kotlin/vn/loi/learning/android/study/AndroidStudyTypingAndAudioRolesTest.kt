package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyTypingAndAudioRolesTest {

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

        val sessionId = SessionId("session-tea")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learner,
                startedAt = Moment(1_000),
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                includedContentIds = setOf(contentId)
            )
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

        val sessionId = SessionId("session-partial")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learner,
                startedAt = Moment(1_000),
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                includedContentIds = setOf(contentId)
            )
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

        val sessionId = SessionId("session-submit-test")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learner,
                startedAt = Moment(1_000),
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                includedContentIds = setOf(contentId)
            )
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
        val state = assertIs<AndroidStudyState.Typing>(facade.load(sessionId.value))

        // Submit with exact visible typed text
        val submitted = assertIs<AndroidStudyState.Typing>(facade.submitText(state, "married"))
        assertTrue(submitted.completed)
        assertEquals(RecallOutcome.CORRECT, submitted.outcome)
    }
}
