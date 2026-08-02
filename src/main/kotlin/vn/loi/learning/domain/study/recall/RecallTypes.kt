package vn.loi.learning.domain.study.recall

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId

interface StableWireValue { val wireId: String }

enum class RecallMode(override val wireId: String) : StableWireValue {
    TYPING("typing"), MULTIPLE_CHOICE("multiple-choice"), LISTENING("listening"),
    IMAGE_RECALL("image-recall"), REVERSE_TRANSLATION("reverse-translation"),
    EXAMPLE_COMPLETION("example-completion"), DICTATION("dictation");

    companion object { fun fromWireId(value: String) = entries.singleOrNull { it.wireId == value } }
}

enum class RecallDirection(override val wireId: String) : StableWireValue {
    SOURCE_TO_TARGET("source-to-target"), TARGET_TO_SOURCE("target-to-source"),
    AUDIO_TO_TEXT("audio-to-text"), IMAGE_TO_TEXT("image-to-text"),
    CONTEXT_TO_TEXT("context-to-text");

    companion object { fun fromWireId(value: String) = entries.singleOrNull { it.wireId == value } }
}

enum class RecallCapability(override val wireId: String) : StableWireValue {
    SOURCE_TEXT("source-text"), TARGET_TRANSLATION("target-translation"),
    PRONUNCIATION("pronunciation"), PART_OF_SPEECH("part-of-speech"), IMAGE("image"),
    WORD_AUDIO("word-audio"), EXAMPLE_SOURCE("example-source"),
    EXAMPLE_TRANSLATION("example-translation"), EXAMPLE_AUDIO("example-audio")
}

enum class RecallAssistance(override val wireId: String) : StableWireValue {
    NONE("none"), EXAMPLE_VIEWED("example-viewed"), EXAMPLE_AUDIO_PLAYED("example-audio-played"),
    LETTER_HINT_USED("letter-hint-used"), PRONUNCIATION_HINT_USED("pronunciation-hint-used"),
    ANSWER_REVEALED("answer-revealed")
}

enum class RecallOutcome(override val wireId: String) : StableWireValue {
    CORRECT("correct"), INCORRECT("incorrect"), PARTIAL("partial"), REVEALED("revealed"),
    SKIPPED("skipped"), TIMED_OUT("timed-out"), INVALID_SUBMISSION("invalid-submission")
}

enum class RecallEvidenceEligibility(override val wireId: String) : StableWireValue {
    STRONG("strong"), STANDARD("standard"), WEAK("weak"), INELIGIBLE("ineligible")
}

enum class RecallProvenance(override val wireId: String) : StableWireValue {
    EVALUATIVE("evaluative"), PRACTICE("practice"), MANUAL("manual")
}

enum class RecallPlatformKind(override val wireId: String) : StableWireValue {
    DESKTOP("desktop"), ANDROID("android"), IOS("ios"), WEB("web"), UNKNOWN("unknown")
}

enum class RecallAnswerKind(override val wireId: String) : StableWireValue {
    TEXT("text"), CHOICE("choice"), ACKNOWLEDGEMENT("acknowledgement")
}

enum class CaseSensitivity(override val wireId: String) : StableWireValue { SENSITIVE("sensitive"), INSENSITIVE("insensitive") }
enum class PunctuationPolicy(override val wireId: String) : StableWireValue { EXACT("exact"), IGNORE("ignore") }
enum class WhitespacePolicy(override val wireId: String) : StableWireValue { EXACT("exact"), NORMALIZE("normalize") }

