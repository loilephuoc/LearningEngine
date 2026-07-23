package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
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

class Opd3DeterministicExporterTest {

    private val topicId = TopicId.deriveForLegacyPackage("ExportTopic", "OPD3")
    private val content1 = Content(id = ContentId("c1"), type = ContentType.WORD, text = ContentText(primaryText = "Cat", translatedText = "Con mèo"))
    private val item1 = LearningItem(id = LearningItemId("c1-meaning-recognition"), contentId = content1.id, mode = LearningMode.MEANING_RECOGNITION)

    private val samplePackage = CanonicalTopicPackage(
        topicId = topicId,
        logicalTopicName = "ExportTopic",
        sourceMetadata = LegacyTopicSourceMetadata("ExportTopic", "ExportTopic.json", "ExportTopic.pkg"),
        contents = listOf(content1),
        learningItems = listOf(item1),
        mediaReferences = emptyList()
    )

    private val exporter = Opd3PackageExporter()

    @Test
    fun `export is byte-for-byte deterministic across repeated runs`() {
        val result1 = exporter.export(samplePackage)
        val result2 = exporter.export(samplePackage)

        assertEquals(result1.sha256Checksum, result2.sha256Checksum)
        assertTrue(result1.zipBytes.contentEquals(result2.zipBytes))
        assertEquals(result1.entryHashes, result2.entryHashes)
    }

    @Test
    fun `export empty package creates valid deterministic zip structure`() {
        val emptyPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "EmptyTopic",
            sourceMetadata = LegacyTopicSourceMetadata("EmptyTopic", "json", "pkg"),
            contents = emptyList(),
            learningItems = emptyList(),
            mediaReferences = emptyList()
        )

        val result = exporter.export(emptyPackage)

        assertNotNull(result.zipBytes)
        assertTrue(result.zipBytes.isNotEmpty())
        assertTrue(result.entryHashes.containsKey("metadata.json"))
        assertTrue(result.entryHashes.containsKey("contents.json"))
        assertTrue(result.entryHashes.containsKey("learning-items.json"))
        assertTrue(result.entryHashes.containsKey("manifest.json"))
    }

    @Test
    fun `export large package creates deterministic zip output`() {
        val largeContents = (1..500).map { i ->
            Content(id = ContentId("c-$i"), type = ContentType.WORD, text = ContentText(primaryText = "Word $i"))
        }
        val largeItems = largeContents.map { c ->
            LearningItem(id = LearningItemId("${c.id.value}-meaning-recognition"), contentId = c.id, mode = LearningMode.MEANING_RECOGNITION)
        }

        val largePackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "LargeTopic",
            sourceMetadata = LegacyTopicSourceMetadata("LargeTopic", "json", "pkg"),
            contents = largeContents,
            learningItems = largeItems,
            mediaReferences = emptyList()
        )

        val result1 = exporter.export(largePackage)
        val result2 = exporter.export(largePackage)

        assertEquals(result1.sha256Checksum, result2.sha256Checksum)
        assertTrue(result1.zipBytes.contentEquals(result2.zipBytes))
    }
}
