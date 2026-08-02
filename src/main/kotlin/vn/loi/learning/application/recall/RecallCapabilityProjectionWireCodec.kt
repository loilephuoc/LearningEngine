package vn.loi.learning.application.recall

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.recall.*

sealed interface RecallCapabilityProjectionDecodeResult {
    data class Success(val projection: RecallCapabilityProjection) : RecallCapabilityProjectionDecodeResult
    data class UnsupportedVersion(val version: Int) : RecallCapabilityProjectionDecodeResult
    data class Malformed(val reason: String) : RecallCapabilityProjectionDecodeResult
}

object RecallCapabilityProjectionWireCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; explicitNulls = true }

    fun encode(value: RecallCapabilityProjection): String = json.encodeToString(value.toDto())

    fun decode(value: String): RecallCapabilityProjectionDecodeResult = try {
        val dto = json.decodeFromString<ProjectionDto>(value)
        if (dto.version != RecallContractVersion.CURRENT.value) {
            RecallCapabilityProjectionDecodeResult.UnsupportedVersion(dto.version)
        } else RecallCapabilityProjectionDecodeResult.Success(dto.toDomain())
    } catch (failure: Exception) {
        RecallCapabilityProjectionDecodeResult.Malformed(failure.message ?: "Malformed capability projection")
    }
}

@Serializable private data class ProjectionDto(
    val version: Int,
    val contentId: String,
    val eligibility: List<EligibilityDto>,
    val assistance: List<String>,
    val media: List<String>,
    val lexical: List<String>,
    val contextual: List<String>,
    val targetStart: Int?,
    val targetEnd: Int?,
    val sourceCapabilities: List<String>,
    val image: String?,
    val wordAudio: String?,
    val exampleAudio: String?
)

@Serializable private data class EligibilityDto(
    val mode: String,
    val directions: List<String>,
    val reasons: List<String>
)

private fun RecallCapabilityProjection.toDto() = ProjectionDto(
    version.value, contentId.value,
    orderedEligibility.map { item ->
        EligibilityDto(
            item.mode.wireId,
            item.orderedDirections.map(RecallDirection::wireId),
            item.orderedUnavailableReasons.map(RecallUnavailableReason::wireId)
        )
    },
    orderedAssistance.map(RecallAssistance::wireId),
    orderedMediaCapabilities.map(RecallMediaCapability::wireId),
    orderedLexicalCapabilities.map(RecallLexicalCapability::wireId),
    orderedContextualCapabilities.map(RecallContextualCapability::wireId),
    completionTarget?.startInclusive, completionTarget?.endExclusive,
    sourceCapabilities.available.sortedBy(RecallCapability::wireId).map(RecallCapability::wireId),
    sourceCapabilities.image?.value, sourceCapabilities.wordAudio?.value, sourceCapabilities.exampleAudio?.value
)

private fun ProjectionDto.toDomain(): RecallCapabilityProjection {
    val id = ContentId(contentId)
    val items = eligibility.map { item ->
        RecallModeEligibility(
            requireNotNull(RecallMode.fromWireId(item.mode)),
            item.directions.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallDirection.fromWireId(wire)) },
            item.reasons.mapTo(linkedSetOf()) { wire ->
                requireNotNull(RecallUnavailableReason.entries.singleOrNull { it.wireId == wire })
            }
        )
    }
    val available = items.filter(RecallModeEligibility::available).mapTo(linkedSetOf(), RecallModeEligibility::mode)
    return RecallCapabilityProjection(
        RecallContractVersion(version), id,
        if (available.isEmpty()) RecallCapabilitySet.empty() else RecallCapabilitySet.of(available),
        items,
        assistance.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallAssistance.entries.singleOrNull { it.wireId == wire }) },
        media.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallMediaCapability.entries.singleOrNull { it.wireId == wire }) },
        lexical.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallLexicalCapability.entries.singleOrNull { it.wireId == wire }) },
        contextual.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallContextualCapability.entries.singleOrNull { it.wireId == wire }) },
        if (targetStart != null && targetEnd != null) RecallCompletionTarget(targetStart, targetEnd) else null,
        RecallContentCapabilities(
            id,
            sourceCapabilities.mapTo(linkedSetOf()) { wire -> requireNotNull(RecallCapability.entries.singleOrNull { it.wireId == wire }) },
            image?.let(::RecallResourceId), wordAudio?.let(::RecallResourceId), exampleAudio?.let(::RecallResourceId)
        )
    )
}
