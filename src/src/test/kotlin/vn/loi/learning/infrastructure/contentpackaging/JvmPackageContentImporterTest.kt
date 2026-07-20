package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

class JvmPackageContentImporterTest {

    @Test
    fun `imports legacy content json from opd3 package`() {
        val packageFile = Files.createTempFile("learning-test-", ".opd3")

        try {
            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("""
                [
                  {
                    "group": "Short Stories",
                    "section": "Section 1",
                    "lesson": "Lesson 1",
                    "en": "She opened the door.",
                    "vi": "Co ay mo cua.",
                    "audio": "door.mp3"
                  }
                ]
                """.trimIndent().toByteArray())
                zip.closeEntry()
            }

            val importer = JvmPackageContentImporter(
                archiveReader = JvmOpd3ArchiveReader(),
                entryReader = JvmOpd3EntryReader()
            )

            val result = importer.importContent(
                PackageScanCandidate(
                    source = packageFile.toString()
                )
            )

            assertEquals(1, result.contents.size)
            assertEquals(5, result.learningItems.size)
            assertEquals(1, result.importedLibraryCount)
            assertEquals(0, result.warnings.size)
        } finally {
            Files.deleteIfExists(packageFile)
        }
    }
}
