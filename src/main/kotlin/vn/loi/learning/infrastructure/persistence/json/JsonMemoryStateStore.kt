package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.infrastructure.persistence.store.MemoryStateStore

class JsonMemoryStateStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : MemoryStateStore {
    private val snapshot = JsonDecodedSnapshot<MemoryStateRecord>(filePath, "MemoryState")

    override fun load(): List<MemoryStateRecord> =
        snapshot.load { JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList(),
            recordType =
                "memory state",
            traceName = "MemoryState"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<MemoryStateRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<MemoryStateRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        } }

    override fun save(
        records: List<MemoryStateRecord>
    ) {
        val content =
            JsonPersistenceCodec.encode(
                records =
                    records
            ) { envelope ->
                json.encodeToString(
                    envelope
                )
            }

        JsonFileWriter.write(
            filePath =
                filePath,
            content =
                content
        )
        snapshot.written(records)
    }

    companion object {

        private fun defaultJson(): Json =
            Json {
                prettyPrint = true
                prettyPrintIndent = "  "
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}
