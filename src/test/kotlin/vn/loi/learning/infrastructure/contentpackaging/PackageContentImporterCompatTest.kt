package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

class PackageContentImporterCompatTest {

    @Test
    fun `malformed opd3 bundle does not fall back to legacy importer`() {
        val packageFile =
            Files.createTempFile(
                "malformed-bundle-",
                ".opd3"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->
                zip.putNextEntry(
                    ZipEntry("manifest.json")
                )

                zip.write(
                    """
                    {
                      "name": "Malformed Bundle",
                      "version": "1.0.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent().toByteArray()
                )

                zip.closeEntry()
            }

            val importer =
                PackageContentImporterCompat(
                    bundleImporter =
                        PackageBundleImporter(
                            BundlePackageReader(
                                JvmOpd3ArchiveReader(),
                                JvmOpd3EntryReader()
                            )
                        ),
                    legacyImporter =
                        JvmPackageContentImporter(
                            archiveReader =
                                JvmOpd3ArchiveReader(),
                            entryReader =
                                JvmOpd3EntryReader()
                        )
                )

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    importer.importContent(
                        PackageScanCandidate(
                            source =
                                packageFile.toString()
                        )
                    )
                }

            assertEquals(
                "Missing package file: metadata.json",
                exception.message
            )
        } finally {
            Files.deleteIfExists(packageFile)
        }
    }

    @Test
    fun `legacy pkg package still falls back to legacy importer`() {
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
                PackageContentImporterCompat(
                    bundleImporter =
                        PackageBundleImporter(
                            BundlePackageReader(
                                JvmOpd3ArchiveReader(),
                                JvmOpd3EntryReader()
                            )
                        ),
                    legacyImporter =
                        JvmPackageContentImporter(
                            archiveReader =
                                JvmOpd3ArchiveReader(),
                            entryReader =
                                JvmOpd3EntryReader()
                        )
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
            assertEquals(
                5,
                result.learningItems.size
            )
            assertEquals(
                1,
                result.importedLibraryCount
            )
            assertEquals(
                0,
                result.warnings.size
            )
        } finally {
            Files.deleteIfExists(packageFile)
        }
    }
}