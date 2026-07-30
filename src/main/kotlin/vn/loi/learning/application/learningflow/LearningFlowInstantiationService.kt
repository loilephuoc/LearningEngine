package vn.loi.learning.application.learningflow

import vn.loi.learning.application.flowtemplate.LearningFlowTemplate
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateSlot
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceSelectionProfile
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.UserChoiceExperienceStrategy
import vn.loi.learning.application.learningstrategy.LearningStrategyDefinition

/**
 * Application-level runtime instantiation service that resolves template slots into concrete
 * ExperienceSelectionResult values and delegates flow construction to LearningFlowPlanner.
 */
class LearningFlowInstantiationService(
    private val planner: LearningFlowPlanner = LearningFlowPlanner()
) {
    fun instantiate(
        template: LearningFlowTemplate,
        strategy: LearningStrategyDefinition,
        experiencePlan: LearningExperiencePlan,
        rotation: ExperienceRotationContext
    ): LearningFlowDefinition {
        val selections = mutableMapOf<LearningFlowTemplateSlot, ExperienceSelectionResult>()

        val requiredSlots =
            template.stages
                .filterIsInstance<vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage.Experience>()
                .mapTo(linkedSetOf()) { it.slot }

        if (LearningFlowTemplateSlot.ROTATED_PRIMARY in requiredSlots) {
            selections[LearningFlowTemplateSlot.ROTATED_PRIMARY] =
                ExperienceSelectionEngine(RoundRobinExperienceStrategy()).select(
                    ExperienceSelectionRequest(
                        ExperienceSelectionProfile.AUTOMATIC.project(experiencePlan.options),
                        rotation.ordinal
                    )
                )
        }

        if (LearningFlowTemplateSlot.OPTIONAL_TYPING in requiredSlots) {
            val typing =
                ExperienceSelectionEngine(
                    UserChoiceExperienceStrategy(LearningExperienceKind.TYPING_RECALL)
                ).select(
                    ExperienceSelectionRequest(
                        ExperienceSelectionProfile.USER_SELECTABLE.project(experiencePlan.options),
                        rotation.ordinal
                    )
                )
            selections[LearningFlowTemplateSlot.OPTIONAL_TYPING] = typing
        }

        return planner.instantiate(template, selections, rotation)
    }
}
