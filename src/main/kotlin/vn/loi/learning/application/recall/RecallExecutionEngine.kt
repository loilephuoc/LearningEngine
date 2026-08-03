package vn.loi.learning.application.recall

import java.text.Normalizer
import java.util.Locale
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.recall.*

typealias SharedTypingRecallFeedbackEvaluator = TypingAnswerEvaluator

enum class RecallExecutionProvenance(override val wireId: String) : StableWireValue {
    PLATFORM_SUBMISSION("platform-submission"), SHARED_CORE_REPLAY("shared-core-replay")
}

enum class RecallExecutionViolation(override val wireId: String) : StableWireValue {
    INVALID_PLAN("invalid-plan"), IDENTITY_MISMATCH("identity-mismatch"), MODE_MISMATCH("mode-mismatch"),
    INVALID_SUBMISSION_KIND("invalid-submission-kind"), INVALID_ASSISTANCE("invalid-assistance"),
    INVALID_TIMESTAMP("invalid-timestamp"), CONTEXT_MISMATCH("context-mismatch"),
    INVALID_RESULT("invalid-result")
}

enum class RecallEvaluationFailure(override val wireId: String) : StableWireValue {
    BLANK_RESPONSE("blank-response"), UNKNOWN_CHOICE("unknown-choice"), MALFORMED_CHOICE_PLAN("malformed-choice-plan"),
    PLAYBACK_INCOMPLETE("playback-incomplete"), INVALID_TEXT_SUBMISSION("invalid-text-submission"),
    RESULT_VALIDATION_FAILED("result-validation-failed")
}

data class RecallAttemptSnapshot(val knownAttemptIds: Set<RecallAttemptId> = emptySet())

data class RecallExecutionPolicy(
    val version: String = "recall-execution-v1",
    val evaluatorVersion: String = "recall-evaluator-v1",
    val lightAssistanceEligibility: RecallEvidenceEligibility = RecallEvidenceEligibility.WEAK,
    val recognitionEligibility: RecallEvidenceEligibility = RecallEvidenceEligibility.WEAK,
    val correctEligibility: RecallEvidenceEligibility = RecallEvidenceEligibility.STANDARD
) { init { require(version.isNotBlank()); require(evaluatorVersion.isNotBlank()) } }

data class RecallExecutionRequest(
    val plan: RecallPlan,
    val submission: RecallSubmission,
    val policy: RecallExecutionPolicy = RecallExecutionPolicy(),
    val attemptSnapshot: RecallAttemptSnapshot = RecallAttemptSnapshot(),
    val evaluationContext: RecallStrategyContext,
    val contractVersion: RecallContractVersion = RecallContractVersion.CURRENT,
    val provenance: RecallExecutionProvenance = RecallExecutionProvenance.PLATFORM_SUBMISSION
)

sealed interface RecallExecutionResult {
    data class Completed(val result: RecallResult) : RecallExecutionResult
    data class InvalidPlan(val violations: List<RecallExecutionViolation>) : RecallExecutionResult
    data class InvalidSubmission(val violations: List<RecallExecutionViolation>) : RecallExecutionResult
    data class IdentityMismatch(val violations: List<RecallExecutionViolation>) : RecallExecutionResult
    data class UnsupportedMode(val mode: RecallMode) : RecallExecutionResult
    data class UnsupportedContractVersion(val version: RecallContractVersion) : RecallExecutionResult
    data class DuplicateAttempt(val attemptId: RecallAttemptId) : RecallExecutionResult
    data class Rejected(val reason: RecallEvaluationFailure) : RecallExecutionResult
    data class EvaluationFailed(val reason: RecallEvaluationFailure) : RecallExecutionResult
}

data class RecallModeEvaluation(
    val outcome: RecallOutcome,
    val correct: Boolean,
    val correctness: RecallCorrectness,
    val normalizedResponse: String?
)

sealed interface RecallModeEvaluationResult {
    data class Evaluated(val evaluation: RecallModeEvaluation) : RecallModeEvaluationResult
    data class Rejected(val reason: RecallEvaluationFailure) : RecallModeEvaluationResult
}

interface RecallModeEvaluator {
    val mode: RecallMode
    fun evaluate(plan: RecallPlan, submission: RecallSubmission): RecallModeEvaluationResult
}

sealed interface RecallModeEvaluatorRegistryResult {
    data class Created(val registry: RecallModeEvaluatorRegistry) : RecallModeEvaluatorRegistryResult
    data class DuplicateEvaluators(val modes: List<RecallMode>) : RecallModeEvaluatorRegistryResult
}

