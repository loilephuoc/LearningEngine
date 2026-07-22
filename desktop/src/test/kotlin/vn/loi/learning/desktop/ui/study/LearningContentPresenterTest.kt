package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.domain.content.model.ContentTextFormat

class LearningContentPresenterTest {
    private val strings = LearningContentRendererStrings(
        "no answer", "no image", "no audio", "image", "audio", "play", "stop", "answer", "example"
    )

    @Test
    fun `question state presents only ordered question blocks`() {
        val image = Files.createTempFile("learning-image", ".png")
        val presenter = LearningContentPresenter(FakeStorage(mapOf("lesson/image.png" to image)), strings)
        val presentation = presenter.present(content(), ReviewWorkspaceState.Question)

        assertEquals(listOf(LearningSectionKind.QUESTION), presentation.sections.map { it.kind })
        assertEquals(2, presentation.sections.single().blocks.size)
        assertIs<PresentedLearningBlock.Text>(presentation.sections.single().blocks[0])
        assertEquals(image, assertIs<PresentedLearningBlock.Image>(presentation.sections.single().blocks[1]).path)
    }

    @Test
    fun `revealed state presents question answer then example`() {
        val presentation = LearningContentPresenter(FakeStorage(), strings)
            .present(content(), ReviewWorkspaceState.AnswerRevealed)

        assertEquals(
            listOf(LearningSectionKind.QUESTION, LearningSectionKind.ANSWER, LearningSectionKind.EXAMPLE),
            presentation.sections.map { it.kind }
        )
        assertEquals("no image", assertIs<PresentedLearningBlock.Unavailable>(presentation.sections[0].blocks[1]).message)
        assertEquals("no audio", assertIs<PresentedLearningBlock.Unavailable>(presentation.sections[1].blocks[1]).message)
    }

    @Test
    fun `unavailable semantic blocks have stable localized fallback`() {
        val content = LearningContent(
            LearningContentSection(listOf(text("question"))),
            LearningContentSection(listOf(LearningContentBlock.UnavailableAnswer)),
            null
        )
        val block = LearningContentPresenter(FakeStorage(), strings)
            .present(content, ReviewWorkspaceState.AnswerRevealed).sections[1].blocks.single()

        assertEquals("no answer", assertIs<PresentedLearningBlock.Unavailable>(block).message)
    }

    @Test
    fun `safe markdown supports structural allowlist and leaves html inert`() {
        val document = SafeMarkdownParser.parse(
            "# Heading\n\nParagraph with **bold** and <script>alert(1)</script>.\n\n1. First\n- Second\n```kotlin\nval x = 1\n```"
        )

        assertIs<SafeMarkdownBlock.Heading>(document.blocks[0])
        assertTrue(assertIs<SafeMarkdownBlock.Paragraph>(document.blocks[1]).text.contains("<script>"))
        assertEquals(true, assertIs<SafeMarkdownBlock.ListItem>(document.blocks[2]).ordered)
        assertEquals(false, assertIs<SafeMarkdownBlock.ListItem>(document.blocks[3]).ordered)
        assertEquals("val x = 1", assertIs<SafeMarkdownBlock.Code>(document.blocks[4]).text)
    }

    @Test
    fun `plain text preserves leading trailing and newline content`() {
        val source = "  first line\nsecond line  "
        val content = LearningContent(
            LearningContentSection(listOf(text(source))),
            LearningContentSection(listOf(text("answer")))
        )

        val block = LearningContentPresenter(FakeStorage(), strings)
            .present(content, ReviewWorkspaceState.Question).sections.single().blocks.single()
        val document = assertIs<PresentedLearningBlock.Text>(block).document

        assertEquals(source, assertIs<SafeMarkdownBlock.Paragraph>(document.blocks.single()).text)
    }

    @Test
    fun `inline markdown removes delimiters without changing content order`() {
        val rendered = inlineMarkdown("a **bold** *italic* `code`")
        assertEquals("a bold italic code", rendered.text)
        assertEquals(3, rendered.spanStyles.size)
    }

    private fun content() = LearningContent(
        question = LearningContentSection(listOf(text("**Question**", ContentTextFormat.MARKDOWN), image("lesson/image.png"))),
        answer = LearningContentSection(listOf(text("Answer"), audio("lesson/audio.wav"))),
        example = LearningContentSection(listOf(text("Example")))
    )

    private fun text(value: String, format: ContentTextFormat = ContentTextFormat.PLAIN_TEXT) =
        LearningContentBlock.Text(value, format)

    private fun image(value: String) = LearningContentBlock.Image(requireNotNull(LocalLearningAssetReference.from(value)))
    private fun audio(value: String) = LearningContentBlock.Audio(requireNotNull(LocalLearningAssetReference.from(value)))

    private class FakeStorage(private val paths: Map<String, Path> = emptyMap()) : ContentMediaStorage {
        override fun resolve(relativePath: String): Path? = paths[relativePath]
        override fun exists(relativePath: String) = relativePath in paths
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset =
            error("not used")
    }
}
