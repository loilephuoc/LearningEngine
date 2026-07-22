package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.application.contentpackaging.InvalidPackageArchiveStructureException
import vn.loi.learning.application.contentpackaging.InvalidPackageFormatException
import vn.loi.learning.application.contentpackaging.InvalidPackageJsonException
import vn.loi.learning.application.contentpackaging.InvalidPackageVersionException
import vn.loi.learning.application.contentpackaging.MissingPackageManifestException
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class JvmOpd3PackageDescriptorReaderTest {

    @Test
    fun `malformed manifest reports entry and preserves parser message`() {
        withPackage(
            manifest = "{"
        ) { archive ->
            val exception =
                assertFailsWith<InvalidPackageJsonException> {
                    createReader().read(
                        PackageScanCandidate(
                            archive.toString()
                        )
                    )
                }

            assertEquals("manifest.json", exception.entryName)
            assertEquals(
                exception.cause?.message,
                exception.message
            )
        }
    }

    @Test
    fun `validates archive structure before reading manifest content`() {
        val packagePath = Files.createTempFile(
            "descriptor-structure-",
            ".opd3"
        )

        try {
            ZipOutputStream(
                Files.newOutputStream(packagePath)
            ).use { output ->
                output.putNextEntry(
                    ZipEntry("../manifest.json")
                )
                output.write("{}".toByteArray())
                output.closeEntry()
            }

            val reader = JvmOpd3PackageDescriptorReader(
                archiveReader = JvmOpd3ArchiveReader(),
                entryReader = Opd3EntryReader { _, _ ->
                    error("Manifest must not be read before structure validation.")
                }
            )

            assertFailsWith<InvalidPackageArchiveStructureException> {
                reader.read(
                    PackageScanCandidate(packagePath.toString())
                )
            }
        } finally {
            Files.deleteIfExists(packagePath)
        }
    }

    @Test
    fun `reads package descriptor from backward compatible manifest`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "1.0",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            val result =
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )

            assertEquals(
                PackageDescriptor(
                    name =
                        "English Elementary",
                    version =
                        "1.0",
                    format =
                        "OPD3"
                ),
                result
            )
        }
    }

    @Test
    fun `reads schema compatibility and dependencies from manifest`() {
        withPackage(
            manifest =
                """
                {
                  "name": "Medical English",
                  "version": "2.0.0",
                  "format": "OPD3",
                  "schemaVersion": 1,
                  "minimumEngineVersion": "1.0.0",
                  "maximumEngineVersion": "3.0.0",
                  "contentCount": 0,
                  "learningItemCount": 0,
                  "dependencies": [
                    {
                      "packageName": "medical-core",
                      "minimumVersion": "1.2.0",
                      "maximumVersion": "2.0.0"
                    }
                  ]
                }
                """.trimIndent()
        ) { archive ->
            val result =
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )

            assertEquals(
                "Medical English",
                result.name
            )

            assertEquals(
                "2.0.0",
                result.version
            )

            assertEquals(
                "OPD3",
                result.format
            )

            assertEquals(
                1,
                result.schemaVersion
            )

            assertEquals(
                "1.0.0",
                result.minimumEngineVersion
            )

            assertEquals(
                "3.0.0",
                result.maximumEngineVersion
            )

            assertEquals(
                setOf(
                    PackageDependency(
                        packageName =
                            "medical-core",
                        minimumVersion =
                            "1.2.0",
                        maximumVersion =
                            "2.0.0"
                    )
                ),
                result.dependencies
            )
        }
    }

    @Test
    fun `accepts package format without case sensitivity and normalizes it`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "1.0",
                  "format": "opd3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            val result =
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )

            assertEquals(
                "OPD3",
                result.format
            )
        }
    }

    @Test
    fun `ignores manifest fields introduced by future compatible producers`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "1.0",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0,
                  "publisher": "Learning Engine",
                  "customMetadata": {
                    "category": "English"
                  }
                }
                """.trimIndent()
        ) { archive ->
            val result =
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )

            assertEquals(
                "English Elementary",
                result.name
            )
        }
    }

    @Test
    fun `rejects missing manifest`() {
        val directory =
            createTempDirectory(
                "opd3-no-manifest"
            )

        val archive =
            directory.resolve(
                "package.opd3"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(
                    archive
                )
            ).use {
                // Deliberately empty archive.
            }

            val exception =
                assertFailsWith<MissingPackageManifestException> {
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )
            }

            assertEquals(
                "manifest.json",
                exception.entryName
            )
            assertEquals(
                "Missing package manifest: manifest.json",
                exception.message
            )
        } finally {
            deleteRecursively(
                directory
            )
        }
    }

    @Test
    fun `rejects invalid package format`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "1.0",
                  "format": "OPD2",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            assertFailsWith<InvalidPackageFormatException> {
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )
            }
        }
    }

    @Test
    fun `rejects blank package version`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            assertFailsWith<InvalidPackageVersionException> {
                createReader().read(
                    PackageScanCandidate(
                        archive.toString()
                    )
                )
            }
        }
    }

    @Test
    fun `rejects blank package name`() {
        withPackage(
            manifest =
                """
                {
                  "name": " ",
                  "version": "1.0",
                  "format": "OPD3",
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    createReader().read(
                        PackageScanCandidate(
                            archive.toString()
                        )
                    )
                }

            assertEquals(
                "Package manifest name must not be blank.",
                exception.message
            )
        }
    }

    @Test
    fun `rejects non-positive schema version`() {
        withPackage(
            manifest =
                """
                {
                  "name": "English Elementary",
                  "version": "1.0",
                  "format": "OPD3",
                  "schemaVersion": 0,
                  "contentCount": 0,
                  "learningItemCount": 0
                }
                """.trimIndent()
        ) { archive ->
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    createReader().read(
                        PackageScanCandidate(
                            archive.toString()
                        )
                    )
                }

            assertEquals(
                "Package manifest schema version must be positive.",
                exception.message
            )
        }
    }

    private fun createReader(): JvmOpd3PackageDescriptorReader =
        JvmOpd3PackageDescriptorReader(
            archiveReader =
                JvmOpd3ArchiveReader(),
            entryReader =
                JvmOpd3EntryReader()
        )

    private fun withPackage(
        manifest: String,
        block: (Path) -> Unit
    ) {
        val directory =
            createTempDirectory(
                "opd3-descriptor"
            )

        val archive =
            directory.resolve(
                "package.opd3"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(
                    archive
                )
            ).use { zip ->
                zip.putNextEntry(
                    ZipEntry(
                        "manifest.json"
                    )
                )

                zip.write(
                    manifest.toByteArray()
                )

                zip.closeEntry()
            }

            block(
                archive
            )
        } finally {
            deleteRecursively(
                directory
            )
        }
    }

    private fun deleteRecursively(
        path: Path
    ) {
        if (
            !Files.exists(
                path
            )
        ) {
            return
        }

        Files.walk(
            path
        ).use { paths ->
            paths
                .sorted(
                    Comparator.reverseOrder()
                )
                .forEach(
                    Files::deleteIfExists
                )
        }
    }
}
