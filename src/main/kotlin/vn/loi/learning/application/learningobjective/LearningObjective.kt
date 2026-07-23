package vn.loi.learning.application.learningobjective

import vn.loi.learning.application.learningexperience.LearningExperiencePlan

@JvmInline
value class LearningObjectiveId(val value: String) {
    init {
        require(value.isNotBlank()) { "Learning objective ID must not be blank." }
    }
}

data class LearningObjective(
    val id: LearningObjectiveId,
    val kind: LearningObjectiveKind
)

enum class LearningObjectiveKind {
    DURABLE_RECALL
}

/**
 * Product policy for choosing the learner outcome. V1 intentionally exposes one conservative
 * objective; adding another objective does not require changing Flow execution.
 */
class LearningObjectivePolicy {
    fun resolve(experiencePlan: LearningExperiencePlan): LearningObjective {
        require(experiencePlan.options.orderedKinds.isNotEmpty())
        return LearningObjective(
            LearningObjectiveId("durable-recall"),
            LearningObjectiveKind.DURABLE_RECALL
        )
    }
}
