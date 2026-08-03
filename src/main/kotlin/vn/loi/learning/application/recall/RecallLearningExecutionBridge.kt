package vn.loi.learning.application.recall

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.session.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.AutomaticRecallEvidenceInput
import vn.loi.learning.domain.study.evidence.RecallResult as EvidenceRecallResult
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus

enum class RecallRatingIntent(override val wireId: String) : StableWireValue {
    AUTOMATIC_SUCCESS("automatic-success"), AUTOMATIC_LAPSE("automatic-lapse"),
    MANUAL_USER("manual-user"), MANUAL_OVERRIDE("manual-override"), NO_RATING("no-rating"),
    PRACTICE_LOCAL_ONLY("practice-local-only")
}

enum class RecallLearningIneligibleReason(override val wireId: String) : StableWireValue {
    INVALID_SUBMISSION("invalid-submission"), SKIPPED("skipped"), TIMED_OUT("timed-out"),
    NO_RATING_POLICY("no-rating-policy"), REVEAL_NOT_ELIGIBLE("reveal-not-eligible")
}

enum class RecallManualActionReason(override val wireId: String) : StableWireValue {
    PARTIAL_RECALL("partial-recall"), EXPLICIT_RATING_REQUIRED("explicit-rating-required")
}

enum class RecallLearningExecutionViolation(override val wireId: String) : StableWireValue {
    IDENTITY_MISMATCH("identity-mismatch"), RESULT_INCONSISTENT("result-inconsistent"),
    INVALID_MANUAL_INTENT("invalid-manual-intent"), CONTEXT_MISMATCH("context-mismatch")
}

enum class RecallLearningCommitFailure(override val wireId: String) : StableWireValue {
    TRANSACTION_REJECTED("transaction-rejected"), PRACTICE_TRANSACTION_UNAVAILABLE("practice-transaction-unavailable"),
    MANUAL_OVERRIDE_UNAVAILABLE("manual-override-unavailable")
}

data class RecallManualRatingIntent(
    val rating: ReviewRating,
    val source: RatingSource,
    val expectedCurrentRating: ReviewRating? = null
) { init { require(source in setOf(RatingSource.MANUAL_USER, RatingSource.MANUAL_USER_OVERRIDE)) } }

data class RecallLearningExecutionPolicy(
    val version: String = "recall-learning-execution-v1",
    val strongExactRating: ReviewRating = ReviewRating.EASY,
    val standardSuccessRating: ReviewRating = ReviewRating.GOOD,
    val weakSuccessRating: ReviewRating = ReviewRating.HARD,
    val lapseRating: ReviewRating = ReviewRating.AGAIN,
    val revealedRating: ReviewRating = ReviewRating.AGAIN,
    val commitRevealAsLapse: Boolean = true,
    val commitSkip: Boolean = false,
    val commitTimeout: Boolean = false
) { init { require(version.isNotBlank()) } }

data class RecallLearningExecutionRequest(
    val recallResult: RecallResult,
    val sessionId: SessionId,
    val learningItemId: LearningItemId,
    val learnerId: LearnerId,
    val contentId: ContentId,
    val executionContext: RecallStrategyContext,
    val manualRatingIntent: RecallManualRatingIntent? = null,
    val policy: RecallLearningExecutionPolicy = RecallLearningExecutionPolicy(),
    val contractVersion: RecallContractVersion = RecallContractVersion.CURRENT
)

data class RecallLearningExecutionDecision(
    val intent: RecallRatingIntent,
    val proposedRating: ReviewRating?,
    val appendAutomaticEvidence: Boolean
)

sealed interface RecallLearningExecutionResult {
    data class Committed(val result: ReviewSessionItemResult, val decision: RecallLearningExecutionDecision) : RecallLearningExecutionResult
    data class PracticeRecorded(val result: CompletePracticeItemResult) : RecallLearningExecutionResult
    data class ManualOverrideCommitted(val result: ManualRatingOverrideResult) : RecallLearningExecutionResult
    data class Ineligible(val reason: RecallLearningIneligibleReason) : RecallLearningExecutionResult
    data class ManualActionRequired(val reason: RecallManualActionReason) : RecallLearningExecutionResult
    data class InvalidRequest(val violations: List<RecallLearningExecutionViolation>) : RecallLearningExecutionResult
    data class IdentityMismatch(val violations: List<RecallLearningExecutionViolation>) : RecallLearningExecutionResult
    data class DuplicateAttempt(val reviewEventId: ReviewEventId) : RecallLearningExecutionResult
    data class StaleSession(val sessionId: SessionId) : RecallLearningExecutionResult
    data class UnsupportedRecallOutcome(val outcome: RecallOutcome) : RecallLearningExecutionResult
    data class UnsupportedContractVersion(val version: RecallContractVersion) : RecallLearningExecutionResult
    data class CommitFailed(val reason: RecallLearningCommitFailure) : RecallLearningExecutionResult
}

