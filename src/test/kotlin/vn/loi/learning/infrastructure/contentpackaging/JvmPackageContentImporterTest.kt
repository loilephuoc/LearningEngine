package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.MissingPackageContentException
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

class JvmPackageContentImporterTest {

    @Test
    fun `imports legacy content json from pkg package`() {
        val packageFile =
            Files.createTempFile(
                "legacy-package-",
                ".pkg"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->
                zip.putNextEntry(
                    ZipEntry("content.json")
                )

                zip.write(
                    """
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
                    """.trimIndent().toByteArray()
                )

                zip.closeEntry()
            }

            val importer =
                JvmPackageContentImporter(
                    archiveReader =
                        JvmOpd3ArchiveReader(),
                    entryReader =
                        JvmOpd3EntryReader()
                )

            val result =
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )

            assertEquals(
                1,
                result.contents.size
            )
        } finally {
            Files.deleteIfExists(packageFile)
        }
    }

    @Test
    fun `rejects legacy package missing content json entry`() {
        val packageFile =
            Files.createTempFile(
                "missing-content-",
                ".pkg"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->
                zip.putNextEntry(
                    ZipEntry("other.json")
                )

                zip.write(
                    "{}".toByteArray()
                )

                zip.closeEntry()
            }

            val importer =
                JvmPackageContentImporter(
                    archiveReader =
                        JvmOpd3ArchiveReader(),
                    entryReader =
                        JvmOpd3EntryReader()
                )

            val exception =
                assertFailsWith<MissingPackageContentException> {
                    importer.importContent(
                        PackageScanCandidate(
                            source =
                                packageFile.toString()
                        )
                    )
                }

            assertEquals(
                "Missing package content entry: content.json",
                exception.message
            )

            assertTrue(
                packageFile.toString().isNotBlank()
            )
        } finally {
            Files.deleteIfExists(packageFile)
        }
    }
}