package vn.loi.learning.application.recall

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningDifficultyProfile
import vn.loi.learning.domain.study.evidence.LearningRecommendation
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

data class ProductionRecallPlanRequest(
    val learnerId: LearnerId,
    val content: Content,
    val learningItemId: LearningItemId,
    val sessionId: SessionId,
    val attemptNonce: RecallAttemptNonce,
    val scopeId: MultipleChoiceScopeId,
    val scopeContents: List<Content>,
    val difficultyProfile: LearningDifficultyProfile?,
    val learningRecommendation: LearningRecommendation?,
    val promotionEvidenceContext: RecallPromotionEvidenceContext,
    val recentModeHistory: RecallModeHistory = RecallModeHistory.empty(),
    val strategyPolicy: AdaptiveRecallStrategyPolicy = AdaptiveRecallStrategyPolicy(),
    val planPolicy: RecallPlanPolicy,
    val distractorPolicy: MultipleChoiceDistractorPolicy = MultipleChoiceDistractorPolicy(),
    val deterministicSeed: RecallDeterministicSeed,
    val generatedAt: Moment,
    val studyMode: StudyMode = StudyMode.ADAPTIVE,
    val contractVersion: RecallContractVersion = RecallContractVersion.CURRENT
)

data class RecallPlanResolutionFailure(
    val mode: RecallMode,
    val result: RecallPlanFactoryResult
)

sealed interface ProductionRecallPlanResult {
    data class Created(
        val plan: RecallPlan,
        val requestedMode: RecallMode,
        val resolvedMode: RecallMode,
        val fallbackFailures: List<RecallPlanResolutionFailure>
    ) : ProductionRecallPlanResult

    data class Unavailable(
        val strategyResult: RecallStrategyDecisionResult,
        val requestedMode: RecallMode? = null,
        val failures: List<RecallPlanResolutionFailure> = emptyList()
    ) : ProductionRecallPlanResult
}

class ProductionRecallPlanResolver(
    private val strategy: AdaptiveRecallStrategy = AdaptiveRecallStrategy(),
    private val planFactory: RecallPlanFactory = RecallPlanFactory()
) {
    fun resolve(request: ProductionRecallPlanRequest): ProductionRecallPlanResult {
        val projection = ContentRecallCapabilityResolver.resolve(request.content)
        val strategyResult = strategy.select(
            RecallStrategyRequest(
                request.learnerId,
                request.content.id,
                projection,
                request.difficultyProfile,
                request.learningRecommendation,
                request.promotionEvidenceContext,
                request.recentModeHistory,
                request.strategyPolicy,
                request.deterministicSeed,
                request.generatedAt
            )
        )
        val selected = when (request.studyMode) {
            StudyMode.LEARN_NEW -> null
            StudyMode.ADAPTIVE -> strategyResult as? RecallStrategyDecisionResult.Selected
            StudyMode.TYPING -> explicitTypingDecision(request, projection)
        }
            ?: return ProductionRecallPlanResult.Unavailable(strategyResult)
        val requestedMode = selected.decision.selectedMode
        val candidates = listOf(
            RecallStrategyCandidate(
                selected.decision.selectedMode,
                selected.decision.selectedDirection,
                modeStrength(selected.decision.selectedMode),
                listOf(selected.decision.primaryReason) + selected.decision.secondaryReasons
            )
        ) + if (request.studyMode == StudyMode.ADAPTIVE) selected.decision.fallbackCandidates else emptyList()
        val failures = mutableListOf<RecallPlanResolutionFailure>()
        for (candidate in candidates) {
            val candidateDecision = selected.decision.copy(
                selectedMode = candidate.mode,
                selectedDirection = candidate.direction
            )
            val provider = if (candidate.mode == RecallMode.MULTIPLE_CHOICE) provider(request) else null
            val result = planFactory.create(
                RecallPlanRequest(
                    request.learnerId,
                    request.content.id,
                    request.learningItemId,
                    request.sessionId,
                    request.attemptNonce,
                    candidateDecision,
                    projection,
                    request.content,
                    request.planPolicy,
                    request.contractVersion,
                    request.deterministicSeed,
                    request.generatedAt,
                    request.promotionEvidenceContext.strategyContext,
                    typedMultipleChoiceOptionProvider = provider
                )
            )
            if (result is RecallPlanFactoryResult.Created) {
                return ProductionRecallPlanResult.Created(
                    result.plan,
                    requestedMode,
                    candidate.mode,
                    failures.toList()
                )
            }
            failures += RecallPlanResolutionFailure(candidate.mode, result)
        }
        return ProductionRecallPlanResult.Unavailable(strategyResult, requestedMode, failures)
    }

    private fun explicitTypingDecision(
        request: ProductionRecallPlanRequest,
        projection: RecallCapabilityProjection
    ): RecallStrategyDecisionResult.Selected? {
        if (!projection.supports(RecallMode.TYPING)) return null
        val direction = RecallDirection.TARGET_TO_SOURCE.takeIf {
            it in projection.supportedDirectionsFor(RecallMode.TYPING)
        } ?: projection.supportedDirectionsFor(RecallMode.TYPING).firstOrNull() ?: return null
        return RecallStrategyDecisionResult.Selected(
            RecallStrategyDecision(
                selectedMode = RecallMode.TYPING,
                selectedDirection = direction,
                confidence = RecallStrategyConfidence.HIGH,
                primaryReason = RecallStrategyReason.CAPABILITY_AVAILABLE,
                secondaryReasons = emptyList(),
                fallbackCandidates = emptyList(),
                rejectedCandidates = emptyList(),
                deterministicSeed = request.deterministicSeed,
                policyVersion = request.strategyPolicy.version,
                decisionVersion = request.strategyPolicy.decisionVersion,
                contentId = request.content.id,
                learnerId = request.learnerId,
                generatedAt = request.generatedAt
            )
        )
    }

    private fun provider(request: ProductionRecallPlanRequest) = DeterministicMultipleChoiceOptionProvider(
        request.scopeId,
        request.scopeContents.map { content ->
            MultipleChoiceCandidate(
                content.id,
                request.scopeId,
                content.text.primaryText,
                content.text.translatedText,
                topicId = content.metadata.tags.sorted().firstOrNull(),
                provenance = MultipleChoiceCandidateProvenance.ACTIVE_SCOPE
            )
        },
        request.distractorPolicy,
        targetTopicId = request.content.metadata.tags.sorted().firstOrNull()
    )

    private fun modeStrength(mode: RecallMode): RecallModeStrength = when (mode) {
        RecallMode.TYPING, RecallMode.DICTATION, RecallMode.LISTENING -> RecallModeStrength.STRONG_RECALL
        RecallMode.REVERSE_TRANSLATION, RecallMode.IMAGE_RECALL -> RecallModeStrength.STANDARD_RECALL
        RecallMode.EXAMPLE_COMPLETION -> RecallModeStrength.ASSISTED_CONTEXT
        RecallMode.MULTIPLE_CHOICE -> RecallModeStrength.RECOGNITION
    }
}
