package vn.loi.learning.application.learningflow

import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceSelectionProfile
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.UserChoiceExperienceStrategy

class LearningFlowPlanner {
    fun plan(
        experiencePlan: LearningExperiencePlan,
        rotation: ExperienceRotationContext
    ): LearningFlowDefinition {
        val automaticOptions =
            ExperienceSelectionProfile.AUTOMATIC.project(experiencePlan.options)
        val primary =
            ExperienceSelectionEngine(RoundRobinExperienceStrategy()).select(
                ExperienceSelectionRequest(automaticOptions, rotation.ordinal)
            )
        val stages = buildList {
            add(
                LearningFlowStage.Experience(
                    LearningFlowStageId("primary"),
                    primary
                )
            )
            if (
                experiencePlan.typingPrompt != null &&
                LearningExperienceKind.TYPING_RECALL in experiencePlan.options.orderedKinds
            ) {
                add(
                    LearningFlowStage.Experience(
                        LearningFlowStageId("typing"),
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
                )
            }
            add(LearningFlowStage.AnswerReveal(LearningFlowStageId("answer-reveal")))
            add(LearningFlowStage.RatingReady(LearningFlowStageId("rating-ready")))
        }
        return LearningFlowDefinition.create(
            id =
                LearningFlowId(
                    "${rotation.sessionId.value}:${rotation.learningItemId.value}:${rotation.ordinal}"
                ),
            context = rotation,
            stages = stages
        )
    }
}
