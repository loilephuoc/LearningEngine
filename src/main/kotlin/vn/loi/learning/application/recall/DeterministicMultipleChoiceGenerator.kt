package vn.loi.learning.application.recall

import java.text.Normalizer
import java.util.Locale
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.recall.*

@JvmInline value class MultipleChoiceScopeId(val value: String) { init { require(value.isNotBlank()) } }

enum class MultipleChoiceCandidateState(override val wireId: String) : StableWireValue {
    ACTIVE("active"), DISABLED("disabled"), SUSPENDED("suspended"), DELETED("deleted")
}

enum class MultipleChoiceCandidateProvenance(override val wireId: String) : StableWireValue {
    ACTIVE_PACKAGE("active-package"), ACTIVE_TOPIC("active-topic"), ACTIVE_SCOPE("active-scope")
}

enum class MultipleChoiceGenerationProvenance(override val wireId: String) : StableWireValue {
    PLAN_FACTORY_PROVIDER("plan-factory-provider"), DIRECT_SHARED_CORE("direct-shared-core")
}

enum class MultipleChoiceFallbackTier(override val wireId: String) : StableWireValue {
    SAME_POS_AND_TOPIC("same-pos-and-topic"), SAME_POS_IN_SCOPE("same-pos-in-scope"),
    SAFE_IN_SCOPE("safe-in-scope"), REDUCED_OPTION_COUNT("reduced-option-count")
}

enum class MultipleChoiceRejectedReason(override val wireId: String) : StableWireValue {
    TARGET_CONTENT("target-content"), SIBLING_CONTENT("sibling-content"), OUT_OF_SCOPE("out-of-scope"),
    MISSING_ANSWER_TEXT("missing-answer-text"), BLANK_ANSWER("blank-answer"),
    DUPLICATE_NORMALIZED_ANSWER("duplicate-normalized-answer"), SAME_AS_CORRECT_ANSWER("same-as-correct-answer"),
    ACCEPTED_ALTERNATIVE_COLLISION("accepted-alternative-collision"), AMBIGUOUS_MEANING("ambiguous-meaning"),
    MALFORMED_CONTENT("malformed-content"), DISABLED_CONTENT("disabled-content"),
    UNSUPPORTED_DIRECTION("unsupported-direction"), POLICY_EXCLUDED("policy-excluded")
}

enum class MultipleChoiceGenerationViolation(override val wireId: String) : StableWireValue {
    IDENTITY_MISMATCH("identity-mismatch"), INVALID_OPTION_COUNT("invalid-option-count"),
    INVALID_POLICY("invalid-policy"), BLANK_CORRECT_ANSWER("blank-correct-answer")
}

data class MultipleChoiceCandidate(
    val contentId: ContentId,
    val scopeId: MultipleChoiceScopeId,
    val sourceText: String?,
    val targetText: String?,
    val partOfSpeech: String? = null,
    val topicId: String? = null,
    val acceptedAlternatives: Set<String> = emptySet(),
    val state: MultipleChoiceCandidateState = MultipleChoiceCandidateState.ACTIVE,
    val provenance: MultipleChoiceCandidateProvenance = MultipleChoiceCandidateProvenance.ACTIVE_SCOPE,
    val malformed: Boolean = false,
    val ambiguousWithTarget: Boolean = false
)

data class RejectedMultipleChoiceCandidate(
    val contentId: ContentId,
    val reasons: List<MultipleChoiceRejectedReason>
)

data class MultipleChoiceDistractorPolicy(
    val desiredOptionCount: Int = 4,
    val minimumSafeOptionCount: Int = 2,
    val allowReducedOptionCount: Boolean = true,
    val samePosWeight: Int = 40,
    val sameTopicWeight: Int = 30,
    val lengthSimilarityWeight: Int = 20,
    val maximumCandidateScanCount: Int = 2_000,
    val punctuationIgnoredForAmbiguity: Boolean = true
) {
    init {
        require(desiredOptionCount >= 2)
        require(minimumSafeOptionCount in 2..desiredOptionCount)
        require(samePosWeight >= 0 && sameTopicWeight >= 0 && lengthSimilarityWeight >= 0)
        require(maximumCandidateScanCount > 0)
    }
}

