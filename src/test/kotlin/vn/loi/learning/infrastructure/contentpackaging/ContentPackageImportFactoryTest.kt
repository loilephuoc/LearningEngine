package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

class ContentPackageImportFactoryTest {

    @Test
    fun `factory content importer imports minimal bundle package`() {
        val directory =
            Files.createTempDirectory(
                "content-package-import-factory-test"
            )

        val packageFile =
            directory.resolve(
                "bundle.opd3"
            )

        try {
            writeMinimalBundlePackage(
                packageFile
            )

            val importer =
                ContentPackageImportFactory
                    .createContentImporter()

            val result =
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )

            assertEquals(
                0,
                result.contents.size
            )

            assertEquals(
                0,
                result.learningItems.size
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `factory creates media enabled content importer`() {
        val directory =
            Files.createTempDirectory(
                "content-package-media-factory-test"
            )

        val mediaDirectory =
            directory.resolve(
                "media"
            )

        try {
            val importer =
                ContentPackageImportFactory
                    .createContentImporter(
                        mediaDirectory
                    )

            assertTrue(
                Files.isDirectory(
                    mediaDirectory
                )
            )

            assertEquals(
                PackageContentImporterCompat::class,
                importer::class
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `factory creates legacy package scanner`() {
        val directory =
            Files.createTempDirectory(
                "legacy-package-scanner-factory-test"
            )

        try {
            Files.createFile(
                directory.resolve(
                    "lesson.json"
                )
            )

            Files.createFile(
                directory.resolve(
                    "lesson.pkg"
                )
            )

            val scanner =
                ContentPackageImportFactory
                    .createLegacyScanner(
                        directory
                    )

            val candidates =
                scanner.scan()

            assertEquals(
                1,
                candidates.size
            )

            assertEquals(
                directory
                    .resolve("lesson.json")
                    .toString(),
                candidates.single().jsonSource
            )

            assertEquals(
                directory
                    .resolve("lesson.pkg")
                    .toString(),
                candidates.single().mediaSource
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `factory creates legacy importer with media storage`() {
        val directory =
            Files.createTempDirectory(
                "legacy-package-importer-factory-test"
            )

        val mediaDirectory =
            directory.resolve(
                "media"
            )

        try {
            val importer =
                ContentPackageImportFactory
                    .createLegacyImporter(
                        mediaDirectory
                    )

            assertTrue(
                Files.isDirectory(
                    mediaDirectory
                )
            )

            assertEquals(
                LegacyOpd3PackageImporter::class,
                importer::class
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun writeMinimalBundlePackage(
        packageFile: Path
    ) {
        ZipOutputStream(
            Files.newOutputStream(
                packageFile
            )
        ).use { zip ->
            zip.putNextEntry(
                ZipEntry(
                    "manifest.json"
                )
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
                """.trimIndent()
                    .toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry(
                    "contents.json"
                )
            )

            zip.write(
                """
                {
                  "contents": []
                }
                """.trimIndent()
                    .toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry(
                    "learning-items.json"
                )
            )

            zip.write(
                """
                {
                  "learningItems": []
                }
                """.trimIndent()
                    .toByteArray()
            )

            zip.closeEntry()

            zip.putNextEntry(
                ZipEntry(
                    "metadata.json"
                )
            )

            zip.write(
                "{}".toByteArray()
            )

            zip.closeEntry()
        }
    }
}