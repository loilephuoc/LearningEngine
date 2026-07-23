package vn.loi.learning.application.contentpackaging

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Service kiểm tra và soi chiếu thông tin chi tiết (Inspector) của một gói OPD3 archive.
 *
 * Thực hiện kiểm tra streaming đọc từng phần (incremental streaming reading) để bảo vệ tài nguyên hệ thống.
 */
class Opd3PackageInspector(
    private val integrityHasher: PackageIntegrityHasher = Sha256PackageIntegrityHasher(),
    private val limits: PackageSafetyLimits = PackageSafetyLimits.DEFAULT,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    fun inspect(zipBytes: ByteArray): PackageInspectionResult {
        require(zipBytes.isNotEmpty()) { "Package zip bytes must not be empty." }

        val entries = mutableMapOf<String, ByteArray>()
        val zipDiagnostics = mutableListOf<String>()
        var totalUncompressedSize = 0L

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                var entry = zip.nextEntry
                var entryCount = 0
                val buffer = ByteArray(8192)

                while (entry != null) {
                    entryCount++
                    if (entryCount > limits.maxEntryCount) {
                        return createErrorResult("ERROR: Package exceeds maximum entry count limit (${limits.maxEntryCount}).")
                    }

                    val name = entry.name
                    if (!entry.isDirectory) {
                        // Canonical path validation
                        when (val pathResult = Opd3PathValidator.validateArchivePath(name)) {
                            is PathValidationResult.Invalid -> {
                                zipDiagnostics += "ERROR: Unsafe archive entry path '$name': ${pathResult.reason}"
                            }
                            is PathValidationResult.Valid -> {}
                        }

                        // Duplicate entry check
                        if (entries.containsKey(name)) {
                            zipDiagnostics += "ERROR: Duplicate ZIP entry name detected: '$name'."
                        } else {
                            // Streaming incremental reading with running byte counters
                            val baos = ByteArrayOutputStream()
                            var entrySize = 0L
                            var bytesRead: Int

                            while (zip.read(buffer).also { bytesRead = it } != -1) {
                                entrySize += bytesRead
                                totalUncompressedSize += bytesRead

                                if (entrySize > limits.maxSingleEntrySizeBytes) {
                                    return createErrorResult("ERROR: Zip entry '$name' exceeds single entry size limit (${limits.maxSingleEntrySizeBytes} bytes).")
                                }
                                if (totalUncompressedSize > limits.maxTotalUncompressedSizeBytes) {
                                    return createErrorResult("ERROR: Package exceeds total uncompressed size limit (${limits.maxTotalUncompressedSizeBytes} bytes).")
                                }
                                baos.write(buffer, 0, bytesRead)
                            }

                            entries[name] = baos.toByteArray()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (exception: Exception) {
            return createErrorResult("ERROR: Invalid ZIP archive: ${exception.message}")
        }

        if (entries.isEmpty()) {
            return createErrorResult(zipDiagnostics.firstOrNull() ?: "ERROR: Invalid ZIP archive: no entries found.")
        }

        val result = inspectEntries(entries)
        return if (zipDiagnostics.isEmpty()) {
            result
        } else {
            result.copy(diagnostics = (zipDiagnostics + result.diagnostics).distinct())
        }
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
        var format = "unknown"

        if (metadataBytes == null) {
            diagnostics += "ERROR: Missing required entry 'metadata.json'."
        } else {
            try {
                val metaObj = json.parseToJsonElement(metadataBytes.toString(Charsets.UTF_8)).jsonObject
                schemaVersion = metaObj["schemaVersion"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                packageVersion = metaObj["version"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                topicName = metaObj["name"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                format = metaObj["format"]?.jsonPrimitive?.contentOrNull ?: "unknown"

                val rawTopicId = metaObj["topicId"]?.jsonPrimitive?.contentOrNull
                if (rawTopicId.isNullOrBlank()) {
                    diagnostics += "ERROR: Missing or invalid mandatory TopicId in metadata.json."
                } else {
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

        // STRICT mode: media-manifest.json is required
        val assetSizes = mutableMapOf<String, Long>()
        var mediaCount = 0
        val mediaManifestBytes = entries["media-manifest.json"]
        if (mediaManifestBytes == null) {
            diagnostics += "ERROR: Missing required entry 'media-manifest.json'."
        } else {
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
                diagnostics += "ERROR: Malformed 'media-manifest.json': ${exception.message}"
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
            val manifestText = manifestBytes.toString(Charsets.UTF_8)
            // Option A: Reject duplicate keys in manifest.json files map
            if (hasDuplicateManifestKeys(manifestText)) {
                diagnostics += "ERROR: Duplicate key detected in manifest.json files map."
            }

            try {
                val manifestObj = json.parseToJsonElement(manifestText).jsonObject
                val filesMap = manifestObj["files"]?.jsonObject
                if (filesMap == null) {
                    diagnostics += "ERROR: Malformed 'manifest.json': missing 'files' field."
                } else {
                    val declaredFiles = mutableSetOf<String>()
                    filesMap.forEach { (path, element) ->
                        declaredFiles.add(path)
                        val declaredHash = element.jsonPrimitive.contentOrNull
                        val actualHash = computedChecksums[path]

                        if (declaredHash == null || declaredHash.length != PackageSafetyLimits.SHA256_HEX_LENGTH || !declaredHash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                            diagnostics += "ERROR: Malformed SHA-256 checksum format for file '$path': '$declaredHash'."
                        } else if (actualHash == null) {
                            diagnostics += "ERROR: Manifest references non-existent file '$path'."
                        } else if (declaredHash.lowercase() != actualHash.lowercase()) {
                            diagnostics += "ERROR: Checksum mismatch for file '$path' (declared: $declaredHash, actual: $actualHash)."
                        }
                    }

                    // Check for unlisted extra files in archive
                    computedChecksums.keys.forEach { actualPath ->
                        if (actualPath != "manifest.json" && actualPath !in declaredFiles) {
                            diagnostics += "ERROR: Unlisted extra archive entry detected: '$actualPath'."
                        }
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
            format = format,
            contentCount = contentCount,
            learningItemCount = learningItemCount,
            mediaCount = mediaCount,
            assetSizes = assetSizes,
            checksums = computedChecksums,
            diagnostics = diagnostics
        )
    }

    private fun hasDuplicateManifestKeys(manifestText: String): Boolean {
        val filesIdx = manifestText.indexOf("\"files\"")
        if (filesIdx == -1) return false
        val openBrace = manifestText.indexOf('{', filesIdx)
        if (openBrace == -1) return false

        var depth = 0
        var closeBrace = -1
        var inString = false
        var escape = false

        for (i in openBrace until manifestText.length) {
            val c = manifestText[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        closeBrace = i
                        break
                    }
                }
            }
        }

        if (closeBrace == -1) return false

        val filesContent = manifestText.substring(openBrace + 1, closeBrace)
        val keyMatches = Regex("\"([^\"]+)\"\\s*:").findAll(filesContent).map { it.groupValues[1] }.toList()
        return keyMatches.size != keyMatches.toSet().size
    }

    private fun createErrorResult(message: String): PackageInspectionResult =
        PackageInspectionResult(
            packageVersion = "unknown",
            schemaVersion = "unknown",
            topicId = null,
            topicName = "unknown",
            format = "unknown",
            contentCount = 0,
            learningItemCount = 0,
            mediaCount = 0,
            assetSizes = emptyMap(),
            checksums = emptyMap(),
            diagnostics = listOf(message)
        )
}