class RecallModeEvaluatorRegistry private constructor(private val evaluators: Map<RecallMode, RecallModeEvaluator>) {
    fun evaluatorFor(mode: RecallMode): RecallModeEvaluator? = evaluators[mode]
    companion object {
        fun create(evaluators: List<RecallModeEvaluator>): RecallModeEvaluatorRegistryResult {
            val duplicates = evaluators.groupBy(RecallModeEvaluator::mode).filterValues { it.size > 1 }.keys.sortedBy { it.wireId }
            return if (duplicates.isEmpty()) RecallModeEvaluatorRegistryResult.Created(RecallModeEvaluatorRegistry(evaluators.associateBy { it.mode }))
            else RecallModeEvaluatorRegistryResult.DuplicateEvaluators(duplicates)
        }
        fun standard(): RecallModeEvaluatorRegistry = (create(listOf(
            TextRecallModeEvaluator(RecallMode.TYPING), MultipleChoiceRecallModeEvaluator,
            TextRecallModeEvaluator(RecallMode.LISTENING), TextRecallModeEvaluator(RecallMode.IMAGE_RECALL),
            TextRecallModeEvaluator(RecallMode.REVERSE_TRANSLATION), TextRecallModeEvaluator(RecallMode.EXAMPLE_COMPLETION),
            TextRecallModeEvaluator(RecallMode.DICTATION)
        )) as RecallModeEvaluatorRegistryResult.Created).registry
    }
}

class RecallExecutionEngine(private val registry: RecallModeEvaluatorRegistry = RecallModeEvaluatorRegistry.standard()) {
    fun execute(request: RecallExecutionRequest): RecallExecutionResult {
        if (request.contractVersion != RecallContractVersion.CURRENT || request.plan.version != RecallContractVersion.CURRENT) {
            return RecallExecutionResult.UnsupportedContractVersion(if (request.contractVersion != RecallContractVersion.CURRENT) request.contractVersion else request.plan.version)
        }
        if (RecallContractValidator.validatePlan(request.plan) !is RecallPlanValidationResult.Valid) {
            return RecallExecutionResult.InvalidPlan(listOf(RecallExecutionViolation.INVALID_PLAN))
        }
        val context = request.submission.context
        if (context.attemptId in request.attemptSnapshot.knownAttemptIds) return RecallExecutionResult.DuplicateAttempt(context.attemptId)
        if (context.planId != request.plan.planId || context.learnerId != request.plan.learnerId ||
            context.contentId != request.plan.contentId || context.sessionId != request.plan.sessionId) {
            return RecallExecutionResult.IdentityMismatch(listOf(RecallExecutionViolation.IDENTITY_MISMATCH))
        }
        val expectedContext = if (request.plan.provenance == RecallProvenance.PRACTICE) RecallStrategyContext.PRACTICE_ONLY else RecallStrategyContext.EVALUATIVE
        if (request.evaluationContext != expectedContext) return RecallExecutionResult.InvalidSubmission(listOf(RecallExecutionViolation.CONTEXT_MISMATCH))
        if (context.submittedAt < request.plan.generatedAt) return RecallExecutionResult.InvalidSubmission(listOf(RecallExecutionViolation.INVALID_TIMESTAMP))
        val assistanceViolation = validateAssistance(request.plan, request.submission)
        if (assistanceViolation) return RecallExecutionResult.InvalidSubmission(listOf(RecallExecutionViolation.INVALID_ASSISTANCE))
        when (val validation = RecallContractValidator.validateSubmission(request.plan, request.submission)) {
            RecallSubmissionValidationResult.Accepted -> Unit
            is RecallSubmissionValidationResult.Rejected -> {
                val violations = validation.reasons.map { reason -> when (reason) {
                    RecallRejectionReason.IDENTITY_MISMATCH -> RecallExecutionViolation.IDENTITY_MISMATCH
                    RecallRejectionReason.MODE_MISMATCH -> RecallExecutionViolation.MODE_MISMATCH
                    else -> RecallExecutionViolation.INVALID_SUBMISSION_KIND
                }}.distinct().sortedBy { it.wireId }
                return RecallExecutionResult.InvalidSubmission(violations)
            }
        }
        val evaluation = terminalEvaluation(request.submission) ?: run {
            val evaluator = registry.evaluatorFor(request.plan.mode) ?: return RecallExecutionResult.UnsupportedMode(request.plan.mode)
            when (val result = evaluator.evaluate(request.plan, request.submission)) {
                is RecallModeEvaluationResult.Evaluated -> result.evaluation
                is RecallModeEvaluationResult.Rejected -> return RecallExecutionResult.Rejected(result.reason)
            }
        }
        val assistance = effectiveAssistance(request.submission)
        val evidence = evidenceEligibility(request, evaluation, assistance)
        val result = RecallResult(
            request.plan.version, request.plan.planId, context.attemptId, context.learnerId, context.contentId,
            request.plan.mode, request.plan.direction, evaluation.outcome, evaluation.correct,
            evaluation.normalizedResponse, TimeSpan(context.submittedAt.epochMillis - request.plan.generatedAt.epochMillis),
            assistance, RecallAssistance.ANSWER_REVEALED in assistance, context.retryCount,
            request.plan.provenance, evidence, null, context.submittedAt, evaluation.correctness,
            request.policy.evaluatorVersion, request.policy.version, context.platform
        )
        return if (RecallResultValidator.validate(result, request.evaluationContext)) RecallExecutionResult.Completed(result)
        else RecallExecutionResult.EvaluationFailed(RecallEvaluationFailure.RESULT_VALIDATION_FAILED)
    }

