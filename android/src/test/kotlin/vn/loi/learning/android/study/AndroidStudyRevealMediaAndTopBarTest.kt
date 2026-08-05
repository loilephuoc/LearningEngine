package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyRevealMediaAndTopBarTest {

    @Test
    fun `all five ContentMedia references map independently in runtime state`() {
        val f = fixtureWithFullMedia()
        val state = assertIs<AndroidStudyState.Runtime>(f.facade.load())

        assertEquals("/resolved/media/prompt.mp3", state.resolvedPromptAudio)
        assertEquals("/resolved/media/answer.mp3", state.resolvedAnswerAudio)
        assertEquals("/resolved/media/example.mp3", state.resolvedExampleAudio)
        assertEquals("/resolved/media/example_trans.mp3", state.resolvedExampleTranslationAudio)
        assertEquals("/resolved/media/image.png", state.resolvedImage)
        assertEquals("/resolved/media/prompt.mp3", state.resolvedAudio)
    }

    @Test
    fun `missing media references map to null`() {
        val f = fixtureWithNoMedia()
        val state = assertIs<AndroidStudyState.Runtime>(f.facade.load())

        assertNull(state.resolvedPromptAudio)
        assertNull(state.resolvedAnswerAudio)
        assertNull(state.resolvedExampleAudio)
        assertNull(state.resolvedExampleTranslationAudio)
        assertNull(state.resolvedImage)
        assertNull(state.resolvedAudio)
    }

    @Test
    fun `Listening prompt authority uses RecallPrompt audio with primaryAudio fallback`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-listening")
        val itemId = LearningItemId("item-listening")
        val content = Content(
            contentId, ContentType.WORD,
            ContentText("listen", "nghe", "example listen", "ví dụ nghe"),
            ContentMedia(primaryAudio = "primary.mp3")
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.LISTENING_RECOGNITION))

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })

        val plan = RecallPlan(
            planId = RecallPlanId("plan-listening"),
            learnerId = learner,
            contentId = contentId,
            learningItemId = itemId,
            sessionId = SessionId("session-listening"),
            mode = RecallMode.LISTENING,
            direction = RecallDirection.AUDIO_TO_TEXT,
            prompt = RecallPrompt.Listening(
                audio = RecallResourceId("listening_specific.mp3")
            ),
            answerContract = RecallAnswerContract(
                canonicalAnswer = "listen",
                normalizationPolicy = RecallNormalizationPolicyId("norm-v1"),
                caseSensitivity = CaseSensitivity.INSENSITIVE,
                punctuationPolicy = PunctuationPolicy.IGNORE,
                whitespacePolicy = WhitespacePolicy.NORMALIZE,
                expectedLanguage = RecallLanguageTag("en"),
                kind = RecallAnswerKind.TEXT
            ),
            availableAssistance = emptySet(),
            evidenceClass = RecallEvidenceEligibility.STRONG,
            deterministicSeed = RecallDeterministicSeed(1L),
            generatedAt = Moment(1_000),
            provenance = RecallProvenance.EVALUATIVE,
            platformRequirements = RecallPlatformRequirements(),
            contentCapabilities = RecallContentCapabilities(
                contentId = contentId,
                available = setOf(RecallCapability.WORD_AUDIO),
                wordAudio = RecallResourceId("listening_specific.mp3")
            )
        )

        val presented = assertIs<AndroidStudyState.Listening>(facade.present(plan))
        assertEquals("/resolved/listening_specific.mp3", presented.resolvedPromptAudio)
        assertEquals("/resolved/listening_specific.mp3", presented.audioPath)
    }

    @Test
    fun `Image Recall prompt authority uses RecallPrompt image with canonical fallback`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-image")
        val itemId = LearningItemId("item-image")
        val content = Content(
            contentId, ContentType.WORD,
            ContentText("cat", "con mèo"),
            ContentMedia(image = "cat_canonical.png")
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })

        val plan = RecallPlan(
            planId = RecallPlanId("plan-image"),
            learnerId = learner,
            contentId = contentId,
            learningItemId = itemId,
            sessionId = SessionId("session-image"),
            mode = RecallMode.IMAGE_RECALL,
            direction = RecallDirection.IMAGE_TO_TEXT,
            prompt = RecallPrompt.ImageRecall(
                image = RecallResourceId("cat_specific.png")
            ),
            answerContract = RecallAnswerContract(
                canonicalAnswer = "cat",
                normalizationPolicy = RecallNormalizationPolicyId("norm-v1"),
                caseSensitivity = CaseSensitivity.INSENSITIVE,
                punctuationPolicy = PunctuationPolicy.IGNORE,
                whitespacePolicy = WhitespacePolicy.NORMALIZE,
                expectedLanguage = RecallLanguageTag("en"),
                kind = RecallAnswerKind.TEXT
            ),
            availableAssistance = emptySet(),
            evidenceClass = RecallEvidenceEligibility.STRONG,
            deterministicSeed = RecallDeterministicSeed(1L),
            generatedAt = Moment(1_000),
            provenance = RecallProvenance.EVALUATIVE,
            platformRequirements = RecallPlatformRequirements(),
            contentCapabilities = RecallContentCapabilities(
                contentId = contentId,
                available = setOf(RecallCapability.IMAGE),
                image = RecallResourceId("cat_specific.png")
            )
        )

        val presented = assertIs<AndroidStudyState.ImageRecall>(facade.present(plan))
        assertEquals("/resolved/cat_specific.png", presented.resolvedImage)
        assertEquals("/resolved/cat_specific.png", presented.imagePath)
    }

    @Test
    fun `reveal and workflow preserve exact session identity and content details`() {
        val f = fixtureWithFullMedia()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())

        val revealed = assertIs<AndroidStudyState.Typing>(f.facade.reveal(initial))
        assertTrue(revealed.revealed)
        assertEquals(initial.plan.sessionId, revealed.plan.sessionId)
        assertEquals(initial.plan.contentId, revealed.plan.contentId)
        assertEquals("/resolved/media/answer.mp3", revealed.resolvedAnswerAudio)
        assertEquals("/resolved/media/example.mp3", revealed.resolvedExampleAudio)
        assertEquals("/resolved/media/example_trans.mp3", revealed.resolvedExampleTranslationAudio)
    }

    private fun fixtureWithFullMedia(): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-full-media")
        val itemId = LearningItemId("item-full-media")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                exampleText = "I eat an apple",
                exampleTranslation = "Tôi ăn một quả táo",
                pronunciation = "/ˈæp.əl/"
            ),
            media = ContentMedia(
                primaryAudio = "media/prompt.mp3",
                translatedAudio = "media/answer.mp3",
                exampleAudio = "media/example.mp3",
                exampleTranslatedAudio = "media/example_trans.mp3",
                image = "media/image.png"
            )
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))

        val sessionId = SessionId("android-session-full-media")
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
        return Fixture(context, learner, itemId, sessionId, facade)
    }

    private fun fixtureWithNoMedia(): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-no-media")
        val itemId = LearningItemId("item-no-media")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "banana",
                translatedText = "quả chuối"
            ),
            media = ContentMedia()
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))

        val sessionId = SessionId("android-session-no-media")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learner,
                startedAt = Moment(1_000),
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                includedContentIds = setOf(contentId)
            )
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { null })
        return Fixture(context, learner, itemId, sessionId, facade)
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val itemId: LearningItemId,
        val sessionId: SessionId,
        val facade: AndroidStudyFacade
    )
}