data class MultipleChoiceGenerationRequest(
    val learnerId: LearnerId,
    val targetContentId: ContentId,
    val scopeId: MultipleChoiceScopeId,
    val direction: RecallDirection,
    val canonicalAnswer: String,
    val acceptedAlternatives: Set<String>,
    val targetPartOfSpeech: String?,
    val targetTopicId: String?,
    val candidates: List<MultipleChoiceCandidate>,
    val policy: MultipleChoiceDistractorPolicy,
    val deterministicSeed: RecallDeterministicSeed,
    val contractVersion: RecallContractVersion,
    val provenance: MultipleChoiceGenerationProvenance
)

sealed interface MultipleChoiceGenerationResult {
    data class Generated(val optionSet: MultipleChoiceOptionSet) : MultipleChoiceGenerationResult
    data class InvalidRequest(val violations: List<MultipleChoiceGenerationViolation>) : MultipleChoiceGenerationResult
    data class InsufficientCandidates(val available: Int, val required: Int) : MultipleChoiceGenerationResult
    data class InsufficientSafeDistractors(
        val available: Int,
        val required: Int,
        val rejected: List<RejectedMultipleChoiceCandidate>
    ) : MultipleChoiceGenerationResult
    data class AmbiguousTarget(val normalizedAnswer: String) : MultipleChoiceGenerationResult
    data class UnsupportedDirection(val direction: RecallDirection) : MultipleChoiceGenerationResult
    data class UnsupportedVersion(val version: RecallContractVersion) : MultipleChoiceGenerationResult
}

