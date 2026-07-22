package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.InvalidPackageArchiveStructureException

class BundlePackageReaderTest {

    @Test
    fun `validates archive structure before reading required content`() {
        val archivePath = Files.createTempFile(
            "bundle-structure-",
            ".opd3"
        )

        try {
            ZipOutputStream(
                Files.newOutputStream(archivePath)
            ).use { output ->
                output.putNextEntry(
                    ZipEntry("../manifest.json")
                )
                output.write("{}".toByteArray())
                output.closeEntry()
            }

            assertFailsWith<InvalidPackageArchiveStructureException> {
                BundlePackageReader(
                    archiveReader = JvmOpd3ArchiveReader(),
                    entryReader = Opd3EntryReader { _, _ ->
                        error("Entry content must not be read before structure validation.")
                    }
                ).read(archivePath)
            }
        } finally {
            Files.deleteIfExists(archivePath)
        }
    }

    private val reader = BundlePackageReader(
        archiveReader = JvmOpd3ArchiveReader(),
        entryReader = JvmOpd3EntryReader()
    )

    @Test
    fun `reads all required bundle files`() {
        val zipPath = Files.createTempFile("bundle-package-", ".zip")

        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            requiredFiles().forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }

        val bundle = reader.read(zipPath)

        assertEquals("{\"metadata\":true}", bundle.metadataJson())
        assertEquals("{\"contents\":true}", bundle.contentsJson())
        assertEquals("{\"learningItems\":true}", bundle.learningItemsJson())
        assertEquals("{\"manifest\":true}", bundle.manifestJson())
    }

    @Test
    fun `reports missing required bundle file`() {
        val zipPath = Files.createTempFile("bundle-package-missing-", ".zip")

        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            requiredFiles()
                .filterKeys { it != "learning-items.json" }
                .forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
        }

        val error = assertFailsWith<IllegalArgumentException> {
            reader.read(zipPath)
        }

        assertTrue(error.message.orEmpty().contains("learning-items.json"))
    }

    private fun requiredFiles(): Map<String, String> = linkedMapOf(
        "metadata.json" to "{\"metadata\":true}",
        "contents.json" to "{\"contents\":true}",
        "learning-items.json" to "{\"learningItems\":true}",
        "manifest.json" to "{\"manifest\":true}"
    )
}
