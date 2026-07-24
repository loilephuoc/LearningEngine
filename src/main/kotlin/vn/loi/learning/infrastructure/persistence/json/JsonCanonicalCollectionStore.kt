package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalCollectionStore

class JsonCanonicalCollectionStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : CanonicalCollectionStore {

    override fun loadAll(): List<CanonicalCollectionRecord> =
        JsonFileReader.read(
            filePath = filePath,
            emptyValue = emptyList(),
            recordType = "canonical-collection"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<List<CanonicalCollectionRecord>>(legacyContent)
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<JsonPersistenceEnvelope<List<CanonicalCollectionRecord>>>(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(records: List<CanonicalCollectionRecord>) {
        val content = JsonPersistenceCodec.encode(records = records) { envelope ->
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