object DeterministicMultipleChoiceGenerator {
    fun generate(request: MultipleChoiceGenerationRequest): MultipleChoiceGenerationResult {
        if (request.contractVersion != RecallContractVersion.CURRENT) return MultipleChoiceGenerationResult.UnsupportedVersion(request.contractVersion)
        if (request.canonicalAnswer.isBlank()) return MultipleChoiceGenerationResult.InvalidRequest(listOf(MultipleChoiceGenerationViolation.BLANK_CORRECT_ANSWER))
        if (request.direction !in SUPPORTED_DIRECTIONS) return MultipleChoiceGenerationResult.UnsupportedDirection(request.direction)

        val normalizedCorrect = normalize(request.canonicalAnswer, request.policy)
        val normalizedAlternatives = request.acceptedAlternatives.mapTo(linkedSetOf()) { normalize(it, request.policy) }
        if (normalizedCorrect in normalizedAlternatives) return MultipleChoiceGenerationResult.AmbiguousTarget(normalizedCorrect)

        val scanned = request.candidates.sortedBy { it.contentId.value }.take(request.policy.maximumCandidateScanCount)
        if (scanned.isEmpty()) return MultipleChoiceGenerationResult.InsufficientCandidates(0, request.policy.desiredOptionCount - 1)
        val rejected = mutableListOf<RejectedMultipleChoiceCandidate>()
        val seenContent = mutableSetOf<ContentId>()
        val seenAnswers = mutableSetOf<String>()
        val eligible = mutableListOf<RankedCandidate>()
        for (candidate in scanned) {
            val reasons = linkedSetOf<MultipleChoiceRejectedReason>()
            if (candidate.contentId == request.targetContentId) reasons += MultipleChoiceRejectedReason.TARGET_CONTENT
            if (!seenContent.add(candidate.contentId)) reasons += MultipleChoiceRejectedReason.SIBLING_CONTENT
            if (candidate.scopeId != request.scopeId) reasons += MultipleChoiceRejectedReason.OUT_OF_SCOPE
            if (candidate.state != MultipleChoiceCandidateState.ACTIVE) reasons += MultipleChoiceRejectedReason.DISABLED_CONTENT
            if (candidate.malformed) reasons += MultipleChoiceRejectedReason.MALFORMED_CONTENT
            val answer = answerSide(candidate, request.direction)
            if (answer == null) reasons += MultipleChoiceRejectedReason.MISSING_ANSWER_TEXT
            else if (answer.isBlank()) reasons += MultipleChoiceRejectedReason.BLANK_ANSWER
            val normalized = answer?.takeIf(String::isNotBlank)?.let { normalize(it, request.policy) }
            if (normalized != null && normalized == normalizedCorrect) reasons += MultipleChoiceRejectedReason.SAME_AS_CORRECT_ANSWER
            if (normalized != null && normalized in normalizedAlternatives) reasons += MultipleChoiceRejectedReason.ACCEPTED_ALTERNATIVE_COLLISION
            if (candidate.ambiguousWithTarget) reasons += MultipleChoiceRejectedReason.AMBIGUOUS_MEANING
            if (normalized != null && reasons.isEmpty() && !seenAnswers.add(normalized)) {
                reasons += MultipleChoiceRejectedReason.DUPLICATE_NORMALIZED_ANSWER
            }
            if (reasons.isNotEmpty()) {
                rejected += RejectedMultipleChoiceCandidate(candidate.contentId, reasons.sortedBy { it.wireId })
            } else {
                eligible += RankedCandidate(candidate, requireNotNull(answer), requireNotNull(normalized), score(candidate, answer, request))
            }
        }

        val desiredDistractors = request.policy.desiredOptionCount - 1
        val minimumDistractors = request.policy.minimumSafeOptionCount - 1
        if (eligible.size < minimumDistractors) {
            return MultipleChoiceGenerationResult.InsufficientSafeDistractors(eligible.size, minimumDistractors, rejected.sortedBy { it.contentId.value })
        }
        val take = if (eligible.size >= desiredDistractors) desiredDistractors
            else if (request.policy.allowReducedOptionCount) eligible.size
            else return MultipleChoiceGenerationResult.InsufficientSafeDistractors(eligible.size, desiredDistractors, rejected.sortedBy { it.contentId.value })
        val selected = eligible.sortedWith(compareByDescending<RankedCandidate> { it.score }
            .thenBy { seededKey(it.candidate.contentId.value, request.deterministicSeed.value) }
            .thenBy { it.candidate.contentId.value }).take(take)
        val tier = when {
            take < desiredDistractors -> MultipleChoiceFallbackTier.REDUCED_OPTION_COUNT
            selected.all { same(it.candidate.partOfSpeech, request.targetPartOfSpeech) && same(it.candidate.topicId, request.targetTopicId) } -> MultipleChoiceFallbackTier.SAME_POS_AND_TOPIC
            selected.all { same(it.candidate.partOfSpeech, request.targetPartOfSpeech) } -> MultipleChoiceFallbackTier.SAME_POS_IN_SCOPE
            else -> MultipleChoiceFallbackTier.SAFE_IN_SCOPE
        }
        val options = buildList {
            add(RecallChoice(stableOptionId("correct", request.targetContentId.value), request.canonicalAnswer, true))
            selected.forEach { add(RecallChoice(stableOptionId("distractor", it.candidate.contentId.value), it.answer, false)) }
        }
        val shuffled = deterministicShuffle(options, request.deterministicSeed.value)
        return MultipleChoiceGenerationResult.Generated(MultipleChoiceOptionSet(
            shuffled, RecallChoiceProviderProvenance.CONTENT_LIBRARY, tier, scanned.size, eligible.size,
            rejected.sortedBy { it.contentId.value }
        ))
    }

