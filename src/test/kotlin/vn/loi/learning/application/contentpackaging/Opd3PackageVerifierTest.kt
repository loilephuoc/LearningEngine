package vn.loi.learning.application.contentpackaging

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.topic.model.TopicId

class Opd3PackageVerifierTest {

    private val topicId = TopicId.deriveForLegacyPackage("VerifyTopic", "OPD3")
    private val content1 = Content(id = ContentId("c1"), type = ContentType.WORD, text = ContentText(primaryText = "Bird"))
    private val samplePackage = CanonicalTopicPackage(
        topicId = topicId,
        logicalTopicName = "VerifyTopic",
        sourceMetadata = LegacyTopicSourceMetadata("VerifyTopic", "json", "pkg"),
        contents = listOf(content1),
        learningItems = emptyList(),
        mediaReferences = emptyList()
    )

    private val exporter = Opd3PackageExporter()
    private val verifier = Opd3PackageVerifier()

    @Test
    fun `verifier validates exported valid OPD3 package successfully`() {
        val exportResult = exporter.export(samplePackage)
        val report = verifier.verify(exportResult.zipBytes)

        assertTrue(report.isValid)
        assertEquals(emptyList(), report.errors)
    }

    @Test
    fun `verifier detects corrupted checksum in manifest`() {
        val exportResult = exporter.export(samplePackage)

        // Create zip with corrupted manifest
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("contents.json"))
            zip.write("[]".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("learning-items.json"))
            zip.write("[]".toByteArray())
            zip.closeEntry()

            val badManifest = """{"schemaVersion":"1.0","files":{"contents.json":"0000000000000000000000000000000000000000000000000000000000000000"}}"""
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(badManifest.toByteArray())
            zip.closeEntry()
        }

        val report = verifier.verify(outputStream.toByteArray())

        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Checksum mismatch") })
    }

    @Test
    fun `verifier detects schema mismatch`() {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            val badMetadata = """{"schemaVersion":"99.0","packageId":"p1","name":"Test","version":"1","format":"OPD3","topicId":"t1","tags":[]}"""
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(badMetadata.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("contents.json"))
            zip.write("[]".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("learning-items.json"))
            zip.write("[]".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"schemaVersion":"99.0","files":{}}""".toByteArray())
            zip.closeEntry()
        }

        val report = verifier.verify(outputStream.toByteArray())

        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Unsupported schema version") })
    }
}
