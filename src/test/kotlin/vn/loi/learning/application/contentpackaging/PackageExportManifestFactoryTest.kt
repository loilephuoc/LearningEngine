package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportManifestFactoryTest {

    @Test
    fun `creates manifest from payload`() {
        val payload = PackageExportPayload(
            descriptor = PackageDescriptor(name = "demo-package", version = "1.0.0", format = "OPD3"),
            contents = emptyList(),
            learningItems = emptyList()
        )

        val manifest = PackageExportManifestFactory().create(payload)

        assertEquals("demo-package", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("OPD3", manifest.format)
        assertEquals(0, manifest.contentCount)
        assertEquals(0, manifest.learningItemCount)
    }
}
