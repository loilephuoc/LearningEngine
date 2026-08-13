package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.persistence.store.ContentStore

class JsonContentStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : ContentStore {
    private val snapshot = JsonDecodedSnapshot<ContentRecord>(filePath, "Content")

    override fun loadAll(): List<ContentRecord> =
        snapshot.load { JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList(),
            recordType =
                "content",
            traceName = "Content"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<ContentRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<ContentRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        } }

    override fun saveAll(
        records: List<ContentRecord>
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
