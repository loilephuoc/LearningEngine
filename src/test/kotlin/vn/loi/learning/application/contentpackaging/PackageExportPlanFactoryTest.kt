package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageExportPlanFactoryTest {

    @Test
    fun `creates export plan with metadata contents learning items and manifest files`() {
        val payload = PackageExportPayload(
            descriptor = PackageDescriptor(name = "demo-package", version = "1.0.0", format = "OPD3"),
            contents = emptyList(),
            learningItems = emptyList()
        )

        val plan = PackageExportPlanFactory().create(payload)

        assertEquals(4, plan.files.size)
        assertEquals(listOf("metadata.json", "contents.json", "learning-items.json", "manifest.json"), plan.files.map { it.relativePath })
        assertTrue(plan.files.first { it.relativePath == "metadata.json" }.content.contains("\"name\""))
        assertTrue(plan.files.first { it.relativePath == "contents.json" }.content.contains("\"contents\""))
        assertTrue(plan.files.first { it.relativePath == "learning-items.json" }.content.contains("\"learningItems\""))
        assertTrue(plan.files.first { it.relativePath == "manifest.json" }.content.contains("\"name\""))
    }
}
