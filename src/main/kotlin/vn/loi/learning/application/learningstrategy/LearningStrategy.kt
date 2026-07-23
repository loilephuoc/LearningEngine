package vn.loi.learning.application.learningstrategy

import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceSelectionProfile
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.UserChoiceExperienceStrategy
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
    val orderedExperiences: List<ExperienceSelectionResult>
) {
    init {
        require(orderedExperiences.isNotEmpty()) {
            "Learning strategy must contain an experience."
        }
        require(
            orderedExperiences.first().selectedKind != LearningExperienceKind.TYPING_RECALL
        ) {
            "Typing cannot be the automatic first experience."
        }
    }
}

/** Owns product decisions about which learning experiences serve an objective. */
class LearningStrategyPlanner {
    fun plan(
        objective: LearningObjective,
        experiencePlan: LearningExperiencePlan,
        rotation: ExperienceRotationContext
    ): LearningStrategyDefinition {
        val primary =
            ExperienceSelectionEngine(RoundRobinExperienceStrategy()).select(
                ExperienceSelectionRequest(
                    ExperienceSelectionProfile.AUTOMATIC.project(experiencePlan.options),
                    rotation.ordinal
                )
            )
        val experiences = buildList {
            add(primary)
            if (
                experiencePlan.typingPrompt != null &&
                LearningExperienceKind.TYPING_RECALL in experiencePlan.options.orderedKinds
            ) {
                add(
                    ExperienceSelectionEngine(
                        UserChoiceExperienceStrategy(LearningExperienceKind.TYPING_RECALL)
                    ).select(
                        ExperienceSelectionRequest(
                            ExperienceSelectionProfile.USER_SELECTABLE
                                .project(experiencePlan.options),
                            rotation.ordinal
                        )
                    )
                )
            }
        }
        return LearningStrategyDefinition(
            LearningStrategyId("${objective.id.value}-standard"),
            objective,
            java.util.Collections.unmodifiableList(experiences)
        )
    }
}
