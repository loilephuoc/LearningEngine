package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.LibraryCollectionStore

class JsonLibraryCollectionStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : LibraryCollectionStore {

    override fun loadAll(): List<LibraryCollectionRecord> =
        JsonFileReader.read(
            filePath = filePath,
            emptyValue = emptyList(),
            recordType = "library collection"
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath = filePath,
                content = content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<LibraryCollectionRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<LibraryCollectionRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(
        records: List<LibraryCollectionRecord>
    ) {
        val content =
            JsonPersistenceCodec.encode(
                records = records
            ) { envelope ->
                json.encodeToString(
                    envelope
                )
            }

        JsonFileWriter.write(
            filePath = filePath,
            content = content
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