object RecallLearningExecutionClassifier {
    fun classify(request: RecallLearningExecutionRequest): RecallLearningExecutionResult? {
        val result = request.recallResult
        request.manualRatingIntent?.let { manual ->
            val expectedSource = if (request.executionContext == RecallStrategyContext.PRACTICE_ONLY) RatingSource.MANUAL_USER_OVERRIDE else RatingSource.MANUAL_USER
            if (manual.source != expectedSource) return RecallLearningExecutionResult.InvalidRequest(listOf(RecallLearningExecutionViolation.INVALID_MANUAL_INTENT))
            return null
        }
        if (request.executionContext == RecallStrategyContext.PRACTICE_ONLY) return null
        return when (result.outcome) {
            RecallOutcome.PARTIAL -> RecallLearningExecutionResult.ManualActionRequired(RecallManualActionReason.PARTIAL_RECALL)
            RecallOutcome.INVALID_SUBMISSION -> RecallLearningExecutionResult.Ineligible(RecallLearningIneligibleReason.INVALID_SUBMISSION)
            RecallOutcome.SKIPPED -> if (request.policy.commitSkip) null else RecallLearningExecutionResult.Ineligible(RecallLearningIneligibleReason.SKIPPED)
            RecallOutcome.TIMED_OUT -> if (request.policy.commitTimeout) null else RecallLearningExecutionResult.Ineligible(RecallLearningIneligibleReason.TIMED_OUT)
            RecallOutcome.REVEALED -> if (request.policy.commitRevealAsLapse) null else RecallLearningExecutionResult.Ineligible(RecallLearningIneligibleReason.REVEAL_NOT_ELIGIBLE)
            RecallOutcome.CORRECT, RecallOutcome.INCORRECT -> null
        }
    }

    fun decision(request: RecallLearningExecutionRequest): RecallLearningExecutionDecision {
        request.manualRatingIntent?.let { manual ->
            return RecallLearningExecutionDecision(
                if (manual.source == RatingSource.MANUAL_USER_OVERRIDE) RecallRatingIntent.MANUAL_OVERRIDE else RecallRatingIntent.MANUAL_USER,
                manual.rating, false
            )
        }
        if (request.executionContext == RecallStrategyContext.PRACTICE_ONLY) {
            return RecallLearningExecutionDecision(RecallRatingIntent.PRACTICE_LOCAL_ONLY, null, false)
        }
        val result = request.recallResult
        val rating = when (result.outcome) {
            RecallOutcome.INCORRECT -> request.policy.lapseRating
            RecallOutcome.REVEALED -> request.policy.revealedRating
            RecallOutcome.SKIPPED, RecallOutcome.TIMED_OUT -> request.policy.lapseRating
            RecallOutcome.CORRECT -> when {
                result.evidenceEligibility == RecallEvidenceEligibility.STRONG && result.correctness == RecallCorrectness.EXACT -> request.policy.strongExactRating
                result.evidenceEligibility in setOf(RecallEvidenceEligibility.STANDARD, RecallEvidenceEligibility.STRONG) -> request.policy.standardSuccessRating
                else -> request.policy.weakSuccessRating
            }
            else -> null
        }
        val evidence = result.outcome in setOf(RecallOutcome.CORRECT, RecallOutcome.INCORRECT) &&
            result.evidenceEligibility in setOf(RecallEvidenceEligibility.STANDARD, RecallEvidenceEligibility.STRONG) &&
            !result.revealUsed && RecallAssistance.ANSWER_REVEALED !in result.assistanceUsed
        return RecallLearningExecutionDecision(
            if (result.outcome == RecallOutcome.INCORRECT || result.outcome == RecallOutcome.REVEALED) RecallRatingIntent.AUTOMATIC_LAPSE else RecallRatingIntent.AUTOMATIC_SUCCESS,
            rating, evidence
        )
    }
}

