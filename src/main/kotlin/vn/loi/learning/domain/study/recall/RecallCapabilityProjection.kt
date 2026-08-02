package vn.loi.learning.domain.study.recall

import vn.loi.learning.domain.content.model.ContentId

enum class RecallUnavailableReason(override val wireId: String) : StableWireValue {
    MISSING_SOURCE_TEXT("missing-source-text"), MISSING_TARGET_TEXT("missing-target-text"),
    MISSING_CANONICAL_ANSWER("missing-canonical-answer"), MISSING_IMAGE("missing-image"),
    MISSING_WORD_AUDIO("missing-word-audio"), MISSING_EXAMPLE_TEXT("missing-example-text"),
    MISSING_EXAMPLE_AUDIO("missing-example-audio"),
    UNSAFE_EXAMPLE_TARGET_SPAN("unsafe-example-target-span"),
    INVALID_MEDIA_REFERENCE("invalid-media-reference"), AMBIGUOUS_DIRECTION("ambiguous-direction"),
    UNSUPPORTED_CONTENT_KIND("unsupported-content-kind"), MALFORMED_CONTENT_DATA("malformed-content-data")
}

enum class RecallMediaCapability(override val wireId: String) : StableWireValue {
    WORD_IMAGE("word-image"), WORD_AUDIO("word-audio"), EXAMPLE_AUDIO("example-audio")
}

enum class RecallLexicalCapability(override val wireId: String) : StableWireValue {
    HAS_SOURCE_TEXT("has-source-text"), HAS_TARGET_TEXT("has-target-text"),
    HAS_PRONUNCIATION("has-pronunciation"), HAS_PART_OF_SPEECH("has-part-of-speech")
}

enum class RecallContextualCapability(override val wireId: String) : StableWireValue {
    HAS_EXAMPLE_SOURCE("has-example-source"), HAS_EXAMPLE_TRANSLATION("has-example-translation"),
    HAS_EXAMPLE_AUDIO("has-example-audio"), HAS_SAFE_COMPLETION_TARGET("has-safe-completion-target")
}

data class RecallCompletionTarget(val startInclusive: Int, val endExclusive: Int) {
    init { require(startInclusive >= 0); require(endExclusive > startInclusive) }
}

data class RecallModeEligibility(
    val mode: RecallMode,
    val directions: Set<RecallDirection>,
    val unavailableReasons: Set<RecallUnavailableReason>
) {
    val available: Boolean get() = directions.isNotEmpty()

    init {
        require(available.xor(unavailableReasons.isNotEmpty())) {
            "A mode must be either available with directions or unavailable with typed reasons."
        }
    }

    val orderedDirections: List<RecallDirection>
        get() = directions.sortedBy(RecallDirection::wireId)
    val orderedUnavailableReasons: List<RecallUnavailableReason>
        get() = unavailableReasons.sortedBy(RecallUnavailableReason::wireId)
}

class RecallCapabilitySet private constructor(private val modes: Set<RecallMode>) {
    val isEmpty: Boolean get() = modes.isEmpty()
    fun supports(mode: RecallMode): Boolean = mode in modes
    val orderedModes: List<RecallMode> get() = modes.sortedBy(RecallMode::wireId)
    override fun equals(other: Any?): Boolean = other is RecallCapabilitySet && modes == other.modes
    override fun hashCode(): Int = modes.hashCode()
    override fun toString(): String = orderedModes.joinToString(prefix = "[", postfix = "]") { it.wireId }

    companion object {
        fun of(modes: Set<RecallMode>) = RecallCapabilitySet(modes.toSet())
        fun empty() = RecallCapabilitySet(emptySet())
    }
}

data class RecallCapabilityProjection(
    val version: RecallContractVersion,
    val contentId: ContentId,
    val capabilitySet: RecallCapabilitySet,
    val eligibility: List<RecallModeEligibility>,
    val availableAssistance: Set<RecallAssistance>,
    val mediaCapabilities: Set<RecallMediaCapability>,
    val lexicalCapabilities: Set<RecallLexicalCapability>,
    val contextualCapabilities: Set<RecallContextualCapability>,
    val completionTarget: RecallCompletionTarget?,
    val sourceCapabilities: RecallContentCapabilities
) {
    init {
        require(sourceCapabilities.contentId == contentId)
        require(eligibility.map(RecallModeEligibility::mode).toSet() == RecallMode.entries.toSet())
        require(eligibility.size == RecallMode.entries.size)
        require(eligibility.filter(RecallModeEligibility::available).map(RecallModeEligibility::mode).toSet() ==
            capabilitySet.orderedModes.toSet())
    }

    val orderedEligibility: List<RecallModeEligibility>
        get() = eligibility.sortedBy { it.mode.wireId }
    val orderedAssistance: List<RecallAssistance>
        get() = availableAssistance.sortedBy(RecallAssistance::wireId)
    val orderedMediaCapabilities: List<RecallMediaCapability>
        get() = mediaCapabilities.sortedBy(RecallMediaCapability::wireId)
    val orderedLexicalCapabilities: List<RecallLexicalCapability>
        get() = lexicalCapabilities.sortedBy(RecallLexicalCapability::wireId)
    val orderedContextualCapabilities: List<RecallContextualCapability>
        get() = contextualCapabilities.sortedBy(RecallContextualCapability::wireId)

    fun supports(mode: RecallMode): Boolean = capabilitySet.supports(mode)
    fun supportedDirectionsFor(mode: RecallMode): List<RecallDirection> =
        eligibility.single { it.mode == mode }.orderedDirections
    fun unavailableReasonsFor(mode: RecallMode): List<RecallUnavailableReason> =
        eligibility.single { it.mode == mode }.orderedUnavailableReasons
    fun supportsAssistance(assistance: RecallAssistance): Boolean = assistance in availableAssistance
}
