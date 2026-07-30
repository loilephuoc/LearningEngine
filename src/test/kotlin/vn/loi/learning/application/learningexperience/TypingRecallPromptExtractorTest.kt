package vn.loi.learning.application.learningexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.learningcontent.LearningTextRole
import vn.loi.learning.domain.content.model.ContentTextFormat

class TypingRecallPromptExtractorTest {
    @Test
    fun `vocabulary uses only canonical primary English`() {
        val learningContent =
            content(
                question = listOf(image("images/socks.png"), text("vớ, tất", LearningTextRole.VIETNAMESE_MEANING)),
                answer =
                    listOf(
                        text("socks", LearningTextRole.PRIMARY_ENGLISH),
                        text("(noun) /sɒks/", LearningTextRole.NEUTRAL),
                        text("vớ, tất", LearningTextRole.VIETNAMESE_MEANING)
                    )
            )

        assertEquals(
            TypingRecallPrompt("socks"),
            TypingRecallPromptExtractor.extract(learningContent)
        )
    }

    @Test
    fun `conversation preserves canonical English sentence`() {
        val expected = "Where is the nearest hospital?"
        val learningContent =
            content(
                question = listOf(text("Bệnh viện gần nhất ở đâu?", LearningTextRole.VIETNAMESE_MEANING)),
                answer =
                    listOf(
                        text(expected, LearningTextRole.PRIMARY_ENGLISH),
                        text("Bệnh viện gần nhất ở đâu?", LearningTextRole.VIETNAMESE_MEANING)
                    )
            )

        assertEquals(expected, TypingRecallPromptExtractor.extract(learningContent)?.expectedAnswer)
    }

    @Test
    fun `Vietnamese meaning without primary English has no prompt`() {
        assertNull(
            TypingRecallPromptExtractor.extract(
                content(
                    answer =
                        listOf(
                            text("vớ, tất", LearningTextRole.VIETNAMESE_MEANING)
                        )
                )
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
    fun `examples and example section never become canonical answer`() {
        val learningContent =
            LearningContent(
                question = LearningContentSection(listOf(text("take off", LearningTextRole.PRIMARY_ENGLISH))),
                answer = LearningContentSection(listOf(text("cất cánh", LearningTextRole.VIETNAMESE_MEANING))),
                example =
                    LearningContentSection(
                        listOf(
                            text("The plane will take off.", LearningTextRole.ENGLISH_EXAMPLE),
                            text("wrong fallback", LearningTextRole.PRIMARY_ENGLISH)
                        )
                    )
            )

        assertEquals("take off", TypingRecallPromptExtractor.extract(learningContent)?.expectedAnswer)
    }

    @Test
    fun `boundary whitespace is trimmed while internal spaces and punctuation are preserved`() {
        val expected = "Where   is the nearest hospital?"

        assertEquals(
            expected,
            TypingRecallPromptExtractor.extract(
                content(answer = listOf(text("  $expected  ", LearningTextRole.PRIMARY_ENGLISH)))
            )?.expectedAnswer
        )
    }

    @Test
    fun `apostrophe hyphen and multi-word canonical text are preserved`() {
        val expected = "Don't take-off yet"

        assertEquals(
            expected,
            TypingRecallPromptExtractor.extract(
                content(answer = listOf(text(expected, LearningTextRole.PRIMARY_ENGLISH)))
            )?.expectedAnswer
        )
    }

    @Test
    fun `first primary English block wins deterministically without joining`() {
        val learningContent =
            content(
                question = listOf(text("first", LearningTextRole.PRIMARY_ENGLISH)),
                answer = listOf(text("second", LearningTextRole.PRIMARY_ENGLISH))
            )

        assertEquals("first", TypingRecallPromptExtractor.extract(learningContent)?.expectedAnswer)
    }

    @Test
    fun `real socks content drives exact prefix and localized mismatch evaluation`() {
        val learningContent =
            content(
                question =
                    listOf(
                        image("images/socks.png"),
                        text("vớ, tất", LearningTextRole.VIETNAMESE_MEANING)
                    ),
                answer =
                    listOf(
                        text("socks", LearningTextRole.PRIMARY_ENGLISH),
                        text("(noun) /sɒks/", LearningTextRole.NEUTRAL),
                        text("vớ, tất", LearningTextRole.VIETNAMESE_MEANING)
                    )
            )
        val prompt = requireNotNull(TypingRecallPromptExtractor.extract(learningContent))
        val evaluator = TypingAnswerEvaluator()

        assertEquals("socks", prompt.expectedAnswer)
        assertEquals(
            TypingAnswerEvaluationStatus.CORRECT,
            evaluator.evaluate(prompt, "socks").status
        )
        val prefix = evaluator.evaluate(prompt, "sock")
        assertEquals(4, prefix.correctPrefixLength)
        assertTrue(
            prefix.differences.none {
                it.kind == TypingDifferenceKind.REPLACEMENT ||
                    it.kind == TypingDifferenceKind.INSERTION
            }
        )
        val mismatch = evaluator.evaluate(prompt, "soaks")
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, mismatch.status)
        assertEquals(TypingDifferenceKind.REPLACEMENT, mismatch.differences[2].kind)
        assertEquals("a", mismatch.differences[2].typedText)
        assertEquals("c", mismatch.differences[2].expectedText)
    }

    @Test
    fun `blank learning text is rejected by the content model before prompt extraction`() {
        val failure =
            runCatching {
                text("   ", LearningTextRole.PRIMARY_ENGLISH)
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun content(
        question: List<LearningContentBlock> =
            listOf(text("question", LearningTextRole.VIETNAMESE_MEANING)),
        answer: List<LearningContentBlock>
    ) =
        LearningContent(
            question = LearningContentSection(question),
            answer = LearningContentSection(answer)
        )

    private fun text(
        value: String,
        role: LearningTextRole
    ) =
        LearningContentBlock.Text(
            value,
            ContentTextFormat.PLAIN_TEXT,
            role
        )

    private fun image(reference: String) =
        LearningContentBlock.Image(
            requireNotNull(LocalLearningAssetReference.from(reference))
        )

    private fun audio(reference: String) =
        LearningContentBlock.Audio(
            requireNotNull(LocalLearningAssetReference.from(reference))
        )
}
