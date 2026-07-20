package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class JvmOpd3ArchiveReaderTest {

    @Test
    fun `opens existing opd3 archive`() {
        val directory = createTempDirectory("opd3-archive-reader")

        try {
            val archive = directory.resolve("package.opd3")

            ZipOutputStream(
                Files.newOutputStream(archive)
            ).use { }

            val reader = JvmOpd3ArchiveReader()

            reader.open(archive).use { zip ->
                assertNotNull(zip)
            }
        } finally {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun `rejects missing package`() {
        val directory = createTempDirectory("opd3-missing")

        try {
            val missing = directory.resolve("missing.opd3")
            val reader = JvmOpd3ArchiveReader()

            assertFailsWith<IllegalArgumentException> {
                reader.open(missing)
            }
        } finally {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }
}
