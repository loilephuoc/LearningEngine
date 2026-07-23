package vn.loi.learning.application.learningexperience

import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock

enum class LearningExperienceKind {
    PROMPT_RECALL,
    LISTENING_RECALL,
    IMAGE_RECALL
}

enum class LearningExperienceSupportingRole {
    MEANING,
    EXAMPLE
}

data class LearningExperienceCapabilities(
    val hasPromptText: Boolean,
    val hasPromptImage: Boolean,
    val hasPromptAudio: Boolean,
    val hasMeaning: Boolean,
    val hasExample: Boolean,
    val hasAnswerAudio: Boolean,
    val hasExampleAudio: Boolean
)

data class LearningExperienceContext(
    val answerRevealed: Boolean
)

data class LearningExperiencePlan(
    val primaryKind: LearningExperienceKind,
    val capabilities: LearningExperienceCapabilities,
    val context: LearningExperienceContext,
    val visibleSupportingRoles: Set<LearningExperienceSupportingRole>
)

/**
 * Platform-independent policy for choosing how stable learning content should be experienced.
 * It is deterministic and observational: it performs no scheduling, mutation, I/O, or playback.
 */
class LearningExperiencePolicy {
    fun plan(
        content: LearningContent?,
        context: LearningExperienceContext
    ): LearningExperiencePlan? {
        content ?: return null
        val capabilities = capabilities(content)
        val primaryKind = when {
            capabilities.hasPromptImage -> LearningExperienceKind.IMAGE_RECALL
            capabilities.hasPromptAudio -> LearningExperienceKind.LISTENING_RECALL
            else -> LearningExperienceKind.PROMPT_RECALL
        }
        val supportingRoles = if (context.answerRevealed) {
            buildSet {
                add(LearningExperienceSupportingRole.MEANING)
                if (content.example != null) {
                    add(LearningExperienceSupportingRole.EXAMPLE)
                }
            }
        } else {
            emptySet()
        }
        return LearningExperiencePlan(
            primaryKind = primaryKind,
            capabilities = capabilities,
            context = context,
            visibleSupportingRoles = supportingRoles
        )
    }

    private fun capabilities(content: LearningContent) =
        LearningExperienceCapabilities(
            hasPromptText = content.question.has<LearningContentBlock.Text>(),
            hasPromptImage = content.question.has<LearningContentBlock.Image>(),
            hasPromptAudio = content.question.has<LearningContentBlock.Audio>(),
            hasMeaning = content.answer.has<LearningContentBlock.Text>(),
            hasExample = content.example != null,
            hasAnswerAudio = content.answer.has<LearningContentBlock.Audio>(),
            hasExampleAudio = content.example?.has<LearningContentBlock.Audio>() == true
        )

    private inline fun <reified T : LearningContentBlock>
        vn.loi.learning.application.learningcontent.LearningContentSection.has(): Boolean =
        blocks.any { it is T }
}
