package vn.loi.learning.infrastructure.contentpackaging

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith
import vn.loi.learning.application.contentpackaging.InvalidPackageArchiveStructureException
import vn.loi.learning.application.contentpackaging.PackageArchiveEntryCountExceededException

class Opd3ArchiveStructureValidatorTest {

    @Test
    fun `accepts valid archive structure`() {
        withArchive(
            REQUIRED_ENTRIES
        ) { archive ->
            Opd3ArchiveStructureValidator().validate(archive)
        }
    }

    @Test
    fun `rejects duplicate exact entry name`() {
        withDuplicateArchive(
            firstName = "alpha.json",
            secondName = "bravo.json"
        ) { archive ->
            assertFailsWith<InvalidPackageArchiveStructureException> {
                Opd3ArchiveStructureValidator().validate(archive)
            }
        }
    }

    @Test
    fun `rejects duplicate required entry name`() {
        withDuplicateArchive(
            firstName = "manifest.json",
            secondName = "manifezt.json"
        ) { archive ->
            assertFailsWith<InvalidPackageArchiveStructureException> {
                Opd3ArchiveStructureValidator().validate(archive)
            }
        }
    }

    @Test
    fun `rejects parent traversal`() =
        assertInvalid("../manifest.json")

    @Test
    fun `rejects absolute Unix path`() =
        assertInvalid("/manifest.json")

    @Test
    fun `rejects absolute Windows path`() =
        assertInvalid("C:\\manifest.json")

    @Test
    fun `rejects backslash separator`() =
        assertInvalid("folder\\manifest.json")

    @Test
    fun `rejects empty path segment`() =
        assertInvalid("folder//manifest.json")

    @Test
    fun `rejects dot path segment`() =
        assertInvalid("./manifest.json")

    @Test
    fun `rejects abnormal leading or trailing entry name`() {
        assertInvalid(" manifest.json")
        assertInvalid("manifest.json/")
    }

    @Test
    fun `rejects names that normalize to the same logical name`() {
        withArchive(
            listOf(
                "folder/A.json",
                "folder/Ａ.json"
            )
        ) { archive ->
            assertFailsWith<InvalidPackageArchiveStructureException> {
                Opd3ArchiveStructureValidator().validate(archive)
            }
        }
    }

    @Test
    fun `rejects case ambiguous required entry`() {
        withArchive(
            listOf("Manifest.json")
        ) { archive ->
            assertFailsWith<InvalidPackageArchiveStructureException> {
                Opd3ArchiveStructureValidator().validate(archive)
            }
        }
    }

    @Test
    fun `accepts archive entry count at configured limit`() {
        withArchive(
            listOf("one.json", "two.json")
        ) { archive ->
            Opd3ArchiveStructureValidator(
                Opd3ArchiveStructureLimits(
                    maximumEntryCount = 2
                )
            ).validate(archive)
        }
    }

    @Test
    fun `rejects archive entry count above configured limit`() {
        withArchive(
            listOf("one.json", "two.json", "three.json")
        ) { archive ->
            assertFailsWith<PackageArchiveEntryCountExceededException> {
                Opd3ArchiveStructureValidator(
                    Opd3ArchiveStructureLimits(
                        maximumEntryCount = 2
                    )
                ).validate(archive)
            }
        }
    }

    private fun assertInvalid(
        entryName: String
    ) {
        withArchive(
            listOf(entryName)
        ) { archive ->
            assertFailsWith<InvalidPackageArchiveStructureException> {
                Opd3ArchiveStructureValidator().validate(archive)
            }
        }
    }

    private fun withArchive(
        entryNames: List<String>,
        assertion: (ZipFile) -> Unit
    ) {
        val archivePath = Files.createTempFile(
            "opd3-structure-",
            ".zip"
        )

        try {
            writeArchive(
                archivePath,
                entryNames
            )

            ZipFile(archivePath.toFile()).use(assertion)
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }

    private fun withDuplicateArchive(
        firstName: String,
        secondName: String,
        assertion: (ZipFile) -> Unit
    ) {
        require(firstName.length == secondName.length)

        val archivePath = Files.createTempFile(
            "opd3-duplicate-",
            ".zip"
        )

        try {
            writeArchive(
                archivePath,
                listOf(firstName, secondName)
            )

            replaceAll(
                archivePath,
                secondName.toByteArray(StandardCharsets.UTF_8),
                firstName.toByteArray(StandardCharsets.UTF_8)
            )

            ZipFile(archivePath.toFile()).use(assertion)
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }

    private fun writeArchive(
        archivePath: Path,
        entryNames: List<String>
    ) {
        ZipOutputStream(
            Files.newOutputStream(archivePath)
        ).use { output ->
            entryNames.forEach { name ->
                output.putNextEntry(ZipEntry(name))
                output.write("{}".toByteArray())
                output.closeEntry()
            }
        }
    }

    private fun replaceAll(
        path: Path,
        target: ByteArray,
        replacement: ByteArray
    ) {
        val bytes = Files.readAllBytes(path)

        for (index in 0..bytes.size - target.size) {
            if (
                target.indices.all { offset ->
                    bytes[index + offset] == target[offset]
                }
            ) {
                replacement.copyInto(
                    destination = bytes,
                    destinationOffset = index
                )
            }
        }

        Files.write(path, bytes)
    }

    private companion object {
        val REQUIRED_ENTRIES = listOf(
            "manifest.json",
            "metadata.json",
            "contents.json",
            "learning-items.json"
        )
    }
}
