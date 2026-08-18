package vn.loi.learning.desktop.ui.browser

import kotlin.test.*
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class ContentProblemProjectionTest {
    @Test
    fun `healthy content with all media present and canonical POS has no problems`() {
        val candidate = item(
            image = "image.png",
            questionAudio = "q.mp3",
            answerAudio = "a.mp3",
            exampleAudio = "e.mp3",
            translationAudio = "t.mp3",
            pos = "NOUN"
        )
        assertTrue(ContentProblemDetector.detect(candidate, MediaReferenceAvailability { true }).isEmpty())
    }

    @Test
    fun `missing audio detects null, blank, or unresolved media reference and flags missing any audio`() {
        val candidate = item(
            questionAudio = null,
            answerAudio = "  ",
            exampleAudio = "broken.mp3",
            translationAudio = "valid.mp3",
            pos = "VERB"
        )
        val availability = MediaReferenceAvailability { ref -> ref == "valid.mp3" }
        val problems = ContentProblemDetector.detect(candidate, availability)

        assertTrue(ContentProblem.MISSING_QUESTION_AUDIO in problems)
        assertTrue(ContentProblem.MISSING_ANSWER_AUDIO in problems)
        assertTrue(ContentProblem.MISSING_EXAMPLE_AUDIO in problems)
        assertFalse(ContentProblem.MISSING_TRANSLATION_AUDIO in problems)
        assertTrue(ContentProblem.MISSING_ANY_AUDIO in problems)
    }

    @Test
    fun `pos health distinguishes missing pos and unknown custom pos`() {
        val missingPosItem = item(
            questionAudio = "q.mp3", answerAudio = "a.mp3",
            exampleAudio = "e.mp3", translationAudio = "t.mp3",
            pos = ""
        )
        val customPosItem = item(
            questionAudio = "q.mp3", answerAudio = "a.mp3",
            exampleAudio = "e.mp3", translationAudio = "t.mp3",
            pos = "SENTENCE"
        )
        val canonicalNounPhraseItem = item(
            questionAudio = "q.mp3", answerAudio = "a.mp3",
            exampleAudio = "e.mp3", translationAudio = "t.mp3",
            pos = "NOUN PHRASE"
        )

        val missingProblems = ContentProblemDetector.detect(missingPosItem, MediaReferenceAvailability { true })
        assertTrue(ContentProblem.MISSING_POS in missingProblems)
        assertFalse(ContentProblem.UNKNOWN_POS in missingProblems)

        val customProblems = ContentProblemDetector.detect(customPosItem, MediaReferenceAvailability { true })
        assertFalse(ContentProblem.MISSING_POS in customProblems)
        assertTrue(ContentProblem.UNKNOWN_POS in customProblems)

        val canonicalProblems = ContentProblemDetector.detect(canonicalNounPhraseItem, MediaReferenceAvailability { true })
        assertFalse(ContentProblem.MISSING_POS in canonicalProblems)
        assertFalse(ContentProblem.UNKNOWN_POS in canonicalProblems)
    }

    @Test
    fun `only Question and Answer are required text`() {
        val problems = ContentProblemDetector.detect(
            item(
                question = " ",
                answer = "",
                questionAudio = "q.mp3", answerAudio = "a.mp3",
                exampleAudio = "e.mp3", translationAudio = "t.mp3",
                pos = "NOUN"
            ),
            MediaReferenceAvailability { true }
        )
        assertEquals(setOf(ContentProblem.INCOMPLETE_REQUIRED_TEXT), problems)
    }

    @Test
    fun `counts are distinct by Content while category counts include multi-problem item once`() {
        val items = listOf(
            item("a", image = "missing.png", questionAudio = "missing.mp3", answerAudio = "a.mp3", exampleAudio = "e.mp3", translationAudio = "t.mp3", pos = "NOUN"),
            item("b", image = "missing.png", questionAudio = "q.mp3", answerAudio = "a.mp3", exampleAudio = "e.mp3", translationAudio = "t.mp3", pos = "NOUN"),
            item("c", image = null, questionAudio = "q.mp3", answerAudio = "a.mp3", exampleAudio = "e.mp3", translationAudio = "t.mp3", pos = "NOUN")
        )
        val projection = ContentProblemDetector.project(items, MediaReferenceAvailability { ref -> ref != "missing.png" && ref != "missing.mp3" })
        assertEquals(2, projection.problematicContentCount)
        assertEquals(2, projection.count(ContentProblem.MISSING_IMAGE))
        assertEquals(1, projection.count(ContentProblem.MISSING_QUESTION_AUDIO))
    }

    @Test
    fun `problem filter composes after existing search filter and sort`() {
        val alpha = item("alpha", question = "Alpha", image = "missing.png", questionAudio = "q.mp3", answerAudio = "a.mp3", exampleAudio = "e.mp3", translationAudio = "t.mp3", pos = "NOUN", index = 2)
        val beta = item("beta", question = "Beta", image = "missing.png", questionAudio = "q.mp3", answerAudio = "a.mp3", exampleAudio = "e.mp3", translationAudio = "t.mp3", pos = "NOUN", index = 1)
        val projection = ContentProblemDetector.project(listOf(alpha, beta), MediaReferenceAvailability { false })
        val state = state(listOf(alpha, beta), projection).copy(
            appliedQuery = "a",
            sortOption = BrowserSortOption.QUESTION_ASC,
            problemFilter = ContentProblemFilter.MISSING_IMAGE
        )
        assertEquals(listOf("alpha", "beta"), state.filteredItems.map { it.contentId.value })
        assertTrue(state.copy(problemFilter = ContentProblemFilter.INCOMPLETE_REQUIRED_TEXT).filteredItems.isEmpty())
    }

    @Test
    fun `projection resolves each nonblank media reference once and never during repeated filtering`() {
        var resolutions = 0
        val items = (1..2_000).map { index ->
            item(
                "id-$index",
                image = "image-$index.png",
                questionAudio = "q-$index.mp3",
                answerAudio = "a-$index.mp3",
                exampleAudio = "e-$index.mp3",
                translationAudio = "t-$index.mp3",
                pos = "NOUN",
                index = index
            )
        }
        val projection = ContentProblemDetector.project(items, MediaReferenceAvailability { resolutions++; true })
        assertEquals(10_000, resolutions) // 5 media slots per item * 2000
        val state = state(items, projection).copy(problemFilter = ContentProblemFilter.ALL_PROBLEMS)
        repeat(20) { state.filteredItems }
        assertEquals(10_000, resolutions)
    }

    private fun state(items: List<PackageContentBrowserItem>, projection: ContentProblemProjection) =
        PackageContentBrowserUiState(InstalledPackageId("installed"), "Package", items, problemProjection = projection)

    private fun item(
        id: String = "content",
        question: String = "Question",
        answer: String = "Answer",
        image: String? = null,
        questionAudio: String? = null,
        answerAudio: String? = null,
        exampleAudio: String? = null,
        translationAudio: String? = null,
        pos: String = "WORD",
        example: String? = null,
        index: Int = 1
    ) = PackageContentBrowserItem(
        index, ContentId(id), question, answer, "", pos, null, null, "Lesson", "Package",
        image != null, listOf(questionAudio, answerAudio, exampleAudio, translationAudio).any { it != null },
        image, questionAudio, questionAudio, answerAudio, exampleAudio, translationAudio,
        example, null, 0, emptyList(), emptyList(), emptySet(),
        "$question $answer lesson".lowercase()
    )
}
