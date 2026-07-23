package vn.loi.learning.application.learningflow

import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult

@JvmInline
value class LearningFlowStageId(val value: String) {
    init {
        require(value.isNotBlank()) { "Flow stage ID must not be blank." }
    }
}

@JvmInline
value class LearningFlowId(val value: String) {
    init {
        require(value.isNotBlank()) { "Flow ID must not be blank." }
    }
}

sealed interface LearningFlowStage {
    val id: LearningFlowStageId

    data class Experience(
        override val id: LearningFlowStageId,
        val selection: ExperienceSelectionResult
    ) : LearningFlowStage

    data class AnswerReveal(
        override val id: LearningFlowStageId
    ) : LearningFlowStage

    data class RatingReady(
        override val id: LearningFlowStageId
    ) : LearningFlowStage
}

class LearningFlowDefinition private constructor(
    val id: LearningFlowId,
    val context: ExperienceRotationContext,
    val stages: List<LearningFlowStage>
) {
    init {
        require(stages.isNotEmpty()) { "Learning flow must contain stages." }
        require(stages.map { it.id }.distinct().size == stages.size) {
            "Learning flow stage IDs must be unique."
        }
        require(stages.last() is LearningFlowStage.RatingReady) {
            "Learning flow must end in rating-ready."
        }
        require(stages.dropLast(1).none { it is LearningFlowStage.RatingReady }) {
            "No stage may follow terminal rating-ready."
        }
        require(stages.count { it is LearningFlowStage.AnswerReveal } == 1) {
            "Learning flow requires exactly one answer-reveal stage."
        }
        require(stages.indexOfFirst { it is LearningFlowStage.AnswerReveal } == stages.lastIndex - 1) {
            "Answer reveal must immediately precede rating-ready."
        }
        require(stages.take(stages.lastIndex - 1).all { it is LearningFlowStage.Experience }) {
            "Only experience stages may precede answer reveal."
        }
    }

    override fun equals(other: Any?) =
        other is LearningFlowDefinition &&
            id == other.id &&
            context == other.context &&
            stages == other.stages

    override fun hashCode() = 31 * (31 * id.hashCode() + context.hashCode()) + stages.hashCode()

    companion object {
        fun create(
            id: LearningFlowId,
            context: ExperienceRotationContext,
            stages: Iterable<LearningFlowStage>
        ) = LearningFlowDefinition(
            id,
            context,
            java.util.Collections.unmodifiableList(stages.toList())
        )
    }
}

data class LearningFlowState(
    val flowId: LearningFlowId,
    val sessionId: vn.loi.learning.domain.study.session.model.SessionId,
    val learningItemId: vn.loi.learning.domain.study.learning.model.LearningItemId,
    val currentStageIndex: Int,
    val completedStageIds: Set<LearningFlowStageId>
) {
    init {
        require(currentStageIndex >= 0)
    }
}

data class LearningFlowProgress(
    val completedExperienceCount: Int,
    val currentExperienceNumber: Int?,
    val totalExperienceCount: Int,
    val isRevealPending: Boolean,
    val isRatingReady: Boolean
) {
    init {
        require(totalExperienceCount > 0)
        require(completedExperienceCount in 0..totalExperienceCount)
        currentExperienceNumber?.let { require(it in 1..totalExperienceCount) }
    }
}
