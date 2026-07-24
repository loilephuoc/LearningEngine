package vn.loi.learning.infrastructure.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.CanonicalMediaReference
import vn.loi.learning.application.contentpackaging.CanonicalMediaStatus
import vn.loi.learning.application.contentpackaging.CanonicalMediaType
import vn.loi.learning.application.contentpackaging.CanonicalTopicPackage
import vn.loi.learning.application.contentpackaging.LegacyTopicSourceMetadata
import vn.loi.learning.application.contentpackaging.Opd3PackageExporter
import vn.loi.learning.application.contentpackaging.Opd3PackageInspector
import vn.loi.learning.application.contentpackaging.Opd3PackageVerifier
import vn.loi.learning.application.contentpackaging.PackageMediaAssetCollector
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class PackagePlatformRoundTripTest {

    @Test
    fun `end to end round trip CanonicalTopicPackage to Media Packaging to OPD3 Export to Inspector to Verification`() {
        val topicId = TopicId.deriveForLegacyPackage("RoundTripTopic", "OPD3")
        val contentId = ContentId("rt-content-1")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(primaryText = "Sun", translatedText = "Mặt trời"),
            media = ContentMedia(primaryAudio = "sun.mp3", image = "sun.png")
        )

        val learningItem = LearningItem(
            id = LearningItemId("rt-content-1-meaning-recognition"),
            contentId = contentId,
            mode = LearningMode.MEANING_RECOGNITION
        )

        val canonicalPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "RoundTripTopic",
            sourceMetadata = LegacyTopicSourceMetadata("RoundTripTopic", "json", "pkg"),
            contents = listOf(content),
            learningItems = listOf(learningItem),
            mediaReferences = listOf(
                CanonicalMediaReference("sun.mp3", "sun.mp3", CanonicalMediaType.AUDIO, contentId, status = CanonicalMediaStatus.PRESENT),
                CanonicalMediaReference("sun.png", "sun.png", CanonicalMediaType.IMAGE, contentId, status = CanonicalMediaStatus.PRESENT)
            )
        )

        // 1. Media Packaging
        val collector = PackageMediaAssetCollector(
            mediaByteReader = { _, assetPath ->
                when (assetPath) {
                    "sun.mp3" -> "mp3-audio-bytes".toByteArray(Charsets.UTF_8)
                    "sun.png" -> "png-image-bytes".toByteArray(Charsets.UTF_8)
                    else -> null
                }
            }
        )

        val mediaBundle = collector.collect(canonicalPackage)
        assertEquals(2, mediaBundle.assetCount)
        assertEquals(2, mediaBundle.manifest.entries.size)

        // 2. OPD3 Export
        val exporter = Opd3PackageExporter(zipWriter = JvmDeterministicZipWriter())
        val exportResult1 = exporter.export(canonicalPackage, mediaBundle)
        val exportResult2 = exporter.export(canonicalPackage, mediaBundle)

        // Verify 100% byte-for-byte determinism
        assertEquals(exportResult1.sha256Checksum, exportResult2.sha256Checksum)
        assertTrue(exportResult1.zipBytes.contentEquals(exportResult2.zipBytes))

        // 3. Package Inspector
        val inspector = Opd3PackageInspector()
        val inspection = inspector.inspect(exportResult1.zipBytes)

        assertTrue(inspection.isValid)
        assertEquals("1.0", inspection.schemaVersion)
        assertEquals(topicId, inspection.topicId)
        assertEquals("RoundTripTopic", inspection.topicName)
        assertEquals(1, inspection.contentCount)
        assertEquals(1, inspection.learningItemCount)
        assertEquals(2, inspection.mediaCount)

        // 4. Verification
        val verifier = Opd3PackageVerifier()
        val report = verifier.verify(exportResult1.zipBytes)

        assertTrue(report.isValid)
        assertEquals(emptyList(), report.errors)
    }
}
