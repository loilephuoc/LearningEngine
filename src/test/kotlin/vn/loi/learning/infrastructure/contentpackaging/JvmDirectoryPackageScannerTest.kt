package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmDirectoryPackageScannerTest {

    @Test
    fun `scanner returns direct bundle and legacy package files in stable order`() {
        val directory =
            createTempDirectory(
                "package-scanner-test"
            )

        try {
            val legacyFile =
                directory
                    .resolve("legacy.pkg")
                    .createFile()

            val secondBundleFile =
                directory
                    .resolve("second.OPD3")
                    .createFile()

            val firstBundleFile =
                directory
                    .resolve("first.opd3")
                    .createFile()

            directory
                .resolve("ignored.json")
                .createFile()

            val nestedDirectory =
                directory
                    .resolve("nested")
                    .createDirectory()

            nestedDirectory
                .resolve("ignored.opd3")
                .createFile()

            nestedDirectory
                .resolve("ignored.pkg")
                .createFile()

            val scanner =
                JvmDirectoryPackageScanner(
                    directory
                )

            val result =
                scanner.scan()

            assertEquals(
                listOf(
                    firstBundleFile.toString(),
                    legacyFile.toString(),
                    secondBundleFile.toString()
                ),
                result.map { candidate ->
                    candidate.source
                }
            )
        } finally {
            Files.walk(directory).use { paths ->
                paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `scanner accepts package extensions without case sensitivity`() {
        val directory =
            createTempDirectory(
                "package-scanner-case-test"
            )

        try {
            val legacyFile =
                directory
                    .resolve("legacy.PKG")
                    .createFile()

            val bundleFile =
                directory
                    .resolve("bundle.OpD3")
                    .createFile()

            val scanner =
                JvmDirectoryPackageScanner(
                    directory
                )

            val result =
                scanner.scan()

            assertEquals(
                listOf(
                    bundleFile.toString(),
                    legacyFile.toString()
                ),
                result.map { candidate ->
                    candidate.source
                }
            )
        } finally {
            Files.walk(directory).use { paths ->
                paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `scanner rejects missing directory`() {
        val missingDirectory =
            createTempDirectory(
                "package-scanner-missing"
            ).resolve("missing")

        assertFailsWith<IllegalArgumentException> {
            JvmDirectoryPackageScanner(
                missingDirectory
            ).scan()
        }
    }
}