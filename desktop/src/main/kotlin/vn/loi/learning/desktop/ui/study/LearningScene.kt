package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperienceSupportingRole
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallAnswerKind
import vn.loi.learning.domain.study.recall.RecallDirection
import vn.loi.learning.domain.study.recall.RecallMode
import vn.loi.learning.domain.study.recall.RecallPrompt

enum class SceneType {
    PROMPT,
    MEANING,
    LISTENING,
    IMAGE,
    EXAMPLE,
    TYPING,
    MULTIPLE_CHOICE,
    EXAMPLE_COMPLETION,
    UNSUPPORTED_RECALL
}

data class LearningSceneContext(
    val answerRevealed: Boolean,
    val stage: vn.loi.learning.domain.study.memory.model.LearningStage? = null
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
    override val supportingScenes: List<LearningScene> = emptyList(),
    val recallPresentation: ListeningRecallPresentation? = null
) : LearningScene {
    override val type = SceneType.LISTENING
}

data class ImageScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    override val supportingScenes: List<LearningScene> = emptyList(),
    val recallPresentation: ImageRecallPresentation? = null
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

data class MultipleChoiceScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    val presentation: MultipleChoicePresentation,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.MULTIPLE_CHOICE
}

data class ExampleCompletionScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock> = emptyList(),
    val presentation: ExampleCompletionPresentationResult,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.EXAMPLE_COMPLETION
}

data class UnsupportedRecallScene(
    override val context: LearningSceneContext,
    override val capabilities: SceneCapabilities,
    override val blocks: List<PresentedLearningBlock>,
    val mode: vn.loi.learning.domain.study.recall.RecallMode,
    override val supportingScenes: List<LearningScene> = emptyList()
) : LearningScene {
    override val type = SceneType.UNSUPPORTED_RECALL
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
        recallPlan: RecallPlan? = null,
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
        val context = LearningSceneContext(
            answerRevealed = plan.context.answerRevealed,
            stage = plan.context.stage
        )
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

        if (recallPlan != null && recallPlan.mode != vn.loi.learning.domain.study.recall.RecallMode.TYPING) {
            return when (DesktopRecallModeRouter.route(recallPlan)) {
                DesktopRecallRenderer.MULTIPLE_CHOICE -> MultipleChoiceScene(
                    context = context,
                    capabilities = capabilities,
                    blocks = sanitizedQuestionBlocks,
                    presentation = requireNotNull(MultipleChoicePresentationResolver.resolve(recallPlan)) {
                        "Multiple Choice RecallPlan is not renderable."
                    },
                    supportingScenes = projectedSupporting
                )
                DesktopRecallRenderer.LISTENING -> {
                    val listening = requireNotNull(
                        ListeningRecallPresentationResolver.resolve(recallPlan, question.blocks)
                    ) { "Listening RecallPlan is not renderable." }
                    ListeningScene(
                        context = context,
                        capabilities = capabilities.copy(hasAudio = listening.blocks.any { it is PresentedLearningBlock.Audio }),
                        blocks = listening.blocks,
                        recallPresentation = listening,
                        supportingScenes = emptyList()
                    )
                }
                DesktopRecallRenderer.IMAGE_RECALL -> {
                    val image = requireNotNull(
                        ImageRecallPresentationResolver.resolve(
                            recallPlan,
                            question.blocks
                        )
                    ) { "Image RecallPlan is not renderable." }
                    ImageScene(
                        context = context,
                        capabilities = capabilities.copy(hasImage = image.image != null),
                        blocks = listOfNotNull(image.image),
                        supportingScenes = emptyList(),
                        recallPresentation = image
                    )
                }
                DesktopRecallRenderer.EXAMPLE_COMPLETION -> ExampleCompletionScene(
                    context = context,
                    capabilities = capabilities.copy(acceptsTyping = true),
                    presentation = requireNotNull(ExampleCompletionPresentationResolver.resolve(recallPlan)) {
                        "Example Completion RecallPlan is not renderable."
                    }
                )
                DesktopRecallRenderer.UNSUPPORTED -> UnsupportedRecallScene(
                    context = context,
                    capabilities = capabilities,
                    blocks = sanitizedQuestionBlocks,
                    mode = recallPlan.mode,
                    supportingScenes = projectedSupporting
                )
                DesktopRecallRenderer.TYPING -> error("Typing is routed by the selected learning experience.")
            }
        }

        return when (selection.selectedKind) {
            LearningExperienceKind.IMAGE_RECALL ->
                ImageScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.LISTENING_RECALL ->
                ListeningScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.PROMPT_RECALL ->
                PromptScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting)

            LearningExperienceKind.TYPING_RECALL ->
                typingScene(context, capabilities, sanitizedQuestionBlocks, projectedSupporting, presentation, recallPlan)
        }
    }

    private fun typingScene(
        context: LearningSceneContext,
        capabilities: SceneCapabilities,
        blocks: List<PresentedLearningBlock>,
        supportingScenes: List<LearningScene>,
        presentation: LearningContentPresentation,
        recallPlan: RecallPlan?
    ): LearningScene {
        val plan = requireNotNull(recallPlan) { "Typing selection requires a RecallPlan." }
        val prompt = TypingRecallPresentationResolver.resolve(plan, presentation)
            ?: return UnsupportedRecallScene(context, capabilities, blocks, plan.mode, supportingScenes)
        return TypingScene(
                    context = context,
                    capabilities = capabilities.copy(acceptsTyping = true),
                    blocks = blocks,
                    prompt = prompt,
                    supportingScenes = supportingScenes
                )
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

internal object TypingRecallPresentationResolver {
    fun resolve(
        plan: RecallPlan,
        presentation: LearningContentPresentation
    ): TypingRecallPrompt? {
        val recallPrompt = plan.prompt as? RecallPrompt.Typing ?: return null
        if (plan.mode != RecallMode.TYPING || plan.direction != RecallDirection.TARGET_TO_SOURCE ||
            plan.answerContract.kind != RecallAnswerKind.TEXT ||
            !plan.platformRequirements.requiresTextInput) return null
        val blocks = presentation.sections.flatMap(PresentedLearningSection::blocks)
        val source = textForRole(blocks, PresentedTextRole.PRIMARY_ENGLISH)
        val target = textForRole(blocks, PresentedTextRole.VIETNAMESE_MEANING)
        if (source != plan.answerContract.canonicalAnswer || target != recallPrompt.sourceText) return null
        return TypingRecallPrompt(plan.answerContract.canonicalAnswer)
    }

    private fun textForRole(blocks: List<PresentedLearningBlock>, role: PresentedTextRole): String? =
        blocks.filterIsInstance<PresentedLearningBlock.Text>()
            .firstOrNull { it.role == role }
            ?.document?.blocks?.joinToString("\n", transform = SafeMarkdownBlock::text)
}
