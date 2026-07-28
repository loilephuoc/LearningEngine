package vn.loi.learning.application.learningcontent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentType

class LearningContentProjectorTest {

    @Test
    fun `projects text media and examples in deterministic order`() {
        val projected = LearningContentProjector.project(
            content(
                text = ContentText(
                    primaryText = "**Question**",
                    translatedText = "Answer",
                    pronunciation = "/answer/",
                    exampleText = "Example",
                    exampleTranslation = "Example answer",
                    primaryFormat = ContentTextFormat.MARKDOWN
                ),
                media = ContentMedia(
                    primaryAudio = "lesson/question.mp3",
                    translatedAudio = "lesson/answer.mp3",
                    image = "lesson/picture.png",
                    exampleAudio = "lesson/example.mp3",
                    exampleTranslatedAudio = "lesson/example-vi.mp3"
                )
            )
        )

        assertEquals(
            listOf(
                LearningContentBlock.Text(
                    "**Question**",
                    ContentTextFormat.MARKDOWN,
                    LearningTextRole.PRIMARY_ENGLISH
                ),
                LearningContentBlock.Image(reference("lesson/picture.png")),
                LearningContentBlock.Audio(reference("lesson/question.mp3"), LearningAudioRole.PRIMARY_WORD)
            ),
            projected.question.blocks
        )
        assertEquals(
            listOf(
                LearningContentBlock.Text(
                    "/answer/",
                    ContentTextFormat.PLAIN_TEXT,
                    LearningTextRole.NEUTRAL
                ),
                LearningContentBlock.Text(
                    "Answer",
                    ContentTextFormat.PLAIN_TEXT,
                    LearningTextRole.VIETNAMESE_MEANING
                ),
                LearningContentBlock.Audio(reference("lesson/answer.mp3"), LearningAudioRole.MEANING_TRANSLATION)
            ),
            projected.answer.blocks
        )
        assertEquals(4, projected.example?.blocks?.size)
        assertEquals(
            listOf(
                LearningTextRole.ENGLISH_EXAMPLE,
                LearningTextRole.VIETNAMESE_EXAMPLE
            ),
            projected.example?.blocks?.filterIsInstance<LearningContentBlock.Text>()?.map { it.role }
        )
        assertEquals(
            listOf(LearningAudioRole.EXAMPLE_PRIMARY, LearningAudioRole.EXAMPLE_TRANSLATION),
            projected.example?.blocks?.filterIsInstance<LearningContentBlock.Audio>()?.map { it.role }
        )
    }

    @Test
    fun `optional answer and example have stable empty semantics`() {
        val projected = LearningContentProjector.project(
            content(text = ContentText(primaryText = "Question"))
        )

        assertEquals(
            listOf(LearningContentBlock.UnavailableAnswer),
            projected.answer.blocks
        )
        assertNull(projected.example)
    }

    @Test
    fun `plain text preserves source newlines without renderer normalization`() {
        val value = "Line one\nLine two"
        val projected = LearningContentProjector.project(
            content(text = ContentText(primaryText = value))
        )

        assertEquals(value, projected.question.textBlocks.single().value)
        assertEquals(
            ContentTextFormat.PLAIN_TEXT,
            projected.question.textBlocks.single().format
        )
    }

    @Test
    fun `unsafe and missing local assets become deterministic fallback blocks`() {
        val projected = LearningContentProjector.project(
            content(
                text = ContentText(primaryText = "Question"),
                media = ContentMedia(
                    image = "https://example.invalid/image.png",
                    primaryAudio = "lesson/missing.mp3"
                )
            )
        ).markMissingAssets { false }

        val image = projected.question.blocks[1]
        val audio = projected.question.blocks[2]
        assertIs<LearningContentBlock.UnavailableAsset>(image)
        assertEquals(LearningAssetKind.IMAGE, image.kind)
        assertIs<LearningContentBlock.UnavailableAsset>(audio)
        assertEquals(LearningAssetKind.AUDIO, audio.kind)
    }

    private fun content(
        text: ContentText,
        media: ContentMedia = ContentMedia()
    ) = Content(
        id = ContentId("content-1"),
        type = ContentType.WORD,
        text = text,
        media = media
    )

    private fun reference(value: String) =
        requireNotNull(LocalLearningAssetReference.from(value))
}
