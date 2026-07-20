package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PackageExportBundleTest {

    @Test
    fun `keeps exported files`() {
        val files = listOf(
            PackageExportFile("metadata.json","{}"),
            PackageExportFile("manifest.json","{}")
        )

        val bundle = PackageExportBundle(files)

        assertEquals(files, bundle.files)
        assertEquals(2, bundle.files.size)
        assertEquals(2, bundle.size)
        assertEquals(false, bundle.isEmpty())
        assertEquals(true, bundle.isNotEmpty())
        assertEquals(listOf("metadata.json","manifest.json"), bundle.paths())
        assertEquals("{}", bundle.file("metadata.json")?.content)
        assertEquals(null, bundle.file("missing.json"))
        assertEquals(listOf("{}", "{}"), bundle.requireFiles("metadata.json", "manifest.json").map { it.content })
        assertEquals(listOf("metadata.json"), bundle.contentFiles().map { it.relativePath })
        assertEquals("{}", bundle.manifestFile().content)
        assertEquals(listOf("metadata.json"), bundle.withoutManifest().map { it.relativePath })
        assertEquals(listOf("metadata.json","manifest.json"), bundle.filePaths)
        assertTrue(bundle.contains("metadata.json"))
        assertEquals("{}", bundle.requireFile("manifest.json").content)
    }

    @Test
    fun `rejects duplicate file paths`() {
        assertFailsWith<IllegalArgumentException> {
            PackageExportBundle(
                listOf(
                    PackageExportFile("manifest.json","{}"),
                    PackageExportFile("manifest.json","{}")
                )
            )
        }
    }
}
