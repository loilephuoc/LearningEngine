package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class DeterministicMultipleChoiceGeneratorTest {
    @Test fun `generates requested count with exactly one correct and stable ids`() {
        val set = generated(request(candidates = candidates(5)))
        assertEquals(4, set.choices.size)
        assertEquals(1, set.choices.count(RecallChoice::correct))
        assertEquals(set.choices.size, set.choices.map(RecallChoice::id).distinct().size)
        assertTrue(set.choices.all { it.id.startsWith("choice-") })
    }

    @Test fun `filters target out of scope blank disabled malformed and ambiguous candidates`() {
        val input = listOf(
            candidate("target", "x", contentId = "target"), candidate("outside", "x", scope = "other"),
            candidate("blank", "  "), candidate("disabled", "x", state = MultipleChoiceCandidateState.DISABLED),
            candidate("malformed", "x", malformed = true), candidate("ambiguous", "x", ambiguous = true),
            candidate("safe", "safe")
        )
        val result = assertIs<MultipleChoiceGenerationResult.InsufficientSafeDistractors>(
            generate(request(candidates = input, policy = policy(desired = 4, minimum = 3))))
        val reasons = result.rejected.flatMap(RejectedMultipleChoiceCandidate::reasons).toSet()
        assertContains(reasons, MultipleChoiceRejectedReason.TARGET_CONTENT)
        assertContains(reasons, MultipleChoiceRejectedReason.OUT_OF_SCOPE)
        assertContains(reasons, MultipleChoiceRejectedReason.BLANK_ANSWER)
        assertContains(reasons, MultipleChoiceRejectedReason.DISABLED_CONTENT)
        assertContains(reasons, MultipleChoiceRejectedReason.MALFORMED_CONTENT)
        assertContains(reasons, MultipleChoiceRejectedReason.AMBIGUOUS_MEANING)
    }

    @Test fun `deduplicates sibling content and normalized answers`() {
        val input = listOf(
            candidate("one", "First"), candidate("one", "Sibling"),
            candidate("two", " Same  Answer "), candidate("three", "same answer"), candidate("four", "safe")
        )
        val result = generated(request(candidates = input, policy = policy(desired = 3, minimum = 2)))
        val reasons = result.rejectedCandidates.flatMap(RejectedMultipleChoiceCandidate::reasons)
        assertContains(reasons, MultipleChoiceRejectedReason.SIBLING_CONTENT)
        assertContains(reasons, MultipleChoiceRejectedReason.DUPLICATE_NORMALIZED_ANSWER)
    }

    @Test fun `same as correct and accepted alternative collisions are rejected`() {
        val input = listOf(candidate("same", "Correct!"), candidate("alt", "accepted"), candidate("safe", "safe"))
        val result = generated(request(candidates = input, accepted = setOf("accepted"), policy = policy(desired = 2, minimum = 2)))
        val reasons = result.rejectedCandidates.flatMap(RejectedMultipleChoiceCandidate::reasons)
        assertContains(reasons, MultipleChoiceRejectedReason.SAME_AS_CORRECT_ANSWER)
        assertContains(reasons, MultipleChoiceRejectedReason.ACCEPTED_ALTERNATIVE_COLLISION)
    }

    @Test fun `ranking prefers same pos topic then length`() {
        val input = listOf(
            candidate("best", "right", pos = "NOUN", topic = "topic"),
            candidate("pos", "very long distractor", pos = "NOUN", topic = "other"),
            candidate("topic", "close", pos = "VERB", topic = "topic"), candidate("other", "other")
        )
        val set = generated(request(candidates = input, policy = policy(desired = 2, minimum = 2)))
        assertContains(set.choices.map(RecallChoice::text), "right")
        assertEquals(MultipleChoiceFallbackTier.SAME_POS_AND_TOPIC, set.fallbackTier)
    }

    @Test fun `fallback tiers expand from pos topic to pos then safe scope`() {
        val tier2 = generated(request(candidates = listOf(candidate("a", "a", pos = "NOUN"), candidate("b", "b", pos = "NOUN")),
            policy = policy(desired = 3, minimum = 2)))
        assertEquals(MultipleChoiceFallbackTier.SAME_POS_IN_SCOPE, tier2.fallbackTier)
        val tier3 = generated(request(candidates = listOf(candidate("a", "a", pos = "VERB"), candidate("b", "b", pos = "ADJ")),
            policy = policy(desired = 3, minimum = 2)))
        assertEquals(MultipleChoiceFallbackTier.SAFE_IN_SCOPE, tier3.fallbackTier)
    }

    @Test fun `reduced option fallback is explicit and policy controlled`() {
        val reduced = generated(request(candidates = candidates(2), policy = policy(desired = 4, minimum = 2, reduce = true)))
        assertEquals(3, reduced.choices.size)
        assertEquals(MultipleChoiceFallbackTier.REDUCED_OPTION_COUNT, reduced.fallbackTier)
        assertIs<MultipleChoiceGenerationResult.InsufficientSafeDistractors>(generate(
            request(candidates = candidates(2), policy = policy(desired = 4, minimum = 2, reduce = false))))
    }

    @Test fun `insufficient safe candidates fails without invented options`() {
        val result = generate(request(candidates = listOf(candidate("same", "Correct")), policy = policy(desired = 4, minimum = 2)))
        assertIs<MultipleChoiceGenerationResult.InsufficientSafeDistractors>(result)
    }

    @Test fun `same seed is identical and different seed preserves membership`() {
        val input = request(candidates = candidates(8), seed = 11)
        val first = generated(input)
        assertEquals(first, generated(input))
        val second = generated(input.copy(deterministicSeed = RecallDeterministicSeed(91)))
        assertEquals(first.choices.map(RecallChoice::text).toSet(), second.choices.map(RecallChoice::text).toSet())
        assertNotEquals(first.choices, second.choices)
    }

    @Test fun `correct option is not fixed at index zero across seeds`() {
        val positions = (1L..12L).map { seed -> generated(request(candidates = candidates(5), seed = seed)).choices.indexOfFirst(RecallChoice::correct) }.toSet()
        assertTrue(positions.size > 1)
        assertTrue(positions.any { it != 0 })
    }

    @Test fun `directions select correct answer side`() {
        val c = listOf(candidate("candidate", target = "dịch", source = "source"))
        assertContains(generated(request(direction = RecallDirection.SOURCE_TO_TARGET, candidates = c, policy = policy(2, 2))).choices.map { it.text }, "dịch")
        listOf(RecallDirection.TARGET_TO_SOURCE, RecallDirection.IMAGE_TO_TEXT, RecallDirection.AUDIO_TO_TEXT).forEach { direction ->
            assertContains(generated(request(direction = direction, candidates = c, policy = policy(2, 2))).choices.map { it.text }, "source")
        }
    }

    @Test fun `unsupported direction and version are typed`() {
        assertIs<MultipleChoiceGenerationResult.UnsupportedDirection>(generate(request(direction = RecallDirection.CONTEXT_TO_TEXT)))
        assertIs<MultipleChoiceGenerationResult.UnsupportedVersion>(generate(request().copy(contractVersion = RecallContractVersion(99))))
    }

    @Test fun `policy validation rejects invalid thresholds`() {
        assertFailsWith<IllegalArgumentException> { policy(desired = 1, minimum = 1) }
        assertFailsWith<IllegalArgumentException> { policy(desired = 3, minimum = 4) }
        assertFailsWith<IllegalArgumentException> { MultipleChoiceDistractorPolicy(maximumCandidateScanCount = 0) }
    }

    @Test fun `bounded large inventory is deterministic`() {
        val input = request(candidates = candidates(5_000), policy = policy(4, 2).copy(maximumCandidateScanCount = 500))
        val first = generated(input)
        assertEquals(500, first.scannedCandidateCount)
        assertEquals(first, generated(input))
    }

    @Test fun `provider integrates into plan factory`() {
        val content = Content(ContentId("target"), ContentType.WORD, ContentText("word", "Correct"))
        val projection = ContentRecallCapabilityResolver.resolve(content)
        val decision = RecallStrategyDecision(RecallMode.MULTIPLE_CHOICE, RecallDirection.SOURCE_TO_TARGET,
            RecallStrategyConfidence.HIGH, RecallStrategyReason.CAPABILITY_AVAILABLE, emptyList(), emptyList(), emptyList(),
            RecallDeterministicSeed(5), "adaptive-recall-v1", RecallContractVersion.CURRENT, content.id, LearnerId("learner"), Moment(1))
        val provider = DeterministicMultipleChoiceOptionProvider(MultipleChoiceScopeId("scope"), candidates(5), policy(), "NOUN", "topic")
        val request = RecallPlanRequest(LearnerId("learner"), content.id, LearningItemId("item"), SessionId("session"), RecallAttemptNonce("attempt"),
            decision, projection, content, RecallPlanPolicy(sourceLanguage = RecallLanguageTag("en"), targetLanguage = RecallLanguageTag("vi")),
            RecallContractVersion.CURRENT, RecallDeterministicSeed(5), Moment(2), RecallStrategyContext.EVALUATIVE,
            typedMultipleChoiceOptionProvider = provider)
        val plan = assertIs<RecallPlanFactoryResult.Created>(RecallPlanFactory().create(request)).plan
        assertEquals(4, assertIs<RecallPrompt.MultipleChoice>(plan.prompt).choices.size)
        assertEquals(RecallPlanValidationResult.Valid, RecallContractValidator.validatePlan(plan))
    }

    @Test fun `provider failure remains typed at factory boundary`() {
        val provider = DeterministicMultipleChoiceOptionProvider(MultipleChoiceScopeId("scope"), emptyList(), policy())
        val result = provider.provideTyped(MultipleChoiceOptionRequest(LearnerId("learner"), ContentId("target"),
            Content(ContentId("target"), ContentType.WORD, ContentText("word", "Correct")), RecallDirection.SOURCE_TO_TARGET,
            "Correct", RecallDeterministicSeed(1)))
        assertEquals(RecallPlanGenerationFailure.INVALID_PROVIDER_OPTIONS, assertIs<MultipleChoiceOptionProviderResult.Failed>(result).reason)
    }

    @Test fun `wire ids are stable and ordinal independent`() {
        assertEquals("same-pos-and-topic", MultipleChoiceFallbackTier.SAME_POS_AND_TOPIC.wireId)
        assertEquals("duplicate-normalized-answer", MultipleChoiceRejectedReason.DUPLICATE_NORMALIZED_ANSWER.wireId)
        assertEquals("active-package", MultipleChoiceCandidateProvenance.ACTIVE_PACKAGE.wireId)
    }

    @Test fun `option set wire round trip preserves choices provenance and diagnostics`() {
        val set = generated(request(candidates = listOf(candidate("same", "Correct")) + candidates(4)))
        val encoded = MultipleChoiceOptionSetWireCodec.encode(set)
        assertEquals(set, assertIs<MultipleChoiceOptionSetDecodeResult.Success>(MultipleChoiceOptionSetWireCodec.decode(encoded)).optionSet)
        assertIs<MultipleChoiceOptionSetDecodeResult.Malformed>(MultipleChoiceOptionSetWireCodec.decode("{}"))
    }

    private fun generate(r: MultipleChoiceGenerationRequest) = DeterministicMultipleChoiceGenerator.generate(r)
    private fun generated(r: MultipleChoiceGenerationRequest) = assertIs<MultipleChoiceGenerationResult.Generated>(generate(r)).optionSet
    private fun policy(desired: Int = 4, minimum: Int = 2, reduce: Boolean = true) = MultipleChoiceDistractorPolicy(
        desiredOptionCount = desired, minimumSafeOptionCount = minimum, allowReducedOptionCount = reduce
    )
    private fun request(
        direction: RecallDirection = RecallDirection.SOURCE_TO_TARGET,
        candidates: List<MultipleChoiceCandidate> = candidates(5), accepted: Set<String> = emptySet(),
        policy: MultipleChoiceDistractorPolicy = policy(), seed: Long = 7
    ) = MultipleChoiceGenerationRequest(LearnerId("learner"), ContentId("target"), MultipleChoiceScopeId("scope"),
        direction, "Correct", accepted, "NOUN", "topic", candidates, policy, RecallDeterministicSeed(seed),
        RecallContractVersion.CURRENT, MultipleChoiceGenerationProvenance.DIRECT_SHARED_CORE)

    private fun candidates(count: Int) = (1..count).map { candidate("c$it", "option-$it", pos = if (it <= 3) "NOUN" else "VERB", topic = if (it <= 2) "topic" else "other") }
    private fun candidate(
        id: String, target: String?, source: String? = "source-$id", contentId: String = id, scope: String = "scope",
        pos: String? = null, topic: String? = null, state: MultipleChoiceCandidateState = MultipleChoiceCandidateState.ACTIVE,
        malformed: Boolean = false, ambiguous: Boolean = false
    ) = MultipleChoiceCandidate(ContentId(contentId), MultipleChoiceScopeId(scope), source, target, pos, topic,
        state = state, malformed = malformed, ambiguousWithTarget = ambiguous)
}
