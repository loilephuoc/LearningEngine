package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Result của thao tác OPD3 Export.
 */
data class Opd3ExportResult(
    val zipBytes: ByteArray,
    val sha256Checksum: String,
    val entryHashes: Map<String, String>,
    val topicId: TopicId
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opd3ExportResult) return false
        return sha256Checksum == other.sha256Checksum && topicId == other.topicId
    }

    override fun hashCode(): Int {
        var result = sha256Checksum.hashCode()
        result = 31 * result + topicId.hashCode()
        return result
    }
}

/**
 * Service ứng dụng thực hiện xuất đinh ninh (byte-for-byte deterministic) gói OPD3 archive từ CanonicalTopicPackage.
 */
class Opd3PackageExporter(
    private val zipWriter: DeterministicZipWriter,
    private val packageIdGenerator: PackageIdGenerator = Sha256PackageIdGenerator(),
    private val integrityHasher: PackageIntegrityHasher = Sha256PackageIntegrityHasher(),
    private val mediaCollector: PackageMediaAssetCollector = PackageMediaAssetCollector(integrityHasher),
    private val json: Json = defaultJson()
) {

    fun export(
        canonicalPackage: CanonicalTopicPackage,
        mediaBundle: CanonicalMediaBundle? = null
    ): Opd3ExportResult {
        val bundle = mediaBundle ?: mediaCollector.collect(canonicalPackage)

        // 1. Chuẩn bị metadata.json
        val descriptor = PackageDescriptor(
            name = canonicalPackage.logicalTopicName,
            version = canonicalPackage.sourceMetadata.version,
            format = canonicalPackage.sourceMetadata.format
        )
        val packageId = packageIdGenerator.generate(descriptor)

        val metadataJsonText = json.encodeToString(
            PackageMetadataExportDto(
                schemaVersion = SCHEMA_VERSION,
                packageId = packageId.value,
                name = canonicalPackage.logicalTopicName,
                version = canonicalPackage.sourceMetadata.version,
                format = canonicalPackage.sourceMetadata.format,
                topicId = canonicalPackage.topicId.value,
                tags = canonicalPackage.tags.sorted()
            )
        )

        // 2. Chuẩn bị contents.json (sắp xếp đinh ninh theo ContentId)
        val sortedContents = canonicalPackage.contents.sortedBy { it.id.value }
        val contentsJsonText = PackageExportContentsSerializer(json).serialize(sortedContents)

        // 3. Chuẩn bị learning-items.json (sắp xếp đinh ninh theo LearningItemId)
        val sortedLearningItems = canonicalPackage.learningItems.sortedBy { it.id.value }
        val learningItemsJsonText = PackageExportLearningItemsSerializer(json).serialize(sortedLearningItems)

        // 4. Chuẩn bị media-manifest.json
        val mediaManifestJsonText = json.encodeToString(
            MediaManifestExportDto(
                entries = bundle.manifest.entries.sortedBy { it.logicalPath }.map { entry ->
                    MediaManifestEntryExportDto(
                        logicalPath = entry.logicalPath,
                        mediaType = entry.mediaType.name,
                        size = entry.size,
                        sha256 = entry.sha256,
                        owningContentIds = entry.owningContentIds.map { it.value }.sorted()
                    )
                }
            )
        )

        // Map lưu các entry file và byte
        val entriesToHash = mutableMapOf<String, ByteArray>()
        entriesToHash["metadata.json"] = metadataJsonText.toByteArray(Charsets.UTF_8)
        entriesToHash["contents.json"] = contentsJsonText.toByteArray(Charsets.UTF_8)
        entriesToHash["learning-items.json"] = learningItemsJsonText.toByteArray(Charsets.UTF_8)
        entriesToHash["media-manifest.json"] = mediaManifestJsonText.toByteArray(Charsets.UTF_8)

        // Thêm các file media
        bundle.assets.sortedBy { it.logicalPath }.forEach { asset ->
            if (!Opd3PathValidator.isSafeMediaPath(asset.logicalPath)) {
                throw IllegalArgumentException("Unsafe media asset path: '${asset.logicalPath}'")
            }
            val archiveMediaPath = "media/${asset.logicalPath.removePrefix("media/")}"
            entriesToHash[archiveMediaPath] = asset.bytes
        }

        // Tính toán hash checksums cho từng entry
        val entryHashes = entriesToHash.mapValues { (_, bytes) ->
            integrityHasher.hash(bytes)
        }.toSortedMap()

        // 5. Chuẩn bị manifest.json
        val manifestJsonText = json.encodeToString(
            ManifestExportDto(
                schemaVersion = SCHEMA_VERSION,
                files = entryHashes
            )
        )
        entriesToHash["manifest.json"] = manifestJsonText.toByteArray(Charsets.UTF_8)

        // Tạo danh sách DeterministicZipEntry
        val zipEntries = entriesToHash.map { (path, bytes) ->
            DeterministicZipEntry(relativePath = path, bytes = bytes)
        }

        val zipBytes = zipWriter.writeZip(zipEntries)
        val zipChecksum = integrityHasher.hash(zipBytes)

        val finalHashes = entryHashes.toMutableMap()
        finalHashes["manifest.json"] = integrityHasher.hash(manifestJsonText.toByteArray(Charsets.UTF_8))

        return Opd3ExportResult(
            zipBytes = zipBytes,
            sha256Checksum = zipChecksum,
            entryHashes = finalHashes,
            topicId = canonicalPackage.topicId
        )
    }

    companion object {
        const val SCHEMA_VERSION = "1.0"

        fun defaultJson(): Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

@kotlinx.serialization.Serializable
private data class PackageMetadataExportDto(
    val schemaVersion: String,
    val packageId: String,
    val name: String,
    val version: String,
    val format: String,
    val topicId: String,
    val tags: List<String>
)

@kotlinx.serialization.Serializable
private data class MediaManifestExportDto(
    val entries: List<MediaManifestEntryExportDto>
)

@kotlinx.serialization.Serializable
private data class MediaManifestEntryExportDto(
    val logicalPath: String,
    val mediaType: String,
    val size: Long,
    val sha256: String,
    val owningContentIds: List<String>
)

@kotlinx.serialization.Serializable
private data class ManifestExportDto(
    val schemaVersion: String,
    val files: Map<String, String>
)
