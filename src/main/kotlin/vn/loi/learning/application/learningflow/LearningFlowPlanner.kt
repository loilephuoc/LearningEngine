package vn.loi.learning.application.learningflow

import vn.loi.learning.application.flowtemplate.LearningFlowTemplate
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateSlot
import vn.loi.learning.application.flowtemplate.LearningFlowTemplateStage
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult

class LearningFlowPlanner {
    fun instantiate(
        template: LearningFlowTemplate,
        selections: Map<LearningFlowTemplateSlot, ExperienceSelectionResult>,
        rotation: ExperienceRotationContext
    ): LearningFlowDefinition {
        val stages =
            template.stages.map { stage ->
                when (stage) {
                    is LearningFlowTemplateStage.Experience ->
                        LearningFlowStage.Experience(
                            LearningFlowStageId(stage.key),
                            requireNotNull(selections[stage.slot]) {
                                "Missing resolved selection for slot ${stage.slot}"
                            }
                        )

                    is LearningFlowTemplateStage.AnswerReveal ->
                        LearningFlowStage.AnswerReveal(LearningFlowStageId(stage.key))

                    is LearningFlowTemplateStage.RatingReady ->
                        LearningFlowStage.RatingReady(LearningFlowStageId(stage.key))
                }
            }
        return LearningFlowDefinition.create(
            id =
                LearningFlowId(
                    "${template.id.value}:${rotation.sessionId.value}:" +
                        "${rotation.learningItemId.value}:${rotation.ordinal}"
                ),
            context = rotation,
            stages = stages
        )
    }
}
