package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals

class PackageExportManifestJsonTest {

    @Test
    fun `creates json dto from manifest`() {
        val manifest = PackageExportManifest(
            name = "demo-package",
            version = "1.0.0",
            format = "OPD3",
            contentCount = 12,
            learningItemCount = 48
        )

        val json = PackageExportManifestJson.from(manifest)

        assertEquals("demo-package", json.name)
        assertEquals("1.0.0", json.version)
        assertEquals("OPD3", json.format)
        assertEquals(12, json.contentCount)
        assertEquals(48, json.learningItemCount)
    }
}
