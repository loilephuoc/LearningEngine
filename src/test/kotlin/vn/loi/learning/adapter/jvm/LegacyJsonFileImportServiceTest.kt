package vn.loi.learning.adapter.jvm

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.importing.LegacyJsonImportService
import vn.loi.learning.infrastructure.LearningEngineFactory

class LegacyJsonFileImportServiceTest {

    @Test
    fun `legacy json file can be imported from disk`() {
        val tempFile = Files.createTempFile(
            "learning-engine-legacy-",
            ".json"
        )

        try {
            Files.writeString(
                tempFile,
                """
                [
                  {
                    "group": "Short Stories",
                    "section": "Section 1",
                    "lesson": "Lesson 1",
                    "en": "She opened the door.",
                    "vi": "Cô ấy mở cửa.",
                    "audio": "door.mp3"
                  }
                ]
                """.trimIndent()
            )

            val engine =
                LearningEngineFactory.createInMemory()

            val importService =
                LegacyJsonImportService(
                    engine = engine,
                    importer = vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter()
                )

            val fileImportService =
                LegacyJsonFileImportService(importService)

            val result =
                fileImportService.import(tempFile)

            assertEquals(
                1,
                result.registeredContentCount
            )

            assertEquals(
                5,
                result.registeredLearningItemCount
            )

            assertEquals(
                0,
                result.skippedRecordCount
            )
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}