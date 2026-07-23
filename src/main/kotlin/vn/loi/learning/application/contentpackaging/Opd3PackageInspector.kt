package vn.loi.learning.application.contentpackaging

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Service kiểm tra và soi chiếu thông tin chi tiết (Inspector) của một gói OPD3 archive.
 * Hỗ trợ kiểm tra từ mảng byte ByteArray hoặc đường dẫn Path tệp hệ thống.
 */
class Opd3PackageInspector(
    private val integrityHasher: PackageIntegrityHasher = Sha256PackageIntegrityHasher(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    fun inspect(zipBytes: ByteArray): PackageInspectionResult {
        require(zipBytes.isNotEmpty()) { "Package zip bytes must not be empty." }

        val entries = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val baos = java.io.ByteArrayOutputStream()
                        zip.copyTo(baos)
                        entries[entry.name] = baos.toByteArray()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (exception: Exception) {
            return PackageInspectionResult(
                packageVersion = "unknown",
                schemaVersion = "unknown",
                topicId = null,
                topicName = "unknown",
                contentCount = 0,
                learningItemCount = 0,
                mediaCount = 0,
                assetSizes = emptyMap(),
                checksums = emptyMap(),
                diagnostics = listOf("ERROR: Invalid ZIP archive: ${exception.message}")
            )
        }

        if (entries.isEmpty()) {
            return PackageInspectionResult(
                packageVersion = "unknown",
                schemaVersion = "unknown",
                topicId = null,
                topicName = "unknown",
                contentCount = 0,
                learningItemCount = 0,
                mediaCount = 0,
                assetSizes = emptyMap(),
                checksums = emptyMap(),
                diagnostics = listOf("ERROR: Invalid ZIP archive: no entries found.")
            )
        }

        return inspectEntries(entries)
    }

    fun inspect(packagePath: Path): PackageInspectionResult {
        require(Files.exists(packagePath)) { "Package file does not exist: $packagePath" }
        val zipBytes = Files.readAllBytes(packagePath)
        return inspect(zipBytes)
    }

    private fun inspectEntries(entries: Map<String, ByteArray>): PackageInspectionResult {
        val diagnostics = mutableListOf<String>()

        val metadataBytes = entries["metadata.json"]
        var schemaVersion = "unknown"
        var packageVersion = "unknown"
        var topicId: TopicId? = null
        var topicName = "unknown"

        if (metadataBytes == null) {
            diagnostics += "ERROR: Missing required entry 'metadata.json'."
        } else {
            try {
                val metaObj = json.parseToJsonElement(metadataBytes.toString(Charsets.UTF_8)).jsonObject
                schemaVersion = metaObj["schemaVersion"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                packageVersion = metaObj["version"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                topicName = metaObj["name"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                val rawTopicId = metaObj["topicId"]?.jsonPrimitive?.contentOrNull
                if (rawTopicId != null) {
                    topicId = TopicId(rawTopicId)
                }
            } catch (exception: Exception) {
                diagnostics += "ERROR: Malformed 'metadata.json': ${exception.message}"
            }
        }

        // Parse content count
        var contentCount = 0
        val contentsBytes = entries["contents.json"]
        if (contentsBytes == null) {
            diagnostics += "ERROR: Missing required entry 'contents.json'."
        } else {
            try {
                val element = json.parseToJsonElement(contentsBytes.toString(Charsets.UTF_8))
                contentCount = when {
                    element is kotlinx.serialization.json.JsonArray -> element.size
                    element is kotlinx.serialization.json.JsonObject -> {
                        element["contents"]?.jsonArray?.size
                            ?: element["content"]?.jsonArray?.size
                            ?: 0
                    }
                    else -> 0
                }
            } catch (exception: Exception) {
                diagnostics += "ERROR: Malformed 'contents.json': ${exception.message}"
            }
        }

        // Parse learning item count
        var learningItemCount = 0
        val itemsBytes = entries["learning-items.json"]
        if (itemsBytes == null) {
            diagnostics += "ERROR: Missing required entry 'learning-items.json'."
        } else {
            try {
                val element = json.parseToJsonElement(itemsBytes.toString(Charsets.UTF_8))
                learningItemCount = when {
                    element is kotlinx.serialization.json.JsonArray -> element.size
                    element is kotlinx.serialization.json.JsonObject -> {
                        element["learningItems"]?.jsonArray?.size
                            ?: element["items"]?.jsonArray?.size
                            ?: 0
                    }
                    else -> 0
                }
            } catch (exception: Exception) {
                diagnostics += "ERROR: Malformed 'learning-items.json': ${exception.message}"
            }
        }

        // Parse media entries
        val assetSizes = mutableMapOf<String, Long>()
        var mediaCount = 0
        val mediaManifestBytes = entries["media-manifest.json"]
        if (mediaManifestBytes != null) {
            try {
                val manifestObj = json.parseToJsonElement(mediaManifestBytes.toString(Charsets.UTF_8)).jsonObject
                val entriesArray = manifestObj["entries"]?.jsonArray
                if (entriesArray != null) {
                    mediaCount = entriesArray.size
                    entriesArray.forEach { element ->
                        val entryObj = element.jsonObject
                        val path = entryObj["logicalPath"]?.jsonPrimitive?.contentOrNull
                        val size = entryObj["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                        if (path != null) {
                            assetSizes[path] = size
                        }
                    }
                }
            } catch (exception: Exception) {
                diagnostics += "WARNING: Malformed 'media-manifest.json': ${exception.message}"
            }
        } else {
            // Fallback: scan media/* entries
            val mediaEntries = entries.keys.filter { it.startsWith("media/") }
            mediaCount = mediaEntries.size
            mediaEntries.forEach { path ->
                assetSizes[path] = entries.getValue(path).size.toLong()
            }
        }

        // Compute entry checksums and check manifest.json
        val computedChecksums = entries.mapValues { (_, bytes) ->
            integrityHasher.hash(bytes)
        }.toSortedMap()

        val manifestBytes = entries["manifest.json"]
        if (manifestBytes == null) {
            diagnostics += "ERROR: Missing required entry 'manifest.json'."
        } else {
            try {
                val manifestObj = json.parseToJsonElement(manifestBytes.toString(Charsets.UTF_8)).jsonObject
                val filesMap = manifestObj["files"]?.jsonObject
                filesMap?.forEach { (path, element) ->
                    val declaredHash = element.jsonPrimitive.contentOrNull
                    val actualHash = computedChecksums[path]
                    if (actualHash == null) {
                        diagnostics += "ERROR: Manifest references non-existent file '$path'."
                    } else if (declaredHash != null && actualHash != declaredHash) {
                        diagnostics += "ERROR: Checksum mismatch for file '$path' (declared: $declaredHash, actual: $actualHash)."
                    }
                }
            } catch (exception: Exception) {
                diagnostics += "ERROR: Malformed 'manifest.json': ${exception.message}"
            }
        }

        return PackageInspectionResult(
            packageVersion = packageVersion,
            schemaVersion = schemaVersion,
            topicId = topicId,
            topicName = topicName,
            contentCount = contentCount,
            learningItemCount = learningItemCount,
            mediaCount = mediaCount,
            assetSizes = assetSizes,
            checksums = computedChecksums,
            diagnostics = diagnostics
        )
    }
}
