package vn.loi.learning.desktop.ui.study

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
 * The current factory never selects it and no input or evaluation behavior exists yet.
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
 * Deterministic Desktop experience generator. It selects presentation only and never changes
 * learning-session, scheduler, rating, evidence, queue, or persistence state.
 */
class LearningSceneFactory {
    fun generate(
        presentation: LearningContentPresentation,
        workspaceState: ReviewWorkspaceState
    ): LearningScene? {
        val question = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.QUESTION }
            ?: return null
        val answer = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.ANSWER }
        val example = presentation.sections
            .firstOrNull { it.kind == LearningSectionKind.EXAMPLE }
        val context = LearningSceneContext(
            answerRevealed = workspaceState is ReviewWorkspaceState.AnswerRevealed
        )
        val capabilities = SceneCapabilities(
            hasAudio = presentation.hasBlock<PresentedLearningBlock.Audio>(),
            hasImage = presentation.hasBlock<PresentedLearningBlock.Image>(),
            hasMeaning = answer != null,
            hasExamples = example != null
        )
        val supporting = buildList {
            answer?.let {
                add(MeaningScene(context, capabilities, it.blocks))
            }
            example?.let {
                add(ExampleScene(context, capabilities, it.blocks))
            }
        }

        return when {
            question.blocks.any { it is PresentedLearningBlock.Image } ->
                ImageScene(context, capabilities, question.blocks, supporting)

            question.blocks.any { it is PresentedLearningBlock.Audio } ->
                ListeningScene(context, capabilities, question.blocks, supporting)

            else ->
                PromptScene(context, capabilities, question.blocks, supporting)
        }
    }

    private inline fun <reified T : PresentedLearningBlock> LearningContentPresentation.hasBlock() =
        sections.any { section -> section.blocks.any { it is T } }
}
