package vn.loi.learning.application.recall

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

@JvmInline value class RecallAttemptNonce(val value: String) { init { require(value.isNotBlank()) } }

enum class RecallPlanViolation(override val wireId: String) : StableWireValue {
    IDENTITY_MISMATCH("identity-mismatch"), INVALID_DIRECTION("invalid-direction"),
    UNSUPPORTED_POLICY_VERSION("unsupported-policy-version"),
    INVALID_ANSWER_CONTRACT("invalid-answer-contract"), INVALID_PROVIDER_OUTPUT("invalid-provider-output"),
    ANSWER_LEAKAGE("answer-leakage"), FINAL_PLAN_INVALID("final-plan-invalid")
}

enum class RecallExternalProviderKind(override val wireId: String) : StableWireValue {
    MULTIPLE_CHOICE_OPTIONS("multiple-choice-options")
}

enum class RecallPlanGenerationFailure(override val wireId: String) : StableWireValue {
    MISSING_CONTENT_TEXT("missing-content-text"), MISSING_MEDIA_REFERENCE("missing-media-reference"),
    MISSING_SAFE_TARGET_SPAN("missing-safe-target-span"), INVALID_PROVIDER_OPTIONS("invalid-provider-options"),
    ANSWER_LEAKAGE("answer-leakage"), FINAL_VALIDATION_FAILED("final-validation-failed")
}

enum class RecallChoiceProviderProvenance(override val wireId: String) : StableWireValue {
    CONTENT_LIBRARY("content-library"), PACKAGE_SCOPE("package-scope"), TEST_FIXTURE("test-fixture")
}

data class RecallPlanPolicy(
    val version: String = "recall-plan-v1",
    val normalizationPolicy: RecallNormalizationPolicyId = RecallNormalizationPolicyId("typing-v1"),
    val sourceLanguage: RecallLanguageTag,
    val targetLanguage: RecallLanguageTag,
    val caseSensitivity: CaseSensitivity = CaseSensitivity.INSENSITIVE,
    val punctuationPolicy: PunctuationPolicy = PunctuationPolicy.IGNORE,
    val whitespacePolicy: WhitespacePolicy = WhitespacePolicy.NORMALIZE
) { init { require(version.isNotBlank()) } }

data class MultipleChoiceOptionRequest(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val content: Content,
    val direction: RecallDirection,
    val canonicalAnswer: String,
    val deterministicSeed: RecallDeterministicSeed
)

data class MultipleChoiceOptionSet(
    val choices: List<RecallChoice>,
    val provenance: RecallChoiceProviderProvenance,
    val fallbackTier: MultipleChoiceFallbackTier? = null,
    val scannedCandidateCount: Int = 0,
    val eligibleCandidateCount: Int = 0,
    val rejectedCandidates: List<RejectedMultipleChoiceCandidate> = emptyList()
)

fun interface MultipleChoiceOptionProvider {
    fun provide(request: MultipleChoiceOptionRequest): MultipleChoiceOptionSet
}

sealed interface MultipleChoiceOptionProviderResult {
    data class Generated(val optionSet: MultipleChoiceOptionSet) : MultipleChoiceOptionProviderResult
    data class Failed(val reason: RecallPlanGenerationFailure) : MultipleChoiceOptionProviderResult
}

fun interface TypedMultipleChoiceOptionProvider {
    fun provideTyped(request: MultipleChoiceOptionRequest): MultipleChoiceOptionProviderResult
}

data class RecallPlanRequest(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val learningItemId: LearningItemId?,
    val sessionId: SessionId,
    val attemptNonce: RecallAttemptNonce,
    val strategyDecision: RecallStrategyDecision,
    val capabilityProjection: RecallCapabilityProjection,
    val content: Content,
    val policy: RecallPlanPolicy,
    val contractVersion: RecallContractVersion,
    val deterministicSeed: RecallDeterministicSeed,
    val generatedAt: Moment,
    val evaluationContext: RecallStrategyContext,
    val multipleChoiceOptionProvider: MultipleChoiceOptionProvider? = null,
    val typedMultipleChoiceOptionProvider: TypedMultipleChoiceOptionProvider? = null
)

