package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class Opd3PackageInspectorTest {

    private val topicId = TopicId.deriveForLegacyPackage("InspectTopic", "OPD3")
    private val content1 = Content(id = ContentId("c1"), type = ContentType.WORD, text = ContentText(primaryText = "Dog"))
    private val item1 = LearningItem(id = LearningItemId("c1-meaning-recognition"), contentId = content1.id, mode = LearningMode.MEANING_RECOGNITION)

    private val canonicalPackage = CanonicalTopicPackage(
        topicId = topicId,
        logicalTopicName = "InspectTopic",
        sourceMetadata = LegacyTopicSourceMetadata("InspectTopic", "json", "pkg"),
        contents = listOf(content1),
        learningItems = listOf(item1),
        mediaReferences = emptyList()
    )

    private val exporter = Opd3PackageExporter()
    private val inspector = Opd3PackageInspector()

    @Test
    fun `inspect valid OPD3 package returns accurate summary stats and counts`() {
        val exportResult = exporter.export(canonicalPackage)
        val inspection = inspector.inspect(exportResult.zipBytes)

        assertTrue(inspection.isValid)
        assertEquals("1.0", inspection.schemaVersion)
        assertEquals("1", inspection.packageVersion)
        assertEquals(topicId, inspection.topicId)
        assertEquals("InspectTopic", inspection.topicName)
        assertEquals(1, inspection.contentCount)
        assertEquals(1, inspection.learningItemCount)
        assertTrue(inspection.checksums.containsKey("metadata.json"))
        assertTrue(inspection.checksums.containsKey("contents.json"))
    }

    @Test
    fun `inspect corrupted bytes reports error diagnostic`() {
        val invalidBytes = "not-a-zip-archive".toByteArray(Charsets.UTF_8)
        val inspection = inspector.inspect(invalidBytes)

        assertFalse(inspection.isValid)
        assertTrue(inspection.errors.isNotEmpty())
        assertTrue(inspection.errors.any { it.contains("Invalid ZIP archive") })
    }
}