class RecallLearningExecutionBridge(
    private val sessions: StudySessionRepository,
    private val learningItems: LearningItemRepository,
    private val reviewEvents: ReviewEventRepository,
    private val queues: StudyQueueService,
    private val reviewSessionItem: ReviewSessionItemUseCase,
    private val completePracticeItem: CompletePracticeItemUseCase,
    private val manualRatingOverride: ManualRatingOverrideUseCase? = null
) {
    fun execute(request: RecallLearningExecutionRequest): RecallLearningExecutionResult {
        if (request.contractVersion != RecallContractVersion.CURRENT || request.recallResult.version != RecallContractVersion.CURRENT) {
            return RecallLearningExecutionResult.UnsupportedContractVersion(
                if (request.contractVersion != RecallContractVersion.CURRENT) request.contractVersion else request.recallResult.version
            )
        }
        val result = request.recallResult
        if (result.learnerId != request.learnerId || result.contentId != request.contentId) {
            return RecallLearningExecutionResult.IdentityMismatch(listOf(RecallLearningExecutionViolation.IDENTITY_MISMATCH))
        }
        if (!RecallResultValidator.validate(result, request.executionContext)) {
            return RecallLearningExecutionResult.InvalidRequest(listOf(RecallLearningExecutionViolation.RESULT_INCONSISTENT))
        }
        val eventId = eventId(result)
        if (reviewEvents.findAll(request.learnerId, request.learningItemId).any { it.id == eventId }) {
            return RecallLearningExecutionResult.DuplicateAttempt(eventId)
        }
        val session = sessions.findById(request.sessionId) ?: return RecallLearningExecutionResult.StaleSession(request.sessionId)
        if (session.status != SessionStatus.ACTIVE || session.learnerId != request.learnerId || session.currentLearningItemId != request.learningItemId) {
            return RecallLearningExecutionResult.StaleSession(request.sessionId)
        }
        val expectedPractice = session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY
        if (expectedPractice != (request.executionContext == RecallStrategyContext.PRACTICE_ONLY)) {
            return RecallLearningExecutionResult.InvalidRequest(listOf(RecallLearningExecutionViolation.CONTEXT_MISMATCH))
        }
        val item = learningItems.findById(request.learningItemId)
            ?: return RecallLearningExecutionResult.IdentityMismatch(listOf(RecallLearningExecutionViolation.IDENTITY_MISMATCH))
        if (item.contentId != request.contentId) return RecallLearningExecutionResult.IdentityMismatch(listOf(RecallLearningExecutionViolation.IDENTITY_MISMATCH))
        val queue = queues.get(request.sessionId) ?: return RecallLearningExecutionResult.StaleSession(request.sessionId)
        if (queue.isCompleted || queue.currentLearningItemId != request.learningItemId) return RecallLearningExecutionResult.StaleSession(request.sessionId)

        RecallLearningExecutionClassifier.classify(request)?.let { return it }
        val decision = RecallLearningExecutionClassifier.decision(request)
        return try {
            when (decision.intent) {
                RecallRatingIntent.PRACTICE_LOCAL_ONLY -> RecallLearningExecutionResult.PracticeRecorded(
                    completePracticeItem.execute(CompletePracticeItemCommand(request.sessionId, request.learningItemId, practiceResult(result)))
                )
                RecallRatingIntent.MANUAL_OVERRIDE -> {
                    val manual = requireNotNull(request.manualRatingIntent)
                    val useCase = manualRatingOverride ?: return RecallLearningExecutionResult.CommitFailed(RecallLearningCommitFailure.MANUAL_OVERRIDE_UNAVAILABLE)
                    RecallLearningExecutionResult.ManualOverrideCommitted(useCase.execute(ManualRatingOverrideCommand(
                        request.sessionId, eventId, request.learningItemId, manual.expectedCurrentRating, manual.rating, result.completedAt
                    )))
                }
                else -> RecallLearningExecutionResult.Committed(
                    reviewSessionItem.execute(ReviewSessionItemCommand(
                        request.sessionId, eventId, request.learningItemId, requireNotNull(decision.proposedRating),
                        result.completedAt, result.latency,
                        if (decision.intent == RecallRatingIntent.MANUAL_USER) RatingSource.MANUAL_USER else RatingSource.STANDARD_REVIEW,
                        automaticEvidence(result).takeIf { decision.appendAutomaticEvidence }
                    )), decision
                )
            }
        } catch (_: IllegalArgumentException) {
            RecallLearningExecutionResult.CommitFailed(RecallLearningCommitFailure.TRANSACTION_REJECTED)
        } catch (_: IllegalStateException) {
            RecallLearningExecutionResult.CommitFailed(RecallLearningCommitFailure.TRANSACTION_REJECTED)
        }
    }

    private fun eventId(result: RecallResult) = ReviewEventId("recall-${result.planId.value}-${result.attemptId.value}")
    private fun automaticEvidence(result: RecallResult) = AutomaticRecallEvidenceInput(
        if (result.correct) EvidenceRecallResult.CORRECT else EvidenceRecallResult.INCORRECT,
        result.revealUsed, result.latency.takeIf { result.mode == RecallMode.TYPING || result.mode == RecallMode.DICTATION }
    )
    private fun practiceResult(result: RecallResult) = when (result.outcome) {
        RecallOutcome.CORRECT -> PracticeRecallResult.CORRECT
        RecallOutcome.PARTIAL -> PracticeRecallResult.ALMOST_CORRECT
        RecallOutcome.REVEALED -> PracticeRecallResult.REVEALED
        else -> PracticeRecallResult.INCORRECT
    }
}