sealed interface RecallPlanFactoryResult {
    data class Created(val plan: RecallPlan) : RecallPlanFactoryResult
    data class InvalidRequest(val violations: List<RecallPlanViolation>) : RecallPlanFactoryResult
    data class StaleStrategyDecision(val reasons: List<RecallStrategyReason>) : RecallPlanFactoryResult
    data class CapabilityMismatch(val reasons: List<RecallUnavailableReason>) : RecallPlanFactoryResult
    data class UnsupportedMode(val mode: RecallMode) : RecallPlanFactoryResult
    data class ExternalGenerationRequired(val mode: RecallMode, val providerKind: RecallExternalProviderKind) : RecallPlanFactoryResult
    data class GenerationFailed(val reason: RecallPlanGenerationFailure) : RecallPlanFactoryResult
    data class UnsupportedContractVersion(val version: RecallContractVersion) : RecallPlanFactoryResult
}

sealed interface RecallPromptBuildResult {
    data class Built(val prompt: RecallPrompt, val answer: RecallAnswerContract) : RecallPromptBuildResult
    data class ExternalRequired(val kind: RecallExternalProviderKind) : RecallPromptBuildResult
    data class Failed(val reason: RecallPlanGenerationFailure) : RecallPromptBuildResult
}

interface RecallPromptFactory { val mode: RecallMode; fun build(request: RecallPlanRequest): RecallPromptBuildResult }

sealed interface RecallPromptFactoryRegistryResult {
    data class Created(val registry: RecallPromptFactoryRegistry) : RecallPromptFactoryRegistryResult
    data class DuplicateHandlers(val modes: List<RecallMode>) : RecallPromptFactoryRegistryResult
}

class RecallPromptFactoryRegistry private constructor(private val factories: Map<RecallMode, RecallPromptFactory>) {
    fun factoryFor(mode: RecallMode): RecallPromptFactory? = factories[mode]
    companion object {
        fun create(factories: List<RecallPromptFactory>): RecallPromptFactoryRegistryResult {
            val duplicates = factories.groupBy(RecallPromptFactory::mode).filterValues { it.size > 1 }.keys.sortedBy { it.wireId }
            return if (duplicates.isEmpty()) RecallPromptFactoryRegistryResult.Created(RecallPromptFactoryRegistry(factories.associateBy { it.mode }))
            else RecallPromptFactoryRegistryResult.DuplicateHandlers(duplicates)
        }
        fun standard(): RecallPromptFactoryRegistry = (create(listOf(
            TextRecallPromptFactory(RecallMode.TYPING), MultipleChoiceRecallPromptFactory,
            MediaRecallPromptFactory(RecallMode.LISTENING), MediaRecallPromptFactory(RecallMode.IMAGE_RECALL),
            TextRecallPromptFactory(RecallMode.REVERSE_TRANSLATION), ExampleCompletionRecallPromptFactory,
            MediaRecallPromptFactory(RecallMode.DICTATION)
        )) as RecallPromptFactoryRegistryResult.Created).registry
    }
}

