package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportServiceTest {

    @Test
    fun `delegates export to exporter`() {
        var receivedPayload: PackageExportPayload? = null
        var receivedManifest: PackageExportManifest? = null
        var receivedDestination: String? = null

        val exporter = PackageContentExporter { payload, manifest, destination ->
            receivedPayload = payload
            receivedManifest = manifest
            receivedDestination = destination
        }

        val service = PackageExportService(exporter)
        val payload = PackageExportPayload(
            descriptor = PackageDescriptor(name = "test-package", version = "1.0.0", format = "OPD3"),
            contents = emptyList(),
            learningItems = emptyList()
        )

        val result = service.export(payload, "package.zip")

        assertEquals(payload, receivedPayload)
        assertEquals("test-package", receivedManifest?.name)
        assertEquals("package.zip", receivedDestination)
        assertEquals(4, result.files.size)
        assertEquals(listOf("metadata.json","contents.json","learning-items.json","manifest.json"), result.files.map { it.relativePath })
        assertEquals("metadata.json", result.files.first().relativePath)
        assertEquals("manifest.json", result.files.last().relativePath)
        assertEquals(listOf("metadata.json","contents.json","learning-items.json","manifest.json"), result.files.map { it.relativePath })
        assertEquals("metadata.json", result.files.first().relativePath)
        assertEquals("manifest.json", result.files.last().relativePath)
    }
}
