package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmLegacyPackageScannerTest {

    @Test
    fun `scanner returns matching JSON and PKG pairs in stable order`() {
        val directory =
            createTempDirectory(
                "legacy-package-scanner-test"
            )

        try {
            val secondJson =
                directory
                    .resolve("Short_Stories_Section_1.json")
                    .createFile()

            val secondPackage =
                directory
                    .resolve("Short_Stories_Section_1.pkg")
                    .createFile()

            val firstJson =
                directory
                    .resolve("2000Cau.json")
                    .createFile()

            val firstPackage =
                directory
                    .resolve("2000Cau.pkg")
                    .createFile()

            val scanner =
                JvmLegacyPackageScanner(
                    directory
                )

            val result =
                scanner.scan()

            assertEquals(
                listOf(
                    firstJson.toString(),
                    secondJson.toString()
                ),
                result.map { candidate ->
                    candidate.jsonSource
                }
            )

            assertEquals(
                listOf(
                    firstPackage.toString(),
                    secondPackage.toString()
                ),
                result.map { candidate ->
                    candidate.mediaSource
                }
            )
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `scanner matches extensions and base names without case sensitivity`() {
        val directory =
            createTempDirectory(
                "legacy-package-scanner-case-test"
            )

        try {
            val jsonFile =
                directory
                    .resolve("OPD_2ND.JSON")
                    .createFile()

            val packageFile =
                directory
                    .resolve("opd_2nd.PkG")
                    .createFile()

            val scanner =
                JvmLegacyPackageScanner(
                    directory
                )

            val result =
                scanner.scan()

            assertEquals(
                1,
                result.size
            )

            assertEquals(
                jsonFile.toString(),
                result.single().jsonSource
            )

            assertEquals(
                packageFile.toString(),
                result.single().mediaSource
            )
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `scanner ignores files without matching companion`() {
        val directory =
            createTempDirectory(
                "legacy-package-scanner-orphan-test"
            )

        try {
            directory
                .resolve("json-only.json")
                .createFile()

            directory
                .resolve("package-only.pkg")
                .createFile()

            directory
                .resolve("ignored.txt")
                .createFile()

            val nestedDirectory =
                directory
                    .resolve("nested")
                    .createDirectory()

            nestedDirectory
                .resolve("nested.json")
                .createFile()

            nestedDirectory
                .resolve("nested.pkg")
                .createFile()

            val scanner =
                JvmLegacyPackageScanner(
                    directory
                )

            assertEquals(
                emptyList(),
                scanner.scan()
            )
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun `scanner rejects missing directory`() {
        val missingDirectory =
            createTempDirectory(
                "legacy-package-scanner-missing-test"
            ).resolve("missing")

        assertFailsWith<IllegalArgumentException> {
            JvmLegacyPackageScanner(
                missingDirectory
            ).scan()
        }
    }

    private fun deleteRecursively(
        directory: java.nio.file.Path
    ) {
        Files.walk(directory).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }
}