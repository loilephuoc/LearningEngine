package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageExportFileFactoryTest {

    @Test
    fun `creates manifest file`() {
        val manifest = PackageExportManifest(
            name = "demo-package",
            version = "1.0.0",
            format = "OPD3",
            contentCount = 12,
            learningItemCount = 48
        )

        val file = PackageExportManifestFileFactory().create(manifest)

        assertEquals("manifest.json", file.relativePath)
        assertTrue(file.content.contains("\"name\""))
        assertTrue(file.content.contains("\"demo-package\""))
        assertTrue(file.content.contains("\"format\": \"OPD3\""))
    }
}
