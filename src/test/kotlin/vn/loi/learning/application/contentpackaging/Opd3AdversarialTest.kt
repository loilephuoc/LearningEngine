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
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode

class Opd3AdversarialTest {

    private val topicId = TopicId.deriveForLegacyPackage("AdversarialTopic", "OPD3")
    private val content1 = Content(id = ContentId("adv-1"), type = ContentType.WORD, text = ContentText(primaryText = "Apple"))
    private val content2 = Content(id = ContentId("adv-2"), type = ContentType.WORD, text = ContentText(primaryText = "Banana"))

    private val item1 = LearningItem(id = LearningItemId("adv-1-mode"), contentId = content1.id, mode = LearningMode.MEANING_RECOGNITION)
    private val item2 = LearningItem(id = LearningItemId("adv-2-mode"), contentId = content2.id, mode = LearningMode.MEANING_RECOGNITION)

    private val exporter = Opd3PackageExporter(zipWriter = vn.loi.learning.infrastructure.contentpackaging.JvmDeterministicZipWriter())
    private val inspector = Opd3PackageInspector()
    private val verifier = Opd3PackageVerifier()

    // 1. identical logical package with different insertion order exports identical bytes
    @Test
    fun `1 identical logical package with different insertion order exports identical bytes`() {
        val pkg1 = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "OrderTest",
            sourceMetadata = LegacyTopicSourceMetadata("OrderTest", "json", "pkg"),
            contents = listOf(content1, content2),
            learningItems = listOf(item1, item2),
            mediaReferences = emptyList(),
            tags = setOf("fruit", "apple", "food")
        )

