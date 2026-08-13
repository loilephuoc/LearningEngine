package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord
import vn.loi.learning.infrastructure.persistence.store.StudyQueueStore

/**
 * JSON implementation của StudyQueueStore.
 *
 * Store chỉ đọc và ghi StudyQueueRecord.
 * Domain conversion thuộc trách nhiệm của Repository và Mapper.
 */
class JsonStudyQueueStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : StudyQueueStore {
    private val snapshot = JsonDecodedSnapshot<StudyQueueRecord>(filePath, "StudyQueue")

    override fun loadAll(): List<StudyQueueRecord> =
        snapshot.load { JsonFileReader.read(
            filePath = filePath,
            emptyValue = emptyList(),
            recordType = "study queue",
            traceName = "StudyQueue"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<StudyQueueRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<StudyQueueRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        } }

    override fun saveAll(
        records: List<StudyQueueRecord>
    ) {
        val content =
            JsonPersistenceCodec.encode(
                records = records
            ) { envelope ->
                json.encodeToString(envelope)
            }

        JsonFileWriter.write(
            filePath = filePath,
            content = content
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