class RecallPlanFactory(private val registry: RecallPromptFactoryRegistry = RecallPromptFactoryRegistry.standard()) {
    fun create(request: RecallPlanRequest): RecallPlanFactoryResult {
        if (request.contractVersion != RecallContractVersion.CURRENT) return RecallPlanFactoryResult.UnsupportedContractVersion(request.contractVersion)
        if (request.strategyDecision.decisionVersion != RecallContractVersion.CURRENT) {
            return RecallPlanFactoryResult.UnsupportedContractVersion(request.strategyDecision.decisionVersion)
        }
        if (request.policy.version != "recall-plan-v1") {
            return RecallPlanFactoryResult.InvalidRequest(listOf(RecallPlanViolation.UNSUPPORTED_POLICY_VERSION))
        }
        if (request.learnerId != request.strategyDecision.learnerId || request.contentId != request.strategyDecision.contentId ||
            request.contentId != request.content.id || request.contentId != request.capabilityProjection.contentId) {
            return RecallPlanFactoryResult.InvalidRequest(listOf(RecallPlanViolation.IDENTITY_MISMATCH))
        }
        val mode = request.strategyDecision.selectedMode
        val currentProjection = ContentRecallCapabilityResolver.resolve(request.content)
        if (currentProjection != request.capabilityProjection) {
            val reasons = currentProjection.unavailableReasonsFor(mode).ifEmpty { listOf(RecallUnavailableReason.MALFORMED_CONTENT_DATA) }
            return RecallPlanFactoryResult.CapabilityMismatch(reasons)
        }
        if (!request.capabilityProjection.supports(mode)) {
            return RecallPlanFactoryResult.StaleStrategyDecision(listOf(RecallStrategyReason.CAPABILITY_UNAVAILABLE))
        }
        if (request.strategyDecision.selectedDirection !in request.capabilityProjection.supportedDirectionsFor(mode)) {
            return RecallPlanFactoryResult.InvalidRequest(listOf(RecallPlanViolation.INVALID_DIRECTION))
        }
        val factory = registry.factoryFor(mode) ?: return RecallPlanFactoryResult.UnsupportedMode(mode)
        val built = when (val result = factory.build(request)) {
            is RecallPromptBuildResult.Built -> result
            is RecallPromptBuildResult.ExternalRequired -> return RecallPlanFactoryResult.ExternalGenerationRequired(mode, result.kind)
            is RecallPromptBuildResult.Failed -> return RecallPlanFactoryResult.GenerationFailed(result.reason)
        }
        if (AnswerLeakageValidator.leaks(built.prompt, built.answer)) {
            return RecallPlanFactoryResult.GenerationFailed(RecallPlanGenerationFailure.ANSWER_LEAKAGE)
        }
        val projectionCapabilities = request.capabilityProjection.sourceCapabilities
        val plan = RecallPlan(
            request.contractVersion, derivePlanId(request), request.learnerId, request.contentId,
            request.learningItemId, request.sessionId, mode, request.strategyDecision.selectedDirection,
            built.prompt, built.answer, assistanceFor(mode, request.capabilityProjection),
            if (request.evaluationContext == RecallStrategyContext.EVALUATIVE) RecallEvidenceEligibility.STANDARD else RecallEvidenceEligibility.INELIGIBLE,
            request.deterministicSeed, request.generatedAt,
            if (request.evaluationContext == RecallStrategyContext.EVALUATIVE) RecallProvenance.EVALUATIVE else RecallProvenance.PRACTICE,
            requirementsFor(mode), projectionCapabilities
        )
        return when (RecallContractValidator.validatePlan(plan)) {
            RecallPlanValidationResult.Valid -> RecallPlanFactoryResult.Created(plan)
            is RecallPlanValidationResult.Invalid -> RecallPlanFactoryResult.GenerationFailed(RecallPlanGenerationFailure.FINAL_VALIDATION_FAILED)
        }
    }

    private fun derivePlanId(r: RecallPlanRequest): RecallPlanId {
        val authority = listOf(r.learnerId.value, r.contentId.value, r.sessionId.value, r.attemptNonce.value,
            r.strategyDecision.selectedMode.wireId, r.strategyDecision.selectedDirection.wireId,
            r.deterministicSeed.value.toString(), r.contractVersion.value.toString()).joinToString("|")
        var hash = -3750763034362895579L
        authority.forEach { hash = (hash xor it.code.toLong()) * 1099511628211L }
        return RecallPlanId("plan-${hash.toULong().toString(16)}")
    }
}

