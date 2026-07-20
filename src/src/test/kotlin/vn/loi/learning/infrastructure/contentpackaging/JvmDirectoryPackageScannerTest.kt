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
    fun `scanner returns only direct opd3 files in stable order`() {
        val directory = createTempDirectory("package-scanner-test")

        try {
            val secondFile = directory.resolve("second.OPD3").createFile()
            val firstFile = directory.resolve("first.opd3").createFile()
            directory.resolve("ignored.json").createFile()
            val nestedDirectory = directory.resolve("nested").createDirectory()
            nestedDirectory.resolve("ignored.opd3").createFile()

            val scanner = JvmDirectoryPackageScanner(directory)

            val result = scanner.scan()

            assertEquals(
                listOf(
                    firstFile.toString(),
                    secondFile.toString()
                ),
                result.map { candidate -> candidate.source }
            )
        } finally {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `scanner rejects missing directory`() {
        val missingDirectory =
            createTempDirectory("package-scanner-missing")
                .resolve("missing")

        assertFailsWith<IllegalArgumentException> {
            JvmDirectoryPackageScanner(missingDirectory).scan()
        }
    }
}
