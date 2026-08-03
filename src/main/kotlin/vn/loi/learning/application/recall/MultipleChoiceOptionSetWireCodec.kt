package vn.loi.learning.application.recall

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.recall.RecallChoice

sealed interface MultipleChoiceOptionSetDecodeResult {
    data class Success(val optionSet: MultipleChoiceOptionSet) : MultipleChoiceOptionSetDecodeResult
    data class Malformed(val reason: String) : MultipleChoiceOptionSetDecodeResult
}

object MultipleChoiceOptionSetWireCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; explicitNulls = true }

    fun encode(value: MultipleChoiceOptionSet): String = json.encodeToString(value.toDto())

    fun decode(value: String): MultipleChoiceOptionSetDecodeResult = try {
        MultipleChoiceOptionSetDecodeResult.Success(json.decodeFromString<OptionSetDto>(value).toDomain())
    } catch (failure: IllegalArgumentException) {
        MultipleChoiceOptionSetDecodeResult.Malformed(failure.message ?: "Malformed option set")
    } catch (failure: kotlinx.serialization.SerializationException) {
        MultipleChoiceOptionSetDecodeResult.Malformed(failure.message ?: "Malformed option set")
    }
}

@Serializable
private data class OptionSetDto(
    val choices: List<ChoiceDto>,
    val provenance: String,
    val fallbackTier: String?,
    val scannedCandidateCount: Int,
    val eligibleCandidateCount: Int,
    val rejectedCandidates: List<RejectedDto>
)

@Serializable private data class ChoiceDto(val id: String, val text: String, val correct: Boolean)
@Serializable private data class RejectedDto(val contentId: String, val reasons: List<String>)

private fun MultipleChoiceOptionSet.toDto() = OptionSetDto(
    choices.map { ChoiceDto(it.id, it.text, it.correct) }, provenance.wireId, fallbackTier?.wireId,
    scannedCandidateCount, eligibleCandidateCount,
    rejectedCandidates.sortedBy { it.contentId.value }.map { rejected ->
        RejectedDto(rejected.contentId.value, rejected.reasons.map(MultipleChoiceRejectedReason::wireId).sorted())
    }
)

private fun OptionSetDto.toDomain() = MultipleChoiceOptionSet(
    choices.map { RecallChoice(it.id, it.text, it.correct) },
    requireNotNull(RecallChoiceProviderProvenance.entries.singleOrNull { it.wireId == provenance }),
    fallbackTier?.let { id -> requireNotNull(MultipleChoiceFallbackTier.entries.singleOrNull { it.wireId == id }) },
    scannedCandidateCount.also { require(it >= 0) }, eligibleCandidateCount.also { require(it >= 0) },
    rejectedCandidates.map { rejected ->
        RejectedMultipleChoiceCandidate(ContentId(rejected.contentId), rejected.reasons.map { id ->
            requireNotNull(MultipleChoiceRejectedReason.entries.singleOrNull { it.wireId == id })
        })
    }
)
