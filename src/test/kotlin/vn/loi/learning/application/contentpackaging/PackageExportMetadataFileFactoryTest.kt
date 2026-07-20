package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportMetadataFileFactoryTest {

    @Test
    fun `creates metadata file`() {
        val descriptor = PackageDescriptor(
            name = "demo-package",
            version = "1.0.0",
            format = "OPD3"
        )

        val file = PackageExportMetadataFileFactory().create(descriptor)

        assertEquals("metadata.json", file.relativePath)
        assertTrue(file.content.contains("\"name\""))
        assertTrue(file.content.contains("\"demo-package\""))
        assertTrue(file.content.contains("\"format\": \"OPD3\""))
    }
}
