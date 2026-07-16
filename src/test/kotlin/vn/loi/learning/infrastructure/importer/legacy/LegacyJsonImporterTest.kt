package vn.loi.learning.infrastructure.importer.legacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningMode

class LegacyJsonImporterTest {

    private val importer = LegacyJsonImporter()

    @Test
    fun `legacy records are converted into content and learning items`() {
        val jsonText =
            """
            [
              {
                "group": "Short Stories",
                "section": "Section 1",
                "lesson": "Lesson 1",
                "en": "She opened the door.",
                "vi": "Cô ấy mở cửa.",
                "ipa": "/ʃiː ˈəʊpənd ðə dɔːr/",
                "image": "door.jpg",
                "audio": "door.mp3",
                "audio_vi": "door_vi.mp3",
                "example": "She quietly opened the front door.",
                "example_vi": "Cô ấy lặng lẽ mở cửa trước.",
                "example_audio": "door_example.mp3"
              }
            ]
            """.trimIndent()

        val result = importer.import(
            sourceName = "Short Stories Section 1.json",
            jsonText = jsonText
        )

        assertEquals(1, result.importedContentCount)
        assertEquals(5, result.importedLearningItemCount)
        assertEquals(0, result.skippedRecordCount)

        val content = result.contents.single()

        assertEquals(ContentType.STORY, content.type)
        assertEquals(
            "She opened the door.",
            content.text.primaryText
        )
        assertEquals(
            "Cô ấy mở cửa.",
            content.text.translatedText
        )
        assertEquals(
            "door.mp3",
            content.media.primaryAudio
        )

        val modes = result.learningItems
            .map { it.mode }
            .toSet()

        assertTrue(
            LearningMode.MEANING_RECOGNITION in modes
        )
        assertTrue(
            LearningMode.MEANING_RECALL in modes
        )
        assertTrue(
            LearningMode.LISTENING_RECOGNITION in modes
        )
        assertTrue(
            LearningMode.DICTATION in modes
        )
        assertTrue(
            LearningMode.SHADOWING in modes
        )
    }

    @Test
    fun `record without english text is skipped`() {
        val jsonText =
            """
            [
              {
                "group": "Short Stories",
                "vi": "Dòng này không có câu tiếng Anh."
              }
            ]
            """.trimIndent()

        val result = importer.import(
            sourceName = "invalid.json",
            jsonText = jsonText
        )

        assertEquals(0, result.importedContentCount)
        assertEquals(0, result.importedLearningItemCount)
        assertEquals(1, result.skippedRecordCount)
        assertEquals(
            "Field 'en' is missing or blank.",
            result.skippedRecords.single().reason
        )
    }

    @Test
    fun `importing identical source produces stable ids`() {
        val jsonText =
            """
            [
              {
                "group": "Conversation",
                "section": "Daily Life",
                "lesson": "Greeting",
                "en": "How are you?",
                "vi": "Bạn khỏe không?"
              }
            ]
            """.trimIndent()

        val first = importer.import(
            sourceName = "conversation.json",
            jsonText = jsonText
        )

        val second = importer.import(
            sourceName = "conversation.json",
            jsonText = jsonText
        )

        assertEquals(
            first.contents.single().id,
            second.contents.single().id
        )

        assertEquals(
            first.learningItems.map { it.id },
            second.learningItems.map { it.id }
        )
    }

    @Test
    fun `unknown json fields are ignored`() {
        val jsonText =
            """
            [
              {
                "en": "Unknown fields should not break import.",
                "vi": "Trường không biết không làm hỏng import.",
                "some_future_field": "future value"
              }
            ]
            """.trimIndent()

        val result = importer.import(
            sourceName = "future.json",
            jsonText = jsonText
        )

        assertEquals(1, result.importedContentCount)
    }
}