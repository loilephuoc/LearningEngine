package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.Content

class PackageExportContentsJsonTest {

    @Test
    fun `creates contents dto from domain contents`() {
        val contents = emptyList<Content>()

        val dto = PackageExportContentsJson.from(contents)

        assertEquals(0, dto.contents.size)
    }
}
