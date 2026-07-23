package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperienceSupportingRole

class DesktopLearningSceneProjectorTest {
    private val projector = DesktopLearningSceneProjector()
    private val questionText =
        PresentedLearningBlock.Text(SafeMarkdownDocument.plain("question"))

    @Test
    fun `image plan becomes image scene even when resolved block is unavailable`() {
        val unavailable = PresentedLearningBlock.Unavailable("image unavailable")

        val scene = projector.project(
            plan(LearningExperienceKind.IMAGE_RECALL, hasImage = true),
            presentation(unavailable)
        )

        assertIs<ImageScene>(scene)
        assertEquals(listOf(unavailable), scene.blocks)
        assertTrue(scene.capabilities.hasImage)
    }

    @Test
    fun `listening plan becomes listening scene without inspecting Desktop blocks`() {
        val misleadingImage = PresentedLearningBlock.Image(Path.of("image.png"), "image")

        val scene = projector.project(
            plan(LearningExperienceKind.LISTENING_RECALL, hasAudio = true),
            presentation(questionText, misleadingImage)
        )

        assertIs<ListeningScene>(scene)
        assertTrue(scene.capabilities.hasAudio)
    }

    @Test
    fun `prompt plan remains prompt scene when Desktop blocks contain image and audio`() {
        val scene = projector.project(
            plan(LearningExperienceKind.PROMPT_RECALL),
            presentation(
                PresentedLearningBlock.Image(Path.of("image.png"), "image"),
                PresentedLearningBlock.Audio(Path.of("prompt.mp3"), "audio", "Pronunciation")
            )
        )

        assertIs<PromptScene>(scene)
    }

    @Test
    fun `revealed plan projects meaning and example in policy order`() {
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
        val plan = plan(
            kind = LearningExperienceKind.PROMPT_RECALL,
            revealed = true,
            supporting = setOf(
                LearningExperienceSupportingRole.MEANING,
                LearningExperienceSupportingRole.EXAMPLE
            )
        )

        val scene = requireNotNull(projector.project(plan, presentation))

        assertEquals(listOf(SceneType.MEANING, SceneType.EXAMPLE), scene.supportingScenes.map { it.type })
        assertIs<MeaningScene>(scene.supportingScenes[0])
        assertIs<ExampleScene>(scene.supportingScenes[1])
        assertTrue(scene.context.answerRevealed)
    }

    @Test
    fun `projector never activates typing and absent input has no scene`() {
        val context = LearningSceneContext(answerRevealed = false)
        val capabilities = SceneCapabilities(false, false, false, false)
        val placeholder = TypingScene(context, capabilities, listOf(questionText))

        assertEquals(SceneType.TYPING, placeholder.type)
        assertFalse(placeholder.capabilities.acceptsTyping)
        assertNull(projector.project(null, presentation(questionText)))
        assertNull(
            projector.project(
                plan(LearningExperienceKind.PROMPT_RECALL),
                LearningContentPresentation(emptyList())
            )
        )
    }

    private fun plan(
        kind: LearningExperienceKind,
        hasImage: Boolean = false,
        hasAudio: Boolean = false,
        revealed: Boolean = false,
        supporting: Set<LearningExperienceSupportingRole> = emptySet()
    ) = LearningExperiencePlan(
        primaryKind = kind,
        capabilities = LearningExperienceCapabilities(
            hasPromptText = true,
            hasPromptImage = hasImage,
            hasPromptAudio = hasAudio,
            hasMeaning = revealed,
            hasExample = LearningExperienceSupportingRole.EXAMPLE in supporting,
            hasAnswerAudio = false,
            hasExampleAudio = false
        ),
        context = LearningExperienceContext(revealed),
        visibleSupportingRoles = supporting
    )

    private fun presentation(
        vararg blocks: PresentedLearningBlock
    ) = LearningContentPresentation(
        listOf(PresentedLearningSection(LearningSectionKind.QUESTION, blocks.toList()))
    )
}
