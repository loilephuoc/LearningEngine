package vn.loi.learning.application.recall

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*

sealed interface RecallResultDecodeResult {
    data class Success(val result: RecallResult) : RecallResultDecodeResult
    data class UnsupportedVersion(val version: Int) : RecallResultDecodeResult
    data class Malformed(val reason: String) : RecallResultDecodeResult
}

object RecallResultWireCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; explicitNulls = true }
    fun encode(result: RecallResult): String = json.encodeToString(result.toDto())
    fun decode(value: String): RecallResultDecodeResult = try {
        val dto = json.decodeFromString<ResultDto>(value)
        if (dto.version != RecallContractVersion.CURRENT.value) RecallResultDecodeResult.UnsupportedVersion(dto.version)
        else RecallResultDecodeResult.Success(dto.toDomain())
    } catch (failure: IllegalArgumentException) {
        RecallResultDecodeResult.Malformed(failure.message ?: "Malformed recall result")
    } catch (failure: kotlinx.serialization.SerializationException) {
        RecallResultDecodeResult.Malformed(failure.message ?: "Malformed recall result")
    }
}

@Serializable
private data class ResultDto(
    val version: Int, val planId: String, val attemptId: String, val learnerId: String,
    val contentId: String, val mode: String, val direction: String, val outcome: String,
    val correct: Boolean, val normalizedResponse: String?, val latencyMillis: Long,
    val assistance: List<String>, val revealUsed: Boolean, val retryCount: Int,
    val provenance: String, val evidenceEligibility: String, val rejectionReason: String?,
    val completedAtEpochMillis: Long, val correctness: String? = null,
    val evaluatorVersion: String = "recall-evaluator-v1",
    val policyVersion: String = "recall-execution-v1",
    val platform: String = RecallPlatformKind.UNKNOWN.wireId
)

private fun RecallResult.toDto() = ResultDto(
    version.value, planId.value, attemptId.value, learnerId.value, contentId.value, mode.wireId,
    direction.wireId, outcome.wireId, correct, normalizedResponse, latency.millis,
    assistanceUsed.map(RecallAssistance::wireId).sorted(), revealUsed, retryCount, provenance.wireId,
    evidenceEligibility.wireId, rejectionReason?.wireId, completedAt.epochMillis, correctness.wireId,
    evaluatorVersion, policyVersion, platform.wireId
)

private fun ResultDto.toDomain() = RecallResult(
    RecallContractVersion(version), RecallPlanId(planId), RecallAttemptId(attemptId), LearnerId(learnerId),
    ContentId(contentId), enumValue(mode, RecallMode.entries), enumValue(direction, RecallDirection.entries),
    enumValue(outcome, RecallOutcome.entries), correct, normalizedResponse, TimeSpan(latencyMillis),
    assistance.mapTo(linkedSetOf()) { enumValue(it, RecallAssistance.entries) }, revealUsed, retryCount,
    enumValue(provenance, RecallProvenance.entries), enumValue(evidenceEligibility, RecallEvidenceEligibility.entries),
    rejectionReason?.let { enumValue(it, RecallRejectionReason.entries) }, Moment(completedAtEpochMillis),
    correctness?.let { enumValue(it, RecallCorrectness.entries) }
        ?: if (correct) RecallCorrectness.NORMALIZED else RecallCorrectness.INCORRECT,
    evaluatorVersion, policyVersion,
    enumValue(platform, RecallPlatformKind.entries)
)

private fun <T : StableWireValue> enumValue(id: String, values: List<T>): T =
    requireNotNull(values.singleOrNull { it.wireId == id }) { "Unknown wire value: $id" }