    private fun terminalEvaluation(submission: RecallSubmission): RecallModeEvaluation? = when (submission) {
        is RecallSubmission.Reveal -> RecallModeEvaluation(RecallOutcome.REVEALED, false, RecallCorrectness.NOT_APPLICABLE, null)
        is RecallSubmission.Skip -> RecallModeEvaluation(RecallOutcome.SKIPPED, false, RecallCorrectness.NOT_APPLICABLE, null)
        is RecallSubmission.Timeout -> RecallModeEvaluation(RecallOutcome.TIMED_OUT, false, RecallCorrectness.NOT_APPLICABLE, null)
        else -> null
    }

    private fun validateAssistance(plan: RecallPlan, submission: RecallSubmission): Boolean {
        val state = submission.context.assistanceState
        if (RecallAssistance.NONE in state && state.size > 1) return true
        if (state.any { it != RecallAssistance.NONE && it !in plan.availableAssistance }) return true
        if (submission !is RecallSubmission.Reveal && RecallAssistance.ANSWER_REVEALED in state) return true
        if (submission is RecallSubmission.Reveal && RecallAssistance.ANSWER_REVEALED !in plan.availableAssistance) return true
        return false
    }

    private fun effectiveAssistance(submission: RecallSubmission): Set<RecallAssistance> {
        val values = submission.context.assistanceState.filterNotTo(linkedSetOf()) { it == RecallAssistance.NONE }
        if (submission is RecallSubmission.Reveal) values += RecallAssistance.ANSWER_REVEALED
        if (values.isEmpty()) values += RecallAssistance.NONE
        return values.sortedBy { it.wireId }.toCollection(linkedSetOf())
    }

    private fun evidenceEligibility(r: RecallExecutionRequest, evaluation: RecallModeEvaluation, assistance: Set<RecallAssistance>): RecallEvidenceEligibility = when {
        r.evaluationContext == RecallStrategyContext.PRACTICE_ONLY -> RecallEvidenceEligibility.INELIGIBLE
        evaluation.outcome != RecallOutcome.CORRECT || RecallAssistance.ANSWER_REVEALED in assistance -> RecallEvidenceEligibility.INELIGIBLE
        r.plan.mode == RecallMode.MULTIPLE_CHOICE -> r.policy.recognitionEligibility
        assistance != setOf(RecallAssistance.NONE) -> r.policy.lightAssistanceEligibility
        else -> minEligibility(r.policy.correctEligibility, r.plan.evidenceClass)
    }

    private fun minEligibility(left: RecallEvidenceEligibility, right: RecallEvidenceEligibility): RecallEvidenceEligibility {
        val rank = mapOf(RecallEvidenceEligibility.INELIGIBLE to 0, RecallEvidenceEligibility.WEAK to 1,
            RecallEvidenceEligibility.STANDARD to 2, RecallEvidenceEligibility.STRONG to 3)
        return if (requireNotNull(rank[left]) <= requireNotNull(rank[right])) left else right
    }
}

