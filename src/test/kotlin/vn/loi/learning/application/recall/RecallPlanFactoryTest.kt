package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class RecallPlanFactoryTest {
    private val factory = RecallPlanFactory()

    @Test fun `typing decision creates validator accepted plan`() {
        val plan = created(request(RecallMode.TYPING, RecallDirection.TARGET_TO_SOURCE))
        assertEquals("translation", assertIs<RecallPrompt.Typing>(plan.prompt).sourceText)
        assertEquals("word", plan.answerContract.canonicalAnswer)
        assertEquals(RecallPlanValidationResult.Valid, RecallContractValidator.validatePlan(plan))
        assertTrue(plan.platformRequirements.requiresTextInput)
    }

    @Test fun `reverse direction swaps prompt and canonical answer`() {
        val plan = created(request(RecallMode.REVERSE_TRANSLATION, RecallDirection.SOURCE_TO_TARGET))
        assertEquals("word", assertIs<RecallPrompt.ReverseTranslation>(plan.prompt).targetText)
        assertEquals("translation", plan.answerContract.canonicalAnswer)
        assertEquals(RecallLanguageTag("vi"), plan.answerContract.expectedLanguage)
    }

    @Test fun `listening and dictation use typed audio without loading it`() {
        listOf(RecallMode.LISTENING, RecallMode.DICTATION).forEach { mode ->
            val plan = created(request(mode, RecallDirection.AUDIO_TO_TEXT, audio = true))
            val resource = when (val prompt = plan.prompt) {
                is RecallPrompt.Listening -> prompt.audio
                is RecallPrompt.Dictation -> prompt.audio
                else -> fail("Unexpected prompt")
            }
            assertEquals(RecallResourceId("asset:audio"), resource)
            assertTrue(plan.platformRequirements.requiresAudioPlayback)
            assertTrue(plan.platformRequirements.requiresTextInput)
        }
    }

    @Test fun `stale audio and image decisions are rejected without reselection`() {
        assertIs<RecallPlanFactoryResult.StaleStrategyDecision>(factory.create(request(RecallMode.LISTENING, RecallDirection.AUDIO_TO_TEXT)))
        assertIs<RecallPlanFactoryResult.StaleStrategyDecision>(factory.create(request(RecallMode.IMAGE_RECALL, RecallDirection.IMAGE_TO_TEXT)))
    }

    @Test fun `content and capability snapshot mismatch is typed`() {
        val withAudio = request(RecallMode.LISTENING, RecallDirection.AUDIO_TO_TEXT, audio = true)
        val changedContent = withAudio.content.copy(media = ContentMedia())
        assertIs<RecallPlanFactoryResult.CapabilityMismatch>(factory.create(withAudio.copy(content = changedContent)))
    }

    @Test fun `image plan uses typed resource and does not expose answer`() {
        val plan = created(request(RecallMode.IMAGE_RECALL, RecallDirection.IMAGE_TO_TEXT, image = true))
        assertEquals(RecallResourceId("asset:image"), assertIs<RecallPrompt.ImageRecall>(plan.prompt).image)
        assertFalse(AnswerLeakageValidator.leaks(plan.prompt, plan.answerContract))
        assertTrue(plan.platformRequirements.requiresImageRendering)
    }

    @Test fun `example completion masks exact safe span`() {
        val plan = created(request(RecallMode.EXAMPLE_COMPLETION, RecallDirection.CONTEXT_TO_TEXT, example = true))
        val prompt = assertIs<RecallPrompt.ExampleCompletion>(plan.prompt)
        assertEquals("A ____ appears.", prompt.example)
        assertEquals("word", plan.answerContract.canonicalAnswer)
        assertFalse(prompt.example.contains("word"))
    }

    @Test fun `unsafe example decision returns stale typed failure`() {
        assertIs<RecallPlanFactoryResult.StaleStrategyDecision>(factory.create(request(RecallMode.EXAMPLE_COMPLETION, RecallDirection.CONTEXT_TO_TEXT)))
    }

    @Test fun `multiple choice requires provider and rejects invalid correct counts`() {
        val base = request(RecallMode.MULTIPLE_CHOICE, RecallDirection.SOURCE_TO_TARGET)
        assertIs<RecallPlanFactoryResult.ExternalGenerationRequired>(factory.create(base))
        listOf(
            listOf(RecallChoice("a", "translation", false), RecallChoice("b", "other", false)),
            listOf(RecallChoice("a", "translation", true), RecallChoice("b", "other", true))
        ).forEach { choices ->
            val result = factory.create(base.copy(multipleChoiceOptionProvider = provider(choices)))
            assertEquals(RecallPlanGenerationFailure.INVALID_PROVIDER_OPTIONS, assertIs<RecallPlanFactoryResult.GenerationFailed>(result).reason)
        }
    }

    @Test fun `valid provider creates deterministic choice plan`() {
        val choices = listOf(RecallChoice("correct", "translation", true), RecallChoice("other", "other", false))
        val plan = created(request(RecallMode.MULTIPLE_CHOICE, RecallDirection.SOURCE_TO_TARGET,
            provider = provider(choices)))
        assertEquals(choices, assertIs<RecallPrompt.MultipleChoice>(plan.prompt).choices)
        assertTrue(plan.platformRequirements.requiresChoiceSelection)
        assertEquals(RecallAnswerKind.CHOICE, plan.answerContract.kind)
    }

    @Test fun `identity and invalid direction return typed invalid request`() {
        val identity = request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET).copy(contentId = ContentId("other"))
        assertContains(assertIs<RecallPlanFactoryResult.InvalidRequest>(factory.create(identity)).violations, RecallPlanViolation.IDENTITY_MISMATCH)
        val direction = request(RecallMode.TYPING, RecallDirection.AUDIO_TO_TEXT)
        assertContains(assertIs<RecallPlanFactoryResult.InvalidRequest>(factory.create(direction)).violations, RecallPlanViolation.INVALID_DIRECTION)
    }

    @Test fun `unsupported contract version is typed`() {
        val result = factory.create(request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET).copy(contractVersion = RecallContractVersion(99)))
        assertEquals(RecallContractVersion(99), assertIs<RecallPlanFactoryResult.UnsupportedContractVersion>(result).version)
    }

    @Test fun `unsupported plan policy version is typed`() {
        val input = request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET)
        val result = factory.create(input.copy(policy = input.policy.copy(version = "future")))
        assertEquals(listOf(RecallPlanViolation.UNSUPPORTED_POLICY_VERSION), assertIs<RecallPlanFactoryResult.InvalidRequest>(result).violations)
    }

    @Test fun `same authority produces same serialized plan and nonce changes identity`() {
        val input = request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET)
        val first = created(input)
        val second = created(input)
        assertEquals(first, second)
        assertEquals(RecallPlanWireCodec.encode(first), RecallPlanWireCodec.encode(second))
        assertNotEquals(first.planId, created(input.copy(attemptNonce = RecallAttemptNonce("attempt-2"))).planId)
        assertEquals(first, assertIs<RecallPlanDecodeResult.Success>(RecallPlanWireCodec.decode(RecallPlanWireCodec.encode(first))).plan)
    }

    @Test fun `assistance is mode appropriate subset of projection`() {
        val typing = created(request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET, example = true))
        assertContains(typing.availableAssistance, RecallAssistance.EXAMPLE_VIEWED)
        assertContains(typing.availableAssistance, RecallAssistance.ANSWER_REVEALED)
        val image = created(request(RecallMode.IMAGE_RECALL, RecallDirection.IMAGE_TO_TEXT, image = true, example = true))
        assertFalse(RecallAssistance.EXAMPLE_VIEWED in image.availableAssistance)
    }

    @Test fun `practice is explicit and never promotion evidence`() {
        val plan = created(request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET,
            context = RecallStrategyContext.PRACTICE_ONLY))
        assertEquals(RecallProvenance.PRACTICE, plan.provenance)
        assertEquals(RecallEvidenceEligibility.INELIGIBLE, plan.evidenceClass)
        val evaluative = created(request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET))
        assertEquals(RecallProvenance.EVALUATIVE, evaluative.provenance)
        assertEquals(RecallEvidenceEligibility.STANDARD, evaluative.evidenceClass)
    }

    @Test fun `registry reports duplicate and factory reports missing handler`() {
        val handler = object : RecallPromptFactory {
            override val mode = RecallMode.TYPING
            override fun build(request: RecallPlanRequest) = error("not invoked")
        }
        assertEquals(listOf(RecallMode.TYPING), assertIs<RecallPromptFactoryRegistryResult.DuplicateHandlers>(
            RecallPromptFactoryRegistry.create(listOf(handler, handler))).modes)
        val empty = assertIs<RecallPromptFactoryRegistryResult.Created>(RecallPromptFactoryRegistry.create(emptyList())).registry
        assertIs<RecallPlanFactoryResult.UnsupportedMode>(RecallPlanFactory(empty).create(request(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET)))
    }

    @Test fun `leakage validator rejects front text containing canonical answer`() {
        val answer = RecallAnswerContract("word", emptyList(), RecallNormalizationPolicyId("typing-v1"),
            CaseSensitivity.INSENSITIVE, PunctuationPolicy.IGNORE, WhitespacePolicy.NORMALIZE,
            RecallLanguageTag("en"), RecallAnswerKind.TEXT)
        assertTrue(AnswerLeakageValidator.leaks(RecallPrompt.Typing("WORD"), answer))
        assertTrue(AnswerLeakageValidator.leaks(RecallPrompt.ExampleCompletion("A word.", RecallTextSpan(2, 6)), answer))
    }

    @Test fun `wire identifiers and provider ordering remain stable`() {
        assertEquals("multiple-choice-options", RecallExternalProviderKind.MULTIPLE_CHOICE_OPTIONS.wireId)
        assertEquals("answer-leakage", RecallPlanGenerationFailure.ANSWER_LEAKAGE.wireId)
        val choices = listOf(RecallChoice("z", "translation", true), RecallChoice("a", "other", false))
        assertEquals(choices, assertIs<RecallPrompt.MultipleChoice>(created(request(RecallMode.MULTIPLE_CHOICE,
            RecallDirection.SOURCE_TO_TARGET, provider = provider(choices))).prompt).choices)
    }

    private fun created(request: RecallPlanRequest) = assertIs<RecallPlanFactoryResult.Created>(factory.create(request)).plan
    private fun provider(choices: List<RecallChoice>) = MultipleChoiceOptionProvider {
        MultipleChoiceOptionSet(choices, RecallChoiceProviderProvenance.TEST_FIXTURE)
    }

    private fun request(
        mode: RecallMode, direction: RecallDirection, audio: Boolean = false, image: Boolean = false,
        example: Boolean = false, provider: MultipleChoiceOptionProvider? = null,
        context: RecallStrategyContext = RecallStrategyContext.EVALUATIVE
    ): RecallPlanRequest {
        val content = Content(ContentId("content"), ContentType.WORD,
            ContentText("word", "translation", exampleText = if (example) "A word appears." else null),
            ContentMedia(primaryAudio = if (audio) "asset:audio" else null, image = if (image) "asset:image" else null))
        val projection = ContentRecallCapabilityResolver.resolve(content)
        return RecallPlanRequest(
            LearnerId("learner"), content.id, LearningItemId("item"), SessionId("session"), RecallAttemptNonce("attempt-1"),
            decision(mode, direction), projection, content, RecallPlanPolicy(sourceLanguage = RecallLanguageTag("en"), targetLanguage = RecallLanguageTag("vi")),
            RecallContractVersion.CURRENT, RecallDeterministicSeed(17), Moment(1_000), context, provider
        )
    }

    private fun decision(mode: RecallMode, direction: RecallDirection) = RecallStrategyDecision(
        mode, direction, RecallStrategyConfidence.HIGH, RecallStrategyReason.CAPABILITY_AVAILABLE, emptyList(), emptyList(), emptyList(),
        RecallDeterministicSeed(17), "adaptive-recall-v1", RecallContractVersion.CURRENT,
        ContentId("content"), LearnerId("learner"), Moment(900)
    )
}
