package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.io.path.createTempDirectory
import vn.loi.learning.infrastructure.contentpackaging.PackageZipWriter
import java.io.File
import java.util.zip.ZipFile

class PackageExportIntegrationTest {

    @Test
    fun `exports complete package zip`() {
        val exporter = PackageContentExporter { _, _, _ -> }
        val service = PackageExportService(exporter)
        val payload = PackageExportPayload(
            descriptor = vn.loi.learning.domain.content.packaging.model.PackageDescriptor("demo", "1.0.0", "OPD3"),
            contents = emptyList(),
            learningItems = emptyList()
        )

        val bundle = service.export(payload, "ignored.zip")
        val zip = File(createTempDirectory().toFile(), "package.zip")

        PackageZipWriter().write(bundle, zip)

        ZipFile(zip).use { archive ->
            assertEquals(4, archive.size())
        }
    }
}
