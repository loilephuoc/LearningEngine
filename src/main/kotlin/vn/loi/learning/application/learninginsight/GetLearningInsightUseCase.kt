package vn.loi.learning.application.learninginsight

import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.LearnerId

data class GetLearningInsightQuery(
    val learnerId: LearnerId,
    val contentId: ContentId,
    val promotionDecision: PromotionDecision? = null,
    val context: LearningInsightContext = LearningInsightContext()
)

sealed interface GetLearningInsightResult {
    data class Success(val insight: LearningInsightBundle) : GetLearningInsightResult
    data class NotFound(val learnerId: LearnerId, val contentId: ContentId) : GetLearningInsightResult
    data class InsufficientData(val insight: LearningInsightBundle) : GetLearningInsightResult
    data class InvalidScope(val reason: LearningInsightFailureReason) : GetLearningInsightResult
    data class Failure(val reason: LearningInsightFailureReason) : GetLearningInsightResult
}

enum class LearningInsightFailureReason { AUTHORITY_IDENTITY_MISMATCH, INVALID_AUTHORITY_STATE }

class GetLearningInsightUseCase(
    private val trajectories: LearningTrajectoryRepository,
    private val profiles: LearningDifficultyProfileCalculator,
    private val strategy: AdaptiveLearningStrategy,
    private val projector: LearningInsightProjector,
    private val clock: EvidenceClock
) {
    fun execute(query: GetLearningInsightQuery): GetLearningInsightResult {
        val trajectory = trajectories.find(query.learnerId, query.contentId)
        if (trajectory == null) {
            if (query.context.mode == LearningInsightMode.PRACTICE_ONLY) {
                return GetLearningInsightResult.InsufficientData(
                    projector.project(
                        query.learnerId, query.contentId, clock.now(), null, null,
                        promotion = null, context = query.context
                    )
                )
            }
            return GetLearningInsightResult.NotFound(query.learnerId, query.contentId)
        }
        if (trajectory.contentId != query.contentId) {
            return GetLearningInsightResult.InvalidScope(
                LearningInsightFailureReason.AUTHORITY_IDENTITY_MISMATCH
            )
        }
        return try {
            val profile = profiles.calculateDifficultyProfile(query.learnerId, trajectory)
            val recommendation = strategy.calculateRecommendation(profile)
            val bundle = projector.project(
                query.learnerId, query.contentId, profile.calculatedAt, profile, recommendation,
                query.promotionDecision, query.context
            )
            if (profile.confidence == DifficultyConfidence.LOW) {
                GetLearningInsightResult.InsufficientData(bundle)
            } else {
                GetLearningInsightResult.Success(bundle)
            }
        } catch (_: IllegalArgumentException) {
            GetLearningInsightResult.Failure(LearningInsightFailureReason.INVALID_AUTHORITY_STATE)
        }
    }
}
