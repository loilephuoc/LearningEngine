package vn.loi.learning.application.learningcontent

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentTextFormat

/** Maps canonical Content into the stable learner-facing block contract. */
object LearningContentProjector {

    fun project(content: Content): LearningContent {
        val question = buildList {
            addText(content.text.primaryText, content.text.primaryFormat)
            addAsset(content.media.image, LearningAssetKind.IMAGE)
            addAsset(content.media.primaryAudio, LearningAssetKind.AUDIO)
        }
        val answer = buildList {
            addText(content.text.pronunciation, ContentTextFormat.PLAIN_TEXT)
            addText(content.text.translatedText, content.text.translatedFormat)
            addAsset(content.media.translatedAudio, LearningAssetKind.AUDIO)
        }.ifEmpty {
            listOf(LearningContentBlock.UnavailableAnswer)
        }
        val example = buildList {
            addText(content.text.exampleText, content.text.exampleFormat)
            addAsset(content.media.exampleAudio, LearningAssetKind.AUDIO)
            addText(
                content.text.exampleTranslation,
                content.text.exampleTranslationFormat
            )
            addAsset(content.media.exampleTranslatedAudio, LearningAssetKind.AUDIO)
        }.takeIf(List<LearningContentBlock>::isNotEmpty)

        return LearningContent(
            question = LearningContentSection(question),
            answer = LearningContentSection(answer),
            example = example?.let(::LearningContentSection)
        )
    }

    private fun MutableList<LearningContentBlock>.addText(
        value: String?,
        format: ContentTextFormat
    ) {
        value?.takeIf(String::isNotBlank)?.let { text ->
            add(LearningContentBlock.Text(text, format))
        }
    }

    private fun MutableList<LearningContentBlock>.addAsset(
        reference: String?,
        kind: LearningAssetKind
    ) {
        reference ?: return
        val localReference = LocalLearningAssetReference.from(reference)
        add(
            when {
                localReference == null ->
                    LearningContentBlock.UnavailableAsset(kind, reference)
                kind == LearningAssetKind.IMAGE ->
                    LearningContentBlock.Image(localReference)
                else -> LearningContentBlock.Audio(localReference)
            }
        )
    }
}
