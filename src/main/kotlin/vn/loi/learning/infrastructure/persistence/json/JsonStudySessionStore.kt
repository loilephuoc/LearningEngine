package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord
import vn.loi.learning.infrastructure.persistence.store.StudySessionStore

/**
 * JSON implementation của StudySessionStore.
 *
 * Store chỉ đọc và ghi StudySessionRecord.
 * Domain conversion thuộc trách nhiệm của Repository và Mapper.
 */
class JsonStudySessionStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : StudySessionStore {

    override fun loadAll(): List<StudySessionRecord> =
        JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList()
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<StudySessionRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<StudySessionRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(
        records: List<StudySessionRecord>
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