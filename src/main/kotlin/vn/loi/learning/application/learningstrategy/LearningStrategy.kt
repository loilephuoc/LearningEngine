package vn.loi.learning.application.learningstrategy

import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningobjective.LearningObjective
import vn.loi.learning.domain.study.memory.model.LearningStage

@JvmInline
value class LearningStrategyId(val value: String) {
    init {
        require(value.isNotBlank()) { "Learning strategy ID must not be blank." }
    }
}

enum class PrimaryExperienceMode {
    ROTATED,
    TYPING
}

data class LearningStrategyDefinition(
    val id: LearningStrategyId,
    val objective: LearningObjective,
    val includeOptionalTyping: Boolean,
    val primaryExperienceMode: PrimaryExperienceMode
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
        val typingFirst =
            includeTyping &&
                experiencePlan.context.stage in
                    setOf(
                        LearningStage.REVIEW,
                        LearningStage.RELEARNING,
                        LearningStage.MASTERED
                    )
        return LearningStrategyDefinition(
            LearningStrategyId(
                "${objective.id.value}-" +
                    if (typingFirst) "typing-first" else "standard"
            ),
            objective,
            includeOptionalTyping = includeTyping && !typingFirst,
            primaryExperienceMode =
                if (typingFirst) PrimaryExperienceMode.TYPING
                else PrimaryExperienceMode.ROTATED
        )
    }
}
