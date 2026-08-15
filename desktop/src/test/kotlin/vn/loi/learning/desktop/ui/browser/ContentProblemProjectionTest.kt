package vn.loi.learning.desktop.ui.browser

import kotlin.test.*
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class ContentProblemProjectionTest {
    @Test
    fun `healthy content and absent optional media have no problems`() {
        assertTrue(ContentProblemDetector.detect(item(), MediaReferenceAvailability { true }).isEmpty())
    }

    @Test
    fun `nonblank unresolved media maps to its exact canonical slot`() {
        val candidate = item(
            image = "image.png", questionAudio = "question.mp3", answerAudio = "answer.mp3",
            exampleAudio = "example.mp3", translationAudio = "translation.mp3"
        )
        assertEquals(ContentProblem.entries.toSet() - ContentProblem.INCOMPLETE_REQUIRED_TEXT,
            ContentProblemDetector.detect(candidate, MediaReferenceAvailability { false }))
    }

    @Test
    fun `only Question and Answer are required text`() {
        val problems = ContentProblemDetector.detect(
            item(question = " ", answer = "", example = null), MediaReferenceAvailability { true }
        )
        assertEquals(setOf(ContentProblem.INCOMPLETE_REQUIRED_TEXT), problems)
    }

    @Test
    fun `counts are distinct by Content while category counts include multi-problem item once`() {
        val items = listOf(
            item("a", image = "missing.png", questionAudio = "missing.mp3"),
            item("b", image = "missing.png"),
            item("c")
        )
        val projection = ContentProblemDetector.project(items, MediaReferenceAvailability { false })
        assertEquals(2, projection.problematicContentCount)
        assertEquals(2, projection.count(ContentProblem.MISSING_IMAGE))
        assertEquals(1, projection.count(ContentProblem.MISSING_QUESTION_AUDIO))
    }

    @Test
    fun `problem filter composes after existing search filter and sort`() {
        val alpha = item("alpha", question = "Alpha", image = "missing.png", index = 2)
        val beta = item("beta", question = "Beta", image = "missing.png", index = 1)
        val projection = ContentProblemDetector.project(listOf(alpha, beta), MediaReferenceAvailability { false })
        val state = state(listOf(alpha, beta), projection).copy(
            appliedQuery = "a",
            sortOption = BrowserSortOption.QUESTION_ASC,
            problemFilter = ContentProblemFilter.MISSING_IMAGE
        )
        assertEquals(listOf("alpha", "beta"), state.filteredItems.map { it.contentId.value })
        assertTrue(state.copy(problemFilter = ContentProblemFilter.MISSING_ANSWER_AUDIO).filteredItems.isEmpty())
    }

    @Test
    fun `projection resolves each nonblank media reference once and never during repeated filtering`() {
        var resolutions = 0
        val items = (1..2_000).map { index -> item("id-$index", image = "image-$index.png", index = index) }
        val projection = ContentProblemDetector.project(items, MediaReferenceAvailability { resolutions++; true })
        assertEquals(2_000, resolutions)
        val state = state(items, projection).copy(problemFilter = ContentProblemFilter.ALL_PROBLEMS)
        repeat(20) { state.filteredItems }
        assertEquals(2_000, resolutions)
    }

    private fun state(items: List<PackageContentBrowserItem>, projection: ContentProblemProjection) =
        PackageContentBrowserUiState(InstalledPackageId("installed"), "Package", items, problemProjection = projection)

    private fun item(
        id: String = "content", question: String = "Question", answer: String = "Answer",
        image: String? = null, questionAudio: String? = null, answerAudio: String? = null,
        exampleAudio: String? = null, translationAudio: String? = null,
        example: String? = null, index: Int = 1
    ) = PackageContentBrowserItem(
        index, ContentId(id), question, answer, "", "WORD", null, null, "Lesson", "Package",
        image != null, listOf(questionAudio, answerAudio, exampleAudio, translationAudio).any { it != null },
        image, questionAudio, questionAudio, answerAudio, exampleAudio, translationAudio,
        example, null, 0, emptyList(), emptyList(), emptySet(),
        "$question $answer lesson".lowercase()
    )
}
