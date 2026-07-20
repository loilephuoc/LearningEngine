package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class PackageScannerRoutingIntegrationTest {

    @Test
    fun `scanner candidates are routed to bundle and legacy importers`() {
        val directory =
            Files.createTempDirectory(
                "package-scanner-routing-test"
            )

        val bundleFile =
            directory.resolve(
                "bundle.opd3"
            )

        val legacyFile =
            directory.resolve(
                "legacy.pkg"
            )

        try {
            createMinimalBundlePackage(
                bundleFile
            )

            createMinimalLegacyPackage(
                legacyFile
            )

            val scanner =
                ContentPackageImportFactory.createScanner(
                    directory
                )

            val importer =
                ContentPackageImportFactory.createContentImporter()

            val candidates =
                scanner.scan()

            assertEquals(
                listOf(
                    bundleFile.toString(),
                    legacyFile.toString()
                ),
                candidates.map { candidate ->
                    candidate.source
                }
            )

            val importedPackages =
                candidates.map { candidate ->
                    importer.importContent(
                        candidate
                    )
                }

            val bundleResult =
                importedPackages[0]

            val legacyResult =
                importedPackages[1]

            assertEquals(
                0,
                bundleResult.contents.size
            )

            assertEquals(
                0,
                bundleResult.learningItems.size
            )

            assertEquals(
                1,
                legacyResult.contents.size
            )

            assertEquals(
                5,
                legacyResult.learningItems.size
            )

            assertEquals(
                1,
                legacyResult.importedLibraryCount
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun createMinimalBundlePackage(
        packageFile: java.nio.file.Path
    ) {
        ZipOutputStream(
            Files.newOutputStream(packageFile)
        ).use { zip ->
            zip.putNextEntry(
                ZipEntry("manifest.json")
            )

            zip.write(
                """
                {
                  "name": "Bundle",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent().toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry("contents.json")
            )

            zip.write(
                """
                {
                  "contents": []
                }
                """.trimIndent().toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry("learning-items.json")
            )

            zip.write(
                """
                {
                  "learningItems": []
                }
                """.trimIndent().toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry("metadata.json")
            )

            zip.write(
                "{}".toByteArray()
            )

            zip.closeEntry()
        }
    }

    private fun createMinimalLegacyPackage(
        packageFile: java.nio.file.Path
    ) {
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
    }
}