private class TextRecallPromptFactory(override val mode: RecallMode) : RecallPromptFactory {
    override fun build(request: RecallPlanRequest): RecallPromptBuildResult {
        val (promptText, answerText, language) = textSides(request) ?: return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_CONTENT_TEXT)
        val prompt = when (mode) {
            RecallMode.TYPING -> RecallPrompt.Typing(promptText)
            RecallMode.REVERSE_TRANSLATION -> RecallPrompt.ReverseTranslation(promptText)
            else -> return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_CONTENT_TEXT)
        }
        return RecallPromptBuildResult.Built(prompt, answer(request, answerText, language, RecallAnswerKind.TEXT))
    }
}

private object MediaRecallPromptFactorySupport {
    fun build(mode: RecallMode, request: RecallPlanRequest): RecallPromptBuildResult {
        val resource = when (mode) {
            RecallMode.IMAGE_RECALL -> request.capabilityProjection.sourceCapabilities.image
            RecallMode.LISTENING, RecallMode.DICTATION -> request.capabilityProjection.sourceCapabilities.wordAudio
            else -> null
        } ?: return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_MEDIA_REFERENCE)
        val prompt = when (mode) {
            RecallMode.IMAGE_RECALL -> RecallPrompt.ImageRecall(resource)
            RecallMode.LISTENING -> RecallPrompt.Listening(resource)
            RecallMode.DICTATION -> RecallPrompt.Dictation(resource)
            else -> return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_MEDIA_REFERENCE)
        }
        return RecallPromptBuildResult.Built(prompt, answer(request, request.content.text.primaryText, request.policy.sourceLanguage, RecallAnswerKind.TEXT))
    }
}

private class MediaRecallPromptFactory(override val mode: RecallMode) : RecallPromptFactory {
    override fun build(request: RecallPlanRequest) = MediaRecallPromptFactorySupport.build(mode, request)
}

private object ExampleCompletionRecallPromptFactory : RecallPromptFactory {
    override val mode = RecallMode.EXAMPLE_COMPLETION
    override fun build(request: RecallPlanRequest): RecallPromptBuildResult {
        val example = request.content.text.exampleText ?: return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_SAFE_TARGET_SPAN)
        val span = request.capabilityProjection.completionTarget ?: return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_SAFE_TARGET_SPAN)
        if (span.endExclusive > example.length) return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_SAFE_TARGET_SPAN)
        val blank = "_".repeat(span.endExclusive - span.startInclusive)
        val masked = example.substring(0, span.startInclusive) + blank + example.substring(span.endExclusive)
        return RecallPromptBuildResult.Built(
            RecallPrompt.ExampleCompletion(masked, RecallTextSpan(span.startInclusive, span.startInclusive + blank.length)),
            answer(request, example.substring(span.startInclusive, span.endExclusive), request.policy.sourceLanguage, RecallAnswerKind.TEXT)
        )
    }
}

private object MultipleChoiceRecallPromptFactory : RecallPromptFactory {
    override val mode = RecallMode.MULTIPLE_CHOICE
    override fun build(request: RecallPlanRequest): RecallPromptBuildResult {
        val (question, canonical, language) = textSides(request) ?: return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.MISSING_CONTENT_TEXT)
        if (request.multipleChoiceOptionProvider == null && request.typedMultipleChoiceOptionProvider == null) {
            return RecallPromptBuildResult.ExternalRequired(RecallExternalProviderKind.MULTIPLE_CHOICE_OPTIONS)
        }
        val providerRequest = MultipleChoiceOptionRequest(request.learnerId, request.contentId, request.content,
            request.strategyDecision.selectedDirection, canonical, request.deterministicSeed)
        val providerResult = request.typedMultipleChoiceOptionProvider?.provideTyped(providerRequest)
            ?: MultipleChoiceOptionProviderResult.Generated(requireNotNull(request.multipleChoiceOptionProvider).provide(providerRequest))
        val result = when (providerResult) {
            is MultipleChoiceOptionProviderResult.Generated -> providerResult.optionSet
            is MultipleChoiceOptionProviderResult.Failed -> return RecallPromptBuildResult.Failed(providerResult.reason)
        }
        if (result.choices.size < 2 || result.choices.count(RecallChoice::correct) != 1 ||
            result.choices.map(RecallChoice::id).distinct().size != result.choices.size ||
            result.choices.singleOrNull(RecallChoice::correct)?.text != canonical) {
            return RecallPromptBuildResult.Failed(RecallPlanGenerationFailure.INVALID_PROVIDER_OPTIONS)
        }
        return RecallPromptBuildResult.Built(RecallPrompt.MultipleChoice(question, result.choices),
            answer(request, canonical, language, RecallAnswerKind.CHOICE))
    }
}

