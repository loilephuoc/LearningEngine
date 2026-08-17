package vn.loi.learning.desktop.ui.browser.imagereuse

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageReuseComparisonAlignmentTest {

    private fun createTarget(
        question: String = "abandon",
        answer: String = "từ bỏ",
        translation: String = "Bỏ rơi, ruồng bỏ",
        exampleText: String = "They abandoned the car.",
        partOfSpeech: String = "verb",
        imageRef: String? = null
    ): ImageReuseTargetItem = ImageReuseTargetItem(
        targetContentId = "target_1",
        targetLesson = "Unit 1",
        question = question,
        answer = answer,
        translation = translation,
        exampleText = exampleText,
        partOfSpeech = partOfSpeech,
        currentImageRef = imageRef,
        candidates = emptyList()
    )

    private fun createCandidate(
        question: String = "abandon",
        answer: String = "bỏ rơi",
        translation: String = "Từ bỏ hoàn toàn một kế hoạch hay người nào đó",
        exampleText: String = "The match was abandoned.",
        partOfSpeech: String = "v",
        imageRef: String = "abandon.jpg"
    ): ImageReuseSourceCandidate = ImageReuseSourceCandidate(
        sourcePackageId = "source_pkg_1",
        sourcePackageName = "Intermediate Vocabulary",
        sourceContentId = "src_1",
        question = question,
        answer = answer,
        translation = translation,
        exampleText = exampleText,
        partOfSpeech = partOfSpeech,
        imageRef = imageRef
    )

    @Test
    fun `createRows generates exactly five rows in canonical sequence with Answer on both sides`() {
        val target = createTarget(
            question = "abandon",
            answer = "từ bỏ (động từ)",
            translation = "bỏ lại phía sau",
            exampleText = "He abandoned his family.",
            partOfSpeech = "v"
        )
        val candidate = createCandidate(
            question = "abandon",
            answer = "ruồng bỏ, rời bỏ",
            translation = "ngừng chăm sóc hoặc hỗ trợ",
            exampleText = "They had to abandon the ship.",
            partOfSpeech = "verb"
        )

        val rows = ImageReuseComparisonProjection.createRows(target, candidate)

        assertEquals(5, rows.size)

        // 1. Question
        assertEquals("Question", rows[0].fieldName)
        assertEquals("abandon", rows[0].targetValue)
        assertEquals("abandon", rows[0].sourceValue)

        // 2. Answer
        assertEquals("Answer", rows[1].fieldName)
        assertEquals("từ bỏ (động từ)", rows[1].targetValue)
        assertEquals("ruồng bỏ, rời bỏ", rows[1].sourceValue)

        // 3. Translation
        assertEquals("Translation", rows[2].fieldName)
        assertEquals("bỏ lại phía sau", rows[2].targetValue)
        assertEquals("ngừng chăm sóc hoặc hỗ trợ", rows[2].sourceValue)

        // 4. Example
        assertEquals("Example", rows[3].fieldName)
        assertEquals("He abandoned his family.", rows[3].targetValue)
        assertEquals("They had to abandon the ship.", rows[3].sourceValue)

        // 5. POS
        assertEquals("POS", rows[4].fieldName)
        assertEquals("v", rows[4].targetValue)
        assertEquals("verb", rows[4].sourceValue)
    }

    @Test
    fun `createRows pairs long text on one side without disturbing semantic field alignment`() {
        val longTranslation = "Đây là một phần giải nghĩa rất dài kéo dài qua nhiều dòng văn bản để kiểm tra khả năng căn chỉnh ngang hàng giữa Target và Source Candidate."
        val target = createTarget(translation = longTranslation)
        val candidate = createCandidate(translation = "Ngắn")

        val rows = ImageReuseComparisonProjection.createRows(target, candidate)

        assertEquals(5, rows.size)
        assertEquals(ImageReuseFieldComparisonRow("Question", "abandon", "abandon"), rows[0])
        assertEquals(ImageReuseFieldComparisonRow("Answer", "từ bỏ", "bỏ rơi"), rows[1])
        assertEquals(ImageReuseFieldComparisonRow("Translation", longTranslation, "Ngắn"), rows[2])
        assertEquals(ImageReuseFieldComparisonRow("Example", "They abandoned the car.", "The match was abandoned."), rows[3])
        assertEquals(ImageReuseFieldComparisonRow("POS", "verb", "v"), rows[4])
    }

    @Test
    fun `createRows preserves blank fields without collapsing rows`() {
        val target = createTarget(exampleText = "", partOfSpeech = "")
        val candidate = createCandidate(answer = "", translation = "")

        val rows = ImageReuseComparisonProjection.createRows(target, candidate)

        assertEquals(5, rows.size)
        assertEquals("", rows[1].sourceValue)
        assertEquals("", rows[2].sourceValue)
        assertEquals("", rows[3].targetValue)
        assertEquals("", rows[4].targetValue)
    }
}
