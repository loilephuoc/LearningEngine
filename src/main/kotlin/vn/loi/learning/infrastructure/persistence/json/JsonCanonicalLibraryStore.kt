package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalLibraryStore

class JsonCanonicalLibraryStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : CanonicalLibraryStore {

    override fun loadAll(): List<CanonicalLibraryRecord> =
        JsonFileReader.read(
            filePath = filePath,
            emptyValue = emptyList(),
            recordType = "canonical-library"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<List<CanonicalLibraryRecord>>(legacyContent)
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<JsonPersistenceEnvelope<List<CanonicalLibraryRecord>>>(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(records: List<CanonicalLibraryRecord>) {
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