private fun textSides(request: RecallPlanRequest): Triple<String, String, RecallLanguageTag>? {
    val source = request.content.text.primaryText
    val target = request.content.text.translatedText?.takeIf(String::isNotBlank)
    return when (request.strategyDecision.selectedDirection) {
        RecallDirection.SOURCE_TO_TARGET -> target?.let { Triple(source, it, request.policy.targetLanguage) }
        RecallDirection.TARGET_TO_SOURCE -> target?.let { Triple(it, source, request.policy.sourceLanguage) }
        else -> null
    }
}

private fun answer(r: RecallPlanRequest, canonical: String, language: RecallLanguageTag, kind: RecallAnswerKind) = RecallAnswerContract(
    canonical, emptyList(), r.policy.normalizationPolicy, r.policy.caseSensitivity,
    r.policy.punctuationPolicy, r.policy.whitespacePolicy, language, kind
)

private fun requirementsFor(mode: RecallMode) = RecallPlatformRequirements(
    requiresTextInput = mode != RecallMode.MULTIPLE_CHOICE,
    requiresChoiceSelection = mode == RecallMode.MULTIPLE_CHOICE,
    requiresAudioPlayback = mode in setOf(RecallMode.LISTENING, RecallMode.DICTATION),
    requiresImageRendering = mode == RecallMode.IMAGE_RECALL,
    requiresExampleRendering = mode == RecallMode.EXAMPLE_COMPLETION
)

private fun assistanceFor(mode: RecallMode, p: RecallCapabilityProjection): Set<RecallAssistance> {
    val allowed = when (mode) {
        RecallMode.TYPING, RecallMode.REVERSE_TRANSLATION -> setOf(RecallAssistance.LETTER_HINT_USED, RecallAssistance.PRONUNCIATION_HINT_USED, RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.ANSWER_REVEALED)
        RecallMode.LISTENING, RecallMode.DICTATION -> setOf(RecallAssistance.EXAMPLE_VIEWED, RecallAssistance.ANSWER_REVEALED)
        RecallMode.IMAGE_RECALL -> setOf(RecallAssistance.PRONUNCIATION_HINT_USED, RecallAssistance.ANSWER_REVEALED)
        RecallMode.EXAMPLE_COMPLETION -> setOf(RecallAssistance.PRONUNCIATION_HINT_USED, RecallAssistance.ANSWER_REVEALED)
        RecallMode.MULTIPLE_CHOICE -> setOf(RecallAssistance.ANSWER_REVEALED)
    }
    return p.availableAssistance.intersect(allowed).toSortedSet(compareBy(RecallAssistance::wireId))
}

object AnswerLeakageValidator {
    fun leaks(prompt: RecallPrompt, answer: RecallAnswerContract): Boolean {
        val canonical = answer.canonicalAnswer.trim()
        if (canonical.isEmpty()) return true
        return when (prompt) {
            is RecallPrompt.Typing -> prompt.sourceText.equals(canonical, ignoreCase = true)
            is RecallPrompt.ReverseTranslation -> prompt.targetText.equals(canonical, ignoreCase = true)
            is RecallPrompt.ExampleCompletion -> prompt.example.contains(canonical, ignoreCase = true)
            is RecallPrompt.MultipleChoice -> false // choices necessarily expose candidate answers.
            is RecallPrompt.Listening, is RecallPrompt.ImageRecall, is RecallPrompt.Dictation -> false
        }
    }
}
