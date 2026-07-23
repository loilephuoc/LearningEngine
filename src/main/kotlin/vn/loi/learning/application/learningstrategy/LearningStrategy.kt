package vn.loi.learning.application.learningstrategy

import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningobjective.LearningObjective

@JvmInline
value class LearningStrategyId(val value: String) {
    init {
        require(value.isNotBlank()) { "Learning strategy ID must not be blank." }
    }
}

data class LearningStrategyDefinition(
    val id: LearningStrategyId,
    val objective: LearningObjective,
    val includeOptionalTyping: Boolean
)

/** Owns product decisions about which learning experiences serve an objective. */
class LearningStrategyPlanner {
    fun plan(
        objective: LearningObjective,
        experiencePlan: LearningExperiencePlan
    ): LearningStrategyDefinition {
        val includeTyping =
            experiencePlan.typingPrompt != null &&
                LearningExperienceKind.TYPING_RECALL in experiencePlan.options.orderedKinds
        return LearningStrategyDefinition(
            LearningStrategyId("${objective.id.value}-standard"),
            objective,
            includeOptionalTyping = includeTyping
        )
    }
}
