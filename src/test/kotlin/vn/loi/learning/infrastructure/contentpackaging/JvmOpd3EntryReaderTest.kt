package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.InvalidPackageTextEncodingException
import vn.loi.learning.application.contentpackaging.PackageEntryTooLargeException

class JvmOpd3EntryReaderTest {

    @Test
    fun `reads UTF-8 text at configured byte limit`() {
        withArchive(
            entryName = "manifest.json",
            content = "éé".toByteArray(Charsets.UTF_8)
        ) { archive ->
            val reader =
                JvmOpd3EntryReader(
                    Opd3EntryReadLimits(
                        maximumTextEntryBytes = 4
                    )
                )

            assertEquals(
                "éé",
                reader.readText(
                    archive = archive,
                    entryName = "manifest.json"
                )
            )
        }
    }

    @Test
    fun `rejects text entry larger than configured byte limit`() {
        withArchive(
            entryName = "contents.json",
            content = "12345".toByteArray()
        ) { archive ->
            val reader =
                JvmOpd3EntryReader(
                    Opd3EntryReadLimits(
                        maximumTextEntryBytes = 4
                    )
                )

            val exception =
                assertFailsWith<PackageEntryTooLargeException> {
                    reader.readText(
                        archive = archive,
                        entryName = "contents.json"
                    )
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains("contents.json")
            )
            assertTrue(
                exception.message.orEmpty()
                    .contains("4 bytes")
            )
        }
    }

    @Test
    fun `rejects malformed UTF-8 text`() {
        withArchive(
            entryName = "metadata.json",
            content = byteArrayOf(
                0xC3.toByte(),
                0x28
            )
        ) { archive ->
            val reader = JvmOpd3EntryReader()

            val exception =
                assertFailsWith<InvalidPackageTextEncodingException> {
                    reader.readText(
                        archive = archive,
                        entryName = "metadata.json"
                    )
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains("metadata.json")
            )
        }
    }

    @Test
    fun `returns null for missing entry`() {
        withArchive(
            entryName = "manifest.json",
            content = "{}".toByteArray()
        ) { archive ->
            assertNull(
                JvmOpd3EntryReader().readText(
                    archive = archive,
                    entryName = "missing.json"
                )
            )
        }
    }

    @Test
    fun `returns null when requested entry is a directory`() {
        val archivePath =
            Files.createTempFile(
                "opd3-directory-entry-",
                ".zip"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(archivePath)
            ).use { output ->
                output.putNextEntry(
                    ZipEntry("manifest.json/")
                )
                output.closeEntry()
            }

            ZipFile(archivePath.toFile()).use { archive ->
                assertNull(
                    JvmOpd3EntryReader().readText(
                        archive = archive,
                        entryName = "manifest.json/"
                    )
                )
            }
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }

    private fun withArchive(
        entryName: String,
        content: ByteArray,
        assertion: (ZipFile) -> Unit
    ) {
        val archivePath =
            Files.createTempFile(
                "opd3-entry-reader-",
                ".zip"
            )

        try {
            ZipOutputStream(
                Files.newOutputStream(archivePath)
            ).use { output ->
                output.putNextEntry(
                    ZipEntry(entryName)
                )
                output.write(content)
                output.closeEntry()
            }

            ZipFile(archivePath.toFile()).use(assertion)
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }
}
