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
    fun `opd3 candidate is routed to bundle importer`() {
        val packageFile =
            Files.createTempFile(
                "bundle-package-",
                ".opd3"
            )

        try {
            createMinimalBundlePackage(
                packageFile
            )

            val importer =
                createImporter()

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
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    @Test
    fun `opd3 extension is matched without case sensitivity`() {
        val directory =
            Files.createTempDirectory(
                "bundle-package-case-test"
            )

        val packageFile =
            directory.resolve(
                "bundle.OpD3"
            )

        try {
            createMinimalBundlePackage(
                packageFile
            )

            val importer =
                createImporter()

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
    fun `malformed opd3 bundle does not fall back to legacy importer`() {
        val packageFile =
            Files.createTempFile(
                "malformed-bundle-",
                ".opd3"
            )

        try {
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
                      "name": "Malformed Bundle",
                      "version": "1.0.0",
                      "format": "OPD3",
                      "contentCount": 0,
                      "learningItemCount": 0
                    }
                    """.trimIndent()
                        .toByteArray()
                )

                zip.closeEntry()
            }

            val importer =
                createImporter()

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
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    @Test
    fun `legacy pkg candidate is routed directly to legacy importer`() {
        val packageFile =
            Files.createTempFile(
                "legacy-package-",
                ".pkg"
            )

        try {
            createMinimalLegacyPackage(
                packageFile
            )

            val importer =
                createImporter()

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
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    @Test
    fun `legacy pkg extension is matched without case sensitivity`() {
        val directory =
            Files.createTempDirectory(
                "legacy-package-case-test"
            )

        val packageFile =
            directory.resolve(
                "legacy.PKG"
            )

        try {
            createMinimalLegacyPackage(
                packageFile
            )

            val importer =
                createImporter()

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
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun `unsupported candidate format is rejected explicitly`() {
        val packageFile =
            Files.createTempFile(
                "unsupported-package-",
                ".zip"
            )

        try {
            val importer =
                createImporter()

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
                "Unsupported package format: $packageFile",
                exception.message
            )
        } finally {
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    @Test
    fun `candidate without extension is rejected explicitly`() {
        val directory =
            Files.createTempDirectory(
                "package-without-extension-test"
            )

        val packageFile =
            directory.resolve(
                "package"
            )

        try {
            Files.createFile(
                packageFile
            )

            val importer =
                createImporter()

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
                "Unsupported package format: $packageFile",
                exception.message
            )
        } finally {
            directory
                .toFile()
                .deleteRecursively()
        }
    }

    private fun createImporter(): PackageContentImporterCompat =
        PackageContentImporterCompat(
            bundleImporter =
                PackageBundleImporter(
                    BundlePackageReader(
                        archiveReader =
                            JvmOpd3ArchiveReader(),
                        entryReader =
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

    private fun createMinimalBundlePackage(
        packageFile: java.nio.file.Path
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

    private fun createMinimalLegacyPackage(
        packageFile: java.nio.file.Path
    ) {
        ZipOutputStream(
            Files.newOutputStream(
                packageFile
            )
        ).use { zip ->
            zip.putNextEntry(
                ZipEntry(
                    "content.json"
                )
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
                """.trimIndent()
                    .toByteArray()
            )

            zip.closeEntry()
        }
    }
}