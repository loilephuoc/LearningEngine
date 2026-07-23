package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord
import vn.loi.learning.infrastructure.persistence.store.InstalledPackageStore

class JsonInstalledPackageStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : InstalledPackageStore {

    override fun loadAll(): List<InstalledPackageRecord> =
        JsonFileReader.read(
            filePath = filePath,
            emptyValue = emptyList(),
            recordType = "installed-package"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<List<InstalledPackageRecord>>(legacyContent)
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<JsonPersistenceEnvelope<List<InstalledPackageRecord>>>(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(records: List<InstalledPackageRecord>) {
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
