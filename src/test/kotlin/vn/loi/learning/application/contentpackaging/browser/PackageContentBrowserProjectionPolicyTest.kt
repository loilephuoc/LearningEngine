package vn.loi.learning.application.contentpackaging.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class PackageContentBrowserProjectionPolicyTest {

    private val sampleItems = listOf(
        PackageContentBrowserItem(
            index = 1,
            contentId = ContentId("cnt-1"),
            questionText = "Apple",
            answerText = "Quả táo",
            pronunciation = "ˈæp.əl",
            partOfSpeech = "noun",
            group = "Food",
            section = "Fruit",
            lesson = "Lesson 1",
            packageName = "Test Package",
            hasImage = true,
            hasAudio = true,
            imageRef = "apple.png",
            audioRef = "apple.mp3",
            exampleText = "An apple a day",
            exampleTranslation = "Một quả táo mỗi ngày",
            learningItemCount = 1,
            learningItemIds = listOf(LearningItemId("item-1")),
            learningModes = listOf(LearningMode.MEANING_RECOGNITION),
            tags = setOf("fruit", "food"),
            searchableText = PackageContentBrowserProjectionPolicy.buildSearchableText(
                "Apple", "Quả táo", "ˈæp.əl", "noun", "Lesson 1", "Food", "Fruit", setOf("fruit", "food")
            )
        ),
        PackageContentBrowserItem(
            index = 2,
            contentId = ContentId("cnt-2"),
            questionText = "Banana",
            answerText = "Quả chuối",
            pronunciation = "bəˈnɑː.nə",
            partOfSpeech = "noun",
            group = "Food",
            section = "Fruit",
            lesson = "Lesson 2",
            packageName = "Test Package",
            hasImage = false,
            hasAudio = true,
            imageRef = null,
            audioRef = "banana.mp3",
            exampleText = "Monkeys eat bananas",
            exampleTranslation = "Khỉ ăn chuối",
            learningItemCount = 1,
            learningItemIds = listOf(LearningItemId("item-2")),
            learningModes = listOf(LearningMode.MEANING_RECOGNITION),
            tags = setOf("fruit"),
            searchableText = PackageContentBrowserProjectionPolicy.buildSearchableText(
                "Banana", "Quả chuối", "bəˈnɑː.nə", "noun", "Lesson 2", "Food", "Fruit", setOf("fruit")
            )
        ),
        PackageContentBrowserItem(
            index = 3,
            contentId = ContentId("cnt-3"),
            questionText = "Cat",
            answerText = "Con mèo",
            pronunciation = "kæt",
            partOfSpeech = "animal",
            group = "Fauna",
            section = "Pets",
            lesson = "Lesson 1",
            packageName = "Test Package",
            hasImage = true,
            hasAudio = false,
            imageRef = "cat.png",
            audioRef = null,
            exampleText = "The cat sleeps",
            exampleTranslation = "Con mèo đang ngủ",
            learningItemCount = 1,
            learningItemIds = listOf(LearningItemId("item-3")),
            learningModes = listOf(LearningMode.MEANING_RECOGNITION),
            tags = setOf("animal"),
            searchableText = PackageContentBrowserProjectionPolicy.buildSearchableText(
                "Cat", "Con mèo", "kæt", "animal", "Lesson 1", "Fauna", "Pets", setOf("animal")
            )
        )
    )

    @Test
    fun `search by question text returns matching rows`() {
        val result = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "apple",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, result.size)
        assertEquals("Apple", result.first().questionText)
    }

    @Test
    fun `search by answer text returns matching rows`() {
        val result = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "con mèo",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, result.size)
        assertEquals("Cat", result.first().questionText)
    }

    @Test
    fun `search by IPA or POS returns matching rows`() {
        val resultIpa = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "bəˈnɑː.nə",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, resultIpa.size)
        assertEquals("Banana", resultIpa.first().questionText)

        val resultPos = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "animal",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, resultPos.size)
        assertEquals("Cat", resultPos.first().questionText)
    }

    @Test
    fun `filter by lesson returns matching rows`() {
        val result = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "Lesson 1",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(2, result.size)
        assertEquals(listOf("Apple", "Cat"), result.map { it.questionText })
    }

    @Test
    fun `filter by has image and missing image`() {
        val hasImage = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.HAS_IMAGE,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(2, hasImage.size)
        assertEquals(listOf("Apple", "Cat"), hasImage.map { it.questionText })

        val missingImage = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.MISSING_IMAGE,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, missingImage.size)
        assertEquals("Banana", missingImage.first().questionText)
    }

    @Test
    fun `filter by has audio and missing audio`() {
        val hasAudio = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.HAS_AUDIO,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(2, hasAudio.size)
        assertEquals(listOf("Apple", "Banana"), hasAudio.map { it.questionText })

        val missingAudio = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.MISSING_AUDIO,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )
        assertEquals(1, missingAudio.size)
        assertEquals("Cat", missingAudio.first().questionText)
    }

    @Test
    fun `sort question A-Z and lesson stability`() {
        val sortQuestion = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.QUESTION_ASC
        )
        assertEquals(listOf("Apple", "Banana", "Cat"), sortQuestion.map { it.questionText })

        val sortLesson = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.LESSON_ASC
        )
        assertEquals(listOf("Apple", "Cat", "Banana"), sortLesson.map { it.questionText })
    }

    @Test
    fun `sort media completeness`() {
        // Items with both media first (Apple = 0 missing), then 1 missing (Banana & Cat = 1 missing, sorted by index)
        val sorted = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = sampleItems,
            query = "",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.MEDIA_COMPLETENESS
        )
        assertEquals("Apple", sorted[0].questionText) // 0 missing
        assertEquals("Banana", sorted[1].questionText) // 1 missing
        assertEquals("Cat", sorted[2].questionText) // 1 missing
    }
}
