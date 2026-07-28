package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperienceSupportingRole
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

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

data class TypingScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    val prompt: TypingRecallPrompt,
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
        require(selection.selectedKind in plan.options.orderedKinds) {
            "Desktop scene projection requires a selection eligible in the supplied plan."
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

        val sanitizedQuestionBlocks = sanitizePromptBlocks(
            questionBlocks = question.blocks,
            kind = selection.selectedKind,
            answerRevealed = plan.context.answerRevealed
        )
        val availableQuestionSupport = if (plan.context.answerRevealed) {
            emptyList()
        } else {
            answer?.blocks.orEmpty().filter { block ->
                block is PresentedLearningBlock.Text &&
                    block.role == PresentedTextRole.VIETNAMESE_MEANING ||
                    block is PresentedLearningBlock.Audio &&
                    block.role == PresentedAudioRole.MEANING_TRANSLATION
            }.takeIf(List<PresentedLearningBlock>::isNotEmpty)?.let { blocks ->
                listOf(MeaningScene(context, capabilities, blocks))
            }.orEmpty()
        }
        val projectedSupporting = supporting + availableQuestionSupport

        return when (selection.selectedKind) {
            LearningExperienceKind.IMAGE_RECALL ->
                ImageScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.LISTENING_RECALL ->
                ListeningScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.PROMPT_RECALL ->
                PromptScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.TYPING_RECALL ->
                TypingScene(
                    context = context,
                    capabilities = capabilities.copy(acceptsTyping = true),
                    blocks = sanitizedQuestionBlocks,
                    prompt = requireNotNull(plan.typingPrompt) {
                        "Typing selection requires an expected-answer prompt."
                    },
                    supportingScenes = projectedSupporting
                )
        }
    }

    private fun sanitizePromptBlocks(
        questionBlocks: List<PresentedLearningBlock>,
        kind: LearningExperienceKind,
        answerRevealed: Boolean
    ): List<PresentedLearningBlock> {
        if (answerRevealed) {
            return questionBlocks
        }
        val imageBlock = questionBlocks.filterIsInstance<PresentedLearningBlock.Image>().firstOrNull()
        val primaryAudioBlock = questionBlocks
            .filterIsInstance<PresentedLearningBlock.Audio>()
            .firstOrNull { it.role == PresentedAudioRole.PRIMARY_WORD }
        val unavailableBlock = questionBlocks.filterIsInstance<PresentedLearningBlock.Unavailable>().firstOrNull()
        val result = buildList {
            when (kind) {
                LearningExperienceKind.IMAGE_RECALL -> {
                    if (imageBlock != null) add(imageBlock)
                    else unavailableBlock?.let(::add)
                }
                LearningExperienceKind.LISTENING_RECALL -> {
                    primaryAudioBlock?.let(::add)
                    imageBlock?.let(::add)
                }
                LearningExperienceKind.PROMPT_RECALL,
                LearningExperienceKind.TYPING_RECALL -> {
                    if (imageBlock != null) add(imageBlock)
                }
            }
        }
        return result.ifEmpty {
            questionBlocks.filter { block ->
                block is PresentedLearningBlock.Unavailable ||
                    block is PresentedLearningBlock.Text &&
                    block.role == PresentedTextRole.INSTRUCTION
            }
        }
    }
}