        val pkg2 = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "OrderTest",
            sourceMetadata = LegacyTopicSourceMetadata("OrderTest", "json", "pkg"),
            contents = listOf(content2, content1),
            learningItems = listOf(item2, item1),
            mediaReferences = emptyList(),
            tags = setOf("food", "fruit", "apple")
        )

        val result1 = exporter.export(pkg1)
        val result2 = exporter.export(pkg2)

        assertEquals(result1.sha256Checksum, result2.sha256Checksum)
        assertTrue(result1.zipBytes.contentEquals(result2.zipBytes))
    }

    // 2. unsafe .. media path rejected
    @Test
    fun `2 unsafe dot dot media path rejected`() {
        val badRef = CanonicalMediaReference(
            referencedAsset = "../secret.txt",
            logicalPath = "../secret.txt",
            mediaType = CanonicalMediaType.AUDIO,
            owningContentId = content1.id,
            status = CanonicalMediaStatus.PRESENT
        )
        val pkg = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "PathTest",
            sourceMetadata = LegacyTopicSourceMetadata("PathTest", "json", "pkg"),
            contents = listOf(content1),
            learningItems = emptyList(),
            mediaReferences = listOf(badRef)
        )

        val collector = PackageMediaAssetCollector(mediaByteReader = { _, _ -> "bytes".toByteArray() })
        val bundle = collector.collect(pkg)

        assertTrue(bundle.diagnostics.any { it.message.contains("Unsafe media path") })
    }

    // 3. unsafe backslash traversal rejected
    @Test
    fun `3 unsafe backslash traversal rejected`() {
        val badRef = CanonicalMediaReference(
            referencedAsset = "audio\\hello.mp3",
            logicalPath = "audio\\hello.mp3",
            mediaType = CanonicalMediaType.AUDIO,
            owningContentId = content1.id,
            status = CanonicalMediaStatus.PRESENT
        )
        val pkg = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "BackslashTest",
            sourceMetadata = LegacyTopicSourceMetadata("BackslashTest", "json", "pkg"),
            contents = listOf(content1),
            learningItems = emptyList(),
            mediaReferences = listOf(badRef)
        )

        val collector = PackageMediaAssetCollector(mediaByteReader = { _, _ -> "bytes".toByteArray() })
        val bundle = collector.collect(pkg)

        assertTrue(bundle.diagnostics.any { it.message.contains("Unsafe media path") })
    }

    // 4. absolute path rejected
    @Test
    fun `4 absolute path rejected`() {
        val badRef = CanonicalMediaReference(
            referencedAsset = "/etc/passwd",
            logicalPath = "/etc/passwd",
            mediaType = CanonicalMediaType.AUDIO,
            owningContentId = content1.id,
            status = CanonicalMediaStatus.PRESENT
        )
        val pkg = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "AbsoluteTest",
            sourceMetadata = LegacyTopicSourceMetadata("AbsoluteTest", "json", "pkg"),
            contents = listOf(content1),
            learningItems = emptyList(),
            mediaReferences = listOf(badRef)
        )

        val collector = PackageMediaAssetCollector(mediaByteReader = { _, _ -> "bytes".toByteArray() })
        val bundle = collector.collect(pkg)

        assertTrue(bundle.diagnostics.any { it.message.contains("Unsafe media path") })
    }

    // 5. duplicate ZIP entry rejected
    @Test
    fun `5 duplicate ZIP entry rejected`() {
        val result = kotlin.runCatching {
            buildCustomZip(
                "metadata.json" to validMetadataJson(),
                "contents.json" to "[]",
                "learning-items.json" to "[]",
                "media-manifest.json" to """{"entries":[]}""",
                "metadata.json" to validMetadataJson(),
                "manifest.json" to """{"schemaVersion":"1.0","files":{}}"""
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.util.zip.ZipException)
    }

    // 6. duplicate manifest path rejected (Task 4 Option A)
    @Test
    fun `6 duplicate manifest path detected and rejected`() {
        val manifestWithDupKey = """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","contents.json":"${hash("[]")}"}}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to manifestWithDupKey
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Duplicate key detected in manifest.json files map") })
    }

    // 7. unlisted extra archive entry detected
    @Test
    fun `7 unlisted extra archive entry detected`() {
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "extra.txt" to "unauthorized data",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash("""{"entries":[]}""")} "}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Unlisted extra archive entry detected") })
    }

    // 8. listed but missing entry detected
    @Test
    fun `8 listed but missing entry detected`() {
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash("""{"entries":[]}""")}","missing.json":"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Manifest references non-existent file") })
    }

    // 9. malformed checksum rejected
    @Test
    fun `9 malformed checksum rejected`() {
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"INVALID-HASH-STRING"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Malformed SHA-256 checksum format") })
    }

    // 10. corrupted content file detected
    @Test
    fun `10 corrupted content file detected`() {
        val declaredHash = hash("uncorrupted-content-payload")
        val metaHash = hash(validMetadataJson())
        val emptyArrayHash = hash("[]")
        val emptyMediaManifestHash = hash("""{"entries":[]}""")
        val manifestJson = """{"schemaVersion":"1.0","files":{"metadata.json":"$metaHash","contents.json":"$declaredHash","learning-items.json":"$emptyArrayHash","media-manifest.json":"$emptyMediaManifestHash"}}"""

        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to manifestJson
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Checksum mismatch for file 'contents.json'") })
    }

    // 11. corrupted media file detected
    @Test
    fun `11 corrupted media file detected`() {
        val realHash = hash("original-audio")
        val mediaManifest = """{"entries":[{"logicalPath":"media/audio.mp3","mediaType":"AUDIO","size":14,"sha256":"$realHash","owningContentIds":[]}]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to mediaManifest,
            "media/audio.mp3" to "corrupted-audio",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash(mediaManifest)}","media/audio.mp3":"$realHash"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Checksum mismatch for file 'media/audio.mp3'") })
    }

    // 12. malformed JSON produces fatal diagnostic
    @Test
    fun `12 malformed JSON produces fatal diagnostic`() {
        val zipBytes = buildCustomZip(
            "metadata.json" to "{ bad json content",
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{}}"""
        )

        val inspection = inspector.inspect(zipBytes)
        assertFalse(inspection.isValid)
        assertTrue(inspection.errors.any { it.contains("Malformed 'metadata.json'") })
    }

    // 13. unsupported schema produces fatal diagnostic
    @Test
    fun `13 unsupported schema produces fatal diagnostic`() {
        val badMetadata = """{"schemaVersion":"99.0","packageId":"p1","name":"Test","version":"1","format":"OPD3","topicId":"t1","tags":[]}"""
        val mediaManifest = """{"entries":[]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to badMetadata,
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to mediaManifest,
            "manifest.json" to """{"schemaVersion":"99.0","files":{"metadata.json":"${hash(badMetadata)}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash(mediaManifest)}"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Unsupported schema version") })
    }

    // 14. missing required OPD3 entry detected
    @Test
    fun `14 missing required OPD3 entry detected`() {
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{}}"""
        )

        val inspection = inspector.inspect(zipBytes)
        assertFalse(inspection.isValid)
        assertTrue(inspection.errors.any { it.contains("Missing required entry 'contents.json'") })
    }

    // 15. orphan media detected or explicitly classified
    @Test
    fun `15 orphan media detected or explicitly classified`() {
        val mediaManifest = """{"entries":[{"logicalPath":"media/audio1.mp3","mediaType":"AUDIO","size":6,"sha256":"${hash("audio1")}","owningContentIds":[]}]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to mediaManifest,
            "media/audio1.mp3" to "audio1",
            "media/orphan.mp3" to "orphan-bytes",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash(mediaManifest)}","media/audio1.mp3":"${hash("audio1")}","media/orphan.mp3":"${hash("orphan-bytes")}"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertTrue(report.warnings.any { it.contains("Orphan media entry detected") })
    }

    // 16. media reference to missing asset detected
    @Test
    fun `16 media reference to missing asset detected`() {
        val mediaManifest = """{"entries":[{"logicalPath":"media/missing.mp3","mediaType":"AUDIO","size":10,"sha256":"${hash("missing")}","owningContentIds":[]}]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to mediaManifest,
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(validMetadataJson())}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash(mediaManifest)}"}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Media manifest references absent media asset") })
    }

    // 17. excessive entry count rejected
    @Test
    fun `17 excessive entry count rejected`() {
        val customInspector = Opd3PackageInspector(limits = PackageSafetyLimits(maxEntryCount = 5))
        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "extra1.txt" to "1",
            "extra2.txt" to "2",
            "manifest.json" to """{"schemaVersion":"1.0","files":{}}"""
        )

        val inspection = customInspector.inspect(zipBytes)
        assertFalse(inspection.isValid)
        assertTrue(inspection.errors.any { it.contains("exceeds maximum entry count limit") })
    }

    // 18. Task 3: Real streaming excessive uncompressed size rejected
    @Test
    fun `18 real streaming excessive uncompressed size rejected`() {
        val smallLimits = PackageSafetyLimits(maxSingleEntrySizeBytes = 20L)
        val customInspector = Opd3PackageInspector(limits = smallLimits)

        val zipBytes = buildCustomZip(
            "metadata.json" to validMetadataJson(),
            "large_entry.bin" to "this entry contains 50 bytes of data which is over 20 bytes limit"
        )

        val inspection = customInspector.inspect(zipBytes)
        assertFalse(inspection.isValid)
        assertTrue(inspection.errors.any { it.contains("exceeds single entry size limit") })
    }

    // 19. Unicode file path behaves deterministically
    @Test
    fun `19 Unicode file path behaves deterministically`() {
        val unicodeRef = CanonicalMediaReference(
            referencedAsset = "âm_thanh/tiếng_mèo.mp3",
            logicalPath = "âm_thanh/tiếng_mèo.mp3",
            mediaType = CanonicalMediaType.AUDIO,
            owningContentId = content1.id,
            status = CanonicalMediaStatus.PRESENT
        )

        val pkg = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "UnicodeTopic",
            sourceMetadata = LegacyTopicSourceMetadata("UnicodeTopic", "json", "pkg"),
            contents = listOf(content1),
            learningItems = emptyList(),
            mediaReferences = listOf(unicodeRef)
        )

        val collector = PackageMediaAssetCollector(mediaByteReader = { _, _ -> "unicode-audio".toByteArray(Charsets.UTF_8) })
        val bundle = collector.collect(pkg)

        val result1 = exporter.export(pkg, bundle)
        val result2 = exporter.export(pkg, bundle)

        assertEquals(result1.sha256Checksum, result2.sha256Checksum)
        assertTrue(result1.zipBytes.contentEquals(result2.zipBytes))
    }

    // 20. valid package still passes end to end round trip
    @Test
    fun `20 valid package still passes end to end round trip`() {
        val pkg = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "RoundTripAdversarial",
            sourceMetadata = LegacyTopicSourceMetadata("RoundTripAdversarial", "json", "pkg"),
            contents = listOf(content1, content2),
            learningItems = listOf(item1, item2),
            mediaReferences = emptyList()
        )

        val exportResult = exporter.export(pkg)
        val inspection = inspector.inspect(exportResult.zipBytes)
        val report = verifier.verify(exportResult.zipBytes)

        assertTrue(inspection.isValid)
        assertTrue(report.isValid)
        assertEquals(emptyList(), report.errors)
    }

    // 21. Task 5: Format validation rejects non-OPD3 format
    @Test
    fun `21 format validation rejects non OPD3 format`() {
        val invalidFormatMetadata = """{"schemaVersion":"1.0","packageId":"p1","name":"Test","version":"1","format":"ZIP","topicId":"${topicId.value}","tags":[]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to invalidFormatMetadata,
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(invalidFormatMetadata)}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash("""{"entries":[]}""")}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Unsupported package format: 'ZIP'") })
    }

    // 22. Task 6: TopicId mandatory validation rejects missing TopicId
    @Test
    fun `22 mandatory TopicId validation rejects missing TopicId`() {
        val missingTopicIdMetadata = """{"schemaVersion":"1.0","packageId":"p1","name":"Test","version":"1","format":"OPD3","topicId":"","tags":[]}"""
        val zipBytes = buildCustomZip(
            "metadata.json" to missingTopicIdMetadata,
            "contents.json" to "[]",
            "learning-items.json" to "[]",
            "media-manifest.json" to """{"entries":[]}""",
            "manifest.json" to """{"schemaVersion":"1.0","files":{"metadata.json":"${hash(missingTopicIdMetadata)}","contents.json":"${hash("[]")}","learning-items.json":"${hash("[]")}","media-manifest.json":"${hash("""{"entries":[]}""")}}"""
        )

        val report = verifier.verify(zipBytes)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.contains("Missing or invalid mandatory TopicId") })
    }

    private fun validMetadataJson(): String =
        """{"schemaVersion":"1.0","packageId":"p1","name":"AdversarialTopic","version":"1","format":"OPD3","topicId":"${topicId.value}","tags":[]}"""

    private fun hash(text: String): String =
        Sha256PackageIntegrityHasher().hash(text)

    private fun buildCustomZip(vararg entries: Pair<String, String>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return outputStream.toByteArray()
    }
}