@JvmInline value class RecallPlanId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class RecallAttemptId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class RecallResourceId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class RecallNormalizationPolicyId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class RecallLanguageTag(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class RecallDeterministicSeed(val value: Long)
@JvmInline value class RecallContractVersion(val value: Int) { init { require(value > 0) }
    companion object { val CURRENT = RecallContractVersion(1) }
}

data class RecallContentCapabilities(
    val contentId: ContentId,
    val available: Set<RecallCapability>,
    val image: RecallResourceId? = null,
    val wordAudio: RecallResourceId? = null,
    val exampleAudio: RecallResourceId? = null
) {
    init {
        require((RecallCapability.IMAGE in available) == (image != null))
        require((RecallCapability.WORD_AUDIO in available) == (wordAudio != null))
        require((RecallCapability.EXAMPLE_AUDIO in available) == (exampleAudio != null))
    }
}

sealed interface RecallPrompt { val mode: RecallMode
    data class Typing(val sourceText: String) : RecallPrompt { override val mode = RecallMode.TYPING }
    data class MultipleChoice(val question: String, val choices: List<RecallChoice>) : RecallPrompt { override val mode = RecallMode.MULTIPLE_CHOICE }
    data class Listening(val audio: RecallResourceId) : RecallPrompt { override val mode = RecallMode.LISTENING }
    data class ImageRecall(val image: RecallResourceId) : RecallPrompt { override val mode = RecallMode.IMAGE_RECALL }
    data class ReverseTranslation(val targetText: String) : RecallPrompt { override val mode = RecallMode.REVERSE_TRANSLATION }
    data class ExampleCompletion(val example: String, val targetSpan: RecallTextSpan) : RecallPrompt { override val mode = RecallMode.EXAMPLE_COMPLETION }
    data class Dictation(val audio: RecallResourceId) : RecallPrompt { override val mode = RecallMode.DICTATION }
}

data class RecallChoice(val id: String, val text: String, val correct: Boolean) { init { require(id.isNotBlank()); require(text.isNotBlank()) } }
data class RecallTextSpan(val startInclusive: Int, val endExclusive: Int) { init { require(startInclusive >= 0); require(endExclusive > startInclusive) } }

data class RecallAnswerContract(
    val canonicalAnswer: String,
    val acceptedAlternatives: List<String> = emptyList(),
    val normalizationPolicy: RecallNormalizationPolicyId,
    val caseSensitivity: CaseSensitivity,
    val punctuationPolicy: PunctuationPolicy,
    val whitespacePolicy: WhitespacePolicy,
    val expectedLanguage: RecallLanguageTag,
    val kind: RecallAnswerKind
) { init { require(canonicalAnswer.isNotBlank()); require(acceptedAlternatives.none(String::isBlank)) } }

data class RecallPlatformRequirements(
    val requiresTextInput: Boolean = false,
    val requiresChoiceSelection: Boolean = false,
    val requiresAudioPlayback: Boolean = false,
    val requiresImageRendering: Boolean = false,
    val requiresExampleRendering: Boolean = false
)

data class RecallPlan(
    val version: RecallContractVersion = RecallContractVersion.CURRENT,
    val planId: RecallPlanId,
    val learnerId: LearnerId,
    val contentId: ContentId,
    val learningItemId: LearningItemId?,
    val sessionId: SessionId,
    val mode: RecallMode,
    val direction: RecallDirection,
    val prompt: RecallPrompt,
    val answerContract: RecallAnswerContract,
    val availableAssistance: Set<RecallAssistance>,
    val evidenceClass: RecallEvidenceEligibility,
    val deterministicSeed: RecallDeterministicSeed,
    val generatedAt: Moment,
    val provenance: RecallProvenance,
    val platformRequirements: RecallPlatformRequirements,
    val contentCapabilities: RecallContentCapabilities
)

data class RecallSubmissionContext(
    val planId: RecallPlanId,
    val attemptId: RecallAttemptId,
    val learnerId: LearnerId,
    val contentId: ContentId,
    val sessionId: SessionId,
    val mode: RecallMode,
    val submittedAt: Moment,
    val assistanceState: Set<RecallAssistance>,
    val platform: RecallPlatformKind,
    val retryCount: Int = 0
) { init { require(retryCount >= 0) } }

sealed interface RecallSubmission { val context: RecallSubmissionContext
    data class TypedText(override val context: RecallSubmissionContext, val text: String) : RecallSubmission
    data class Choice(override val context: RecallSubmissionContext, val choiceId: String) : RecallSubmission
    data class PlaybackAcknowledgement(override val context: RecallSubmissionContext) : RecallSubmission
    data class Reveal(override val context: RecallSubmissionContext) : RecallSubmission
    data class Skip(override val context: RecallSubmissionContext) : RecallSubmission
    data class Timeout(override val context: RecallSubmissionContext) : RecallSubmission
}

enum class RecallRejectionReason(override val wireId: String) : StableWireValue {
    UNSUPPORTED_VERSION("unsupported-version"), MODE_MISMATCH("mode-mismatch"),
    IDENTITY_MISMATCH("identity-mismatch"), DUPLICATE_ATTEMPT("duplicate-attempt"),
    INVALID_PROMPT("invalid-prompt"), MISSING_CAPABILITY("missing-capability"),
    INVALID_SUBMISSION_KIND("invalid-submission-kind")
}

data class RecallResult(
    val version: RecallContractVersion,
    val planId: RecallPlanId,
    val attemptId: RecallAttemptId,
    val learnerId: LearnerId,
    val contentId: ContentId,
    val mode: RecallMode,
    val direction: RecallDirection,
    val outcome: RecallOutcome,
    val correct: Boolean,
    val normalizedResponse: String?,
    val latency: TimeSpan,
    val assistanceUsed: Set<RecallAssistance>,
    val revealUsed: Boolean,
    val retryCount: Int,
    val provenance: RecallProvenance,
    val evidenceEligibility: RecallEvidenceEligibility,
    val rejectionReason: RecallRejectionReason?,
    val completedAt: Moment
) { init { require(retryCount >= 0) } }

fun interface RecallClock { fun now(): Moment }
