package vn.loi.learning.application.flowtemplate

import vn.loi.learning.application.learningstrategy.LearningStrategyDefinition
import vn.loi.learning.application.learningstrategy.PrimaryExperienceMode

@JvmInline
value class LearningFlowTemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "Flow template ID must not be blank." }
    }
}

enum class LearningFlowTemplateSlot {
    ROTATED_PRIMARY,
    OPTIONAL_TYPING
}

sealed interface LearningFlowTemplateStage {
    val key: String

    data class Experience(
        override val key: String,
        val slot: LearningFlowTemplateSlot
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
            when (strategy.primaryExperienceMode) {
                PrimaryExperienceMode.ROTATED ->
                    add(
                        LearningFlowTemplateStage.Experience(
                            key = "primary",
                            slot = LearningFlowTemplateSlot.ROTATED_PRIMARY
                        )
                    )
                PrimaryExperienceMode.TYPING ->
                    add(
                        LearningFlowTemplateStage.Experience(
                            key = "typing-primary",
                            slot = LearningFlowTemplateSlot.OPTIONAL_TYPING
                        )
                    )
            }
            if (strategy.includeOptionalTyping) {
                add(
                    LearningFlowTemplateStage.Experience(
                        key = "experience-1",
                        slot = LearningFlowTemplateSlot.OPTIONAL_TYPING
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
