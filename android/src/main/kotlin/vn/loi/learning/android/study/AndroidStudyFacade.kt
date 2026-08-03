package vn.loi.learning.android.study

import java.util.concurrent.atomic.AtomicLong
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.application.recall.ProductionRecallPlanRequest
import vn.loi.learning.application.recall.ProductionRecallPlanResult
import vn.loi.learning.application.recall.MultipleChoiceScopeId
import vn.loi.learning.application.recall.RecallAttemptNonce
import vn.loi.learning.application.recall.RecallPlanPolicy
import vn.loi.learning.application.recall.RecallExecutionRequest
import vn.loi.learning.application.recall.RecallExecutionResult
import vn.loi.learning.application.recall.RecallLearningExecutionRequest
import vn.loi.learning.application.recall.RecallLearningExecutionResult
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.LearningApplicationContext

sealed interface AndroidStudyState {
    data object Empty : AndroidStudyState
    data class Unsupported(val mode: RecallMode) : AndroidStudyState
    data class Typing(
        val plan: RecallPlan,
        val prompt: String,
        val answer: String = "",
        val evaluation: TypingAnswerEvaluationStatus = TypingAnswerEvaluationStatus.EMPTY,
        val revealed: Boolean = false,
        val completed: Boolean = false
    ) : AndroidStudyState
    data object Complete : AndroidStudyState
    data class Failed(val message: String) : AndroidStudyState
}

/** Thin platform facade: Shared Application owns planning, evaluation, learning and queue mutation. */
class AndroidStudyFacade(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val evaluator = TypingAnswerEvaluator()
    private val attemptSequence = AtomicLong()
    private val submittedPlans = mutableSetOf<RecallPlanId>()
    private var currentItem: NextSessionItem? = null

    fun load(restoredSessionId: String? = null): AndroidStudyState {
        val session = restoredSessionId
            ?.let(::SessionId)
            ?.let(context.engine::getSession)
            ?: context.engine.getActiveSession(learnerId)
            ?: return AndroidStudyState.Empty
        val next = context.engine.getNextSessionItem(session.id, Moment(now()))
            ?: return AndroidStudyState.Complete
        currentItem = next
        return createState(next)
    }

    fun updateAnswer(state: AndroidStudyState.Typing, answer: String): AndroidStudyState.Typing {
        if (state.completed || state.revealed) return state
        val evaluation = evaluator.evaluate(TypingRecallPrompt(state.plan.answerContract.canonicalAnswer), answer)
        return state.copy(answer = answer, evaluation = evaluation.status)
    }

    fun submitIfCorrect(state: AndroidStudyState.Typing): AndroidStudyState {
        if (state.completed || state.revealed || state.plan.planId in submittedPlans) return state
        val evaluation = evaluator.evaluate(TypingRecallPrompt(state.plan.answerContract.canonicalAnswer), state.answer)
        if (!evaluation.isCorrect) return state.copy(evaluation = evaluation.status)
        return execute(state, RecallAssistance.NONE, state.answer)
    }

    fun reveal(state: AndroidStudyState.Typing): AndroidStudyState {
        if (state.completed || state.plan.planId in submittedPlans) return state
        return execute(state, RecallAssistance.ANSWER_REVEALED, null)
    }

    fun next(state: AndroidStudyState.Typing): AndroidStudyState {
        if (!state.completed) return state
        return load(state.plan.sessionId.value)
    }

    private fun execute(
        state: AndroidStudyState.Typing,
        assistance: RecallAssistance,
        answer: String?
    ): AndroidStudyState {
        submittedPlans += state.plan.planId
        val submittedAt = Moment(maxOf(now(), state.plan.generatedAt.epochMillis))
        val submissionContext = RecallSubmissionContext(
            planId = state.plan.planId,
            attemptId = RecallAttemptId("android-${attemptSequence.incrementAndGet()}"),
            learnerId = state.plan.learnerId,
            contentId = state.plan.contentId,
            sessionId = state.plan.sessionId,
            mode = RecallMode.TYPING,
            submittedAt = submittedAt,
            assistanceState = setOf(assistance),
            platform = RecallPlatformKind.ANDROID
        )
        val submission = if (answer == null) {
            RecallSubmission.Reveal(submissionContext)
        } else {
            RecallSubmission.TypedText(submissionContext, answer)
        }
        val item = currentItem ?: return AndroidStudyState.Failed("Study item is unavailable.")
        val strategyContext = if (item.session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY) {
            RecallStrategyContext.PRACTICE_ONLY
        } else {
            RecallStrategyContext.EVALUATIVE
        }
        val result = context.engine.executeRecall(
            RecallExecutionRequest(state.plan, submission, evaluationContext = strategyContext)
        ) as? RecallExecutionResult.Completed
            ?: return AndroidStudyState.Failed("Shared recall execution rejected the attempt.")
        val learning = context.engine.executeRecallLearning(
            RecallLearningExecutionRequest(
                recallResult = result.result,
                sessionId = item.session.id,
                learningItemId = item.item.learningItem.id,
                learnerId = learnerId,
                contentId = item.item.content.id,
                executionContext = strategyContext
            )
        )
        if (learning !is RecallLearningExecutionResult.Committed &&
            learning !is RecallLearningExecutionResult.PracticeRecorded) {
            return AndroidStudyState.Failed("Shared learning execution rejected the attempt.")
        }
        return state.copy(revealed = answer == null, completed = true)
    }

    private fun createState(next: NextSessionItem): AndroidStudyState {
        val generatedAt = Moment(now())
        val nonce = RecallAttemptNonce(
            "${next.session.totalReviews}-${next.item.learningItem.id.value}-${next.progress?.currentPosition ?: 0}"
        )
        val scope = next.session.includedContentIds.mapNotNull { context.contentRepository?.findById(it) }
            .ifEmpty { listOf(next.item.content) }
        val result = context.engine.createProductionRecallPlan(
            ProductionRecallPlanRequest(
                learnerId = learnerId,
                content = next.item.content,
                learningItemId = next.item.learningItem.id,
                sessionId = next.session.id,
                attemptNonce = nonce,
                scopeId = MultipleChoiceScopeId(next.session.id.value),
                scopeContents = scope,
                difficultyProfile = null,
                learningRecommendation = null,
                promotionEvidenceContext = RecallPromotionEvidenceContext(
                    if (next.session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY)
                        RecallStrategyContext.PRACTICE_ONLY else RecallStrategyContext.EVALUATIVE
                ),
                planPolicy = RecallPlanPolicy(
                    sourceLanguage = RecallLanguageTag("en"),
                    targetLanguage = RecallLanguageTag("vi"),
                    punctuationPolicy = PunctuationPolicy.EXACT
                ),
                deterministicSeed = RecallDeterministicSeed(
                    (next.session.id.value + next.item.learningItem.id.value + next.session.totalReviews).hashCode().toLong()
                ),
                generatedAt = generatedAt
            )
        )
        val plan = (result as? ProductionRecallPlanResult.Created)?.plan
            ?: return AndroidStudyState.Failed("Shared recall planning is unavailable.")
        val prompt = plan.prompt as? RecallPrompt.Typing
            ?: return AndroidStudyState.Unsupported(plan.mode)
        return AndroidStudyState.Typing(plan, prompt.sourceText)
    }
}
