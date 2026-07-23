package vn.loi.learning.application.learningexperience

import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock

enum class LearningExperienceKind {
    PROMPT_RECALL,
    LISTENING_RECALL,
    IMAGE_RECALL,
    TYPING_RECALL
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

class LearningExperienceOptions private constructor(
    val orderedKinds: List<LearningExperienceKind>
) {
    init {
        require(orderedKinds.isNotEmpty()) {
            "At least one learning experience must be available."
        }
        require(orderedKinds.distinct().size == orderedKinds.size) {
            "Available learning experiences must be unique."
        }
    }

    override fun equals(other: Any?) =
        other is LearningExperienceOptions && orderedKinds == other.orderedKinds

    override fun hashCode() = orderedKinds.hashCode()

    override fun toString() = "LearningExperienceOptions($orderedKinds)"

    companion object {
        fun from(orderedKinds: Iterable<LearningExperienceKind>): LearningExperienceOptions {
            val normalized = orderedKinds.toList().distinct()
            return LearningExperienceOptions(
                java.util.Collections.unmodifiableList(
                    normalized.ifEmpty { listOf(LearningExperienceKind.PROMPT_RECALL) }
                )
            )
        }
    }
}

enum class ExperienceSelectionProfile {
    AUTOMATIC,
    USER_SELECTABLE;

    fun project(options: LearningExperienceOptions): LearningExperienceOptions =
        when (this) {
            AUTOMATIC ->
                LearningExperienceOptions.from(
                    options.orderedKinds.filter { it in AUTOMATIC_KINDS }
                )

            USER_SELECTABLE -> options
        }

    private companion object {
        val AUTOMATIC_KINDS =
            setOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.PROMPT_RECALL
            )
    }
}

data class LearningExperiencePlan(
    val options: LearningExperienceOptions,
    val capabilities: LearningExperienceCapabilities,
    val context: LearningExperienceContext,
    val visibleSupportingRoles: Set<LearningExperienceSupportingRole>,
    val typingPrompt: TypingRecallPrompt? = null
)

/**
 * Platform-independent policy for planning eligible experiences over stable learning content.
 * It is deterministic and observational: it performs no scheduling, mutation, I/O, or playback.
 */
class LearningExperiencePolicy {
    fun plan(
        content: LearningContent?,
        context: LearningExperienceContext
    ): LearningExperiencePlan? {
        content ?: return null
        val capabilities = capabilities(content)
        val typingPrompt = TypingRecallPromptExtractor.extract(content)
        val options = LearningExperienceOptions.from(
            buildList {
                if (capabilities.hasPromptImage) {
                    add(LearningExperienceKind.IMAGE_RECALL)
                }
                if (capabilities.hasPromptAudio) {
                    add(LearningExperienceKind.LISTENING_RECALL)
                }
                add(LearningExperienceKind.PROMPT_RECALL)
                if (typingPrompt != null) {
                    add(LearningExperienceKind.TYPING_RECALL)
                }
            }
        )
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
            options = options,
            capabilities = capabilities,
            context = context,
            visibleSupportingRoles = supportingRoles,
            typingPrompt = typingPrompt
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
