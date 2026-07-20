package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content

class PackageExportContentsFileFactoryTest {

    @Test
    fun `creates contents file`() {
        val file = PackageExportContentsFileFactory().create(emptyList<Content>())

        assertEquals("contents.json", file.relativePath)
        assertTrue(file.content.contains("\"contents\""))
        assertTrue(file.content.contains("[]"))
    }
}
