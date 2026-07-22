package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentType

class PackageExportContentJsonLearningContentTest {

    @Test
    fun `package dto round trips markdown and local asset references`() {
        val content = Content(
            id = ContentId("package-rich-content"),
            type = ContentType.ARTICLE,
            text = ContentText(
                primaryText = "# Prompt",
                translatedText = "**Answer**",
                primaryFormat = ContentTextFormat.MARKDOWN,
                translatedFormat = ContentTextFormat.MARKDOWN
            ),
            media = ContentMedia(
                primaryAudio = "package/audio/prompt.mp3",
                image = "package/images/prompt.png"
            )
        )

        assertEquals(content, PackageExportContentJson.from(content).toDomain())
    }

    @Test
    fun `legacy package dto defaults every text field to plain text`() {
        val restored = PackageExportContentJson(
            id = "legacy-package-content",
            type = "WORD",
            primaryText = "word"
        ).toDomain()

        assertEquals(ContentTextFormat.PLAIN_TEXT, restored.text.primaryFormat)
        assertEquals(ContentTextFormat.PLAIN_TEXT, restored.text.translatedFormat)
    }
}