private class TextRecallModeEvaluator(override val mode: RecallMode) : RecallModeEvaluator {
    override fun evaluate(plan: RecallPlan, submission: RecallSubmission): RecallModeEvaluationResult {
        val text = (submission as? RecallSubmission.TypedText)?.text
            ?: return if (submission is RecallSubmission.PlaybackAcknowledgement) RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.PLAYBACK_INCOMPLETE)
            else RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.INVALID_TEXT_SUBMISSION)
        if (text.isBlank()) return RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.BLANK_RESPONSE)
        val match = RecallAnswerContractEvaluator.evaluate(plan.answerContract, text)
        return RecallModeEvaluationResult.Evaluated(RecallModeEvaluation(
            if (match.correct) RecallOutcome.CORRECT else RecallOutcome.INCORRECT,
            match.correct, match.correctness, match.normalizedResponse
        ))
    }
}

private object MultipleChoiceRecallModeEvaluator : RecallModeEvaluator {
    override val mode = RecallMode.MULTIPLE_CHOICE
    override fun evaluate(plan: RecallPlan, submission: RecallSubmission): RecallModeEvaluationResult {
        val choiceId = (submission as? RecallSubmission.Choice)?.choiceId
            ?: return RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.INVALID_TEXT_SUBMISSION)
        val choices = (plan.prompt as? RecallPrompt.MultipleChoice)?.choices
            ?: return RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.MALFORMED_CHOICE_PLAN)
        if (choices.count(RecallChoice::correct) != 1) return RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.MALFORMED_CHOICE_PLAN)
        val selected = choices.singleOrNull { it.id == choiceId }
            ?: return RecallModeEvaluationResult.Rejected(RecallEvaluationFailure.UNKNOWN_CHOICE)
        return RecallModeEvaluationResult.Evaluated(RecallModeEvaluation(
            if (selected.correct) RecallOutcome.CORRECT else RecallOutcome.INCORRECT, selected.correct,
            if (selected.correct) RecallCorrectness.EXACT else RecallCorrectness.INCORRECT, choiceId
        ))
    }
}

data class RecallAnswerMatch(val correct: Boolean, val correctness: RecallCorrectness, val normalizedResponse: String)

object RecallAnswerContractEvaluator {
    fun evaluate(contract: RecallAnswerContract, response: String): RecallAnswerMatch {
        val authorities = listOf(contract.canonicalAnswer) + contract.acceptedAlternatives
        val rawExact = authorities.any { it == response }
        if (rawExact) return RecallAnswerMatch(true, RecallCorrectness.EXACT, response)
        val normalizedResponse = normalize(contract, response)
        val correct = authorities.any { normalize(contract, it) == normalizedResponse }
        return RecallAnswerMatch(correct, if (correct) RecallCorrectness.NORMALIZED else RecallCorrectness.INCORRECT, normalizedResponse)
    }

    private fun normalize(contract: RecallAnswerContract, value: String): String {
        if (contract.normalizationPolicy.value == "typing-v1" &&
            contract.caseSensitivity == CaseSensitivity.INSENSITIVE &&
            contract.whitespacePolicy == WhitespacePolicy.NORMALIZE &&
            contract.punctuationPolicy == PunctuationPolicy.EXACT) {
            return TypingAnswerEvaluator().evaluate(TypingRecallPrompt(contract.canonicalAnswer), value).normalizedAnswer
        }
        var normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
        if (contract.whitespacePolicy == WhitespacePolicy.NORMALIZE) normalized = normalized.trim().replace(Regex("\\s+"), " ")
        if (contract.caseSensitivity == CaseSensitivity.INSENSITIVE) normalized = normalized.lowercase(Locale.ROOT)
        if (contract.punctuationPolicy == PunctuationPolicy.IGNORE) normalized = normalized.replace(Regex("\\p{P}+"), "")
        return normalized
    }
}

object RecallResultValidator {
    fun validate(result: RecallResult, context: RecallStrategyContext): Boolean {
        if (result.latency.millis < 0) return false
        if (result.outcome == RecallOutcome.CORRECT && !result.correct) return false
        if (result.outcome != RecallOutcome.CORRECT && result.correct) return false
        if (result.revealUsed && result.evidenceEligibility != RecallEvidenceEligibility.INELIGIBLE) return false
        if (context == RecallStrategyContext.PRACTICE_ONLY && result.evidenceEligibility != RecallEvidenceEligibility.INELIGIBLE) return false
        if (result.outcome in setOf(RecallOutcome.REVEALED, RecallOutcome.SKIPPED, RecallOutcome.TIMED_OUT, RecallOutcome.INVALID_SUBMISSION) &&
            result.evidenceEligibility != RecallEvidenceEligibility.INELIGIBLE) return false
        return result.evaluatorVersion.isNotBlank() && result.policyVersion.isNotBlank()
    }
}
