package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningflow.LearningFlowDefinition
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.domain.study.recall.StudyMode

internal data class DesktopLearningFlowIntent(
    val experiencePlan: LearningExperiencePlan,
    val definition: LearningFlowDefinition,
    val studyMode: StudyMode
)

internal object DesktopRecallStudyModeResolver {
    fun resolve(
        productBrainPlanner: ProductBrainPlanner,
        content: LearningContent,
        context: LearningExperienceContext,
        rotation: ExperienceRotationContext
    ): DesktopLearningFlowIntent? {
        val experiencePlan = productBrainPlanner.planExperience(content, context) ?: return null
        val definition = productBrainPlanner.planFlow(experiencePlan, rotation)
        val primaryExperience = definition.stages
            .filterIsInstance<LearningFlowStage.Experience>()
            .first()
            .selection
            .selectedKind
        return DesktopLearningFlowIntent(
            experiencePlan = experiencePlan,
            definition = definition,
            studyMode = if (primaryExperience == LearningExperienceKind.TYPING_RECALL) {
                StudyMode.TYPING
            } else {
                StudyMode.ADAPTIVE
            }
        )
    }
}
