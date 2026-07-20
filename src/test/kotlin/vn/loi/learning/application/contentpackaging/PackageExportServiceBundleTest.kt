package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportServiceBundleTest {

    @Test
    fun `returns complete export bundle`() {
        val exporter = PackageContentExporter { _, _, _ -> }
        val service = PackageExportService(exporter)
        val payload = PackageExportPayload(
            descriptor = PackageDescriptor(name = "demo-package", version = "1.0.0", format = "OPD3"),
            contents = emptyList(),
            learningItems = emptyList()
        )

        val bundle = service.export(payload, "package.zip")

        assertEquals(4, bundle.files.size)
        assertEquals(listOf("metadata.json", "contents.json", "learning-items.json", "manifest.json"), bundle.filePaths)
        assertEquals(true, bundle.contains("manifest.json"))
    }
}
