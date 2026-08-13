package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord
import vn.loi.learning.infrastructure.persistence.store.LearningItemStore

class JsonLearningItemStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : LearningItemStore {
    private val snapshot = JsonDecodedSnapshot<LearningItemRecord>(filePath, "LearningItem")

    override fun loadAll(): List<LearningItemRecord> =
        snapshot.load { JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList(),
            recordType =
                "learning item",
            traceName = "LearningItem"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<LearningItemRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<LearningItemRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        } }

    override fun saveAll(
        records: List<LearningItemRecord>
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
