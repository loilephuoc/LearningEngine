package vn.loi.learning.application.learningcontent

import vn.loi.learning.domain.content.model.ContentTextFormat

/** Ordered, renderer-neutral content presented during one learning item. */
data class LearningContent(
    val question: LearningContentSection,
    val answer: LearningContentSection,
    val example: LearningContentSection? = null
) {
    fun markMissingAssets(
        exists: (LocalLearningAssetReference) -> Boolean
    ): LearningContent =
        copy(
            question = question.markMissingAssets(exists),
            answer = answer.markMissingAssets(exists),
            example = example?.markMissingAssets(exists)
        )
}

data class LearningContentSection(
    val blocks: List<LearningContentBlock>
) {
    init {
        require(blocks.isNotEmpty()) {
            "A learning content section must contain at least one block."
        }
    }

    val textBlocks: List<LearningContentBlock.Text>
        get() = blocks.filterIsInstance<LearningContentBlock.Text>()

    private fun unavailable(
        kind: LearningAssetKind,
        reference: LocalLearningAssetReference
    ) = LearningContentBlock.UnavailableAsset(kind, reference.value)

    fun markMissingAssets(
        exists: (LocalLearningAssetReference) -> Boolean
    ): LearningContentSection =
        copy(
            blocks = blocks.map { block ->
                when {
                    block is LearningContentBlock.Image && !exists(block.reference) ->
                        unavailable(LearningAssetKind.IMAGE, block.reference)
                    block is LearningContentBlock.Audio && !exists(block.reference) ->
                        unavailable(LearningAssetKind.AUDIO, block.reference)
                    else -> block
                }
            }
        )
}

sealed interface LearningContentBlock {

    data class Text(
        val value: String,
        val format: ContentTextFormat
    ) : LearningContentBlock {
        init {
            require(value.isNotBlank()) {
                "Learning text must not be blank."
            }
        }
    }

    data class Image(
        val reference: LocalLearningAssetReference
    ) : LearningContentBlock

    data class Audio(
        val reference: LocalLearningAssetReference,
        val role: LearningAudioRole = LearningAudioRole.OTHER
    ) : LearningContentBlock

    data object UnavailableAnswer : LearningContentBlock

    /** Stable fallback when an imported asset reference is unsafe or unsupported. */
    data class UnavailableAsset(
        val kind: LearningAssetKind,
        val originalReference: String
    ) : LearningContentBlock
}

enum class LearningAssetKind {
    IMAGE,
    AUDIO
}

enum class LearningAudioRole {
    PRIMARY_WORD,
    MEANING_TRANSLATION,
    EXAMPLE_PRIMARY,
    EXAMPLE_TRANSLATION,
    OTHER
}

@JvmInline
value class LocalLearningAssetReference private constructor(
    val value: String
) {
    companion object {
        fun from(reference: String): LocalLearningAssetReference? {
            val normalized = reference.trim().replace('\\', '/')
            if (
                normalized.isEmpty() ||
                normalized.startsWith('/') ||
                URI_SCHEME.matches(normalized) ||
                normalized.split('/').any { segment ->
                    segment.isEmpty() || segment == "." || segment == ".."
                } ||
                "://" in normalized
            ) {
                return null
            }
            return LocalLearningAssetReference(normalized)
        }

        private val URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:.*")
    }
}
