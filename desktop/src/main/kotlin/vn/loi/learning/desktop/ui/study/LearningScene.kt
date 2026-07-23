package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperienceSupportingRole

enum class SceneType {
    PROMPT,
    MEANING,
    LISTENING,
    IMAGE,
    EXAMPLE,
    TYPING
}

data class LearningSceneContext(
    val answerRevealed: Boolean
)

data class SceneCapabilities(
    val hasAudio: Boolean,
    val hasImage: Boolean,
    val hasMeaning: Boolean,
    val hasExamples: Boolean,
    val acceptsTyping: Boolean = false
)

sealed interface LearningScene {
    val type: SceneType
    val context: LearningSceneContext
    val capabilities: SceneCapabilities
    val blocks: List<PresentedLearningBlock>
    val supportingScenes: List<LearningScene>
}

data class PromptScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.PROMPT
}

data class ListeningScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.LISTENING
}

data class ImageScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.IMAGE
}

data class MeaningScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.MEANING
}

data class ExampleScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.EXAMPLE
}

/**
 * Reserved scene contract for a future typed-recall capability.
 * The current projector never selects it and no input or evaluation behavior exists yet.
 */
data class TypingScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.TYPING
}

/**
 * Projects an authoritative platform-neutral plan into Path/localization-ready Desktop scenes.
 * It never re-evaluates image/audio eligibility.
 */
class DesktopLearningSceneProjector {
    fun project(
        plan: LearningExperiencePlan?,
        selection: ExperienceSelectionResult?,
        presentation: LearningContentPresentation,
    ): LearningScene? {
        plan ?: return null
        selection ?: return null
        require(selection.availableKinds == plan.options.orderedKinds) {
            "Desktop scene projection requires selection from the supplied experience plan."
        }
        val question = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.QUESTION }
            ?: return null
        val answer = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.ANSWER }
        val example = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.EXAMPLE }
        val context = LearningSceneContext(plan.context.answerRevealed)
        val capabilities = SceneCapabilities(
            hasAudio =
                plan.capabilities.hasPromptAudio ||
                    plan.capabilities.hasAnswerAudio ||
                    plan.capabilities.hasExampleAudio,
            hasImage = plan.capabilities.hasPromptImage,
            hasMeaning = plan.capabilities.hasMeaning,
            hasExamples = plan.capabilities.hasExample
        )
        val supporting = buildList {
            if (LearningExperienceSupportingRole.MEANING in plan.visibleSupportingRoles) {
                answer?.let {
                    add(MeaningScene(context, capabilities, it.blocks))
                }
            }
            if (LearningExperienceSupportingRole.EXAMPLE in plan.visibleSupportingRoles) {
                example?.let {
                    add(ExampleScene(context, capabilities, it.blocks))
                }
            }
        }

        return when (selection.selectedKind) {
            LearningExperienceKind.IMAGE_RECALL ->
                ImageScene(context, capabilities, question.blocks, supporting)

            LearningExperienceKind.LISTENING_RECALL ->
                ListeningScene(context, capabilities, question.blocks, supporting)

            LearningExperienceKind.PROMPT_RECALL ->
                PromptScene(context, capabilities, question.blocks, supporting)
        }
    }
}
