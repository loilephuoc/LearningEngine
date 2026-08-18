package vn.loi.learning.desktop.ui.browser.export

import java.io.File
import java.time.Instant
import kotlin.test.*
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter
import vn.loi.learning.domain.content.model.ContentId

class ContentMaintenanceExporterTest {
    @Test
    fun `export serializes canonical fields and explicitly omits FSRS and study state`() {
        val item = PackageContentBrowserItem(
            index = 1,
            contentId = ContentId("cnt-123"),
            questionText = "Question text",
            answerText = "Answer text",
            pronunciation = "/test/",
            partOfSpeech = "NOUN",
            group = "Group",
            section = "Section",
            lesson = "Lesson",
            packageName = "Test Package",
            hasImage = true,
            hasAudio = true,
            imageRef = "img.png",
            audioRef = "q.mp3",
            questionAudioRef = "q.mp3",
            answerAudioRef = "a.mp3",
            exampleAudioRef = "e.mp3",
            translationAudioRef = "t.mp3",
            exampleText = "Example text",
            exampleTranslation = "Translation text",
            learningItemCount = 0,
            learningItemIds = emptyList(),
            learningModes = emptyList(),
            tags = emptySet(),
            searchableText = "searchable"
        )

        val jsonString = ContentMaintenanceExporter.buildJsonString(
            packageId = "pkg-1",
            packageName = "Test Package",
            scope = ContentMaintenanceExportScope.CURRENT_FILTER_RESULTS,
            filter = ContentProblemFilter.MISSING_ANY_AUDIO,
            items = listOf(item),
            timestamp = Instant.parse("2026-08-18T12:00:00Z")
        )

        assertTrue(jsonString.contains(""""format": "learning-engine-content-maintenance""""))
        assertTrue(jsonString.contains(""""packageId": "pkg-1""""))
        assertTrue(jsonString.contains(""""packageName": "Test Package""""))
        assertTrue(jsonString.contains(""""contentId": "cnt-123""""))
        assertTrue(jsonString.contains(""""question": "Question text""""))
        assertTrue(jsonString.contains(""""answer": "Answer text""""))
        assertTrue(jsonString.contains(""""partOfSpeech": "NOUN""""))
        assertTrue(jsonString.contains(""""pronunciation": "/test/""""))

        // Ensure study history and FSRS fields are not present
        assertFalse(jsonString.contains("stability"))
        assertFalse(jsonString.contains("difficulty"))
        assertFalse(jsonString.contains("due"))
        assertFalse(jsonString.contains("elapsedDays"))
        assertFalse(jsonString.contains("scheduledDays"))
        assertFalse(jsonString.contains("reps"))
        assertFalse(jsonString.contains("lapses"))
        assertFalse(jsonString.contains("state"))
        assertFalse(jsonString.contains("reviewEvent"))
    }

    @Test
    fun `exportToFile writes valid UTF-8 file to disk`() {
        val tempDir = java.nio.file.Files.createTempDirectory("export_test")
        try {
            val targetFile = File(tempDir.toFile(), "test_export.json")
            val item = PackageContentBrowserItem(
                index = 1,
                contentId = ContentId("c-1"),
                questionText = "Tiếng Việt có dấu",
                answerText = "Câu trả lời tiếng Việt",
                pronunciation = "",
                partOfSpeech = "NOUN",
                group = null,
                section = null,
                lesson = "Lesson 1",
                packageName = "Gói tiếng Việt",
                hasImage = false,
                hasAudio = false,
                imageRef = null,
                audioRef = null,
                questionAudioRef = null,
                answerAudioRef = null,
                exampleAudioRef = null,
                translationAudioRef = null,
                exampleText = null,
                exampleTranslation = null,
                learningItemCount = 0,
                learningItemIds = emptyList(),
                learningModes = emptyList(),
                tags = emptySet(),
                searchableText = ""
            )

            val exported = ContentMaintenanceExporter.exportToFile(
                targetFile = targetFile,
                packageId = "pkg-1",
                packageName = "Gói tiếng Việt",
                scope = ContentMaintenanceExportScope.ALL_ITEMS,
                filter = ContentProblemFilter.NONE,
                items = listOf(item)
            )

            assertTrue(exported.exists())
            val content = exported.readText(Charsets.UTF_8)
            assertTrue(content.contains("Tiếng Việt có dấu"))
            assertTrue(content.contains("Câu trả lời tiếng Việt"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
