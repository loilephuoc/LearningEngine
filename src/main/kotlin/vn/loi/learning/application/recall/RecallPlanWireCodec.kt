package vn.loi.learning.application.recall

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

sealed interface RecallPlanDecodeResult {
    data class Success(val plan: RecallPlan) : RecallPlanDecodeResult
    data class UnsupportedVersion(val version: Int) : RecallPlanDecodeResult
    data class Malformed(val reason: String) : RecallPlanDecodeResult
}

object RecallPlanWireCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; explicitNulls = true }

    fun encode(plan: RecallPlan): String = json.encodeToString(plan.toDto())

    fun decode(value: String): RecallPlanDecodeResult = try {
        val dto = json.decodeFromString<RecallPlanDto>(value)
        if (dto.version != RecallContractVersion.CURRENT.value) {
            RecallPlanDecodeResult.UnsupportedVersion(dto.version)
        } else {
            RecallPlanDecodeResult.Success(dto.toDomain())
        }
    } catch (failure: IllegalArgumentException) {
        RecallPlanDecodeResult.Malformed(failure.message ?: "Malformed recall plan")
    } catch (failure: kotlinx.serialization.SerializationException) {
        RecallPlanDecodeResult.Malformed(failure.message ?: "Malformed recall plan")
    }
}

@Serializable
private data class RecallPlanDto(
    val version: Int, val planId: String, val learnerId: String, val contentId: String,
    val learningItemId: String?, val sessionId: String, val mode: String, val direction: String,
    val prompt: RecallPromptDto, val answer: RecallAnswerDto,
    val assistance: List<String>, val evidenceClass: String, val deterministicSeed: Long,
    val generatedAtEpochMillis: Long, val provenance: String,
    val requirements: RecallRequirementsDto, val capabilities: RecallCapabilitiesDto
)

@Serializable
private data class RecallPromptDto(
    val kind: String, val text: String? = null, val resourceId: String? = null,
    val choices: List<RecallChoiceDto> = emptyList(), val spanStart: Int? = null, val spanEnd: Int? = null
)

@Serializable private data class RecallChoiceDto(val id: String, val text: String, val correct: Boolean)
@Serializable private data class RecallAnswerDto(
    val canonical: String, val alternatives: List<String>, val normalizationPolicy: String,
    val caseSensitivity: String, val punctuationPolicy: String, val whitespacePolicy: String,
    val expectedLanguage: String, val kind: String
)
@Serializable private data class RecallRequirementsDto(
    val textInput: Boolean, val choiceSelection: Boolean, val audioPlayback: Boolean,
    val imageRendering: Boolean, val exampleRendering: Boolean
)
@Serializable private data class RecallCapabilitiesDto(
    val values: List<String>, val image: String?, val wordAudio: String?, val exampleAudio: String?
)

private fun RecallPlan.toDto() = RecallPlanDto(
    version.value, planId.value, learnerId.value, contentId.value, learningItemId?.value,
    sessionId.value, mode.wireId, direction.wireId, prompt.toDto(),
    RecallAnswerDto(
        answerContract.canonicalAnswer, answerContract.acceptedAlternatives,
        answerContract.normalizationPolicy.value, answerContract.caseSensitivity.wireId,
        answerContract.punctuationPolicy.wireId, answerContract.whitespacePolicy.wireId,
        answerContract.expectedLanguage.value, answerContract.kind.wireId
    ),
    availableAssistance.map(RecallAssistance::wireId).sorted(), evidenceClass.wireId,
    deterministicSeed.value, generatedAt.epochMillis, provenance.wireId,
    RecallRequirementsDto(
        platformRequirements.requiresTextInput, platformRequirements.requiresChoiceSelection,
        platformRequirements.requiresAudioPlayback, platformRequirements.requiresImageRendering,
        platformRequirements.requiresExampleRendering
    ),
    RecallCapabilitiesDto(
        contentCapabilities.available.map(RecallCapability::wireId).sorted(),
        contentCapabilities.image?.value, contentCapabilities.wordAudio?.value,
        contentCapabilities.exampleAudio?.value
    )
)

