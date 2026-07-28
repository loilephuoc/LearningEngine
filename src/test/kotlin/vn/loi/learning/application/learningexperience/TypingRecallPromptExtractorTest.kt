package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.learningcontent.LearningTextRole
import vn.loi.learning.domain.content.model.ContentTextFormat

class TypingRecallPromptExtractorTest {
    @Test
    fun `extracts one answer text block`() {
        assertEquals(
            TypingRecallPrompt("answer"),
            TypingRecallPromptExtractor.extract(content(answer = listOf(text("answer"))))
        )
    }

    @Test
    fun `joins multiple answer blocks in semantic order and trims their boundaries`() {
        assertEquals(
            TypingRecallPrompt("first\nsecond"),
            TypingRecallPromptExtractor.extract(
                content(answer = listOf(text(" first "), text("second")))
            )
        )
    }

    @Test
    fun `media-only and unavailable answers have no typing prompt`() {
        assertNull(
            TypingRecallPromptExtractor.extract(
                content(answer = listOf(audio("answer.mp3")))
            )
        )
        assertNull(
            TypingRecallPromptExtractor.extract(
                content(answer = listOf(LearningContentBlock.UnavailableAnswer))
            )
        )
    }

    @Test
    fun `example text is never included in expected answer`() {
        val content = LearningContent(
            question = LearningContentSection(listOf(text("question"))),
            answer = LearningContentSection(listOf(text("answer"))),
            example = LearningContentSection(listOf(text("example answer")))
        )

        assertEquals(
            TypingRecallPrompt("answer"),
            TypingRecallPromptExtractor.extract(content)
        )
    }

    @Test
    fun `markdown source remains semantic source and is not visually interpreted`() {
        val markdown = LearningContentBlock.Text(
            "**answer**",
            ContentTextFormat.MARKDOWN,
            LearningTextRole.VIETNAMESE_MEANING
        )

        assertEquals(
            TypingRecallPrompt("**answer**"),
            TypingRecallPromptExtractor.extract(content(answer = listOf(markdown)))
        )
    }

    private fun content(answer: List<LearningContentBlock>) =
        LearningContent(
            question = LearningContentSection(listOf(text("question"))),
            answer = LearningContentSection(answer)
        )

    private fun text(value: String) =
        LearningContentBlock.Text(
            value,
            ContentTextFormat.PLAIN_TEXT,
            LearningTextRole.VIETNAMESE_MEANING
        )

    private fun audio(reference: String) =
        LearningContentBlock.Audio(
            requireNotNull(LocalLearningAssetReference.from(reference))
        )
}
