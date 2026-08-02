package vn.loi.learning.application.recall

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.recall.*

object RecallContentCapabilityResolver {
    fun resolve(content: Content): RecallContentCapabilities {
        val capabilities = buildSet {
            add(RecallCapability.SOURCE_TEXT)
            if (!content.text.translatedText.isNullOrBlank()) add(RecallCapability.TARGET_TRANSLATION)
            if (!content.text.pronunciation.isNullOrBlank()) add(RecallCapability.PRONUNCIATION)
            if (vn.loi.learning.application.partofspeech.PartOfSpeechExtractor.primary(content) != null) {
                add(RecallCapability.PART_OF_SPEECH)
            }
            if (content.media.image != null) add(RecallCapability.IMAGE)
            if (content.media.primaryAudio != null) add(RecallCapability.WORD_AUDIO)
            if (!content.text.exampleText.isNullOrBlank()) add(RecallCapability.EXAMPLE_SOURCE)
            if (!content.text.exampleTranslation.isNullOrBlank()) add(RecallCapability.EXAMPLE_TRANSLATION)
            if (content.media.exampleAudio != null) add(RecallCapability.EXAMPLE_AUDIO)
        }
        return RecallContentCapabilities(
            content.id, capabilities,
            content.media.image?.let(::RecallResourceId),
            content.media.primaryAudio?.let(::RecallResourceId),
            content.media.exampleAudio?.let(::RecallResourceId)
        )
    }
}

sealed interface RecallPlanValidationResult {
    data object Valid : RecallPlanValidationResult
    data class Invalid(val reasons: Set<RecallRejectionReason>) : RecallPlanValidationResult
}

sealed interface RecallSubmissionValidationResult {
    data object Accepted : RecallSubmissionValidationResult
    data class Rejected(val reasons: Set<RecallRejectionReason>) : RecallSubmissionValidationResult
}

object RecallContractValidator {
    fun validatePlan(plan: RecallPlan): RecallPlanValidationResult {
        val reasons = linkedSetOf<RecallRejectionReason>()
        if (plan.version != RecallContractVersion.CURRENT) reasons += RecallRejectionReason.UNSUPPORTED_VERSION
        if (plan.prompt.mode != plan.mode) reasons += RecallRejectionReason.MODE_MISMATCH
        if (plan.contentCapabilities.contentId != plan.contentId) reasons += RecallRejectionReason.IDENTITY_MISMATCH
        if (!requirementsSupport(plan.mode, plan.platformRequirements)) reasons += RecallRejectionReason.INVALID_PROMPT
        when (val prompt = plan.prompt) {
            is RecallPrompt.Listening, is RecallPrompt.Dictation ->
                if (RecallCapability.WORD_AUDIO !in plan.contentCapabilities.available) reasons += RecallRejectionReason.MISSING_CAPABILITY
            is RecallPrompt.ImageRecall ->
                if (RecallCapability.IMAGE !in plan.contentCapabilities.available) reasons += RecallRejectionReason.MISSING_CAPABILITY
            is RecallPrompt.MultipleChoice -> {
                if (prompt.choices.size < 2 || prompt.choices.count(RecallChoice::correct) != 1) {
                    reasons += RecallRejectionReason.INVALID_PROMPT
                }
            }
            is RecallPrompt.ExampleCompletion -> {
                if (RecallCapability.EXAMPLE_SOURCE !in plan.contentCapabilities.available ||
                    prompt.targetSpan.endExclusive > prompt.example.length
                ) reasons += RecallRejectionReason.MISSING_CAPABILITY
            }
            is RecallPrompt.ReverseTranslation ->
                if (RecallCapability.TARGET_TRANSLATION !in plan.contentCapabilities.available) reasons += RecallRejectionReason.MISSING_CAPABILITY
            is RecallPrompt.Typing -> Unit
        }
        return if (reasons.isEmpty()) RecallPlanValidationResult.Valid else RecallPlanValidationResult.Invalid(reasons)
    }

    fun validateSubmission(
        plan: RecallPlan,
        submission: RecallSubmission,
        existingAttemptIds: Set<RecallAttemptId> = emptySet()
    ): RecallSubmissionValidationResult {
        val context = submission.context
        val reasons = linkedSetOf<RecallRejectionReason>()
        if (plan.version != RecallContractVersion.CURRENT) reasons += RecallRejectionReason.UNSUPPORTED_VERSION
        if (context.mode != plan.mode) reasons += RecallRejectionReason.MODE_MISMATCH
        if (context.planId != plan.planId || context.learnerId != plan.learnerId ||
            context.contentId != plan.contentId || context.sessionId != plan.sessionId
        ) reasons += RecallRejectionReason.IDENTITY_MISMATCH
        if (context.attemptId in existingAttemptIds) reasons += RecallRejectionReason.DUPLICATE_ATTEMPT
        if (!submissionKindAllowed(plan.mode, submission)) reasons += RecallRejectionReason.INVALID_SUBMISSION_KIND
        return if (reasons.isEmpty()) RecallSubmissionValidationResult.Accepted
        else RecallSubmissionValidationResult.Rejected(reasons)
    }

    private fun submissionKindAllowed(mode: RecallMode, submission: RecallSubmission): Boolean = when (submission) {
        is RecallSubmission.TypedText -> mode != RecallMode.MULTIPLE_CHOICE
        is RecallSubmission.Choice -> mode == RecallMode.MULTIPLE_CHOICE
        is RecallSubmission.PlaybackAcknowledgement -> mode == RecallMode.LISTENING
        is RecallSubmission.Reveal, is RecallSubmission.Skip, is RecallSubmission.Timeout -> true
    }

    private fun requirementsSupport(mode: RecallMode, value: RecallPlatformRequirements): Boolean = when (mode) {
        RecallMode.TYPING, RecallMode.REVERSE_TRANSLATION -> value.requiresTextInput
        RecallMode.MULTIPLE_CHOICE -> value.requiresChoiceSelection
        RecallMode.LISTENING -> value.requiresAudioPlayback
        RecallMode.IMAGE_RECALL -> value.requiresImageRendering && value.requiresTextInput
        RecallMode.EXAMPLE_COMPLETION -> value.requiresExampleRendering && value.requiresTextInput
        RecallMode.DICTATION -> value.requiresAudioPlayback && value.requiresTextInput
    }
}

object RecallEvidenceEligibilityPolicy {
    fun resolve(
        outcome: RecallOutcome,
        assistance: Set<RecallAssistance>,
        provenance: RecallProvenance
    ): RecallEvidenceEligibility = when {
        provenance != RecallProvenance.EVALUATIVE -> RecallEvidenceEligibility.INELIGIBLE
        outcome == RecallOutcome.REVEALED || RecallAssistance.ANSWER_REVEALED in assistance ->
            RecallEvidenceEligibility.INELIGIBLE
        outcome == RecallOutcome.CORRECT && assistance == setOf(RecallAssistance.NONE) ->
            RecallEvidenceEligibility.STANDARD
        outcome == RecallOutcome.CORRECT -> RecallEvidenceEligibility.WEAK
        else -> RecallEvidenceEligibility.INELIGIBLE
    }
}
