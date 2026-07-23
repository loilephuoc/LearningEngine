package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LearningSceneFactoryTest {
    private val factory = LearningSceneFactory()
    private val questionText =
        PresentedLearningBlock.Text(SafeMarkdownDocument.plain("question"))

    @Test
    fun `plain question becomes prompt scene`() {
        val scene = factory.generate(
            presentation(questionText),
            ReviewWorkspaceState.Question
        )

        assertIs<PromptScene>(scene)
        assertEquals(SceneType.PROMPT, scene.type)
        assertFalse(scene.context.answerRevealed)
        assertFalse(scene.capabilities.hasAudio)
        assertFalse(scene.capabilities.hasImage)
    }

    @Test
    fun `question audio becomes listening scene`() {
        val scene = factory.generate(
            presentation(
                questionText,
                PresentedLearningBlock.Audio(Path.of("prompt.mp3"), "audio", "Pronunciation")
            ),
            ReviewWorkspaceState.Question
        )

        assertIs<ListeningScene>(scene)
        assertTrue(scene.capabilities.hasAudio)
    }

    @Test
    fun `image takes deterministic scene priority while retaining audio capability`() {
        val scene = factory.generate(
            presentation(
                questionText,
                PresentedLearningBlock.Audio(Path.of("prompt.mp3"), "audio", "Pronunciation"),
                PresentedLearningBlock.Image(Path.of("image.png"), "image")
            ),
            ReviewWorkspaceState.Question
        )

        assertIs<ImageScene>(scene)
        assertTrue(scene.capabilities.hasImage)
        assertTrue(scene.capabilities.hasAudio)
    }

    @Test
    fun `reveal adds meaning and example scenes without changing the primary experience`() {
        val presentation = LearningContentPresentation(
            listOf(
                PresentedLearningSection(LearningSectionKind.QUESTION, listOf(questionText)),
                PresentedLearningSection(
                    LearningSectionKind.ANSWER,
                    listOf(PresentedLearningBlock.Text(SafeMarkdownDocument.plain("meaning")))
                ),
                PresentedLearningSection(
                    LearningSectionKind.EXAMPLE,
                    listOf(PresentedLearningBlock.Text(SafeMarkdownDocument.plain("example")))
                )
            )
        )

        val scene = factory.generate(presentation, ReviewWorkspaceState.AnswerRevealed)

        assertIs<PromptScene>(scene)
        assertTrue(scene.context.answerRevealed)
        assertEquals(listOf(SceneType.MEANING, SceneType.EXAMPLE), scene.supportingScenes.map { it.type })
        assertIs<MeaningScene>(scene.supportingScenes[0])
        assertIs<ExampleScene>(scene.supportingScenes[1])
        assertTrue(scene.capabilities.hasMeaning)
        assertTrue(scene.capabilities.hasExamples)
        assertEquals(scene, factory.generate(presentation, ReviewWorkspaceState.AnswerRevealed))
    }

    @Test
    fun `typing scene is an explicit inert placeholder and is never factory selected`() {
        val context = LearningSceneContext(answerRevealed = false)
        val capabilities = SceneCapabilities(
            hasAudio = false,
            hasImage = false,
            hasMeaning = false,
            hasExamples = false
        )

        val placeholder = TypingScene(context, capabilities, listOf(questionText))

        assertEquals(SceneType.TYPING, placeholder.type)
        assertFalse(placeholder.capabilities.acceptsTyping)
        assertFalse(factory.generate(presentation(questionText), ReviewWorkspaceState.Question) is TypingScene)
    }

    @Test
    fun `empty presentation produces no scene`() {
        assertEquals(
            null,
            factory.generate(LearningContentPresentation(emptyList()), ReviewWorkspaceState.Preparing)
        )
    }

    private fun presentation(
        vararg blocks: PresentedLearningBlock
    ) = LearningContentPresentation(
        listOf(PresentedLearningSection(LearningSectionKind.QUESTION, blocks.toList()))
    )
}
