package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord
import vn.loi.learning.infrastructure.persistence.store.ReviewEventStore

class JsonReviewEventStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : ReviewEventStore {
    private val snapshot = JsonDecodedSnapshot<ReviewEventRecord>(filePath, "ReviewEvent")

    override fun loadAll(): List<ReviewEventRecord> =
        snapshot.load { JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList(),
            recordType =
                "review event",
            traceName = "ReviewEvent"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<ReviewEventRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<ReviewEventRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        } }

    override fun saveAll(
        records: List<ReviewEventRecord>
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
