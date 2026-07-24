package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.KnowledgeGraphRecord
import vn.loi.learning.infrastructure.persistence.store.KnowledgeGraphStore

/**
 * JSON-backed implementation of [KnowledgeGraphStore].
 *
 * Lưu toàn bộ graph (nodes + edges) trong một file JSON duy nhất.
 * File không tồn tại → trả về graph rỗng (missing-file behavior theo convention).
 * Ghi thông qua atomic replacement (JsonFileWriter pattern).
 *
 * Sử dụng envelope schema-versioned (JsonPersistenceCodec).
 */
class JsonKnowledgeGraphStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : KnowledgeGraphStore {

    override fun load(): KnowledgeGraphRecord =
        JsonFileReader.read(
            filePath = filePath,
            emptyValue = KnowledgeGraphRecord(),
            recordType = "knowledge-graph"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { _ ->
                    // KnowledgeGraph has no legacy array format.
                    // This branch cannot be reached from files produced by this store.
                    throw UnsupportedOperationException(
                        "Knowledge graph store has no legacy array format: $filePath"
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<JsonPersistenceEnvelope<KnowledgeGraphRecord>>(
                        envelopeContent
                    )
                }
            )
        }

    override fun save(record: KnowledgeGraphRecord) {
        val content = JsonPersistenceCodec.encode(records = record) { envelope ->
            json.encodeToString(envelope)
        }
        JsonFileWriter.write(filePath = filePath, content = content)
    }

    companion object {
        private fun defaultJson(): Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
