package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId

class MediaPackagingTest {

    private val topicId = TopicId.deriveForLegacyPackage("MediaTopic", "OPD3")
    private val contentId1 = ContentId("content-1")
    private val contentId2 = ContentId("content-2")

    @Test
    fun `media packaging collects present assets and computes sha256 checksums`() {
        val canonicalPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "MediaTopic",
            sourceMetadata = LegacyTopicSourceMetadata("MediaTopic", "json", "pkg"),
            contents = emptyList(),
            learningItems = emptyList(),
            mediaReferences = listOf(
                CanonicalMediaReference(
                    referencedAsset = "audio/hello.mp3",
                    logicalPath = "audio/hello.mp3",
                    mediaType = CanonicalMediaType.AUDIO,
                    owningContentId = contentId1,
                    status = CanonicalMediaStatus.PRESENT
                )
            )
        )

        val collector = PackageMediaAssetCollector(
            mediaByteReader = { _, assetPath ->
                if (assetPath == "audio/hello.mp3") "fake-mp3-bytes".toByteArray(Charsets.UTF_8) else null
            }
        )

        val bundle = collector.collect(canonicalPackage)

        assertEquals(1, bundle.assetCount)
        val asset = bundle.assets.single()
        assertEquals("audio/hello.mp3", asset.logicalPath)
        assertEquals(CanonicalMediaType.AUDIO, asset.mediaType)
        assertTrue(asset.sha256.isNotBlank())
        assertEquals(1, bundle.manifest.entries.size)
        assertFalse(bundle.hasUnresolvedAssets)
    }

    @Test
    fun `media packaging eliminates duplicate assets deterministically`() {
        val canonicalPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "MediaTopic",
            sourceMetadata = LegacyTopicSourceMetadata("MediaTopic", "json", "pkg"),
            contents = emptyList(),
            learningItems = emptyList(),
            mediaReferences = listOf(
                CanonicalMediaReference("icon.png", "icon.png", CanonicalMediaType.IMAGE, contentId1, status = CanonicalMediaStatus.PRESENT),
                CanonicalMediaReference("icon.png", "icon.png", CanonicalMediaType.IMAGE, contentId2, status = CanonicalMediaStatus.PRESENT)
            )
        )

        val collector = PackageMediaAssetCollector(
            mediaByteReader = { _, _ -> "icon-bytes".toByteArray(Charsets.UTF_8) }
        )

        val bundle = collector.collect(canonicalPackage)

        assertEquals(1, bundle.assetCount)
        assertEquals(1, bundle.manifest.entries.size)
        val entry = bundle.manifest.entries.single()
        assertEquals(listOf(contentId1, contentId2), entry.owningContentIds)
    }

    @Test
    fun `media packaging emits warning diagnostics for missing assets`() {
        val canonicalPackage = CanonicalTopicPackage(
            topicId = topicId,
            logicalTopicName = "MediaTopic",
            sourceMetadata = LegacyTopicSourceMetadata("MediaTopic", "json", "pkg"),
            contents = emptyList(),
            learningItems = emptyList(),
            mediaReferences = listOf(
                CanonicalMediaReference("missing.mp3", "missing.mp3", CanonicalMediaType.AUDIO, contentId1, status = CanonicalMediaStatus.MISSING)
            )
        )

        val collector = PackageMediaAssetCollector()
        val bundle = collector.collect(canonicalPackage)

        assertEquals(0, bundle.assetCount)
        assertTrue(bundle.hasUnresolvedAssets)
        assertEquals(1, bundle.diagnostics.size)
        assertEquals(CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE, bundle.diagnostics.single().code)
    }

    @Test
    fun `checksum stability ensures identical input produces identical SHA256`() {
        val hasher = Sha256PackageIntegrityHasher()
        val data = "identical-media-data-payload".toByteArray(Charsets.UTF_8)

        val hash1 = hasher.hash(data)
        val hash2 = hasher.hash(data)

        assertEquals(hash1, hash2)
    }
}
