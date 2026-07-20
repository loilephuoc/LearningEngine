package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItem

class PackageExportLearningItemsFileFactoryTest {

    @Test
    fun `creates learning items file`() {
        val file = PackageExportLearningItemsFileFactory().create(emptyList<LearningItem>())

        assertEquals("learning-items.json", file.relativePath)
        assertTrue(file.content.contains("\"learningItems\""))
        assertTrue(file.content.contains("[]"))
    }
}
