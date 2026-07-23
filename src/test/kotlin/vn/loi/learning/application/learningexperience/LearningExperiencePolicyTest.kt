package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningcontent.LearningAssetKind
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.domain.content.model.ContentTextFormat

class LearningExperiencePolicyTest {
    private val policy = LearningExperiencePolicy()
    private val question = text("question")
    private val meaning = text("meaning")

    @Test
    fun `image audio text produce canonical ordered options`() {
        val plan = requireNotNull(
            policy.plan(
                content(question, audio("prompt.mp3"), image("prompt.png")),
                hidden()
            )
        )

        assertEquals(
            listOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            ),
            plan.options.orderedKinds
        )
    }

    @Test
    fun `image without audio produces image then prompt`() {
        val plan = requireNotNull(
            policy.plan(content(question, image("prompt.png")), hidden())
        )

        assertEquals(
            listOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            ),
            plan.options.orderedKinds
        )
    }

    @Test
    fun `audio without image produces listening then prompt`() {
        val plan = requireNotNull(
            policy.plan(content(question, audio("prompt.mp3")), hidden())
        )

        assertEquals(
            listOf(
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            ),
            plan.options.orderedKinds
        )
    }

    @Test
    fun `text-only and empty options use prompt fallback`() {
        val plan = requireNotNull(policy.plan(content(question), hidden()))

        assertEquals(
            listOf(LearningExperienceKind.PROMPT_RECALL),
            plan.options.orderedKinds
        )
        assertEquals(
            listOf(LearningExperienceKind.PROMPT_RECALL),
            LearningExperienceOptions.from(emptyList()).orderedKinds
        )
    }

    @Test
    fun `capabilities describe semantic content without resolving media`() {
        val content = LearningContent(
            question = LearningContentSection(
                listOf(question, image("declared.png"), audio("prompt.mp3"))
            ),
            answer = LearningContentSection(listOf(meaning, audio("answer.mp3"))),
            example = LearningContentSection(listOf(text("example"), audio("example.mp3")))
        )

        val capabilities = requireNotNull(policy.plan(content, hidden())).capabilities

        assertEquals(
            LearningExperienceCapabilities(
                hasPromptText = true,
                hasPromptImage = true,
                hasPromptAudio = true,
                hasMeaning = true,
                hasExample = true,
                hasAnswerAudio = true,
                hasExampleAudio = true
            ),
            capabilities
        )
    }

    @Test
    fun `reveal changes supporting roles without changing options`() {
        val content = LearningContent(
            LearningContentSection(listOf(question, image("prompt.png"), audio("prompt.mp3"))),
            LearningContentSection(listOf(meaning)),
            LearningContentSection(listOf(text("example")))
        )

        val hidden = requireNotNull(policy.plan(content, hidden()))
        val revealed = requireNotNull(
            policy.plan(content, LearningExperienceContext(answerRevealed = true))
        )

        assertEquals(hidden.options, revealed.options)
        assertTrue(hidden.visibleSupportingRoles.isEmpty())
        assertEquals(
            setOf(
                LearningExperienceSupportingRole.MEANING,
                LearningExperienceSupportingRole.EXAMPLE
            ),
            revealed.visibleSupportingRoles
        )
    }

    @Test
    fun `option generation is deterministic and null content is safely absent`() {
        val content = content(question, audio("prompt.mp3"))

        assertEquals(policy.plan(content, hidden()), policy.plan(content, hidden()))
        assertNull(policy.plan(null, hidden()))
    }

    @Test
    fun `unsafe unavailable media is not eligible for media recall`() {
        val content = LearningContent(
            LearningContentSection(
                listOf(
                    question,
                    LearningContentBlock.UnavailableAsset(
                        LearningAssetKind.IMAGE,
                        "../unsafe.png"
                    )
                )
            ),
            LearningContentSection(listOf(meaning))
        )

        val plan = requireNotNull(policy.plan(content, hidden()))

        assertEquals(
            listOf(LearningExperienceKind.PROMPT_RECALL),
            plan.options.orderedKinds
        )
        assertFalse(plan.capabilities.hasPromptImage)
    }

    private fun hidden() = LearningExperienceContext(answerRevealed = false)

    private fun content(vararg questionBlocks: LearningContentBlock) =
        LearningContent(
            LearningContentSection(questionBlocks.toList()),
            LearningContentSection(listOf(meaning))
        )

    private fun text(value: String) =
        LearningContentBlock.Text(value, ContentTextFormat.PLAIN_TEXT)

    private fun image(value: String) =
        LearningContentBlock.Image(
            requireNotNull(LocalLearningAssetReference.from(value))
        )

    private fun audio(value: String) =
        LearningContentBlock.Audio(
            requireNotNull(LocalLearningAssetReference.from(value))
        )
}
