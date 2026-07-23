package vn.loi.learning.application.session.completion

import vn.loi.learning.application.decision.AdaptiveAction
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.domain.study.memory.model.ReviewRating

class ProductBrainSessionCompletion(
    private val reflectionGenerator: SessionReflectionGenerator = SessionReflectionGenerator(),
    private val summaryGenerator: SessionSummaryGenerator = SessionSummaryGenerator()
) {
    fun schedulingOutcome(
        rating: ReviewRating,
        scheduledIntervalMillis: Long,
        nextReviewAtEpochMillis: Long
    ): SessionSchedulingOutcome {
        require(scheduledIntervalMillis >= 0L)
        require(nextReviewAtEpochMillis >= 0L)
        return SessionSchedulingOutcome(
            rating = rating,
            scheduledIntervalMillis = scheduledIntervalMillis,
            nextReviewAtEpochMillis = nextReviewAtEpochMillis,
            guidance =
                if (scheduledIntervalMillis == 0L) {
                    "Review this material again as soon as it becomes available."
                } else {
                    "Your next review has been scheduled to reinforce this learning."
                }
        )
    }

    fun prepare(input: SessionCompletionInput): SessionCompletionPlan {
        require(input.sceneResult.sceneId == input.evidence.sceneId) {
            "Scene result and learning evidence must describe the same scene."
        }
        require(input.evidence.evidenceId == input.decisionTrace.evidenceId) {
            "Decision trace must reference the supplied learning evidence."
        }
        require(input.decision.decisionId == input.decisionExplanation.decisionId) {
            "Decision explanation must reference the supplied adaptive decision."
        }
        require(input.finalDifficultyLevel == input.decision.newDifficultyLevel) {
            "Final difficulty must match the adaptive decision."
        }

        val needsReinforcement =
            input.evidence.performance != EvidencePerformance.CORRECT ||
                input.decision.action == AdaptiveAction.REPEAT_SIMILAR_SCENE

        return SessionCompletionPlan(
            input = input,
            reflection = reflectionGenerator.generate(input.evidence),
            summary = summaryGenerator.generate(input),
            learningOutcome =
                SessionLearningOutcome(
                    demonstratedLearning = input.decisionExplanation.observation,
                    needsReinforcement = needsReinforcement
                ),
            recommendedRating = recommendedRating(input)
        )
    }

    fun complete(
        plan: SessionCompletionPlan,
        schedulingOutcome: SessionSchedulingOutcome,
        sessionId: String,
        completedAtEpochMillis: Long
    ): SessionCompletionResult {
        require(schedulingOutcome.rating == plan.recommendedRating) {
            "Scheduling outcome must use the Product Brain completion rating."
        }
        require(completedAtEpochMillis >= 0L) {
            "Completion time must not be negative."
        }

        return SessionCompletionResult(
            sessionId = sessionId,
            completedAtEpochMillis = completedAtEpochMillis,
            input = plan.input,
            reflection = plan.reflection,
            summary = plan.summary,
            learningOutcome = plan.learningOutcome,
            schedulingOutcome = schedulingOutcome
        )
    }

    private fun recommendedRating(input: SessionCompletionInput): ReviewRating =
        when (input.evidence.performance) {
            EvidencePerformance.INCORRECT -> ReviewRating.AGAIN
            EvidencePerformance.PARTIAL -> ReviewRating.HARD
            EvidencePerformance.CORRECT ->
                if (input.decision.action == AdaptiveAction.INCREASE_DIFFICULTY) {
                    ReviewRating.EASY
                } else {
                    ReviewRating.GOOD
                }
        }
}
