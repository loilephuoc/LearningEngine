package vn.loi.learning.application.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import vn.loi.learning.infrastructure.contentpackaging.BundlePackageReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3ArchiveReader
import vn.loi.learning.infrastructure.contentpackaging.JvmOpd3EntryReader
import vn.loi.learning.infrastructure.contentpackaging.PackageBundleImporter

class PackageBundleImporterTest {

    private val importer =
        PackageBundleImporter(
            BundlePackageReader(
                JvmOpd3ArchiveReader(),
                JvmOpd3EntryReader()
            )
        )

    @Test
    fun `imports bundle with multiple contents and learning items`() {
        val packageFile =
            Files.createTempFile(
                "multi-content-bundle-",
                ".opd3"
            )

        try {
            createBundle(
                packageFile,
                contentsJson =
                    """
                    {
                      "contents": [
                        {
                          "id": "content-1",
                          "type": "SENTENCE",
                          "primaryText": "Hello world",
                          "translatedText": "Xin chao",
                          "tags": [],
                          "customFields": {}
                        },
                        {
                          "id": "content-2",
                          "type": "SENTENCE",
                          "primaryText": "Good morning",
                          "translatedText": "Chao buoi sang",
                          "tags": [],
                          "customFields": {}
                        }
                      ]
                    }
                    """,
                learningItemsJson =
                    """
                    {
                      "learningItems": [
                        {
                          "id": "item-1",
                          "contentId": "content-1",
                          "mode": "MEANING_RECOGNITION",
                          "isEnabled": true
                        },
                        {
                          "id": "item-2",
                          "contentId": "content-2",
                          "mode": "LISTENING_RECOGNITION",
                          "isEnabled": true
                        }
                      ]
                    }
                    """,
                contentCount = 2,
                learningItemCount = 2
            )

            val result =
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )

            assertNotNull(result)

            assertEquals(
                2,
                result.contents.size
            )

            assertEquals(
                2,
                result.learningItems.size
            )

        } finally {
            Files.deleteIfExists(packageFile)
        }
    }


    @Test
    fun `rejects bundle missing required learning items file`() {
        val packageFile =
            Files.createTempFile(
                "missing-learning-items-",
                ".opd3"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(packageFile)
            ).use { zip ->

                writeEntry(
                    zip,
                    "manifest.json",
                    manifest()
                )

                writeEntry(
                    zip,
                    "contents.json",
                    """
                    {
                      "contents": []
                    }
                    """
                )

                writeEntry(
                    zip,
                    "metadata.json",
                    """
                    {
                      "name": "Broken Bundle"
                    }
                    """
                )
            }

            assertFailsWith<Exception> {
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )
            }

        } finally {
            Files.deleteIfExists(packageFile)
        }
    }


    private fun createBundle(
        file: java.nio.file.Path,
        contentsJson: String,
        learningItemsJson: String,
        contentCount: Int,
        learningItemCount: Int
    ) {
        ZipOutputStream(
            Files.newOutputStream(file)
        ).use { zip ->

            writeEntry(
                zip,
                "manifest.json",
                manifest(
                    contentCount,
                    learningItemCount
                )
            )

            writeEntry(
                zip,
                "contents.json",
                contentsJson
            )

            writeEntry(
                zip,
                "learning-items.json",
                learningItemsJson
            )

            writeEntry(
                zip,
                "metadata.json",
                """
                {
                  "name": "Test Bundle"
                }
                """
            )
        }
    }


    private fun manifest(
        contentCount: Int = 0,
        learningItemCount: Int = 0
    ): String =
        """
        {
          "name": "Test Bundle",
          "version": "1.0.0",
          "format": "OPD3",
          "contentCount": $contentCount,
          "learningItemCount": $learningItemCount
        }
        """


    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        content: String
    ) {
        zip.putNextEntry(
            ZipEntry(name)
        )

        zip.write(
            content.trimIndent().toByteArray()
        )

        zip.closeEntry()
    }
}