    private fun answerSide(candidate: MultipleChoiceCandidate, direction: RecallDirection): String? = when (direction) {
        RecallDirection.SOURCE_TO_TARGET -> candidate.targetText
        RecallDirection.TARGET_TO_SOURCE, RecallDirection.IMAGE_TO_TEXT, RecallDirection.AUDIO_TO_TEXT -> candidate.sourceText
        RecallDirection.CONTEXT_TO_TEXT -> null
    }

    private fun score(c: MultipleChoiceCandidate, answer: String, r: MultipleChoiceGenerationRequest): Int {
        var value = 0
        if (same(c.partOfSpeech, r.targetPartOfSpeech)) value += r.policy.samePosWeight
        if (same(c.topicId, r.targetTopicId)) value += r.policy.sameTopicWeight
        val distance = kotlin.math.abs(answer.length - r.canonicalAnswer.length)
        value += (r.policy.lengthSimilarityWeight - distance).coerceAtLeast(0)
        return value
    }

    private fun normalize(value: String, policy: MultipleChoiceDistractorPolicy): String {
        var result = Normalizer.normalize(value, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        if (policy.punctuationIgnoredForAmbiguity) result = result.replace(Regex("\\p{P}+"), "")
        return result.trim()
    }

    private fun same(left: String?, right: String?): Boolean = left != null && right != null && left.equals(right, ignoreCase = true)
    private fun stableOptionId(kind: String, authority: String) = "choice-$kind-${authority.encodeStable()}"
    private fun String.encodeStable() = map { it.code.toString(16).padStart(4, '0') }.joinToString("")

    private fun deterministicShuffle(input: List<RecallChoice>, seed: Long): List<RecallChoice> {
        val values = input.toMutableList()
        var state = seed xor -7046029254386353131L
        for (index in values.lastIndex downTo 1) {
            state = state xor (state shl 13); state = state xor (state ushr 7); state = state xor (state shl 17)
            val swap = (state.toULong() % (index + 1).toULong()).toInt()
            val current = values[index]; values[index] = values[swap]; values[swap] = current
        }
        return values
    }

    private fun seededKey(value: String, seed: Long): Long {
        var hash = seed xor -3750763034362895579L
        value.forEach { hash = (hash xor it.code.toLong()) * 1099511628211L }
        return hash
    }

    private data class RankedCandidate(val candidate: MultipleChoiceCandidate, val answer: String, val normalized: String, val score: Int)
    private val SUPPORTED_DIRECTIONS = setOf(RecallDirection.SOURCE_TO_TARGET, RecallDirection.TARGET_TO_SOURCE, RecallDirection.IMAGE_TO_TEXT, RecallDirection.AUDIO_TO_TEXT)
}

class DeterministicMultipleChoiceOptionProvider(
    private val scopeId: MultipleChoiceScopeId,
    private val candidates: List<MultipleChoiceCandidate>,
    private val policy: MultipleChoiceDistractorPolicy,
    private val targetPartOfSpeech: String? = null,
    private val targetTopicId: String? = null,
    private val acceptedAlternatives: Set<String> = emptySet()
) : TypedMultipleChoiceOptionProvider {
    override fun provideTyped(request: MultipleChoiceOptionRequest): MultipleChoiceOptionProviderResult {
        val result = DeterministicMultipleChoiceGenerator.generate(MultipleChoiceGenerationRequest(
            request.learnerId, request.contentId, scopeId, request.direction, request.canonicalAnswer,
            acceptedAlternatives, targetPartOfSpeech, targetTopicId, candidates, policy,
            request.deterministicSeed, RecallContractVersion.CURRENT, MultipleChoiceGenerationProvenance.PLAN_FACTORY_PROVIDER
        ))
        return when (result) {
            is MultipleChoiceGenerationResult.Generated -> MultipleChoiceOptionProviderResult.Generated(result.optionSet)
            else -> MultipleChoiceOptionProviderResult.Failed(RecallPlanGenerationFailure.INVALID_PROVIDER_OPTIONS)
        }
    }
}
