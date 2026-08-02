package vn.loi.learning.application.recall

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.recall.*

object ContentRecallCapabilityResolver {
    fun resolve(content: Content): RecallCapabilityProjection {
        val source = normalized(content.text.primaryText)
        val target = normalized(content.text.translatedText)
        val pronunciation = normalized(content.text.pronunciation)
        val example = normalized(content.text.exampleText)
        val exampleTranslation = normalized(content.text.exampleTranslation)
        val image = validMedia(content.media.image)
        val wordAudio = validMedia(content.media.primaryAudio)
        val exampleAudio = validMedia(content.media.exampleAudio)
        val malformedImage = content.media.image != null && image == null
        val malformedWordAudio = content.media.primaryAudio != null && wordAudio == null
        val malformedExampleAudio = content.media.exampleAudio != null && exampleAudio == null
        val completionTarget = if (source != null && example != null) safeTarget(example, source) else null
        val sourceCapabilities = RecallContentCapabilityResolver.resolve(content)

        val eligibility = RecallMode.entries.sortedBy(RecallMode::wireId).map { mode ->
            eligibility(
                mode, source, target, image, wordAudio, example, completionTarget,
                malformedImage, malformedWordAudio
            )
        }
        val availableModes = eligibility.filter(RecallModeEligibility::available).mapTo(linkedSetOf(), RecallModeEligibility::mode)
        val media = buildSet {
            if (image != null) add(RecallMediaCapability.WORD_IMAGE)
            if (wordAudio != null) add(RecallMediaCapability.WORD_AUDIO)
            if (exampleAudio != null) add(RecallMediaCapability.EXAMPLE_AUDIO)
        }
        val lexical = buildSet {
            if (source != null) add(RecallLexicalCapability.HAS_SOURCE_TEXT)
            if (target != null) add(RecallLexicalCapability.HAS_TARGET_TEXT)
            if (pronunciation != null) add(RecallLexicalCapability.HAS_PRONUNCIATION)
            if (RecallCapability.PART_OF_SPEECH in sourceCapabilities.available) {
                add(RecallLexicalCapability.HAS_PART_OF_SPEECH)
            }
        }
        val contextual = buildSet {
            if (example != null) add(RecallContextualCapability.HAS_EXAMPLE_SOURCE)
            if (exampleTranslation != null) add(RecallContextualCapability.HAS_EXAMPLE_TRANSLATION)
            if (exampleAudio != null) add(RecallContextualCapability.HAS_EXAMPLE_AUDIO)
            if (completionTarget != null) add(RecallContextualCapability.HAS_SAFE_COMPLETION_TARGET)
        }
        val assistance = buildSet {
            add(RecallAssistance.NONE)
            if (source != null) {
                add(RecallAssistance.ANSWER_REVEALED)
                add(RecallAssistance.LETTER_HINT_USED)
            }
            if (pronunciation != null) add(RecallAssistance.PRONUNCIATION_HINT_USED)
            if (example != null) add(RecallAssistance.EXAMPLE_VIEWED)
            if (exampleAudio != null) add(RecallAssistance.EXAMPLE_AUDIO_PLAYED)
        }

        return RecallCapabilityProjection(
            RecallContractVersion.CURRENT, content.id,
            if (availableModes.isEmpty()) RecallCapabilitySet.empty() else RecallCapabilitySet.of(availableModes),
            eligibility, assistance, media, lexical, contextual, completionTarget, sourceCapabilities
        ).also {
            check(!malformedExampleAudio || RecallMediaCapability.EXAMPLE_AUDIO !in it.mediaCapabilities)
        }
    }

    private fun eligibility(
        mode: RecallMode,
        source: String?,
        target: String?,
        image: String?,
        wordAudio: String?,
        example: String?,
        completionTarget: RecallCompletionTarget?,
        malformedImage: Boolean,
        malformedWordAudio: Boolean
    ): RecallModeEligibility {
        val reasons = linkedSetOf<RecallUnavailableReason>()
        fun requireSource() {
            if (source == null) {
                reasons += RecallUnavailableReason.MISSING_SOURCE_TEXT
                reasons += RecallUnavailableReason.MISSING_CANONICAL_ANSWER
            }
        }
        fun requireTarget() { if (target == null) reasons += RecallUnavailableReason.MISSING_TARGET_TEXT }
        val directions: Set<RecallDirection> = when (mode) {
            RecallMode.TYPING, RecallMode.MULTIPLE_CHOICE -> {
                requireSource(); requireTarget()
                if (reasons.isEmpty()) setOf(RecallDirection.SOURCE_TO_TARGET, RecallDirection.TARGET_TO_SOURCE) else emptySet()
            }
            RecallMode.LISTENING, RecallMode.DICTATION -> {
                requireSource()
                if (wordAudio == null) reasons += if (malformedWordAudio) {
                    RecallUnavailableReason.INVALID_MEDIA_REFERENCE
                } else RecallUnavailableReason.MISSING_WORD_AUDIO
                if (reasons.isEmpty()) setOf(RecallDirection.AUDIO_TO_TEXT) else emptySet()
            }
            RecallMode.IMAGE_RECALL -> {
                requireSource()
                if (image == null) reasons += if (malformedImage) {
                    RecallUnavailableReason.INVALID_MEDIA_REFERENCE
                } else RecallUnavailableReason.MISSING_IMAGE
                if (reasons.isEmpty()) setOf(RecallDirection.IMAGE_TO_TEXT) else emptySet()
            }
            RecallMode.REVERSE_TRANSLATION -> {
                requireSource(); requireTarget()
                if (source != null && target != null && source.equals(target, ignoreCase = true)) {
                    reasons += RecallUnavailableReason.AMBIGUOUS_DIRECTION
                }
                if (reasons.isEmpty()) setOf(RecallDirection.SOURCE_TO_TARGET, RecallDirection.TARGET_TO_SOURCE) else emptySet()
            }
            RecallMode.EXAMPLE_COMPLETION -> {
                requireSource()
                if (example == null) reasons += RecallUnavailableReason.MISSING_EXAMPLE_TEXT
                else if (completionTarget == null) reasons += RecallUnavailableReason.UNSAFE_EXAMPLE_TARGET_SPAN
                if (reasons.isEmpty()) setOf(RecallDirection.CONTEXT_TO_TEXT) else emptySet()
            }
        }
        return RecallModeEligibility(mode, directions, reasons)
    }

    private fun normalized(value: String?): String? = value?.trim()?.takeIf(String::isNotBlank)

    private fun validMedia(value: String?): String? = normalized(value)?.takeIf { reference ->
        reference.none(Char::isISOControl)
    }

    private fun safeTarget(example: String, answer: String): RecallCompletionTarget? {
        val matches = buildList {
            var start = example.indexOf(answer)
            while (start >= 0) {
                val end = start + answer.length
                val leftSafe = start == 0 || !example[start - 1].isLetterOrDigit()
                val rightSafe = end == example.length || !example[end].isLetterOrDigit()
                if (leftSafe && rightSafe) add(RecallCompletionTarget(start, end))
                start = example.indexOf(answer, start + 1)
            }
        }
        return matches.singleOrNull()
    }
}
