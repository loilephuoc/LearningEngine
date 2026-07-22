package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.infrastructure.persistence.record.ContentRecord

class ContentRecordMapperLearningContentTest {

    @Test
    fun `round trips text formats media and learner-facing metadata`() {
        val content = Content(
            id = ContentId("rich-content"),
            type = ContentType.ARTICLE,
            text = ContentText(
                primaryText = "# Question",
                translatedText = "Answer",
                exampleText = "*Example*",
                primaryFormat = ContentTextFormat.MARKDOWN,
                exampleFormat = ContentTextFormat.MARKDOWN
            ),
            media = ContentMedia(
                primaryAudio = "package/audio.mp3",
                image = "package/image.png"
            ),
            metadata = ContentMetadata(
                title = "Rich content",
                tags = setOf("phase-6"),
                source = "test"
            )
        )

        assertEquals(
            content,
            ContentRecordMapper.toDomain(ContentRecordMapper.toRecord(content))
        )
    }

    @Test
    fun `legacy record defaults to plain text and absent media`() {
        val restored = ContentRecordMapper.toDomain(
            ContentRecord(
                id = "legacy-content",
                type = "WORD",
                primaryText = "legacy",
                translatedText = null,
                pronunciation = null,
                exampleText = null,
                exampleTranslation = null
            )
        )

        assertEquals(ContentTextFormat.PLAIN_TEXT, restored.text.primaryFormat)
        assertEquals(ContentMedia(), restored.media)
    }
}
