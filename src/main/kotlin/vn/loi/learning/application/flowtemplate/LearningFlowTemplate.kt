package vn.loi.learning.application.flowtemplate

import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningstrategy.LearningStrategyDefinition

@JvmInline
value class LearningFlowTemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "Flow template ID must not be blank." }
    }
}

sealed interface LearningFlowTemplateStage {
    val key: String

    data class Experience(
        override val key: String,
        val selection: ExperienceSelectionResult
    ) : LearningFlowTemplateStage

    data class AnswerReveal(
        override val key: String = "answer-reveal"
    ) : LearningFlowTemplateStage

    data class RatingReady(
        override val key: String = "rating-ready"
    ) : LearningFlowTemplateStage
}

data class LearningFlowTemplate(
    val id: LearningFlowTemplateId,
    val strategyId: vn.loi.learning.application.learningstrategy.LearningStrategyId,
    val stages: List<LearningFlowTemplateStage>
) {
    init {
        require(stages.isNotEmpty()) { "Flow template must contain stages." }
        require(stages.map { it.key }.all(String::isNotBlank))
        require(stages.map { it.key }.distinct().size == stages.size)
        require(stages.last() is LearningFlowTemplateStage.RatingReady)
        require(stages.count { it is LearningFlowTemplateStage.AnswerReveal } == 1)
        require(
            stages.indexOfFirst { it is LearningFlowTemplateStage.AnswerReveal } ==
                stages.lastIndex - 1
        ) {
            "Template answer reveal must immediately precede rating-ready."
        }
        require(stages.take(stages.lastIndex - 1).all { it is LearningFlowTemplateStage.Experience })
    }
}

/** Owns the ordered presentation recipe; Flow execution only instantiates this template. */
class LearningFlowTemplateFactory {
    fun create(strategy: LearningStrategyDefinition): LearningFlowTemplate {
        val stages = buildList {
            strategy.orderedExperiences.forEachIndexed { index, selection ->
                add(
                    LearningFlowTemplateStage.Experience(
                        key = if (index == 0) "primary" else "experience-$index",
                        selection = selection
                    )
                )
            }
            add(LearningFlowTemplateStage.AnswerReveal())
            add(LearningFlowTemplateStage.RatingReady())
        }
        return LearningFlowTemplate(
            LearningFlowTemplateId("${strategy.id.value}-flow"),
            strategy.id,
            java.util.Collections.unmodifiableList(stages)
        )
    }
}