private fun RecallPrompt.toDto(): RecallPromptDto = when (this) {
    is RecallPrompt.Typing -> RecallPromptDto(mode.wireId, text = sourceText)
    is RecallPrompt.MultipleChoice -> RecallPromptDto(
        mode.wireId, text = question,
        choices = choices.map { RecallChoiceDto(it.id, it.text, it.correct) }
    )
    is RecallPrompt.Listening -> RecallPromptDto(mode.wireId, resourceId = audio.value)
    is RecallPrompt.ImageRecall -> RecallPromptDto(mode.wireId, resourceId = image.value)
    is RecallPrompt.ReverseTranslation -> RecallPromptDto(mode.wireId, text = targetText)
    is RecallPrompt.ExampleCompletion -> RecallPromptDto(
        mode.wireId, text = example, spanStart = targetSpan.startInclusive,
        spanEnd = targetSpan.endExclusive
    )
    is RecallPrompt.Dictation -> RecallPromptDto(mode.wireId, resourceId = audio.value)
}

private fun RecallPlanDto.toDomain(): RecallPlan {
    val parsedMode = requireNotNull(RecallMode.fromWireId(mode)) { "Unknown recall mode: $mode" }
    return RecallPlan(
        RecallContractVersion(version), RecallPlanId(planId), LearnerId(learnerId), ContentId(contentId),
        learningItemId?.let(::LearningItemId), SessionId(sessionId), parsedMode,
        requireNotNull(RecallDirection.fromWireId(direction)) { "Unknown direction: $direction" },
        prompt.toDomain(), answer.toDomain(),
        assistance.mapTo(linkedSetOf()) { id -> requireNotNull(RecallAssistance.entries.singleOrNull { it.wireId == id }) },
        requireNotNull(RecallEvidenceEligibility.entries.singleOrNull { it.wireId == evidenceClass }),
        RecallDeterministicSeed(deterministicSeed), Moment(generatedAtEpochMillis),
        requireNotNull(RecallProvenance.entries.singleOrNull { it.wireId == provenance }),
        RecallPlatformRequirements(
            requirements.textInput, requirements.choiceSelection, requirements.audioPlayback,
            requirements.imageRendering, requirements.exampleRendering
        ),
        RecallContentCapabilities(
            ContentId(contentId),
            capabilities.values.mapTo(linkedSetOf()) { id -> requireNotNull(RecallCapability.entries.singleOrNull { it.wireId == id }) },
            capabilities.image?.let(::RecallResourceId), capabilities.wordAudio?.let(::RecallResourceId),
            capabilities.exampleAudio?.let(::RecallResourceId)
        )
    ).also { require(it.mode == it.prompt.mode) }
}

private fun RecallPromptDto.toDomain(): RecallPrompt = when (kind) {
    RecallMode.TYPING.wireId -> RecallPrompt.Typing(requireNotNull(text))
    RecallMode.MULTIPLE_CHOICE.wireId -> RecallPrompt.MultipleChoice(
        requireNotNull(text), choices.map { RecallChoice(it.id, it.text, it.correct) }
    )
    RecallMode.LISTENING.wireId -> RecallPrompt.Listening(RecallResourceId(requireNotNull(resourceId)))
    RecallMode.IMAGE_RECALL.wireId -> RecallPrompt.ImageRecall(RecallResourceId(requireNotNull(resourceId)))
    RecallMode.REVERSE_TRANSLATION.wireId -> RecallPrompt.ReverseTranslation(requireNotNull(text))
    RecallMode.EXAMPLE_COMPLETION.wireId -> RecallPrompt.ExampleCompletion(
        requireNotNull(text), RecallTextSpan(requireNotNull(spanStart), requireNotNull(spanEnd))
    )
    RecallMode.DICTATION.wireId -> RecallPrompt.Dictation(RecallResourceId(requireNotNull(resourceId)))
    else -> error("Unknown prompt kind: $kind")
}

private fun RecallAnswerDto.toDomain() = RecallAnswerContract(
    canonical, alternatives, RecallNormalizationPolicyId(normalizationPolicy),
    requireNotNull(CaseSensitivity.entries.singleOrNull { it.wireId == caseSensitivity }),
    requireNotNull(PunctuationPolicy.entries.singleOrNull { it.wireId == punctuationPolicy }),
    requireNotNull(WhitespacePolicy.entries.singleOrNull { it.wireId == whitespacePolicy }),
    RecallLanguageTag(expectedLanguage),
    requireNotNull(RecallAnswerKind.entries.singleOrNull { it.wireId == kind })
)
